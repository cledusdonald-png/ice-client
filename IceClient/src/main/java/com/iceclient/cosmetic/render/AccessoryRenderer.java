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

      // Walking pets live in PetRenderer -- they have their own position and
      // heading, which cannot be expressed inside the player's transform. A
      // perched one is the opposite: it is attached to the body, so it belongs
      // here where that transform already exists.
      com.iceclient.cosmetic.PetModel perched = perchedPet(p);

      if(hat == null && wings == null && perched == null) {
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

      if(perched != null) {
         drawPerchedPet(perched, p, pt);
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
      } else if("hat_tophat".equals(c.getId())) {
         box(0.62F, 0.03F, 0.62F);                    // brim
         GlStateManager.translate(0.0F, -0.03F, 0.0F);
         box(0.40F, 0.34F, 0.40F);                    // crown
         setColor(0x8FD4E8, 1.0F);
         GlStateManager.translate(0.0F, -0.24F, 0.0F);
         box(0.42F, 0.05F, 0.42F);                    // band
      } else if("hat_visor".equals(c.getId())) {
         box(0.50F, 0.05F, 0.50F);
         GlStateManager.translate(0.0F, -0.01F, -0.22F);
         box(0.44F, 0.03F, 0.22F);                    // brim, forward only
      } else if("hat_horns".equals(c.getId())) {
         for(int s = -1; s <= 1; s += 2) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(s * 0.17F, 0.02F, 0.0F);
            // Curved by stacking shrinking segments, each leaning further back.
            for(int i = 0; i < 5; ++i) {
               float f = 1.0F - i * 0.16F;
               box(0.11F * f, 0.09F, 0.11F * f);
               GlStateManager.translate(s * 0.018F, -0.085F, 0.030F);
               GlStateManager.rotate(s * 4.0F, 0.0F, 0.0F, 1.0F);
            }
            GlStateManager.popMatrix();
         }
      } else if("hat_antlers".equals(c.getId())) {
         for(int s = -1; s <= 1; s += 2) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(s * 0.13F, 0.0F, 0.0F);

            // Main beam, leaning out and back.
            for(int i = 0; i < 4; ++i) {
               box(0.06F, 0.10F, 0.06F);
               GlStateManager.translate(s * 0.045F, -0.095F, 0.022F);
            }

            // Two tines off the beam.
            GlStateManager.pushMatrix();
            GlStateManager.rotate(s * 42.0F, 0.0F, 0.0F, 1.0F);
            box(0.05F, 0.16F, 0.05F);
            GlStateManager.popMatrix();

            GlStateManager.translate(s * 0.02F, -0.09F, 0.0F);
            GlStateManager.rotate(-s * 30.0F, 0.0F, 0.0F, 1.0F);
            box(0.05F, 0.14F, 0.05F);
            GlStateManager.popMatrix();
         }
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
      String id = c.getId();

      for(int side = -1; side <= 1; side += 2) {
         if("wings_dragon".equals(id)) {
            batWing(side, rgb, beat, open);
         } else if("wings_crystal".equals(id)) {
            crystalWing(side, rgb, beat, open);
         } else if("wings_butterfly".equals(id)) {
            butterflyWing(side, rgb, beat, open);
         } else if("wings_mech".equals(id)) {
            mechWing(side, rgb, beat, open);
         } else if("wings_ethereal".equals(id)) {
            etherealWing(side, rgb, beat, open);
         } else {
            featheredWing(side, rgb, beat, open);
         }
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
      // Three rows, back to front, each shorter and paler than the one behind.
      // { count, start along spine, end along spine, length, depth, shade }
      //
      // Counts are high and the spans overlap on purpose: a feather here is a
      // flat quad, so what makes a wing look feathered is many of them lying
      // over each other like roof tiles. The first versions used six or seven
      // spaced apart, which is a comb, not a wing.
      float[][] rows = new float[][]{
            {14.0F, 0.10F, 1.00F, 0.86F, 0.000F, 1.00F},
            {12.0F, 0.08F, 0.84F, 0.58F, 0.035F, 0.88F},
            {10.0F, 0.06F, 0.66F, 0.36F, 0.070F, 0.76F}};

      for(float[] row : rows) {
         int count = (int)row[0];

         for(int i = 0; i < count; ++i) {
            float t = count == 1 ? 0.0F : (float)i / (float)(count - 1);
            float u = row[1] + (row[2] - row[1]) * t;

            // Each feather a shade off its neighbour, so the overlaps are
            // visible as separate feathers rather than one solid mass.
            float alt = i % 2 == 0 ? 1.0F : 0.93F;
            setColor(shade(rgb, row[5] * alt), 0.97F);

            float[] root = spinePoint(side, u, beat, open, row[4]);

            // Longest in the outer third rather than the middle -- the widest
            // part of a wing is past halfway, and that is what stops it reading
            // as a symmetrical leaf.
            float bias = (float)Math.sin(Math.pow(t, 0.72D) * Math.PI);
            float len = row[3] * (0.42F + 0.58F * bias);

            float[] tip = trailingPoint(root, side, len, beat, open, u);

            // Wide enough to overlap the next feather along.
            feather(root[0], root[1], root[2], tip[0], tip[1], tip[2],
                  0.085F + 0.045F * (1.0F - t));
         }
      }
   }

   /**
    * A point along the wing's leading edge, {@code u} from 0 at the shoulder to
    * 1 at the tip.
    *
    * <p>Wings are built on a curve rather than a fan. Every earlier version
    * radiated straight feathers out of a single point at the shoulder, which
    * reads as a spiky fan from any angle -- a real wing has a spine that arcs up
    * and out, with feathers hanging along its length. Putting that arc in one
    * function also means the four wing types share a skeleton and differ only in
    * what they hang off it.
    */
   private static float[] spinePoint(int side, float u, float beat, float open, float depth) {
      // Rises steeply out of the shoulder before spreading. Wings that leave the
      // back at a shallow angle read as arms held out; the height is what makes
      // the silhouette a wing.
      float lift = (float)Math.toRadians(46.0D + open * 0.30D + beat * 7.0D * u);
      float curl = u * u * 0.55F;          // the tip curls up and forward
      float reach = 1.24F * u;

      return new float[]{
            side * (reach * (float)Math.cos(lift) + curl * 0.06F),
            -(reach * (float)Math.sin(lift) + curl * 0.42F),
            depth + reach * (0.18F + 0.16F * u)};
   }

   /** Where something hanging off the spine at {@code root} ends up. */
   private static float[] trailingPoint(float[] root, int side, float len,
                                        float beat, float open, float u) {
      // Trailing edge sweeps back along the body and downward, swinging with
      // the beat and further out toward the tip.
      float drop = (float)Math.toRadians(62.0D - open * 0.20D - beat * 7.0D * u);

      return new float[]{
            root[0] + side * len * (float)Math.cos(drop) * 0.30F,
            root[1] + len * (float)Math.sin(drop),
            root[2] + len * 0.30F};
   }

   /**
    * Bat wings: membrane webbed between long fingers.
    *
    * <p>Built as the panels <em>between</em> adjacent fingers rather than as
    * separate feathers, which is what makes it read as one continuous skin with
    * a scalloped trailing edge instead of a fan.
    */
   private static void batWing(int side, int rgb, float beat, float open) {
      int fingers = 5;
      float[][] tip = new float[fingers][];

      // Fingers radiate from the shoulder to points spaced along the spine, each
      // overshooting it, so the membrane between them scallops the way skin does.
      for(int i = 0; i < fingers; ++i) {
         float t = (float)i / (float)(fingers - 1);
         float[] s = spinePoint(side, 0.35F + t * 0.65F, beat, open, 0.0F);
         float reach = 1.0F + 0.30F * (float)Math.sin((0.2D + t * 0.7D) * Math.PI);

         tip[i] = new float[]{s[0] * reach, s[1] * reach + t * 0.16F, s[2] * reach};
      }

      setColor(shade(rgb, 0.78F), 0.93F);
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();

      for(int i = 0; i < fingers - 1; ++i) {
         wr.begin(6, DefaultVertexFormats.POSITION);
         wr.pos(side * 0.06F, 0.0F, 0.0F).endVertex();
         wr.pos(tip[i][0], tip[i][1], tip[i][2]).endVertex();
         // Mid point pulled in and down: that sag is what makes it read as skin
         // stretched between bones rather than a flat panel.
         wr.pos((tip[i][0] + tip[i + 1][0]) * 0.44F,
               (tip[i][1] + tip[i + 1][1]) * 0.44F + 0.13F,
               (tip[i][2] + tip[i + 1][2]) * 0.44F).endVertex();
         wr.pos(tip[i + 1][0], tip[i + 1][1], tip[i + 1][2]).endVertex();
         tess.draw();
      }

      // Fingers drawn over the membrane, so the ribs stay readable.
      setColor(shade(rgb, 1.35F), 1.0F);
      for(int i = 0; i < fingers; ++i) {
         feather(side * 0.05F, 0.0F, 0.0F, tip[i][0], tip[i][1], tip[i][2], 0.030F);
      }
   }

   /** Crystal wings: a handful of hard angular shards rather than a fan. */
   private static void crystalWing(int side, int rgb, float beat, float open) {
      int shards = 7;

      for(int i = 0; i < shards; ++i) {
         float t = (float)i / (float)(shards - 1);
         float u = 0.18F + t * 0.82F;

         float[] root = spinePoint(side, u, beat, open, 0.0F);

         // Shards point outward past the spine rather than hanging back from
         // it -- hard and radiating, the opposite of a feather's droop.
         float grow = 1.20F + 0.30F * (float)Math.sin(t * Math.PI);
         float[] tip = new float[]{root[0] * grow, root[1] * grow - 0.05F, root[2] * grow};

         // Alternating brightness keeps adjacent shards distinguishable without
         // any lighting to separate them.
         setColor(shade(rgb, i % 2 == 0 ? 1.15F : 0.80F), 0.90F);
         shard(side * 0.05F, 0.0F, 0.0F, tip[0], tip[1], tip[2],
               0.085F + 0.055F * (1.0F - t));
      }
   }

   /**
    * The pet to draw on this player's shoulder, or null if theirs walks.
    *
    * <p>The user's choice wins; a pet's own {@code perchByDefault} only decides
    * what happens before anyone has expressed one.
    */
   static com.iceclient.cosmetic.PetModel perchedPet(EntityPlayer p) {
      com.iceclient.cosmetic.PetModel m = petModelFor(p);
      if(m == null) {
         return null;
      }

      return CosmeticManager.isPetOnShoulder() || m.perchByDefault ? m : null;
   }

   /** Resolves a player's pet to a model, custom or catalogue. */
   static com.iceclient.cosmetic.PetModel petModelFor(EntityPlayer p) {
      if(p == Minecraft.getMinecraft().thePlayer
            && CosmeticManager.getCustomPet() != null) {
         return com.iceclient.cosmetic.CustomPets.get(CosmeticManager.getCustomPet());
      }

      Cosmetic c = worn(p, CosmeticType.PET);
      return c == null ? null : BuiltInPets.get(c.getId());
   }

   /**
    * Draws a pet sitting on the player's right shoulder.
    *
    * <p>Rides the body rather than the head, so it stays put while you look
    * around -- a pet that swung with the camera would read as attached to the
    * face. It leans into turns and settles with a slow breath, which is what
    * stops it looking welded on.
    */
   private void drawPerchedPet(com.iceclient.cosmetic.PetModel m, EntityPlayer p, float pt) {
      GlStateManager.pushMatrix();

      // Outboard of the head, on top of the arm.
      //
      // The head is 8 units wide, so it reaches 0.25 either side of centre, and
      // a pet roughly 0.3 wide centred at 0.30 still overlapped it by a third --
      // which is the clipping. Centre goes to 0.42, clear of the head entirely,
      // and down to shoulder level rather than up alongside the jaw.
      GlStateManager.translate(-0.42F, 0.04F, 0.02F);

      // Lean against the turn, and breathe.
      float turn = wrapDeg(p.renderYawOffset - p.prevRenderYawOffset);
      float lean = Math.max(-14.0F, Math.min(14.0F, turn * 0.8F));
      float breathe = (float)Math.sin((p.ticksExisted + pt) * 0.08D) * 0.008F;

      GlStateManager.rotate(lean, 0.0F, 0.0F, 1.0F);
      GlStateManager.translate(0.0F, breathe, 0.0F);

      // Face forward: the pet's +z is its front, and -z is the player's front
      // in this space, so it needs turning about.
      GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

      // Smaller than on the ground: at full size a pet is as tall as the head,
      // which looks less like a companion and more like a growth.
      float s = m.scale * 0.58F;
      GlStateManager.scale(s, s, s);

      for(com.iceclient.cosmetic.PetModel.Part part : m.parts) {
         GlStateManager.pushMatrix();
         // Pet models are built +y up; this space is +y down.
         GlStateManager.translate(part.x, -part.y, part.z);
         setColor(part.color, part.alpha);

         if("spike".equals(part.shape)) {
            if(!part.down) {
               GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
            }
            spike(part.w, part.h);
         } else {
            box(part.w, part.h, part.d);
         }

         GlStateManager.popMatrix();
      }

      GlStateManager.popMatrix();
   }

   private static float wrapDeg(float d) {
      while(d < -180.0F) {
         d += 360.0F;
      }

      while(d >= 180.0F) {
         d -= 360.0F;
      }

      return d;
   }

   /**
    * Butterfly: two broad rounded panels a side, with spots.
    *
    * <p>Panels rather than a fan, and rounded rather than pointed -- the whole
    * read comes from having a large continuous area, which is the opposite of
    * every other wing here.
    */
   private static void butterflyWing(int side, int rgb, float beat, float open) {
      // Upper panel reaches along the spine; the lower one is shorter and hangs
      // below it, which is the shape that says butterfly rather than bird.
      // { u along spine, panel half-height, depth, shade, drop }
      float[][] panels = new float[][]{
            {0.92F, 0.46F, 0.00F, 1.00F, 0.00F},
            {0.62F, 0.34F, 0.05F, 0.78F, 0.34F}};

      for(float[] p : panels) {
         float[] c = spinePoint(side, p[0], beat, open, p[2]);
         float cx = c[0];
         float cy = c[1] + p[4];
         float cz = c[2];

         setColor(shade(rgb, p[3]), 0.91F);
         roundPanel(side, cx, cy, cz, p[1], p[2]);

         // Pale eyespots toward the outer edge, the one marking that reads at
         // distance when the panel itself is a flat colour.
         setColor(shade(rgb, 1.7F), 0.88F);
         spot(cx * 0.70F, cy * 0.70F, cz * 0.70F + 0.005F, p[1] * 0.26F);
         spot(cx * 0.42F, cy * 0.42F - p[1] * 0.32F, cz * 0.42F + 0.005F, p[1] * 0.15F);
      }
   }

   /** A rounded blade from the shoulder out to a centre point. */
   private static void roundPanel(int side, float cx, float cy, float cz,
                                  float half, float rootZ) {
      float len = (float)Math.sqrt(cx * cx + cy * cy);
      if(len < 1.0E-4F) {
         return;
      }

      float ux = cx / len;
      float uy = cy / len;
      float px = -uy * half;
      float py = ux * half;

      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(6, DefaultVertexFormats.POSITION);

      wr.pos(side * 0.05F, 0.0F, rootZ).endVertex();

      // Ellipse-ish outline, fatter on the leading edge than the trailing one.
      for(int i = 0; i <= 12; ++i) {
         float t = i / 12.0F;
         double a = Math.PI * (t - 0.5D);
         float along = 0.45F + 0.55F * (float)Math.cos(a);
         float across = (float)Math.sin(a);
         float fat = across > 0.0F ? 1.0F : 0.72F;

         wr.pos(cx * along + px * across * fat,
               cy * along + py * across * fat,
               rootZ + (cz - rootZ) * along).endVertex();
      }

      tess.draw();
   }

   /** A small disc, for butterfly markings. */
   private static void spot(float x, float y, float z, float r) {
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(6, DefaultVertexFormats.POSITION);
      wr.pos(x, y, z).endVertex();

      for(int a = 0; a <= 360; a += 30) {
         double rad = Math.toRadians(a);
         wr.pos(x + (float)Math.cos(rad) * r, y + (float)Math.sin(rad) * r, z).endVertex();
      }

      tess.draw();
   }

   /**
    * Mechanical: hard plates on a jointed spar.
    *
    * <p>Everything here is straight-edged and evenly spaced on purpose. The
    * other wings are organic and asymmetric; regularity is what makes this one
    * read as built rather than grown.
    */
   private static void mechWing(int side, int rgb, float beat, float open) {
      int plates = 5;

      // Spar along the spine first, so the plates read as mounted to it.
      setColor(shade(rgb, 0.50F), 1.0F);
      float[] end = spinePoint(side, 1.0F, beat, open, 0.0F);
      feather(side * 0.05F, 0.0F, 0.0F, end[0], end[1], end[2], 0.042F);

      for(int i = 0; i < plates; ++i) {
         float t = (float)i / (float)(plates - 1);
         float u = 0.22F + t * 0.74F;

         float[] root = spinePoint(side, u, beat, open, 0.02F * i);
         // Plates hang square off the spar and shorten toward the tip, which is
         // the regularity that makes this read as built rather than grown.
         float len = 0.52F * (1.0F - t * 0.45F);
         float[] tip = trailingPoint(root, side, len, beat, open, u);

         setColor(shade(rgb, i % 2 == 0 ? 0.95F : 0.76F), 0.96F);
         plate(root[0], root[1], root[2], tip[0], tip[1], tip[2], 0.13F, 0.075F);

         // Bright strip along each plate's leading edge.
         setColor(shade(rgb, 1.6F), 1.0F);
         plate(root[0], root[1], root[2] + 0.004F,
               root[0] + (tip[0] - root[0]) * 0.94F,
               root[1] + (tip[1] - root[1]) * 0.94F,
               tip[2] + 0.004F, 0.028F, 0.016F);
      }
   }

   /** A straight-edged quad, wide at the root and narrower at the tip. */
   private static void plate(float rx, float ry, float rz,
                             float tx, float ty, float tz,
                             float rootW, float tipW) {
      float dx = tx - rx;
      float dy = ty - ry;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      if(len < 1.0E-4F) {
         return;
      }

      float px = -dy / len;
      float py = dx / len;

      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION);
      wr.pos(rx + px * rootW, ry + py * rootW, rz).endVertex();
      wr.pos(tx + px * tipW, ty + py * tipW, tz).endVertex();
      wr.pos(tx - px * tipW, ty - py * tipW, tz).endVertex();
      wr.pos(rx - px * rootW, ry - py * rootW, rz).endVertex();
      tess.draw();
   }

   /**
    * Ethereal: stacked translucent arcs with no solid edge.
    *
    * <p>Deliberately has no outline at all -- each layer is fainter and slightly
    * larger than the one under it, so the shape is suggested by accumulation
    * rather than drawn. Depth-writing stays off so the layers blend into each
    * other instead of occluding.
    */
   private static void etherealWing(int side, int rgb, float beat, float open) {
      float sweep = 0.44F + (open - 30.0F) / 150.0F;
      int layers = 5;

      GlStateManager.depthMask(false);

      for(int l = 0; l < layers; ++l) {
         float grow = 1.0F + l * 0.11F;
         float alpha = 0.36F - l * 0.055F;
         float pulse = 1.0F + (float)Math.sin(beat * 2.0D + l * 0.8D) * 0.05F;

         setColor(shade(rgb, 1.0F + l * 0.12F), Math.max(0.05F, alpha));

         for(int i = 0; i < 6; ++i) {
            float t = i / 5.0F;
            float u = 0.20F + t * 0.80F;

            float[] root = spinePoint(side, u, beat, open, 0.03F * l);
            float len = 0.52F * grow * pulse * (0.55F + 0.45F * (float)Math.sin(t * Math.PI));
            float[] tip = trailingPoint(root, side, len, beat, open, u);

            feather(root[0], root[1], root[2],
                  tip[0] * grow, tip[1] * grow, tip[2],
                  0.095F + 0.04F * (1.0F - t));
         }
      }

      GlStateManager.depthMask(true);
   }

   /** A four-point sliver: base quad pinching straight to a tip. */
   private static void shard(float rx, float ry, float rz,
                             float tx, float ty, float tz, float w) {
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
      wr.begin(6, DefaultVertexFormats.POSITION);
      wr.pos(rx + px, ry + py, rz).endVertex();
      wr.pos(rx + dx * 0.30F + px * 0.75F, ry + dy * 0.30F + py * 0.75F, rz + (tz - rz) * 0.30F).endVertex();
      wr.pos(tx, ty, tz).endVertex();
      wr.pos(rx + dx * 0.30F - px * 0.75F, ry + dy * 0.30F - py * 0.75F, rz + (tz - rz) * 0.30F).endVertex();
      wr.pos(rx - px, ry - py, rz).endVertex();
      tess.draw();
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

   /** HSV to RGB, for the rainbow trail. h/s/v are 0-1. */
   private static float[] hsv(float h, float s, float v) {
      float i = (float)Math.floor(h * 6.0F);
      float f = h * 6.0F - i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - f * s);
      float t = v * (1.0F - (1.0F - f) * s);

      switch((int)i % 6) {
         case 0: return new float[]{v, t, p};
         case 1: return new float[]{q, v, p};
         case 2: return new float[]{p, v, t};
         case 3: return new float[]{p, q, v};
         case 4: return new float[]{t, p, v};
         default: return new float[]{v, p, q};
      }
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

      // Two-sided: a ribbon is a single plane, so with culling on it vanishes
      // from whichever side faces away.
      GlStateManager.disableCull();

      // Points are stored in world space, so the current camera position comes
      // off here rather than being baked in when they were recorded.
      net.minecraft.client.renderer.entity.RenderManager rm =
            Minecraft.getMinecraft().getRenderManager();

      boolean rainbow = "trail_rainbow".equals(c.getId());
      int rgb = c.getColor();
      long now = System.currentTimeMillis();

      // Drawn twice, in perpendicular planes. A flat ribbon disappears entirely
      // when you look along its edge, which is exactly what happens whenever you
      // run straight at or away from someone -- the case it most needs to work.
      for(int pass = 0; pass < 2; ++pass) {
         Tessellator tess = Tessellator.getInstance();
         WorldRenderer wr = tess.getWorldRenderer();
         wr.begin(5, DefaultVertexFormats.POSITION_COLOR);

         for(int i = 0; i < pts.size(); ++i) {
            double[] q = pts.get(i);
            float t = (float)i / (float)(pts.size() - 1);

            float alpha = t * t * 0.62F;          // fades off faster at the tail
            float w = 0.06F + t * 0.20F;          // and narrows to a point

            float r;
            float g;
            float b;

            if(rainbow) {
               // Hue runs along the ribbon and drifts over time, so it reads as
               // a moving band rather than a flat colour that happens to cycle.
               float[] c3 = hsv((t * 0.75F + now / 2600.0F) % 1.0F, 0.85F, 1.0F);
               r = c3[0];
               g = c3[1];
               b = c3[2];
            } else {
               // Everything else brightens toward the near end, which gives the
               // ribbon a hot core trailing into its own colour.
               float lift = 0.55F + 0.45F * t;
               r = Math.min(1.0F, (rgb >> 16 & 255) / 255.0F * lift + t * 0.25F);
               g = Math.min(1.0F, (rgb >> 8 & 255) / 255.0F * lift + t * 0.25F);
               b = Math.min(1.0F, (rgb & 255) / 255.0F * lift + t * 0.25F);
            }

            double x = q[0] - rm.viewerPosX;
            double y = q[1] - rm.viewerPosY + 1.0D;
            double z = q[2] - rm.viewerPosZ;

            // A slow curl, so a trail left while standing still is not a
            // dead straight line.
            double curl = Math.sin(i * 0.55D + now / 420.0D) * 0.05D * t;

            if(pass == 0) {
               wr.pos(x, y - w + curl, z).color(r, g, b, alpha).endVertex();
               wr.pos(x, y + w + curl, z).color(r, g, b, alpha).endVertex();
            } else {
               // Horizontal, offset along the direction of travel so the two
               // planes cross rather than sitting on top of each other.
               double dx = 0.0D;
               double dz = 0.0D;

               if(i > 0) {
                  double[] prev = pts.get(i - 1);
                  double vx = q[0] - prev[0];
                  double vz = q[2] - prev[2];
                  double len = Math.sqrt(vx * vx + vz * vz);
                  if(len > 1.0E-4D) {
                     dx = -vz / len * w;
                     dz = vx / len * w;
                  }
               }

               wr.pos(x - dx, y + curl, z - dz).color(r, g, b, alpha * 0.75F).endVertex();
               wr.pos(x + dx, y + curl, z + dz).color(r, g, b, alpha * 0.75F).endVertex();
            }
         }

         tess.draw();
      }

      GlStateManager.enableCull();

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
