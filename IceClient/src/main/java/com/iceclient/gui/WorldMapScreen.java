package com.iceclient.gui;

import com.iceclient.minimap.MapChunk;
import com.iceclient.minimap.MinimapCache;
import com.iceclient.module.modules.render.Minimap;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/**
 * Full-screen explorable map: drag to pan, wheel to zoom, click to read
 * coordinates.
 *
 * <p>Renders into its own {@link DynamicTexture} rather than reusing the HUD
 * minimap's. The HUD texture is permanently centred on the player at the HUD's
 * zoom, which is precisely what an explorable map must not be -- panning away
 * from the player is the entire feature.
 *
 * <p>Only terrain already sampled into {@link MinimapCache} can be drawn, and
 * the cache holds what has been near the player. Panning past that shows the
 * unexplored plate rather than inventing terrain, because the chunks genuinely
 * are not on the client to sample.
 */
public class WorldMapScreen extends GuiScreen {

   private static final int TEX = 512;

   /** Blocks per texture pixel. Larger is further out. */
   private static final double MIN_SCALE = 0.25D;
   private static final double MAX_SCALE = 8.0D;

   private final Minimap minimap;

   private DynamicTexture texture;
   private int[] pixels;

   private double centerX;
   private double centerZ;
   private double scale = 1.0D;

   private boolean dragging;
   private int lastMouseX;
   private int lastMouseY;

   /** Set whenever pan/zoom moves, so the texture is rebuilt once per change. */
   private boolean dirty = true;

   public WorldMapScreen(Minimap minimap) {
      this.minimap = minimap;
   }

   public void initGui() {
      if(this.mc.thePlayer != null) {
         this.centerX = this.mc.thePlayer.posX;
         this.centerZ = this.mc.thePlayer.posZ;
      }

      this.dirty = true;
   }

