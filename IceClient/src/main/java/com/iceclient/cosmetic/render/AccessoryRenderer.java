package com.iceclient.cosmetic.render;

import com.iceclient.cosmetic.Cosmetic;
import com.iceclient.cosmetic.CosmeticManager;
import com.iceclient.cosmetic.CosmeticType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hats, wings, pets and trails.
 *
 * <p>All drawn as untextured geometry in a single flat colour. At the size these
 * appear on screen a clean silhouette reads far better than a texture a handful
 * of pixels across, and it means adding a cosmetic is a shape and a colour
 * rather than another PNG to draw.
 *
 * <p>Like the cape, this hangs off {@link RenderPlayerEvent.Post} and rebuilds
 * the model transform by hand, because the event fires after the renderer has
 * popped its matrix.
 */
public final class AccessoryRenderer {

   private static final float PLAYER_SCALE = 0.9375F;
   private static final float MODEL_Y = -1.5078125F;

   /** Top of the head in this model space; see the note in drawHat. */
   private static final float HEAD_TOP = -0.42F;

   /** Recent positions per player, for trails. Bounded, and cleared on world change. */
   private final Map<String, List<double[]>> trails = new HashMap<String, List<double[]>>();
   private static final int TRAIL_POINTS = 22;

   @SubscribeEvent
   public void onRenderPlayer(RenderPlayerEvent.Post event) {
      EntityPlayer p = event.entityPlayer;
      if(p == null || p.isInvisible()) {
         return;
      }

      float pt = event.partialRenderTick;
      Cosmetic hat = worn(p, CosmeticType.HAT);
      Cosmetic wings = worn(p, CosmeticType.WINGS);
      Cosmetic trail = worn(p, CosmeticType.TRAIL);

      if(trail != null) {
         // WORLD coordinates, not event.x/y/z -- those are relative to the
         // camera, so storing them meant every point in the trail moved as soon
         // as the camera did and the whole thing smeared. The camera offset is
         // applied at draw time instead, where it is current.
         recordTrail(p, p.posX, p.posY, p.posZ);
         drawTrail(p, trail);
      }

      // Pets are not handled here any more: they walk on the ground with their
      // own position and heading, which cannot be expressed inside the player's
      // transform. See PetRenderer.
      if(hat == null && wings == null) {
         return;
      }

      GlStateManager.pushMatrix();
      GlStateManager.disableTexture2D();
      GlStateManager.disableLighting();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);

      // Culling off. These are hand-built shapes with one winding order, so with
      // culling on every face pointing away from the camera vanishes -- which is
      // why the wings looked absent and the hats looked like they were inside
      // the head. Two-sided is correct for thin geometry anyway.
      GlStateManager.disableCull();

      float bodyYaw = interpolate(p.prevRenderYawOffset, p.renderYawOffset, pt);
      GlStateManager.translate(event.x, event.y, event.z);
      GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.scale(-1.0F, -1.0F, 1.0F);
      GlStateManager.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
      GlStateManager.translate(0.0F, MODEL_Y, 0.0F);

      if(hat != null) {
         drawHat(hat, p, pt);
      }

      if(wings != null) {
         drawWings(wings, p, pt);
      }

      GlStateManager.enableCull();
      GlStateManager.disableBlend();
      GlStateManager.enableLighting();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   // ------------------------------------------------------------------
   // pieces
   // ------------------------------------------------------------------

   /** Head is 8 units tall topping out at y=0 in model space, so hats sit above that. */
   private void drawHat(Cosmetic c, EntityPlayer p, float pt) {
      GlStateManager.pushMatrix();

      // Follow the head, not the body, so a hat turns when you look around.
      float headYaw = interpolate(p.prevRotationYawHead, p.rotationYawHead, pt)
            - interpolate(p.prevRenderYawOffset, p.renderYawOffset, pt);
      float headPitch = interpolate(p.prevRotationPitch, p.rotationPitch, pt);

      GlStateManager.rotate(headYaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(headPitch, 1.0F, 0.0F, 0.0F);

      // Worked out rather than guessed at, since guessing put hats a metre up:
      // this space has its origin 1.41 blocks above the feet with +y pointing
      // DOWN and one unit = 0.9375 blocks. The head tops out at 1.8 blocks, so
      // the crown of the head is (1.8 - 1.41) / 0.9375 = 0.42 units negative.
      GlStateManager.translate(0.0F, HEAD_TOP, 0.0F);

      setColor(c.getColor(), 1.0F);

      if("hat_crown".equals(c.getId())) {
         // Band sitting on the head, spikes rising from it.
         box(0.34F, 0.05F, 0.34F);

         for(int i = 0; i < 5; ++i) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(i * 72.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, -0.05F, -0.13F);
            GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
            spike(0.07F, 0.20F);
            GlStateManager.popMatrix();
         }
      } else if("hat_beanie".equals(c.getId())) {
         // Pulled down over the head a little, the way a beanie sits.
         GlStateManager.translate(0.0F, 0.07F, 0.0F);
         box(0.56F, 0.16F, 0.56F);
         GlStateManager.translate(0.0F, -0.16F, 0.0F);
         box(0.46F, 0.10F, 0.46F);
         GlStateManager.translate(0.0F, -0.10F, 0.0F);
         box(0.12F, 0.07F, 0.12F);      // bobble
      } else if("hat_halo".equals(c.getId())) {
         GlStateManager.translate(0.0F, -0.22F, 0.0F);
         ring(0.26F, 0.04F);
      }

      GlStateManager.popMatrix();
   }

