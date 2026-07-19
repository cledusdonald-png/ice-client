package com.unclesam.client.util;

import java.awt.Color;

public final class ColorUtil {
   public static final int ACCENT = (new Color(140, 90, 255)).getRGB();

   private ColorUtil() {
   }

   public static int chroma(int speedMillis, int offset, float saturation, float brightness) {
      float hue = (float)((System.currentTimeMillis() + (long)offset * 10L) % (long)speedMillis) / (float)speedMillis;
      return Color.HSBtoRGB(hue, saturation, brightness);
   }

   public static int chroma(int offset) {
      return chroma(3000, offset, 0.8F, 1.0F);
   }

   public static int interpolate(int a, int b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int aa = a >> 24 & 255;
      int ar = a >> 16 & 255;
      int ag = a >> 8 & 255;
      int ab = a & 255;
      int ba = b >> 24 & 255;
      int br = b >> 16 & 255;
      int bg = b >> 8 & 255;
      int bb = b & 255;
      int na = (int)((float)aa + (float)(ba - aa) * t);
      int nr = (int)((float)ar + (float)(br - ar) * t);
      int ng = (int)((float)ag + (float)(bg - ag) * t);
      int nb = (int)((float)ab + (float)(bb - ab) * t);
      return na << 24 | nr << 16 | ng << 8 | nb;
   }

   public static int withAlpha(int color, int alpha) {
      return color & 16777215 | (alpha & 255) << 24;
   }
}