   public void onGuiClosed() {
      // Driver-side allocation: dropping the reference alone would leak it for
      // the rest of the session, and this screen can be reopened often.
      if(this.texture != null) {
         this.texture.deleteGlTexture();
         this.texture = null;
         this.pixels = null;
      }

   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   private void ensureTexture() {
      if(this.texture == null) {
         this.texture = new DynamicTexture(TEX, TEX);
         this.pixels = this.texture.getTextureData();
         this.dirty = true;
      }

   }

   /**
    * Rasterises cached chunks around the pan centre.
    *
    * <p>Rebuilt only when something moved. At 512x512 this is a quarter of a
    * million samples, which is fine once but not every frame.
    */
   private void rebuild() {
      this.ensureTexture();
      MinimapCache cache = this.minimap.getCache();

      int half = TEX / 2;

      for(int py = 0; py < TEX; ++py) {
         for(int px = 0; px < TEX; ++px) {
            int worldX = MathHelper.floor_double(this.centerX + (double)(px - half) * this.scale);
            int worldZ = MathHelper.floor_double(this.centerZ + (double)(py - half) * this.scale);

            MapChunk chunk = cache.get(worldX >> 4, worldZ >> 4);
            this.pixels[py * TEX + px] = chunk == null
                  ? 0xFF101014
                  : chunk.colorAt(worldX & 15, worldZ & 15);
         }
      }

      this.texture.updateDynamicTexture();
      this.dirty = false;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();

      if(this.dirty) {
         this.rebuild();
      }

      if(this.texture == null) {
         return;
      }

      int size = Math.min(this.width, this.height) - 40;
      int left = (this.width - size) / 2;
      int top = (this.height - size) / 2;

      GlStateManager.enableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.bindTexture(this.texture.getGlTextureId());

      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

      GL11.glBegin(GL11.GL_QUADS);
      GL11.glTexCoord2f(0.0F, 0.0F);
      GL11.glVertex2f((float)left, (float)top);
      GL11.glTexCoord2f(0.0F, 1.0F);
      GL11.glVertex2f((float)left, (float)(top + size));
      GL11.glTexCoord2f(1.0F, 1.0F);
      GL11.glVertex2f((float)(left + size), (float)(top + size));
      GL11.glTexCoord2f(1.0F, 0.0F);
      GL11.glVertex2f((float)(left + size), (float)top);
      GL11.glEnd();

      GlStateManager.disableTexture2D();

      this.drawEntities(left, top, size);
      this.drawFrame(left, top, size);

      GlStateManager.enableTexture2D();
      this.drawLabels(left, top, size, mouseX, mouseY);
   }

   /** Pixels on screen per world block, accounting for texture-to-quad scaling. */
   private double pixelsPerBlock(int size) {
      return (double)size / (double)TEX / this.scale;
   }

   private void drawEntities(int left, int top, int size) {
      if(this.mc.theWorld == null) {
         return;
      }

      double ppb = this.pixelsPerBlock(size);
      int cx = left + size / 2;
      int cy = top + size / 2;

      for(Entity e : this.mc.theWorld.loadedEntityList) {
         if(!(e instanceof EntityPlayer)) {
            continue;
         }

         int ex = cx + (int)Math.round((e.posX - this.centerX) * ppb);
         int ey = cy + (int)Math.round((e.posZ - this.centerZ) * ppb);

         if(ex < left || ex > left + size || ey < top || ey > top + size) {
            continue;
         }

         boolean self = e == this.mc.thePlayer;
         Gui.drawRect(ex - 2, ey - 2, ex + 2, ey + 2, self ? 0xFFFFFFFF : 0xFFFF5555);
      }

   }

   private void drawFrame(int left, int top, int size) {
      int col = 0xFF8C5AFF;
      Gui.drawRect(left - 1, top - 1, left + size + 1, top, col);
      Gui.drawRect(left - 1, top + size, left + size + 1, top + size + 1, col);
      Gui.drawRect(left - 1, top, left, top + size, col);
      Gui.drawRect(left + size, top, left + size + 1, top + size, col);
   }

   private void drawLabels(int left, int top, int size, int mouseX, int mouseY) {
      String zoomLabel = "Scale: " + String.format("%.2f", Double.valueOf(this.scale)) + " blocks/px";
      this.fontRendererObj.drawStringWithShadow(zoomLabel, (float)(left), (float)(top - 12), 0xFFAAAAAA);

      String hint = "Drag to pan  ·  Wheel to zoom  ·  Space to recentre";
      int hw = this.fontRendererObj.getStringWidth(hint);
      this.fontRendererObj.drawStringWithShadow(hint, (float)(left + size - hw),
            (float)(top + size + 4), 0xFF666666);

      // Coordinates under the cursor, which is what makes this usable for
      // calling out a base location rather than just looking pretty.
      if(mouseX >= left && mouseX <= left + size && mouseY >= top && mouseY <= top + size) {
         double ppb = this.pixelsPerBlock(size);
         int wx = (int)Math.round(this.centerX + (double)(mouseX - (left + size / 2)) / ppb);
         int wz = (int)Math.round(this.centerZ + (double)(mouseY - (top + size / 2)) / ppb);
         String coords = wx + ", " + wz;
         this.fontRendererObj.drawStringWithShadow(coords, (float)(mouseX + 8), (float)(mouseY - 10),
               0xFFFFFFFF);
      }

   }

   protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
      super.mouseClicked(mouseX, mouseY, button);
      if(button == 0) {
         this.dragging = true;
         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
      }

   }

   protected void mouseReleased(int mouseX, int mouseY, int state) {
      super.mouseReleased(mouseX, mouseY, state);
      if(state == 0) {
         this.dragging = false;
      }

   }

   protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceClick) {
      if(!this.dragging) {
         return;
      }

      int size = Math.min(this.width, this.height) - 40;
      double ppb = this.pixelsPerBlock(size);

      this.centerX -= (double)(mouseX - this.lastMouseX) / ppb;
      this.centerZ -= (double)(mouseY - this.lastMouseY) / ppb;
      this.lastMouseX = mouseX;
      this.lastMouseY = mouseY;
      this.dirty = true;
   }

   public void handleMouseInput() throws IOException {
      super.handleMouseInput();

      int wheel = Mouse.getEventDWheel();
      if(wheel == 0) {
         return;
      }

      // Multiplicative so each notch feels the same at every zoom level; a
      // fixed step crawls when far out and jumps when close in.
      double factor = wheel > 0 ? 0.8D : 1.25D;
      double next = this.scale * factor;

      this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, next));
      this.dirty = true;
   }

   protected void keyTyped(char typedChar, int keyCode) throws IOException {
      if(keyCode == 57 && this.mc.thePlayer != null) {
         this.centerX = this.mc.thePlayer.posX;
         this.centerZ = this.mc.thePlayer.posZ;
         this.dirty = true;
         return;
      }

      super.keyTyped(typedChar, keyCode);
   }
}