   /**
    * Feathered wings on the upper back.
    *
    * <p>Built as rows of individual feathers rather than one silhouette. A
    * single flat sheet reads as a shape cut from paper -- it disappears edge-on
    * and has no depth from any other angle. Three staggered rows, each feather
    * swept a little further back than the last, is what gives the layered look
    * of a real wing, and it still costs only a few dozen triangles.
    */
   private void drawWings(Cosmetic c, EntityPlayer p, float pt) {
      GlStateManager.pushMatrix();
      // Upper back: a little below the neck (+y is down here) and behind it.
      // The cape sits at z = +0.125, which is what establishes +z as "back".
      // The body's back surface is at z = +0.125 in this space (its box is 4
      // units deep, scaled by 0.0625). Roots go behind that, or the feathers
      // start inside the chest.
      GlStateManager.translate(0.0F, 0.14F, 0.20F);

      float speed = (float)Math.min(0.35D,
            Math.sqrt(p.motionX * p.motionX + p.motionZ * p.motionZ));
      float beat = (float)Math.sin((p.ticksExisted + pt) * 0.18D);
      float flap = beat * (5.0F + speed * 70.0F);
      float open = 30.0F + speed * 45.0F + flap;

      int rgb = c.getColor();

      for(int side = -1; side <= 1; side += 2) {
         featheredWing(side, rgb, beat, open);
      }

      GlStateManager.popMatrix();
   }

   /**
    * One wing: three rows of feathers fanning up and out from the shoulder.
    *
    * <p>Every feather's tip is computed directly rather than reached by rotating
    * the matrix. Two earlier attempts put the wings through the player's chest
    * because this model space has X and Y flipped, which reverses what
    * {@code glRotate} does about those axes -- and getting that sign wrong is
    * invisible until you look at the model. Building the vertices from explicit
    * offsets removes the convention entirely: {@code BACK} is positive z because
    * that is where the cape hangs, and up is negative y because the space is
    * flipped. Both are stated once, here.
    */
   private static void featheredWing(int side, int rgb, float beat, float open) {
      // row -> { count, spread degrees, base length, depth, shade }
      // Lengths are in this space's units, where 1 = 0.9375 blocks -- so the
      // longest primaries reach about a block out from the shoulder, roughly a
      // player's height across the pair. The first pass was half this and read
      // as a shrug rather than a wingspan.
      float[][] rows = new float[][]{
            {9.0F, 76.0F, 1.15F, 0.00F, 1.00F},
            {7.0F, 64.0F, 0.82F, 0.05F, 0.87F},
            {6.0F, 50.0F, 0.55F, 0.10F, 0.74F}};

      // How far the whole wing lies back rather than out to the side. Driven by
      // the flap, so the wings sweep back as they beat.
      float sweep = 0.42F + (open - 30.0F) / 160.0F;

      for(float[] row : rows) {
         int count = (int)row[0];
         float spread = row[1];
         float length = row[2];
         float depth = row[3];
         float shade = row[4];

         setColor(shade(rgb, shade), 0.95F);

         for(int i = 0; i < count; ++i) {
            float t = count == 1 ? 0.0F : (float)i / (float)(count - 1);

            // Fan from swept-down at the outside to raised near the shoulder.
            float ang = (float)Math.toRadians(-26.0D + t * spread + beat * 3.0D * t);
            float len = length * (0.64F + 0.36F * (float)Math.sin(t * Math.PI));

            float ux = (float)Math.cos(ang);      // outward
            float uy = -(float)Math.sin(ang);     // up (negated: +y is down)

            feather(side * 0.06F, 0.0F, depth,
                  side * ux * len, uy * len, depth + len * sweep,
                  0.085F + 0.05F * (1.0F - t));
         }
      }
   }

