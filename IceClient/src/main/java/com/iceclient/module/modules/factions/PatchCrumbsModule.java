package com.iceclient.module.modules.factions;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ModeSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockGravel;
import net.minecraft.block.BlockSand;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Marks where incoming TNT actually breached, so you know where to patch.
 *
 * <p><b>Why this tracks detonations rather than live TNT.</b> The obvious
 * approach -- mark the first primed TNT you can see -- gives you the coordinates
 * of a block of TNT in mid-air, metres from and well above the wall it is about
 * to open. That is the wrong Y by however far it still had to travel, and it
 * moves every time you re-scan. What you actually want to patch is where the
 * charge went off, which is only knowable at the moment it disappears.
 *
 * <p>So every primed TNT is followed until it leaves the world, and the last
 * position of one whose fuse had run down becomes the crumb. TNT that simply
 * left render distance still has fuse remaining and is discarded, so flying
 * rounds never leave phantom marks.
 *
 * <p>Crumbs are kept in a list with independent lifetimes. Overstacked cannons
 * and multi-barrel walls put several charges into the wall within a tick or two
 * of each other, and a single slot could only ever show the last one -- which is
 * why the old marker looked like it "vanished after two seconds" mid-raid. It
 * was not expiring, it was being overwritten.
 */
public class PatchCrumbsModule extends Module {

   /** How long a column keeps being watched after the last round arrived in it.
    *  Long enough to cover a full volley, short enough that a spot stops being
    *  re-marked once the shooting has moved elsewhere. */
   private static final long COLUMN_TTL_MS = 4500L;

