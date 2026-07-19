package com.iceclient.module.modules.render;

import com.iceclient.gui.WorldMapScreen;
import com.iceclient.minimap.MapChunk;
import com.iceclient.minimap.MinimapCache;
import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.ModeSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.BindUtil;
import com.iceclient.util.ColorUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

/**
 * Top-down minimap with zoom, rotation, cave mode, an entity radar and a
 * full-screen world map.
 *
 * <p>Rendering goes through a single {@link DynamicTexture} rather than a rect
 * per block. At 1x zoom the map covers roughly 65,000 blocks -- as immediate-mode
 * quads that is tens of thousands of draw calls a frame. Uploading the pixels
 * once per tick and drawing one textured quad keeps it to a single call
 * regardless of zoom.
 *
 * <p>The circular shape is cut in the pixel buffer by writing zero alpha outside
 * the radius, which avoids needing a stencil buffer that 1.8.9's framebuffer
 * setup does not reliably provide.
 */
public class Minimap extends HudModule {

   /** Texture is square and fixed; zoom changes how much world maps onto it. */
   public static final int TEX = 256;

   private final BooleanSetting showInHud = this.addBool("Show in HUD", true);
   private final NumberSetting mapWidth = this.addNumber("Map width", 128.0D, 48.0D, 320.0D, 8.0D);
   private final NumberSetting mapHeight = this.addNumber("Map height", 128.0D, 48.0D, 320.0D, 8.0D);
   private final ModeSetting zoom = this.addMode("Zoom", "1x", "0.5x", "1x", "2x", "4x");
   private final ModeSetting shape = this.addMode("Map shape", "Square", "Square", "Circle");
   private final BooleanSetting rotate = this.addBool("Rotate with player", true);
   private final BooleanSetting antiAlias = this.addBool("Anti-aliasing", true);
   private final BooleanSetting caveMode = this.addBool("Cave mode", false);

   private final BooleanSetting borderOn = this.addBool("Border", true);
   private final ColorSetting borderColor = this.addColor("Border colour", 0xFF000000);
   private final NumberSetting borderAlpha = this.addNumber("Border alpha", 90.0D, 0.0D, 255.0D, 5.0D);
   private final NumberSetting borderThickness = this.addNumber("Border thickness", 3.0D, 1.0D, 8.0D, 1.0D);
   private final BooleanSetting cardinals = this.addBool("Cardinal directions", true);
   private final ColorSetting cardinalColor = this.addColor("Cardinal text colour", 0xFFFFFFFF);
   private final BooleanSetting cardinalShadow = this.addBool("Cardinal text shadow", true);
   private final ModeSetting markerStyle = this.addMode("Player marker", "Arrow", "Arrow", "Triangle", "Dot");
   private final NumberSetting markerSize = this.addNumber("Marker size", 1.0D, 0.5D, 3.0D, 0.1D);

   private final BooleanSetting playerRadar = this.addBool("Player radar", true);
   private final NumberSetting headSize = this.addNumber("Player head size", 1.0D, 0.5D, 2.5D, 0.1D);
   private final BooleanSetting playerNames = this.addBool("Player names", true);
   private final NumberSetting nameSize = this.addNumber("Player name size", 0.5D, 0.25D, 1.0D, 0.05D);
   private final BooleanSetting hostileMobs = this.addBool("Hostile mobs", false);
   private final BooleanSetting passiveMobs = this.addBool("Passive mobs", false);

   private final BooleanSetting worldMapOn = this.addBool("World map", true);

   private final KeybindSetting zoomInKey = this.addKeybind("Zoom in", 0);
   private final KeybindSetting zoomOutKey = this.addKeybind("Zoom out", 0);
   private final KeybindSetting fullViewKey = this.addKeybind("Full view (hold)", 0);
   private final KeybindSetting worldMapKey = this.addKeybind("Open world map", 0);

   private final MinimapCache cache = new MinimapCache();

   private DynamicTexture texture;
   private int[] pixels;

   private boolean zoomInWasDown;
   private boolean zoomOutWasDown;
   private boolean worldMapWasDown;