   /**
    * One feather, from a root to a tip in 3D.
    *
    * @param w half-width at the base; the shape tapers to a point at the tip
    */
   private static void feather(float rx, float ry, float rz,
                               float tx, float ty, float tz, float w) {
      // Widen across the axis the feather is least aligned with, so a feather
      // pointing straight up is still broad rather than edge-on.
      float dx = tx - rx;
      float dy = ty - ry;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      if(len < 1.0E-4F) {
         return;
      }

      float px = -dy / len * w;
      float py = dx / len * w;

      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(6, DefaultVertexFormats.POSITION);   // fan

      wr.pos(rx, ry, rz).endVertex();
      wr.pos(rx + px * 0.6F, ry + py * 0.6F, rz).endVertex();
      wr.pos(rx + dx * 0.45F + px, ry + dy * 0.45F + py, rz + (tz - rz) * 0.45F).endVertex();
      wr.pos(rx + dx * 0.80F + px * 0.7F, ry + dy * 0.80F + py * 0.7F, rz + (tz - rz) * 0.80F).endVertex();
      wr.pos(tx, ty, tz).endVertex();
      wr.pos(rx + dx * 0.80F - px * 0.5F, ry + dy * 0.80F - py * 0.5F, rz + (tz - rz) * 0.80F).endVertex();
      wr.pos(rx + dx * 0.40F - px * 0.8F, ry + dy * 0.40F - py * 0.8F, rz + (tz - rz) * 0.40F).endVertex();
      wr.pos(rx - px * 0.6F, ry - py * 0.6F, rz).endVertex();

      tess.draw();
   }

   private static int shade(int rgb, float f) {
      int r = (int)((rgb >> 16 & 255) * f);
      int g = (int)((rgb >> 8 & 255) * f);
      int b = (int)((rgb & 255) * f);
      return r << 16 | g << 8 | b;
   }


   // ------------------------------------------------------------------
   // trails
   // ------------------------------------------------------------------

   private void recordTrail(EntityPlayer p, double x, double y, double z) {
      String key = p.getName();
      List<double[]> pts = this.trails.get(key);
      if(pts == null) {
         pts = new ArrayList<double[]>();
         this.trails.put(key, pts);
      }

      // Only record when actually moving, or standing still fills the buffer
      // with a single point and the trail becomes a blob.
      if(!pts.isEmpty()) {
         double[] last = pts.get(pts.size() - 1);
         double dx = last[0] - x;
         double dy = last[1] - y;
         double dz = last[2] - z;
         if(dx * dx + dy * dy + dz * dz < 0.02D) {
            return;
         }
      }

      pts.add(new double[]{x, y, z});
      while(pts.size() > TRAIL_POINTS) {
         pts.remove(0);
      }
   }

   /** Fading ribbon through the recorded points. */
   private void drawTrail(EntityPlayer p, Cosmetic c) {
      List<double[]> pts = this.trails.get(p.getName());
      if(pts == null || pts.size() < 2) {
         return;
      }

      GlStateManager.pushMatrix();
      GlStateManager.disableTexture2D();
      GlStateManager.disableLighting();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.depthMask(false);

      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(5, DefaultVertexFormats.POSITION_COLOR);

      int rgb = c.getColor();
      float r = (rgb >> 16 & 255) / 255.0F;
      float g = (rgb >> 8 & 255) / 255.0F;
      float b = (rgb & 255) / 255.0F;

      // Points are stored in world space, so the current camera position comes
      // off here rather than being baked in when they were recorded.
      net.minecraft.client.renderer.entity.RenderManager rm =
            Minecraft.getMinecraft().getRenderManager();

      for(int i = 0; i < pts.size(); ++i) {
         double[] q = pts.get(i);
         float t = (float)i / (float)(pts.size() - 1);
         float alpha = t * 0.55F;              // oldest points faintest
         float w = 0.10F + t * 0.16F;

         double x = q[0] - rm.viewerPosX;
         double y = q[1] - rm.viewerPosY;
         double z = q[2] - rm.viewerPosZ;

         wr.pos(x, y + 1.0D - w, z).color(r, g, b, alpha).endVertex();
         wr.pos(x, y + 1.0D + w, z).color(r, g, b, alpha).endVertex();
      }

      tess.draw();

      GlStateManager.depthMask(true);
      GlStateManager.disableBlend();
      GlStateManager.enableLighting();
      GlStateManager.enableTexture2D();
      GlStateManager.popMatrix();
   }