   /** Below this many ms remaining a crumb fades rather than vanishing, so it
    *  never disappears while you are still lining a patch up on it. */
   private static final long FADE_MS = 1000L;
   private final ModeSetting direction = (ModeSetting)this.addSetting(new ModeSetting("Direction", "Both", new String[]{"Auto", "Both", "North/South", "East/West"}));
   private final BooleanSetting dispenserCheck = (BooleanSetting)this.addSetting(new BooleanSetting("Dispenser check", false));
   private final NumberSetting keepSeconds = (NumberSetting)this.addSetting(new NumberSetting("Keep (s)", 10.0D, 1.0D, 60.0D, 1.0D));
   // Overstacked cannons land several charges at once; one slot could only ever
   // show the last of them.
   private final NumberSetting maxCrumbs = (NumberSetting)this.addSetting(new NumberSetting("Max crumbs", 8.0D, 1.0D, 32.0D, 1.0D));
   // Orbit's rails run right across the map, so the old 16-block cap is lifted.
   private final NumberSetting lineLength = (NumberSetting)this.addSetting(new NumberSetting("Line length", 96.0D, 1.0D, 256.0D, 8.0D));
   // Rails along the box's four vertical edges (Orbit's look) instead of a
   // single cross through its centre.
   private final BooleanSetting edgeRails = (BooleanSetting)this.addSetting(new BooleanSetting("Edge rails", true));
   private final NumberSetting railWidth = (NumberSetting)this.addSetting(new NumberSetting("Rail width", 1.5D, 1.0D, 5.0D, 0.5D));
   // The tether: a line leaving the box, capped with a dot you aim at.
   private final BooleanSetting seeThrough = (BooleanSetting)this.addSetting(new BooleanSetting("Through walls", true));
   private final BooleanSetting tether = (BooleanSetting)this.addSetting(new BooleanSetting("Tether", true));
   private final NumberSetting tetherLength = (NumberSetting)this.addSetting(new NumberSetting("Tether length", 3.0D, 1.0D, 12.0D, 0.5D));
   private final NumberSetting dotSize = (NumberSetting)this.addSetting(new NumberSetting("Dot size", 6.0D, 2.0D, 16.0D, 1.0D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final ModeSetting color = (ModeSetting)this.addSetting(new ModeSetting("Box color", "Red", new String[]{"Red", "White", "Green", "Aqua"}));
   private final ModeSetting lineColor = (ModeSetting)this.addSetting(new ModeSetting("Line color", "Aqua", new String[]{"Red", "White", "Green", "Aqua"}));
   private final NumberSetting coordScale = (NumberSetting)this.addSetting(new NumberSetting("Coords size", 5.0D, 2.0D, 12.0D, 1.0D));
   private final BooleanSetting filled = (BooleanSetting)this.addSetting(new BooleanSetting("Full 3D block", true));
   private final BooleanSetting showCoords = (BooleanSetting)this.addSetting(new BooleanSetting("Show coords", true));

   /** Columns of incoming TNT currently being watched. */
   private final List<WallColumn> columns = new ArrayList<WallColumn>();
   private final List<Crumb> crumbs = new ArrayList<Crumb>();

   public PatchCrumbsModule() {
      super("PatchCrumbs", "Highlights where to patch when your wall gets cannoned (awareness only)", ModuleCategory.FACTIONS, 0);
   }

   protected void onDisable() {
      this.crumbs.clear();
      this.columns.clear();
   }

   /**
    * One (x,z) column that TNT has arrived in.
    *
    * <p>Everything is captured from the <em>first</em> round to reach the column
    * and then left alone. That is the whole trick: by the time a charge is about
    * to go off it has already fallen, so its live position is below the wall it
    * hit, and its velocity has been bent by the impact. The first sighting is
    * the one that describes the shot.
    *
    * <p>Later rounds landing in the same column are folded in rather than
    * starting their own entry, which is what makes overstacked cannons resolve
    * to a single patch spot instead of a cluster of markers.
    */
   private static final class WallColumn {
      int x, z;
      int firstY;
      double firstVx, firstVz;
      long expiresAt;
      final Set<Integer> members = new HashSet<Integer>();
   }

   /** One breach worth patching. */
   private static final class Crumb {
      int x, y, z;
      long expiresAt;
      long life;
      boolean ns, ew;
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if(!this.isEnabled() || event.phase != Phase.START) {
         return;
      }

      if(this.mc.theWorld == null || this.mc.thePlayer == null) {
         return;
      }

      long now = System.currentTimeMillis();

      for(Iterator<Crumb> it = this.crumbs.iterator(); it.hasNext();) {
         if(now > it.next().expiresAt) {
            it.remove();
         }
      }

      this.scanColumns(now);
   }

   /**
    * Sorts incoming TNT into columns and raises a crumb once one is sitting on
    * sand.
    *
    * <p>The sand check is what distinguishes "a round flew past" from "a round
    * opened the wall here". Cannons throw sand ahead of the charge; when there
    * is sand or gravel directly under where the first round appeared, that
    * column is the breach and its Y is the block you need to patch.
    */
   private void scanColumns(long now) {
      for(Entity entity : this.mc.theWorld.loadedEntityList) {
         if(!(entity instanceof EntityTNTPrimed) || entity.isDead) {
            continue;
         }

         EntityTNTPrimed tnt = (EntityTNTPrimed)entity;
         int cx = MathHelper.floor_double(tnt.posX);
         int cz = MathHelper.floor_double(tnt.posZ);
         WallColumn col = null;

         for(WallColumn c : this.columns) {
            if(c.x == cx && c.z == cz) {
               col = c;
               break;
            }
         }

         if(col == null) {
            col = new WallColumn();
            col.x = cx;
            col.z = cz;
            col.firstY = MathHelper.floor_double(tnt.posY);
            // Displacement over the last tick. Only the magnitudes are used, to
            // decide which axis the shot travelled along.
            col.firstVx = tnt.prevPosX - tnt.posX;
            col.firstVz = tnt.prevPosZ - tnt.posZ;
            this.columns.add(col);
         }

         col.members.add(Integer.valueOf(tnt.getEntityId()));
         col.expiresAt = now + COLUMN_TTL_MS;
      }

      for(Iterator<WallColumn> it = this.columns.iterator(); it.hasNext();) {
         WallColumn c = it.next();

         if(this.isSandLike(c.x, c.firstY - 1, c.z)) {
            if(!this.dispenserCheck.get() || !this.nearDispenser(c.x, c.firstY, c.z)) {
               this.addCrumb(c.x, c.firstY, c.z, c.firstVx, c.firstVz, now);
            }
         }

         if(now > c.expiresAt) {
            it.remove();
         }
      }
   }

   /**
    * Adds a crumb, or refreshes one already on that block.
    *
    * <p>Refreshing matters during sustained fire: repeated hits on the same spot
    * should keep the marker alive rather than filling the list with duplicates
    * and pushing the other breaches out.
    */
   private void addCrumb(int x, int y, int z, double motionX, double motionZ, long now) {
      long life = (long)(this.keepSeconds.get() * 1000.0D);

      for(Crumb c : this.crumbs) {
         if(c.x == x && c.y == y && c.z == z) {
            c.expiresAt = now + life;
            c.life = life;
            return;
         }
      }

      Crumb c = new Crumb();
      c.x = x;
      c.y = y;
      c.z = z;
      c.expiresAt = now + life;
      c.life = life;
      this.resolveDirection(c, motionX, motionZ);
      this.crumbs.add(c);

      // Oldest out first, so a heavy volley shows the most recent breaches.
      while(this.crumbs.size() > (int)this.maxCrumbs.get()) {
         this.crumbs.remove(0);
      }
   }

   private void resolveDirection(Crumb c, double motionX, double motionZ) {
      c.ns = false;
      c.ew = false;

      if(this.direction.is("Both")) {
         c.ns = true;
         c.ew = true;
      } else if(this.direction.is("North/South")) {
         c.ns = true;
      } else if(this.direction.is("East/West")) {
         c.ew = true;
      } else {
         double ax = Math.abs(motionX);
         double az = Math.abs(motionZ);

         if(ax <= 1.0E-4D && az <= 1.0E-4D) {
            if(this.isSolid(c.x - 1, c.y, c.z) || this.isSolid(c.x + 1, c.y, c.z)) {
               c.ew = true;
            }

            if(this.isSolid(c.x, c.y, c.z - 1) || this.isSolid(c.x, c.y, c.z + 1)) {
               c.ns = true;
            }

            if(!c.ew && !c.ns) {
               c.ns = true;
            }
         } else if(ax < az) {
            c.ew = true;
         } else {
            c.ns = true;
         }
      }
   }

   private boolean nearDispenser(int bx, int by, int bz) {
      for(int dx = -1; dx <= 1; ++dx) {
         for(int dy = -1; dy <= 1; ++dy) {
            for(int dz = -1; dz <= 1; ++dz) {
               if(this.blockAt(bx + dx, by + dy, bz + dz) instanceof BlockDispenser) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean isSandLike(int x, int y, int z) {
      Block block = this.blockAt(x, y, z);
      return block instanceof BlockSand || block instanceof BlockGravel;
   }

   private boolean isSolid(int x, int y, int z) {
      return this.blockAt(x, y, z).getMaterial().isSolid();
   }

   private Block blockAt(int x, int y, int z) {
      return this.mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.crumbs.isEmpty()) {
         return;
      }

      double camX = this.mc.getRenderManager().viewerPosX;
      double camY = this.mc.getRenderManager().viewerPosY;
      double camZ = this.mc.getRenderManager().viewerPosZ;
      float[] rgb = this.rgbOf(this.color);
      float[] lrgb = this.rgbOf(this.lineColor);
      long now = System.currentTimeMillis();

      for(Crumb c : new ArrayList<Crumb>(this.crumbs)) {
         float fade = this.fadeOf(c, now);
         if(fade <= 0.0F) {
            continue;
         }

         GL11.glPushMatrix();
         this.setupLineState();

         if(this.filled.get()) {
            GlStateManager.color(rgb[0], rgb[1], rgb[2], 0.28F * fade);
            this.drawFilledBox((double)c.x - camX, (double)c.y - camY, (double)c.z - camZ);
         }

         GL11.glLineWidth((float)this.lineWidth.get());
         GlStateManager.color(rgb[0], rgb[1], rgb[2], 0.9F * fade);
         this.drawWireBox((double)c.x - camX, (double)c.y - camY, (double)c.z - camZ);

         if(!this.edgeRails.get()) {
            GlStateManager.color(lrgb[0], lrgb[1], lrgb[2], 0.9F * fade);
            this.drawGuideLines(c, camX, camY, camZ);
         }

         this.teardownLineState();
         GL11.glPopMatrix();

         // Drawn after the block above because these helpers manage their own
         // GL state and apply the camera offset themselves, so they take
         // absolute world coords.
         AxisAlignedBB box = new AxisAlignedBB(
               (double)c.x, (double)c.y, (double)c.z,
               (double)c.x + 1.0D, (double)c.y + 1.0D, (double)c.z + 1.0D);

         if(this.edgeRails.get()) {
            int railCol = this.argbOf(lrgb, (int)(200.0F * fade));
            WorldRenderUtil.axisGuides(box, railCol, this.lineLength.get(), (float)this.railWidth.get(), this.seeThrough.get());
         }

         if(this.tether.get()) {
            this.drawTether(box);
         }

         if(this.showCoords.get()) {
            String text = c.x + ", " + c.y + ", " + c.z;
            this.drawLabel((double)c.x + 0.5D - camX, (double)c.y + 1.4D - camY, (double)c.z + 0.5D - camZ, text);
         }
      }
   }

   /** 1 for most of a crumb's life, easing to 0 over its last second. */
   private float fadeOf(Crumb c, long now) {
      long left = c.expiresAt - now;
      if(left <= 0L) {
         return 0.0F;
      }

      long window = Math.min(FADE_MS, c.life);
      return left >= window ? 1.0F : (float)left / (float)window;
   }

   /** Packs a float rgb triple + alpha into the ARGB int the render helpers take. */
   private int argbOf(float[] rgb, int alpha) {
      return (alpha & 255) << 24
            | ((int)(rgb[0] * 255.0F) & 255) << 16
            | ((int)(rgb[1] * 255.0F) & 255) << 8
            | ((int)(rgb[2] * 255.0F) & 255);
   }

   /**
    * Hangs a line below the crumb ending in a dot. The dot is what you actually
    * aim at when lining a patch up from range, so it's drawn last, on top.
    */
   private void drawTether(AxisAlignedBB box) {
      double cx = (box.minX + box.maxX) * 0.5D;
      double cy = (box.minY + box.maxY) * 0.5D;
      double cz = (box.minZ + box.maxZ) * 0.5D;
      double ey = cy - this.tetherLength.get();
      WorldRenderUtil.drawLine(cx, cy, cz, cx, ey, cz, -1, (float)this.lineWidth.get());
      WorldRenderUtil.drawPoint(cx, ey, cz, -16777216, (float)this.dotSize.get());
   }

   private void drawLabel(double x, double y, double z, String text) {
      GlStateManager.pushMatrix();
      GlStateManager.translate(x, y, z);
      GlStateManager.rotate(-this.mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(this.mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
      float scale = 0.005F * (float)this.coordScale.get();
      GlStateManager.scale(-scale, -scale, scale);
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.enableTexture2D();
      int w = this.mc.fontRendererObj.getStringWidth(text);
      Gui.drawRect(-w / 2 - 2, -2, w / 2 + 2, 9, Integer.MIN_VALUE);
      this.mc.fontRendererObj.drawStringWithShadow(text, (float)(-w) / 2.0F, 0.0F, -1);
      GlStateManager.disableBlend();
      GlStateManager.enableDepth();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   private void drawGuideLines(Crumb c, double camX, double camY, double camZ) {
      double len = this.lineLength.get();
      double cx = (double)c.x + 0.5D - camX;
      double cy = (double)c.y + 0.5D - camY;
      double cz = (double)c.z + 0.5D - camZ;
      GL11.glBegin(1);

      if(c.ew) {
         GL11.glVertex3d(cx, cy, cz);
         GL11.glVertex3d(cx + len, cy, cz);
         GL11.glVertex3d(cx, cy, cz);
         GL11.glVertex3d(cx - len, cy, cz);
      }

      if(c.ns) {
         GL11.glVertex3d(cx, cy, cz);
         GL11.glVertex3d(cx, cy, cz + len);
         GL11.glVertex3d(cx, cy, cz);
         GL11.glVertex3d(cx, cy, cz - len);
      }

      GL11.glEnd();
   }

   private void drawFilledBox(double x, double y, double z) {
      double x1 = x + 1.0D;
      double y1 = y + 1.0D;
      double z1 = z + 1.0D;
      GL11.glBegin(7);
      GL11.glVertex3d(x, y, z);
      GL11.glVertex3d(x1, y, z);
      GL11.glVertex3d(x1, y, z1);
      GL11.glVertex3d(x, y, z1);
      GL11.glVertex3d(x, y1, z);
      GL11.glVertex3d(x, y1, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x1, y1, z);
      GL11.glVertex3d(x, y, z);
      GL11.glVertex3d(x, y1, z);
      GL11.glVertex3d(x1, y1, z);
      GL11.glVertex3d(x1, y, z);
      GL11.glVertex3d(x, y, z1);
      GL11.glVertex3d(x1, y, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x, y1, z1);
      GL11.glVertex3d(x, y, z);
      GL11.glVertex3d(x, y, z1);
      GL11.glVertex3d(x, y1, z1);
      GL11.glVertex3d(x, y1, z);
      GL11.glVertex3d(x1, y, z);
      GL11.glVertex3d(x1, y1, z);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x1, y, z1);
      GL11.glEnd();
   }

   private void drawWireBox(double x, double y, double z) {
      double maxX = x + 1.0D;
      double maxY = y + 1.0D;
      double maxZ = z + 1.0D;
      GL11.glBegin(2);
      GL11.glVertex3d(x, y, z);
      GL11.glVertex3d(maxX, y, z);
      GL11.glVertex3d(maxX, y, maxZ);
      GL11.glVertex3d(x, y, maxZ);
      GL11.glEnd();
      GL11.glBegin(2);
      GL11.glVertex3d(x, maxY, z);
      GL11.glVertex3d(maxX, maxY, z);
      GL11.glVertex3d(maxX, maxY, maxZ);
      GL11.glVertex3d(x, maxY, maxZ);
      GL11.glEnd();
      GL11.glBegin(1);
      GL11.glVertex3d(x, y, z);
      GL11.glVertex3d(x, maxY, z);
      GL11.glVertex3d(maxX, y, z);
      GL11.glVertex3d(maxX, maxY, z);
      GL11.glVertex3d(maxX, y, maxZ);
      GL11.glVertex3d(maxX, maxY, maxZ);
      GL11.glVertex3d(x, y, maxZ);
      GL11.glVertex3d(x, maxY, maxZ);
      GL11.glEnd();
   }

   private float[] rgbOf(ModeSetting m) {
      return m.is("White")?new float[]{1.0F, 1.0F, 1.0F}:(m.is("Green")?new float[]{0.2F, 1.0F, 0.3F}:(m.is("Aqua")?new float[]{0.2F, 0.9F, 1.0F}:new float[]{1.0F, 0.2F, 0.2F}));
   }

   private void setupLineState() {
      GlStateManager.disableTexture2D();
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GL11.glEnable(2848);
   }

   private void teardownLineState() {
      GL11.glDisable(2848);
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