   public Minimap() {
      super("Minimap", "Top-down map of the surrounding terrain", ModuleCategory.MECHANIC, 0,
            HudModule.Anchor.TOP_RIGHT);
   }

   public MinimapCache getCache() {
      return this.cache;
   }

   public boolean isCaveMode() {
      return this.caveMode.get();
   }

   /** Blocks covered by one texture pixel. Higher zoom means fewer. */
   public double blocksPerPixel() {
      String z = this.zoom.get();
      if("0.5x".equals(z)) {
         return 2.0D;
      } else if("2x".equals(z)) {
         return 0.5D;
      } else if("4x".equals(z)) {
         return 0.25D;
      } else {
         return 1.0D;
      }
   }

   /**
    * Held-key full view temporarily doubles the map, which is the point of the
    * bind -- a quick look at more ground without changing the configured size.
    */
   private boolean fullView() {
      return this.fullViewKey.getKeyCode() != 0
            && this.mc.currentScreen == null
            && BindUtil.isDown(this.fullViewKey.getKeyCode());
   }

   public int getWidth() {
      int w = (int)this.mapWidth.get();
      return this.fullView() ? w * 2 : w;
   }

   public int getHeight() {
      int h = (int)this.mapHeight.get();
      return this.fullView() ? h * 2 : h;
   }

   public boolean isEmpty() {
      // Respect the HUD toggle without disabling the module, so the world map
      // keybind still works with the overlay hidden.
      return !this.showInHud.get() || super.isEmpty();
   }

   protected void onDisable() {
      this.cache.clear();
      this.releaseTexture();
   }

   /**
    * Deletes the GL texture rather than just dropping the reference: these are
    * driver-side allocations and leaking one per toggle would grow VRAM use for
    * the session.
    */
   private void releaseTexture() {
      if(this.texture != null) {
         this.texture.deleteGlTexture();
         this.texture = null;
         this.pixels = null;
      }

   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled() || this.mc.thePlayer == null) {
         return;
      }

      this.pollKeys();

      int ccx = MathHelper.floor_double(this.mc.thePlayer.posX) >> 4;
      int ccz = MathHelper.floor_double(this.mc.thePlayer.posZ) >> 4;
      this.cache.update(this.mc, ccx, ccz, this.radiusChunks(), this.caveMode.get());

