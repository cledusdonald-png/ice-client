package com.iceclient.util;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public final class RenderUtil {
   public static boolean hudBackground = false;

   private RenderUtil() {
   }

   public static void rect(int left, int top, int right, int bottom, int color) {
      if(left > right) {
         int t = left;
         left = right;
         right = t;
      }

      if(top > bottom) {
         int t = top;
         top = bottom;
         bottom = t;
      }

      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.color(r, g, b, a);
      GL11.glBegin(7);
      GL11.glVertex2d((double)left, (double)bottom);
      GL11.glVertex2d((double)right, (double)bottom);
      GL11.glVertex2d((double)right, (double)top);
      GL11.glVertex2d((double)left, (double)top);
      GL11.glEnd();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void border(int left, int top, int right, int bottom, int color) {
      rect(left, top, right, top + 1, color);
      rect(left, bottom - 1, right, bottom, color);
      rect(left, top, left + 1, bottom, color);
      rect(right - 1, top, right, bottom, color);
   }

   public static void panel(int left, int top, int right, int bottom, int bg, int borderColor) {
      if(hudBackground) {
         rect(left, top, right, bottom, bg);
         border(left, top, right, bottom, borderColor);
      }
   }

   public static void text(FontRenderer font, String s, int x, int y, int color) {
      font.drawStringWithShadow(s, (float)x, (float)y, color);
   }
}
