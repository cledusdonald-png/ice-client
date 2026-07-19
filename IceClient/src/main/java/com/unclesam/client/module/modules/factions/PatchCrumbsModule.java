package com.unclesam.client.module.modules.factions;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ModeSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.WorldRenderUtil;
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

public class PatchCrumbsModule extends Module {
   private static final double STILL_EPSILON = 1.0E-4D;
   private final ModeSetting detect = (ModeSetting)this.addSetting(new ModeSetting("Detect", "Velocity", new String[]{"Velocity", "Settled"}));
   private final ModeSetting direction = (ModeSetting)this.addSetting(new ModeSetting("Direction", "Both", new String[]{"Auto", "Both", "North/South", "East/West"}));
   private final BooleanSetting dispenserCheck = (BooleanSetting)this.addSetting(new BooleanSetting("Dispenser check", false));
   private final NumberSetting keepSeconds = (NumberSetting)this.addSetting(new NumberSetting("Keep (s)", 6.0D, 1.0D, 60.0D, 1.0D));
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
   private static final long VELOCITY_THROTTLE_MS = 1000L;
   private boolean active = false;
   private int crumbX;
   private int crumbY;
   private int crumbZ;
   private long expiresAt = 0L;
   private boolean drawNS = false;
   private boolean drawEW = false;
   private long nextScanAt = 0L;

   public PatchCrumbsModule() {
      super("PatchCrumbs", "Highlights where to patch when your wall gets cannoned (awareness only)", ModuleCategory.FACTIONS, 0);
   }

   protected void onDisable() {
      this.active = false;
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.START) {
         if(this.mc.theWorld != null && this.mc.thePlayer != null) {
            long now = System.currentTimeMillis();
            if(this.active && now > this.expiresAt) {
               this.active = false;
            }

            boolean velocityMode = this.detect.is("Velocity");
            if(!velocityMode || now >= this.nextScanAt) {
               for(Entity entity : this.mc.theWorld.loadedEntityList) {
                  if(entity instanceof EntityTNTPrimed) {
                     EntityTNTPrimed tnt = (EntityTNTPrimed)entity;
                     if(!this.dispenserCheck.get() || !this.nearDispenser(tnt)) {
                        int x = MathHelper.floor_double(tnt.posX);
                        int y = MathHelper.floor_double(tnt.posY);
                        int z = MathHelper.floor_double(tnt.posZ);
                        if((!this.active || x != this.crumbX || z != this.crumbZ) && (!this.detect.is("Settled") || Math.abs(tnt.motionX) <= 1.0E-4D && Math.abs(tnt.motionZ) <= 1.0E-4D && this.isSandLike(x, y - 1, z))) {
                           this.setCrumb(x, y, z, tnt.motionX, tnt.motionZ, now);
                           if(velocityMode) {
                              this.nextScanAt = now + 1000L;
                           }

                           return;
                        }
                     }
                  }
               }

            }
         }
      }
   }

   private void setCrumb(int x, int y, int z, double motionX, double motionZ, long now) {
      this.crumbX = x;
      this.crumbY = y;
      this.crumbZ = z;
      this.expiresAt = now + (long)(this.keepSeconds.get() * 1000.0D);
      this.resolveDirection(motionX, motionZ);
      this.active = true;
   }

   private void resolveDirection(double motionX, double motionZ) {
      this.drawNS = false;
      this.drawEW = false;
      if(this.direction.is("Both")) {
         this.drawNS = true;
         this.drawEW = true;
      } else if(this.direction.is("North/South")) {
         this.drawNS = true;
      } else if(this.direction.is("East/West")) {
         this.drawEW = true;
      } else {
         double ax = Math.abs(motionX);
         double az = Math.abs(motionZ);
         if(ax <= 1.0E-4D && az <= 1.0E-4D) {
            if(this.isSolid(this.crumbX - 1, this.crumbY, this.crumbZ) || this.isSolid(this.crumbX + 1, this.crumbY, this.crumbZ)) {
               this.drawEW = true;
            }

            if(this.isSolid(this.crumbX, this.crumbY, this.crumbZ - 1) || this.isSolid(this.crumbX, this.crumbY, this.crumbZ + 1)) {
               this.drawNS = true;
            }

            if(!this.drawEW && !this.drawNS) {
               this.drawNS = true;
            }
         } else if(ax < az) {
            this.drawEW = true;
         } else {
            this.drawNS = true;
         }
      }

   }

   private boolean nearDispenser(EntityTNTPrimed tnt) {
      int bx = MathHelper.floor_double(tnt.posX);
      int by = MathHelper.floor_double(tnt.posY);
      int bz = MathHelper.floor_double(tnt.posZ);

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
      if(this.isEnabled() && this.active) {
         if(System.currentTimeMillis() > this.expiresAt) {
            this.active = false;
         } else {
            double camX = this.mc.getRenderManager().viewerPosX;
            double camY = this.mc.getRenderManager().viewerPosY;
            double camZ = this.mc.getRenderManager().viewerPosZ;
            float[] rgb = this.rgbOf(this.color);
            float[] lrgb = this.rgbOf(this.lineColor);
            GL11.glPushMatrix();
            this.setupLineState();
            if(this.filled.get()) {
               GlStateManager.color(rgb[0], rgb[1], rgb[2], 0.28F);
               this.drawFilledBox((double)this.crumbX - camX, (double)this.crumbY - camY, (double)this.crumbZ - camZ);
            }

            GL11.glLineWidth((float)this.lineWidth.get());
            GlStateManager.color(rgb[0], rgb[1], rgb[2], 0.9F);
            this.drawWireBox((double)this.crumbX - camX, (double)this.crumbY - camY, (double)this.crumbZ - camZ);
            if(!this.edgeRails.get()) {
               GlStateManager.color(lrgb[0], lrgb[1], lrgb[2], 0.9F);
               this.drawGuideLines(camX, camY, camZ);
            }

            this.teardownLineState();
            GL11.glPopMatrix();

            // Drawn after the block above because these helpers manage their own
            // GL state and apply the camera offset themselves, so they take
            // absolute world coords.
            AxisAlignedBB box = new AxisAlignedBB(
                  (double)this.crumbX, (double)this.crumbY, (double)this.crumbZ,
                  (double)this.crumbX + 1.0D, (double)this.crumbY + 1.0D, (double)this.crumbZ + 1.0D);

            if(this.edgeRails.get()) {
               int railCol = this.argbOf(lrgb, 200);
               WorldRenderUtil.axisGuides(box, railCol, this.lineLength.get(), (float)this.railWidth.get(), this.seeThrough.get());
            }

            if(this.tether.get()) {
               this.drawTether(box);
            }
            if(this.showCoords.get()) {
               String text = this.crumbX + ", " + this.crumbY + ", " + this.crumbZ;
               this.drawLabel((double)this.crumbX + 0.5D - camX, (double)this.crumbY + 1.4D - camY, (double)this.crumbZ + 0.5D - camZ, text);
            }

         }
      }
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

   private void drawGuideLines(double camX, double camY, double camZ) {
      double len = this.lineLength.get();
      double cx = (double)this.crumbX + 0.5D - camX;
      double cy = (double)this.crumbY + 0.5D - camY;
      double cz = (double)this.crumbZ + 0.5D - camZ;
      GL11.glBegin(1);
      if(this.drawEW) {
         GL11.glVertex3d(cx, cy, cz);
         GL11.glVertex3d(cx + len, cy, cz);
         GL11.glVertex3d(cx, cy, cz);
         GL11.glVertex3d(cx - len, cy, cz);
      }

      if(this.drawNS) {
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
