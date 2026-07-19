package com.unclesam.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

public final class WorldRenderUtil {
   private WorldRenderUtil() {
   }

   private static RenderManager rm() {
      return Minecraft.getMinecraft().getRenderManager();
   }

   public static double camX() {
      return rm().viewerPosX;
   }

   public static double camY() {
      return rm().viewerPosY;
   }

   public static double camZ() {
      return rm().viewerPosZ;
   }

   /**
    * Outline that respects terrain: with {@code throughWalls} false the box is
    * depth-tested, so it's hidden behind blocks like a real object.
    */
   public static void outlineBox(AxisAlignedBB box, int color, float lineWidth, boolean throughWalls) {
      if(throughWalls) {
         outlineBox(box, color, lineWidth);
         return;
      }

      beginWorld(lineWidth);
      // beginWorld kills depth testing for the see-through markers; put it back
      // so this outline is occluded by anything in front of it.
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      applyColor(color);

      double x1 = box.minX;
      double y1 = box.minY;
      double z1 = box.minZ;
      double x2 = box.maxX;
      double y2 = box.maxY;
      double z2 = box.maxZ;

      GL11.glBegin(GL11.GL_LINES);
      line(x1, y1, z1, x2, y1, z1);
      line(x2, y1, z1, x2, y1, z2);
      line(x2, y1, z2, x1, y1, z2);
      line(x1, y1, z2, x1, y1, z1);
      line(x1, y2, z1, x2, y2, z1);
      line(x2, y2, z1, x2, y2, z2);
      line(x2, y2, z2, x1, y2, z2);
      line(x1, y2, z2, x1, y2, z1);
      line(x1, y1, z1, x1, y2, z1);
      line(x2, y1, z1, x2, y2, z1);
      line(x2, y1, z2, x2, y2, z2);
      line(x1, y1, z2, x1, y2, z2);
      GL11.glEnd();

      endWorld();
   }

   public static void outlineBox(AxisAlignedBB box, int color, float lineWidth) {
      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      GlStateManager.pushMatrix();
      GlStateManager.translate(-camX(), -camY(), -camZ());
      GL11.glDisable(3553);
      GL11.glDisable(2896);
      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glLineWidth(lineWidth);
      GL11.glColor4f(r, g, b, a);
      double x1 = box.minX;
      double y1 = box.minY;
      double z1 = box.minZ;
      double x2 = box.maxX;
      double y2 = box.maxY;
      double z2 = box.maxZ;
      GL11.glBegin(1);
      line(x1, y1, z1, x2, y1, z1);
      line(x2, y1, z1, x2, y1, z2);
      line(x2, y1, z2, x1, y1, z2);
      line(x1, y1, z2, x1, y1, z1);
      line(x1, y2, z1, x2, y2, z1);
      line(x2, y2, z1, x2, y2, z2);
      line(x2, y2, z2, x1, y2, z2);
      line(x1, y2, z2, x1, y2, z1);
      line(x1, y1, z1, x1, y2, z1);
      line(x2, y1, z1, x2, y2, z1);
      line(x2, y1, z2, x2, y2, z2);
      line(x1, y1, z2, x1, y2, z2);
      GL11.glEnd();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(2929);
      GL11.glDisable(3042);
      GL11.glEnable(3553);
      GlStateManager.popMatrix();
   }