      if(this.showInHud.get() || this.mc.currentScreen instanceof WorldMapScreen) {
         this.uploadPixels();
      }

   }

   private void pollKeys() {
      if(this.mc.currentScreen != null) {
         this.zoomInWasDown = false;
         this.zoomOutWasDown = false;
         this.worldMapWasDown = false;
         return;
      }

      boolean in = this.zoomInKey.getKeyCode() != 0 && BindUtil.isDown(this.zoomInKey.getKeyCode());
      if(in && !this.zoomInWasDown) {
         this.stepZoom(1);
      }

      this.zoomInWasDown = in;

      boolean out = this.zoomOutKey.getKeyCode() != 0 && BindUtil.isDown(this.zoomOutKey.getKeyCode());
      if(out && !this.zoomOutWasDown) {
         this.stepZoom(-1);
      }

      this.zoomOutWasDown = out;

      boolean map = this.worldMapKey.getKeyCode() != 0 && BindUtil.isDown(this.worldMapKey.getKeyCode());
      if(map && !this.worldMapWasDown && this.worldMapOn.get()) {
         this.mc.displayGuiScreen(new WorldMapScreen(this));
      }

      this.worldMapWasDown = map;
   }

   private void stepZoom(int direction) {
      String[] steps = new String[]{"0.5x", "1x", "2x", "4x"};
      String cur = this.zoom.get();

      for(int i = 0; i < steps.length; ++i) {
         if(steps[i].equals(cur)) {
            int next = Math.max(0, Math.min(steps.length - 1, i + direction));
            this.zoom.setByName(steps[next]);
            // Zoom changes how much world each pixel covers, so everything
            // cached at the old scale is the wrong resolution.
            this.cache.clear();
            return;
         }
      }

   }

   /** How many chunks of world the texture has to cover at the current zoom. */
   private int radiusChunks() {
      double blocksAcross = (double)TEX * this.blocksPerPixel();
      return (int)Math.ceil(blocksAcross / 2.0D / 16.0D) + 1;
   }

   private void ensureTexture() {
      if(this.texture == null) {
         this.texture = new DynamicTexture(TEX, TEX);
         this.pixels = this.texture.getTextureData();
      }

   }

   /** Rasterises the cached chunk colours into the texture, centred on the player. */
   private void uploadPixels() {
      this.ensureTexture();

      double scale = this.blocksPerPixel();
      double centerX = this.mc.thePlayer.posX;
      double centerZ = this.mc.thePlayer.posZ;

      int half = TEX / 2;
      int radiusSq = half * half;
      boolean circle = this.shape.is("Circle");

      for(int py = 0; py < TEX; ++py) {
         for(int px = 0; px < TEX; ++px) {
            int idx = py * TEX + px;

            int dx = px - half;
            int dy = py - half;

            if(circle && dx * dx + dy * dy > radiusSq) {
               // Fully transparent: this is what gives the circular cut-out
               // without needing a stencil.
               this.pixels[idx] = 0;
               continue;
            }

            int worldX = MathHelper.floor_double(centerX + (double)dx * scale);
            int worldZ = MathHelper.floor_double(centerZ + (double)dy * scale);

            MapChunk chunk = this.cache.get(worldX >> 4, worldZ >> 4);
            // Unsampled chunks read as a dark plate rather than a hole, so the
            // map keeps a consistent silhouette while it fills in.
            this.pixels[idx] = chunk == null ? 0xFF14141A : chunk.colorAt(worldX & 15, worldZ & 15);
         }
      }

      this.texture.updateDynamicTexture();
   }

   public void render(int x, int y) {
      if(this.mc.thePlayer == null || this.texture == null || !this.showInHud.get()) {
         return;
      }

      int w = this.getWidth();
      int h = this.getHeight();
      int cx = x + w / 2;
      int cy = y + h / 2;
      float yaw = this.rotate.get() ? -this.mc.thePlayer.rotationYaw - 180.0F : 0.0F;

      this.drawMapQuad(cx, cy, w, h, yaw);
      this.drawRadar(cx, cy, w, h, yaw);

      if(this.cardinals.get()) {
         this.drawCardinals(cx, cy, w, h, yaw);
      }

      if(this.borderOn.get()) {
         this.drawBorder(x, y, w, h);
      }

      this.drawPlayerMarker(cx, cy);
   }

   /**
    * Draws the map texture, cropping rather than stretching when the map is not
    * square -- stretching would make one axis' scale differ from the other and
    * distances would read wrong along it.
    */
   private void drawMapQuad(int cx, int cy, int w, int h, float yaw) {
      GlStateManager.pushMatrix();
      GlStateManager.enableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      GlStateManager.translate((float)cx, (float)cy, 0.0F);
      if(yaw != 0.0F) {
         GlStateManager.rotate(yaw, 0.0F, 0.0F, 1.0F);
      }

      GlStateManager.bindTexture(this.texture.getGlTextureId());
      this.applyFiltering();

      int m = Math.max(w, h);
      float uHalf = (float)w / (float)m / 2.0F;
      float vHalf = (float)h / (float)m / 2.0F;

      GL11.glBegin(GL11.GL_QUADS);
      GL11.glTexCoord2f(0.5F - uHalf, 0.5F - vHalf);
      GL11.glVertex2f((float)(-w / 2), (float)(-h / 2));
      GL11.glTexCoord2f(0.5F - uHalf, 0.5F + vHalf);
      GL11.glVertex2f((float)(-w / 2), (float)(h / 2));
      GL11.glTexCoord2f(0.5F + uHalf, 0.5F + vHalf);
      GL11.glVertex2f((float)(w / 2), (float)(h / 2));
      GL11.glTexCoord2f(0.5F + uHalf, 0.5F - vHalf);
      GL11.glVertex2f((float)(w / 2), (float)(-h / 2));
      GL11.glEnd();

      GlStateManager.popMatrix();
   }

   /**
    * Linear filtering smooths the block edges when the map is scaled; nearest
    * keeps them crisp. Set per draw because the texture is shared with the
    * world map screen, which may want the other setting.
    */
   private void applyFiltering() {
      int filter = this.antiAlias.get() ? GL11.GL_LINEAR : GL11.GL_NEAREST;
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
   }

   /**
    * Entity markers, drawn unrotated in screen space with their offsets rotated
    * by hand -- drawing them inside the rotated matrix would spin the heads and
    * names themselves, which looks wrong for flat markers.
    */
   private void drawRadar(int cx, int cy, int w, int h, float yaw) {
      double pxPerBlock = 1.0D / this.blocksPerPixel() * ((double)Math.max(w, h) / (double)TEX);
      double rad = Math.toRadians((double)yaw);
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);

      int limitX = w / 2 - 2;
      int limitY = h / 2 - 2;
      boolean circle = this.shape.is("Circle");
      int radius = Math.min(limitX, limitY);

      for(Entity e : this.mc.theWorld.loadedEntityList) {
         if(e == this.mc.thePlayer || !this.shouldShow(e)) {
            continue;
         }

         double ox = (e.posX - this.mc.thePlayer.posX) * pxPerBlock;
         double oz = (e.posZ - this.mc.thePlayer.posZ) * pxPerBlock;

         int dx = (int)Math.round(ox * cos - oz * sin);
         int dy = (int)Math.round(ox * sin + oz * cos);

         // Clip to the map rather than letting distant entities draw over the
         // rest of the HUD.
         if(circle) {
            if(dx * dx + dy * dy > radius * radius) {
               continue;
            }
         } else if(Math.abs(dx) > limitX || Math.abs(dy) > limitY) {
            continue;
         }

         if(e instanceof EntityPlayer && this.playerRadar.get()) {
            this.drawPlayerBlip((EntityPlayer)e, cx + dx, cy + dy);
         } else {
            Gui.drawRect(cx + dx - 1, cy + dy - 1, cx + dx + 1, cy + dy + 1, this.mobColor(e));
         }
      }

   }

   private boolean shouldShow(Entity e) {
      if(e instanceof EntityPlayer) {
         return this.playerRadar.get();
      } else if(e instanceof IMob) {
         return this.hostileMobs.get();
      } else if(e instanceof IAnimals || e instanceof EntityLiving) {
         return this.passiveMobs.get();
      } else {
         return false;
      }
   }

   private int mobColor(Entity e) {
      return e instanceof IMob ? 0xFFFF5555 : 0xFF55FF55;
   }

   /** A player's face from their skin, with their name above it. */
   private void drawPlayerBlip(EntityPlayer p, int sx, int sy) {
      int size = Math.max(4, Math.round(8.0F * (float)this.headSize.get()));
      int hx = sx - size / 2;
      int hy = sy - size / 2;

      ResourceLocation skin = null;
      if(p instanceof AbstractClientPlayer) {
         skin = ((AbstractClientPlayer)p).getLocationSkin();
      }

      if(skin != null) {
         GlStateManager.enableTexture2D();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.mc.getTextureManager().bindTexture(skin);
         // The face is the 8x8 region at (8,8) of the 64x64 skin.
         Gui.drawScaledCustomSizeModalRect(hx, hy, 8.0F, 8.0F, 8, 8, size, size, 64.0F, 64.0F);
      } else {
         Gui.drawRect(hx, hy, hx + size, hy + size, 0xFFFFFFFF);
      }

      if(this.playerNames.get()) {
         this.drawScaledName(p.getName(), sx, hy - 2, (float)this.nameSize.get());
      }

   }

   private void drawScaledName(String name, int cx, int baseY, float scale) {
      GlStateManager.pushMatrix();
      GlStateManager.scale(scale, scale, 1.0F);

      int w = this.mc.fontRendererObj.getStringWidth(name);
      // Positions are divided by the scale so the label lands where it would
      // have unscaled, rather than drifting toward the screen origin.
      float x = (float)cx / scale - (float)w / 2.0F;
      float y = (float)baseY / scale - 9.0F;

      this.mc.fontRendererObj.drawStringWithShadow(name, x, y, 0xFFFFFFFF);
      GlStateManager.popMatrix();
   }

   /** N/E/S/W around the edge, rotating with the map so they stay truthful. */
   private void drawCardinals(int cx, int cy, int w, int h, float yaw) {
      String[] letters = new String[]{"N", "E", "S", "W"};
      double rad = Math.toRadians((double)yaw);
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);

      int rx = w / 2 - 7;
      int ry = h / 2 - 7;
      int color = ColorUtil.withAlpha(this.cardinalColor.getRGB(), 255);

      for(int i = 0; i < letters.length; ++i) {
         // North is -Z, then clockwise; each letter is 90 degrees on from it.
         double ang = Math.toRadians((double)(i * 90));
         double ux = Math.sin(ang);
         double uz = -Math.cos(ang);

         int px = (int)Math.round((ux * (double)rx) * cos - (uz * (double)ry) * sin);
         int py = (int)Math.round((ux * (double)rx) * sin + (uz * (double)ry) * cos);

         int tw = this.mc.fontRendererObj.getStringWidth(letters[i]);
         int tx = cx + px - tw / 2;
         int ty = cy + py - 4;

         if(this.cardinalShadow.get()) {
            this.mc.fontRendererObj.drawStringWithShadow(letters[i], (float)tx, (float)ty, color);
         } else {
            this.mc.fontRendererObj.drawString(letters[i], tx, ty, color);
         }
      }

   }

   private void drawPlayerMarker(int cx, int cy) {
      GlStateManager.disableTexture2D();

      float s = (float)this.markerSize.get();
      int arm = Math.max(1, Math.round(3.0F * s));
      int color = 0xFFFFFFFF;

      if(this.markerStyle.is("Dot")) {
         Gui.drawRect(cx - arm, cy - arm, cx + arm, cy + arm, color);
      } else if(this.markerStyle.is("Triangle")) {
         this.drawTriangle(cx, cy, (float)arm * 1.6F, color);
      } else {
         // Arrow: a stem with a wider base, pointing up. When the map rotates
         // the world turns underneath it, so a fixed arrow is always correct;
         // when it does not, the arrow is turned to face the player's yaw.
         GlStateManager.pushMatrix();
         GlStateManager.translate((float)cx, (float)cy, 0.0F);
         if(!this.rotate.get()) {
            GlStateManager.rotate(this.mc.thePlayer.rotationYaw + 180.0F, 0.0F, 0.0F, 1.0F);
         }

         Gui.drawRect(-1, -arm, 1, arm, color);
         Gui.drawRect(-arm + 1, arm - 2, arm - 1, arm, color);
         GlStateManager.popMatrix();
      }

      GlStateManager.enableTexture2D();
   }

   private void drawTriangle(int cx, int cy, float size, int color) {
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)cx, (float)cy, 0.0F);
      if(!this.rotate.get()) {
         GlStateManager.rotate(this.mc.thePlayer.rotationYaw + 180.0F, 0.0F, 0.0F, 1.0F);
      }

      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;

      GlStateManager.color(r, g, b, a);
      GL11.glBegin(GL11.GL_TRIANGLES);
      GL11.glVertex2f(0.0F, -size);
      GL11.glVertex2f(-size * 0.7F, size);
      GL11.glVertex2f(size * 0.7F, size);
      GL11.glEnd();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      GlStateManager.popMatrix();
   }

   private void drawBorder(int x, int y, int w, int h) {
      int t = (int)this.borderThickness.get();
      int col = ColorUtil.withAlpha(this.borderColor.getRGB(), (int)this.borderAlpha.get());

      Gui.drawRect(x, y, x + w, y + t, col);
      Gui.drawRect(x, y + h - t, x + w, y + h, col);
      Gui.drawRect(x, y, x + t, y + h, col);
      Gui.drawRect(x + w - t, y, x + w, y + h, col);
   }
}
