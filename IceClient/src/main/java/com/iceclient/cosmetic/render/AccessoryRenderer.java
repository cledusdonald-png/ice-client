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
         recordTrail(p, event.x, event.y, event.z);
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

   /** Two angled sheets on the back, opening and closing as you move. */
   private void drawWings(Cosmetic c, EntityPlayer p, float pt) {
      GlStateManager.pushMatrix();
      // Upper back: a little below the neck (+y is down here) and behind it.
      // The cape sits at z = +0.125, which is what establishes +z as "back".
      GlStateManager.translate(0.0F, 0.16F, 0.14F);

      float speed = (float)Math.min(0.35D,
            Math.sqrt(p.motionX * p.motionX + p.motionZ * p.motionZ));
      float flap = (float)Math.sin((p.ticksExisted + pt) * 0.35D) * (4.0F + speed * 90.0F);
      float open = 22.0F + speed * 70.0F + flap;

      setColor(c.getColor(), 0.82F);

      for(int side = -1; side <= 1; side += 2) {
         GlStateManager.pushMatrix();
         GlStateManager.rotate(side * open, 0.0F, 1.0F, 0.0F);
         GlStateManager.rotate(-14.0F, 1.0F, 0.0F, 0.0F);
         wingSheet(side);
         GlStateManager.popMatrix();
      }

      GlStateManager.popMatrix();
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

      for(int i = 0; i < pts.size(); ++i) {
         double[] q = pts.get(i);
         float t = (float)i / (float)(pts.size() - 1);
         float alpha = t * 0.55F;              // oldest points faintest
         float w = 0.10F + t * 0.16F;

         wr.pos(q[0], q[1] + 1.0D - w, q[2]).color(r, g, b, alpha).endVertex();
         wr.pos(q[0], q[1] + 1.0D + w, q[2]).color(r, g, b, alpha).endVertex();
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
