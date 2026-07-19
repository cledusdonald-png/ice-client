package com.unclesam.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class IceMainMenuScreen extends GuiScreen {
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
      this.drawGradientRect(0, 0, this.width, this.height, -16447474, -16115674);
      double t = (double)System.currentTimeMillis() / 1000.0D;
      this.glow((float)this.width * 0.32F + (float)Math.sin(t * 0.25D) * 40.0F, (float)this.height * 0.3F, 200.0F, 576505599);
      this.glow((float)this.width * 0.7F + (float)Math.cos(t * 0.2D) * 36.0F, (float)this.height * 0.62F, 240.0F, 439320486);
      int logoCy = this.height / 2 - 92;
      this.drawCrystal((float)this.width / 2.0F, (float)logoCy, 26.0F);
      this.drawWordmark(this.width / 2, logoCy + 34);
      String tag = "Factions tooling, sharpened.";
      this.fontRendererObj.drawString(tag, this.width / 2 - this.fontRendererObj.getStringWidth(tag) / 2, logoCy + 74, -8087640);

      for(IceMainMenuScreen.Item it : this.items) {
         boolean hov = it.hit(mouseX, mouseY);
         this.roundRect(it.x, it.y, it.x + it.w, it.y + it.h, 5.0F, hov?677168895:352321535);
         if(hov) {
            this.roundBorder(it.x, it.y, it.x + it.w, it.y + it.h, 5.0F, 866773503);
            drawRect(it.x, it.y + 5, it.x + 2, it.y + it.h - 5, -9447681);
         }

         int tw = this.fontRendererObj.getStringWidth(it.label);
         this.fontRendererObj.drawStringWithShadow(it.label, (float)(it.x + it.w / 2 - tw / 2), (float)(it.y + it.h / 2 - 4), hov?-1379073:-2890766);
      }

      this.fontRendererObj.drawString("Ice Client v0.1.0", 6, this.height - 12, -8087640);
      String mcv = "Minecraft 1.8.9";
      this.fontRendererObj.drawString(mcv, this.width - this.fontRendererObj.getStringWidth(mcv) - 6, this.height - 12, -12694952);
      super.drawScreen(mouseX, mouseY, partialTicks);
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
