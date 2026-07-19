package com.iceclient.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public final class BoxBatch {
   private final List<BoxBatch.Box> boxes = new ArrayList();

   public BoxBatch() {
   }

   public boolean isEmpty() {
      return this.boxes.isEmpty();
   }

   public void add(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float fillA, float lineA) {
      BoxBatch.Box box = new BoxBatch.Box();
      box.x0 = x0;
      box.y0 = y0;
      box.z0 = z0;
      box.x1 = x1;
      box.y1 = y1;
      box.z1 = z1;
      box.r = r;
      box.g = g;
      box.b = b;
      box.fillA = fillA;
      box.lineA = lineA;
      this.boxes.add(box);
   }

   public void flush(float lineWidth, boolean throughWalls) {
      if(!this.boxes.isEmpty()) {
         GlStateManager.pushMatrix();
         GlStateManager.disableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.blendFunc(770, 771);
         GlStateManager.depthMask(false);
         if(throughWalls) {
            GlStateManager.disableDepth();
         }

         GL11.glBegin(7);

         for(BoxBatch.Box b : this.boxes) {
            GL11.glColor4f(b.r, b.g, b.b, b.fillA);
            this.fillQuads(b);
         }

         GL11.glEnd();
         GL11.glEnable(2848);
         GL11.glLineWidth(lineWidth);
         GL11.glBegin(1);

         for(BoxBatch.Box b : this.boxes) {
            GL11.glColor4f(b.r, b.g, b.b, b.lineA);
            this.edgeLines(b);
         }

         GL11.glEnd();
         GL11.glDisable(2848);
         GlStateManager.depthMask(true);
         if(throughWalls) {
            GlStateManager.enableDepth();
         }

         GlStateManager.disableBlend();
         GlStateManager.enableTexture2D();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.popMatrix();
         this.boxes.clear();
      }
   }

   private void fillQuads(BoxBatch.Box b) {
      double x0 = b.x0;
      double y0 = b.y0;
      double z0 = b.z0;
      double x1 = b.x1;
      double y1 = b.y1;
      double z1 = b.z1;
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x1, y0, z1);
   }

   private void edgeLines(BoxBatch.Box b) {
      double x0 = b.x0;
      double y0 = b.y0;
      double z0 = b.z0;
      double x1 = b.x1;
      double y1 = b.y1;
      double z1 = b.z1;
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y1, z1);
   }

   private static final class Box {
      double x0;
      double y0;
      double z0;
      double x1;
      double y1;
      double z1;
      float r;
      float g;
      float b;
      float fillA;
      float lineA;

      private Box() {
      }
   }
}