   // ------------------------------------------------------------------
   // primitives
   // ------------------------------------------------------------------

   private static void setColor(int rgb, float alpha) {
      GlStateManager.color((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F,
            (rgb & 255) / 255.0F, alpha);
   }

   private static void box(float w, float h, float d) {
      float x = w / 2.0F;
      float z = d / 2.0F;
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION);

      float[][] faces = new float[][]{
            {-x, 0, -z, x, 0, -z, x, -h, -z, -x, -h, -z},
            {-x, 0, z, -x, -h, z, x, -h, z, x, 0, z},
            {-x, 0, -z, -x, -h, -z, -x, -h, z, -x, 0, z},
            {x, 0, -z, x, 0, z, x, -h, z, x, -h, -z},
            {-x, -h, -z, x, -h, -z, x, -h, z, -x, -h, z},
            {-x, 0, -z, -x, 0, z, x, 0, z, x, 0, -z}};

      for(float[] f : faces) {
         for(int i = 0; i < 12; i += 3) {
            wr.pos(f[i], f[i + 1], f[i + 2]).endVertex();
         }
      }

      tess.draw();
   }

   /** Four-sided taper, base at the origin pointing up. */
   private static void spike(float base, float height) {
      float b = base / 2.0F;
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(4, DefaultVertexFormats.POSITION);

      float[][] corners = new float[][]{{-b, -b}, {b, -b}, {b, b}, {-b, b}};

      for(int i = 0; i < 4; ++i) {
         float[] c1 = corners[i];
         float[] c2 = corners[(i + 1) % 4];
         wr.pos(c1[0], 0, c1[1]).endVertex();
         wr.pos(c2[0], 0, c2[1]).endVertex();
         wr.pos(0, -height, 0).endVertex();
      }

      tess.draw();
   }

   private static void ring(float radius, float thickness) {
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(5, DefaultVertexFormats.POSITION);

      for(int a = 0; a <= 360; a += 12) {
         double rad = Math.toRadians(a);
         float cx = (float)Math.cos(rad);
         float cz = (float)Math.sin(rad);
         wr.pos(cx * radius, 0, cz * radius).endVertex();
         wr.pos(cx * (radius - thickness), 0, cz * (radius - thickness)).endVertex();
      }

      tess.draw();
   }

   /**
    * A wing silhouette, filled as a fan from the shoulder.
    *
    * <p>Traced as an outline rather than assembled from separate triangles: the
    * first version fanned three "fingers" from one point and produced
    * overlapping slivers that read as a solid arrowhead. An outline with a
    * notched trailing edge is what makes it look like a wing from any angle.
    *
    * <p>Coordinates are (outward, up). Model +y is down here, so they are
    * negated on the way out.
    */
   private static void wingSheet(int side) {
      // Leading edge sweeping out to the tip, then a scalloped trailing edge
      // coming back to the root -- the notches are what read as feathers.
      float[][] outline = new float[][]{
            {0.04F, 0.14F},
            {0.30F, 0.30F},
            {0.58F, 0.34F},
            {0.82F, 0.22F},   // tip
            {0.70F, 0.06F},
            {0.56F, 0.12F},
            {0.50F, -0.06F},
            {0.36F, 0.02F},
            {0.30F, -0.16F},
            {0.18F, -0.04F},
            {0.10F, -0.20F},
            {0.02F, -0.06F}};

      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();

      wr.begin(6, DefaultVertexFormats.POSITION);   // GL_TRIANGLE_FAN
      wr.pos(0, 0, 0).endVertex();

      for(float[] pt : outline) {
         wr.pos(side * pt[0], -pt[1], 0).endVertex();
      }

      tess.draw();
   }

   // ------------------------------------------------------------------

   private static Cosmetic worn(EntityPlayer p, CosmeticType type) {
      if(p == Minecraft.getMinecraft().thePlayer) {
         return CosmeticManager.getEquippedItem(type);
      }

      return CosmeticManager.getRemote(p.getName(), type);
   }

   private static float interpolate(float prev, float now, float pt) {
      float d = now - prev;

      while(d < -180.0F) {
         d += 360.0F;
      }

      while(d >= 180.0F) {
         d -= 360.0F;
      }

      return prev + pt * d;
   }
}