   private static void line(double x1, double y1, double z1, double x2, double y2, double z2) {
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x2, y2, z2);
   }

   // ---------------------------------------------------------------------
   // Crumb/marker drawing. All in absolute world coords with depth-test off
   // so markers stay visible through terrain.
   // ---------------------------------------------------------------------

   private static void beginWorld(float lineWidth) {
      GlStateManager.pushMatrix();
      GlStateManager.translate(-camX(), -camY(), -camZ());
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glLineWidth(lineWidth);
   }

   private static void endWorld() {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_BLEND);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GlStateManager.popMatrix();
   }

   private static void applyColor(int color) {
      GL11.glColor4f(
            (float)(color >> 16 & 255) / 255.0F,
            (float)(color >> 8 & 255) / 255.0F,
            (float)(color & 255) / 255.0F,
            (float)(color >> 24 & 255) / 255.0F);
   }

   /** Fill the six faces of {@code box}. Use a low-alpha colour -- this is a tint. */
   public static void filledBox(AxisAlignedBB box, int color) {
      beginWorld(1.0F);
      applyColor(color);
      double x1 = box.minX;
      double y1 = box.minY;
      double z1 = box.minZ;
      double x2 = box.maxX;
      double y2 = box.maxY;
      double z2 = box.maxZ;
      GL11.glBegin(GL11.GL_QUADS);
      quad(x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
      quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
      quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
      quad(x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
      quad(x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
      quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
      GL11.glEnd();
      endWorld();
   }

   private static void quad(double ax, double ay, double az, double bx, double by, double bz,
                            double cx, double cy, double cz, double dx, double dy, double dz) {
      GL11.glVertex3d(ax, ay, az);
      GL11.glVertex3d(bx, by, bz);
      GL11.glVertex3d(cx, cy, cz);
      GL11.glVertex3d(dx, dy, dz);
   }

   /**
    * A world-space segment that respects terrain when {@code throughWalls} is
    * false, so it's hidden behind blocks like a real object.
    */
   public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2,
                               int color, float lineWidth, boolean throughWalls) {
      if(throughWalls) {
         drawLine(x1, y1, z1, x2, y2, z2, color, lineWidth);
         return;
      }

      beginWorld(lineWidth);
      // beginWorld clears depth testing for the see-through markers; put it back.
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      applyColor(color);
      GL11.glBegin(GL11.GL_LINES);
      line(x1, y1, z1, x2, y2, z2);
      GL11.glEnd();
      endWorld();
   }

   /**
    * Axis guides that respect terrain when {@code throughWalls} is false.
    */
   public static void axisGuides(AxisAlignedBB box, int color, double reach, float lineWidth,
                                 boolean throughWalls) {
      if(throughWalls) {
         axisGuides(box, color, reach, lineWidth);
         return;
      }

      beginWorld(lineWidth);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      applyColor(color);
      GL11.glBegin(GL11.GL_LINES);

      double[] xs = new double[]{box.minX, box.maxX};
      double[] ys = new double[]{box.minY, box.maxY};
      double[] zs = new double[]{box.minZ, box.maxZ};

      for(double y : ys) {
         for(double z : zs) {
            line(box.minX - reach, y, z, box.maxX + reach, y, z);
         }
      }

      for(double y : ys) {
         for(double x : xs) {
            line(x, y, box.minZ - reach, x, y, box.maxZ + reach);
         }
      }

      GL11.glEnd();
      endWorld();
   }

   /** A single world-space segment. */
   public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2,
                               int color, float lineWidth) {
      beginWorld(lineWidth);
      applyColor(color);
      GL11.glBegin(GL11.GL_LINES);
      line(x1, y1, z1, x2, y2, z2);
      GL11.glEnd();
      endWorld();
   }

   /**
    * Axis-aligned guide rails running through {@code box} and extending
    * {@code reach} blocks past it in both directions on each horizontal axis,
    * drawn off all four corners so they read as parallel rails converging on
    * the target from across the map.
    */
   public static void axisGuides(AxisAlignedBB box, int color, double reach, float lineWidth) {
      beginWorld(lineWidth);
      applyColor(color);
      GL11.glBegin(GL11.GL_LINES);
      double[] xs = new double[]{box.minX, box.maxX};
      double[] ys = new double[]{box.minY, box.maxY};
      double[] zs = new double[]{box.minZ, box.maxZ};
      for (double y : ys) {
         for (double z : zs) {
            line(box.minX - reach, y, z, box.maxX + reach, y, z);
         }
      }
      for (double y : ys) {
         for (double x : xs) {
            line(x, y, box.minZ - reach, x, y, box.maxZ + reach);
         }
      }
      GL11.glEnd();
      endWorld();
   }

   /**
    * A flat ring lying on the XZ plane, centred on (x, y, z). Drawn as a closed
    * line loop so it reads as an ellipse from any angle -- the ground marker
    * under a rally point.
    */
   public static void horizontalCircle(double x, double y, double z, double radius,
                                       int color, float lineWidth, int segments) {
      beginWorld(lineWidth);
      applyColor(color);
      GL11.glBegin(GL11.GL_LINE_LOOP);
      for (int i = 0; i < segments; ++i) {
         double a = 2.0D * Math.PI * (double)i / (double)segments;
         GL11.glVertex3d(x + Math.cos(a) * radius, y, z + Math.sin(a) * radius);
      }
      GL11.glEnd();
      endWorld();
   }

   /**
    * A beacon-style column at (x, z) rising {@code height} blocks from {@code y}.
    *
    * <p>Shared by waypoints and rally markers so both look identical and gain
    * new options together. Cull is disabled so the inside of the column is
    * visible when you're stood in it, and the depth mask is cleared so
    * overlapping beams blend instead of z-fighting.
    */
   public static void beam(double x, double y, double z, double height, double width,
                           int color, boolean throughWalls) {
      double h = width / 2.0D;

      GlStateManager.pushMatrix();
      GlStateManager.translate(x - camX(), y - camY(), z - camZ());
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GlStateManager.disableCull();
      GlStateManager.depthMask(false);
      if(throughWalls) {
         GlStateManager.disableDepth();
      }

      applyColor(color);

      GL11.glBegin(GL11.GL_QUADS);
      // Four walls of the column.
      quad(-h, 0.0D, -h, -h, height, -h, h, height, -h, h, 0.0D, -h);
      quad(h, 0.0D, h, h, height, h, -h, height, h, -h, 0.0D, h);
      quad(-h, 0.0D, h, -h, height, h, -h, height, -h, -h, 0.0D, -h);
      quad(h, 0.0D, -h, h, height, -h, h, height, h, h, 0.0D, h);
      GL11.glEnd();

      if(throughWalls) {
         GlStateManager.enableDepth();
      }

      GlStateManager.depthMask(true);
      GlStateManager.enableCull();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   /** A filled dot at a world position -- the end cap for a tether line. */
   public static void drawPoint(double x, double y, double z, int color, float size) {
      beginWorld(1.0F);
      applyColor(color);
      GL11.glEnable(GL11.GL_POINT_SMOOTH);
      GL11.glPointSize(size);
      GL11.glBegin(GL11.GL_POINTS);
      GL11.glVertex3d(x, y, z);
      GL11.glEnd();
      GL11.glDisable(GL11.GL_POINT_SMOOTH);
      endWorld();
   }

   public static void text3d(String text, double x, double y, double z, int color, float scale) {
      Minecraft mc = Minecraft.getMinecraft();
      FontRenderer font = mc.fontRendererObj;
      RenderManager r = rm();
      GlStateManager.pushMatrix();
      GlStateManager.translate(x - camX(), y - camY(), z - camZ());
      GlStateManager.rotate(-r.playerViewY, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(r.playerViewX, 1.0F, 0.0F, 0.0F);
      GlStateManager.scale(-scale, -scale, scale);
      GlStateManager.disableLighting();
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      int w = font.getStringWidth(text);
      font.drawString(text, -w / 2, 0, color);
      GlStateManager.enableDepth();
      GlStateManager.enableLighting();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }
}
