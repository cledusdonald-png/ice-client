package com.iceclient.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class IceMainMenuScreen extends GuiScreen {

   /** Backdrop art. Drawn cover-fit, so it fills any window without stretching. */
   private static final ResourceLocation BG =
         new ResourceLocation("iceclient", "textures/gui/menu_bg.png");
   private static final float BG_ASPECT = 1024.0F / 576.0F;

   /** Minecraft's font has no smaller size, so the news panel is scaled down. */
   private static final float NEWS_SCALE = 0.7F;

   /**
    * What changed in this build. Update this when releasing -- it is the first
    * thing anyone sees, so a stale list is worse than none.
    */
   private static final String[][] NEWS = new String[][]{
         {"Printer breaks safely", "Clears wrong blocks, spares redstone"},
         {"Pistons protected", "Never broken mid-extension"},
         {"One auto-tick", "Two were fighting over repeaters"},
         {"Patch Crumbs rebuilt", "Correct Y, every breach at once"},
         {"New menu + launcher", "Sections, and this backdrop"}};

   private static final int BG_TOP = -16447474;
   private static final int BG_BOT = -16115674;
   private static final int ICE = -9447681;
   private static final int ICE_BRIGHT = -5641729;
   private static final int FROST = -1379073;
   private static final int STEEL = -8087640;
   private static final int BTN_BG = 352321535;
   private static final int BTN_HOVER = 677168895;
   private static final int BTN_LINE = 866773503;
   private final List<IceMainMenuScreen.Item> items = new ArrayList();

   public IceMainMenuScreen() {
   }

   public void initGui() {
      this.items.clear();
      int bw = 220;
      int bh = 24;
      int gap = 8;
      int startY = this.height / 2 - 6;
      int cx = this.width / 2 - bw / 2;
      this.add(cx, startY, bw, bh, "Singleplayer", () -> {
         this.mc.displayGuiScreen(new GuiSelectWorld(this));
      });
      this.add(cx, startY + bh + gap, bw, bh, "Multiplayer", () -> {
         this.mc.displayGuiScreen(new GuiMultiplayer(this));
      });
      this.add(cx, startY + 2 * (bh + gap), bw, bh, "Options", () -> {
         this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
      });
      this.add(cx, startY + 3 * (bh + gap), bw, bh, "Quit Game", () -> {
         this.mc.shutdown();
      });
   }

   private void add(int x, int y, int w, int h, String label, Runnable action) {
      IceMainMenuScreen.Item it = new IceMainMenuScreen.Item();
      it.x = x;
      it.y = y;
      it.w = w;
      it.h = h;
      it.label = label;
      it.action = action;
      this.items.add(it);
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawBackground();

      int logoCy = this.height / 2 - 96;
      this.drawCrystal((float)this.width / 2.0F, (float)logoCy, 26.0F);
      this.drawWordmark(this.width / 2, logoCy + 34);

      String tag = "COLD.  CALCULATED.  DEADLY.";
      this.fontRendererObj.drawStringWithShadow(tag,
            (float)(this.width / 2 - this.fontRendererObj.getStringWidth(tag) / 2),
            (float)(logoCy + 76), -8087640);

      // Divider, matching the launcher's snowflake rule.
      int ry = logoCy + 92;
      int half = 78;
      drawRect(this.width / 2 - half, ry, this.width / 2 - 10, ry + 1, 1157627903);
      drawRect(this.width / 2 + 10, ry, this.width / 2 + half, ry + 1, 1157627903);

      this.drawNews();

      for(IceMainMenuScreen.Item it : this.items) {
         boolean hov = it.hit(mouseX, mouseY);
         this.roundRect(it.x, it.y, it.x + it.w, it.y + it.h, 5.0F, hov ? 0xF0123449 : 0xD00A1B2C);
         this.roundBorder(it.x, it.y, it.x + it.w, it.y + it.h, 5.0F, hov ? 0xFF6FD6FF : 0x40A9E9FF);

         int tw = this.fontRendererObj.getStringWidth(it.label);
         this.fontRendererObj.drawStringWithShadow(it.label, (float)(it.x + it.w / 2 - tw / 2), (float)(it.y + it.h / 2 - 4), hov?-1379073:-2890766);
      }

      this.fontRendererObj.drawString("Ice Client v" + com.iceclient.IceClient.displayVersion(), 6, this.height - 12, -8087640);
      String mcv = "Minecraft 1.8.9";
      this.fontRendererObj.drawString(mcv, this.width - this.fontRendererObj.getStringWidth(mcv) - 6, this.height - 12, -12694952);
      super.drawScreen(mouseX, mouseY, partialTicks);
   }

   /**
    * Fills the screen with the backdrop art, then darkens it.
    *
    * <p>Cover-fit rather than stretched: the texture coordinates are inset on
    * whichever axis has spare room, so the image is cropped instead of squashed
    * at window shapes that do not match its 16:9. The scrim on top is what makes
    * white text readable over a bright aurora -- without it the menu is pretty
    * and unusable.
    */
   private void drawBackground() {
      float screenAspect = (float)this.width / (float)this.height;
      float u0 = 0.0F;
      float u1 = 1.0F;
      float v0 = 0.0F;
      float v1 = 1.0F;

      if(screenAspect > BG_ASPECT) {
         float vh = BG_ASPECT / screenAspect;
         v0 = (1.0F - vh) / 2.0F;
         v1 = v0 + vh;
      } else {
         float uw = screenAspect / BG_ASPECT;
         u0 = (1.0F - uw) / 2.0F;
         u1 = u0 + uw;
      }

      this.mc.getTextureManager().bindTexture(BG);
      GlStateManager.enableTexture2D();
      GlStateManager.disableLighting();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      GL11.glBegin(7);
      GL11.glTexCoord2f(u0, v1);
      GL11.glVertex2f(0.0F, (float)this.height);
      GL11.glTexCoord2f(u1, v1);
      GL11.glVertex2f((float)this.width, (float)this.height);
      GL11.glTexCoord2f(u1, v0);
      GL11.glVertex2f((float)this.width, 0.0F);
      GL11.glTexCoord2f(u0, v0);
      GL11.glVertex2f(0.0F, 0.0F);
      GL11.glEnd();

      // Darker at the edges than the middle, so the art still reads behind the
      // logo while the buttons and corners stay legible.
      this.drawGradientRect(0, 0, this.width, this.height / 2, 0x66000000, 0x33000000);
      this.drawGradientRect(0, this.height / 2, this.width, this.height, 0x33000000, 0x99000000);
   }

   /**
    * Recent changes, mirroring the launcher's news card.
    *
    * <p>Drawn at {@link #NEWS_SCALE} and sized from the widest line it actually
    * contains. The first version hardcoded a width and drew at full size, so
    * longer entries ran straight across the menu buttons -- the panel has to be
    * measured from its content, not guessed.
    */
   private void drawNews() {
      String[][] lines = NEWS;

      float s = NEWS_SCALE;
      int widest = this.fontRendererObj.getStringWidth("LATEST NEWS");

      for(String[] l : lines) {
         widest = Math.max(widest, this.fontRendererObj.getStringWidth(l[0]));
         widest = Math.max(widest, this.fontRendererObj.getStringWidth(l[1]));
      }

      int pad = 9;
      int w = (int)((float)widest * s) + pad * 2;

      // Never reach the centre column, whatever the text says.
      int limit = this.width / 2 - 118 - 14;
      if(limit > 60 && w > limit) {
         w = limit;
      }

      int lineH = (int)(9.0F * s) + 1;
      int entryH = lineH * 2 + 4;
      int x = 14;
      int h = pad * 2 + lineH + 4 + lines.length * entryH;
      int y = this.height / 2 - h / 2 + 10;

      this.roundRect(x, y, x + w, y + h, 5.0F, 0xC00A1B2C);
      this.roundBorder(x, y, x + w, y + h, 5.0F, 0x40A9E9FF);

      this.drawScaled("LATEST NEWS", x + pad, y + pad, s, 0xFF6FD6FF);

      for(int i = 0; i < lines.length; ++i) {
         int ly = y + pad + lineH + 5 + i * entryH;
         this.drawScaled(lines[i][0], x + pad, ly, s, -1379073);
         this.drawScaled(lines[i][1], x + pad, ly + lineH, s, 0xFF7D8DA0);
      }
   }

   /** Draws text at a fraction of the font's size, anchored top-left. */
   private void drawScaled(String text, int x, int y, float scale, int color) {
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)x, (float)y, 0.0F);
      GlStateManager.scale(scale, scale, 1.0F);
      this.fontRendererObj.drawStringWithShadow(text, 0.0F, 0.0F, color);
      GlStateManager.popMatrix();
   }

   private void drawWordmark(int centerX, int y) {
      float scale = 2.6F;
      int wIce = this.fontRendererObj.getStringWidth("ICE ");
      int wClient = this.fontRendererObj.getStringWidth("CLIENT");
      float totalW = (float)(wIce + wClient) * scale;
      float startX = (float)centerX - totalW / 2.0F;
      GlStateManager.pushMatrix();
      GlStateManager.translate(startX, (float)y, 0.0F);
      GlStateManager.scale(scale, scale, 1.0F);
      this.fontRendererObj.drawStringWithShadow("ICE ", 0.0F, 0.0F, -9447681);
      this.fontRendererObj.drawStringWithShadow("CLIENT", (float)wIce, 0.0F, -1379073);
      GlStateManager.popMatrix();
   }

   private void drawCrystal(float cx, float cy, float r) {
      float w = r * 0.62F;
      float h = r * 0.42F;
      float[][] p = new float[][]{{cx, cy - r}, {cx + w, cy - h}, {cx + w, cy + h}, {cx, cy + r}, {cx - w, cy + h}, {cx - w, cy - h}};
      GlStateManager.pushMatrix();
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GL11.glEnable(2848);
      GL11.glLineWidth(4.0F);
      this.setColor(1084877311);
      this.crystalLines(p, cx, cy);
      GL11.glLineWidth(1.6F);
      this.setColor(-6298369);
      this.crystalLines(p, cx, cy);
      GL11.glDisable(2848);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   private void crystalLines(float[][] p, float cx, float cy) {
      GL11.glBegin(2);

      for(float[] v : p) {
         GL11.glVertex2f(v[0], v[1]);
      }

      GL11.glEnd();
      GL11.glBegin(1);
      GL11.glVertex2f(p[0][0], p[0][1]);
      GL11.glVertex2f(p[3][0], p[3][1]);
      GL11.glVertex2f(p[5][0], p[5][1]);
      GL11.glVertex2f(p[2][0], p[2][1]);
      GL11.glVertex2f(p[1][0], p[1][1]);
      GL11.glVertex2f(p[4][0], p[4][1]);
      GL11.glEnd();
   }

   private void glow(float cx, float cy, float r, int color) {
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GL11.glBegin(6);
      this.setColor(color);
      GL11.glVertex2f(cx, cy);
      this.setColor(color & 16777215);

      for(int a = 0; a <= 360; a += 20) {
         double ang = Math.toRadians((double)a);
         GL11.glVertex2f(cx + (float)Math.cos(ang) * r, cy + (float)Math.sin(ang) * r);
      }

      GL11.glEnd();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void roundRect(int l, int t, int r, int b, float rad, int color) {
      this.setColor(color);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GL11.glBegin(7);
      this.quad((float)l + rad, (float)t, (float)r - rad, (float)b);
      this.quad((float)l, (float)t + rad, (float)l + rad, (float)b - rad);
      this.quad((float)r - rad, (float)t + rad, (float)r, (float)b - rad);
      GL11.glEnd();
      this.fan((float)l + rad, (float)t + rad, rad, 180, 270);
      this.fan((float)r - rad, (float)t + rad, rad, 270, 360);
      this.fan((float)r - rad, (float)b - rad, rad, 0, 90);
      this.fan((float)l + rad, (float)b - rad, rad, 90, 180);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void roundBorder(int l, int t, int r, int b, float rad, int color) {
      this.setColor(color);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GL11.glEnable(2848);
      GL11.glLineWidth(1.0F);
      GL11.glBegin(2);
      this.arcVerts((float)l + rad, (float)t + rad, rad, 180, 270);
      this.arcVerts((float)r - rad, (float)t + rad, rad, 270, 360);
      this.arcVerts((float)r - rad, (float)b - rad, rad, 0, 90);
      this.arcVerts((float)l + rad, (float)b - rad, rad, 90, 180);
      GL11.glEnd();
      GL11.glDisable(2848);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void arcVerts(float cx, float cy, float rad, int a0, int a1) {
      for(int a = a0; a <= a1; a += 15) {
         double ang = Math.toRadians((double)a);
         GL11.glVertex2f(cx + (float)Math.cos(ang) * rad, cy + (float)Math.sin(ang) * rad);
      }

   }

   private void quad(float x1, float y1, float x2, float y2) {
      GL11.glVertex2f(x1, y2);
      GL11.glVertex2f(x2, y2);
      GL11.glVertex2f(x2, y1);
      GL11.glVertex2f(x1, y1);
   }

   private void fan(float cx, float cy, float rad, int a0, int a1) {
      GL11.glBegin(6);
      GL11.glVertex2f(cx, cy);

      for(int a = a0; a <= a1; a += 10) {
         double ang = Math.toRadians((double)a);
         GL11.glVertex2f(cx + (float)Math.cos(ang) * rad, cy + (float)Math.sin(ang) * rad);
      }

      GL11.glEnd();
   }

   private void setColor(int argb) {
      float a = (float)(argb >>> 24 & 255) / 255.0F;
      float r = (float)(argb >> 16 & 255) / 255.0F;
      float g = (float)(argb >> 8 & 255) / 255.0F;
      float b = (float)(argb & 255) / 255.0F;
      GlStateManager.color(r, g, b, a);
   }

   protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if(mouseButton == 0) {
         for(IceMainMenuScreen.Item it : this.items) {
            if(it.hit(mouseX, mouseY)) {
               it.action.run();
               return;
            }
         }

      }
   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   private static class Item {
      int x;
      int y;
      int w;
      int h;
      String label;
      Runnable action;

      private Item() {
      }

      boolean hit(int mx, int my) {
         return mx >= this.x && mx <= this.x + this.w && my >= this.y && my <= this.y + this.h;
      }
   }
}
