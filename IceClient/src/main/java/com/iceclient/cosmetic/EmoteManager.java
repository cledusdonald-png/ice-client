package com.iceclient.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Plays emotes by overriding the player model's limb angles.
 *
 * <p>There is no animation system in 1.8.9 to hook into, so an emote is a
 * function from elapsed time to a set of limb rotations, applied at the tail of
 * {@code ModelBiped.setRotationAngles} -- after vanilla has finished posing the
 * model, or its walk cycle would simply overwrite ours.
 *
 * <p>Emotes are deliberately short and non-looping. A held pose would need
 * cancelling on damage, movement, screen changes and disconnect, all of which
 * are easy to miss; something that always ends on its own cannot leave you
 * stuck in a T-pose because an edge case was forgotten.
 */
public final class EmoteManager {

   /** Who is emoting, and since when. */
   private static final Map<String, Playing> playing = new HashMap<String, Playing>();

   private static final class Playing {
      String id;
      long startedAt;
   }

   private EmoteManager() {
   }

   /**
    * Emote lengths in milliseconds.
    *
    * <p>Longer than they need to be for the animation's sake, because an emote
    * also has to survive the trip to everyone else: a viewer polls, sees it, and
    * needs enough of it left to be worth watching. Short emotes were arriving
    * with a fraction of a second to run.
    */
   public static long durationOf(String id) {
      if("emote_sit".equals(id)) {
         return 6000L;
      }

      if("emote_floss".equals(id)) {
         return 4500L;
      }

      return 3500L;   // wave
   }

   public static void start(EntityPlayer p, String id) {
      if(p == null || CosmeticRegistry.byId(id) == null) {
         return;
      }

      Playing e = new Playing();
      e.id = id;
      e.startedAt = System.currentTimeMillis();
      playing.put(p.getName().toLowerCase(), e);
   }

   /** Used when a remote player's emote arrives with its own start time. */
   public static void startRemote(String name, String id, long startedAt) {
      if(name == null || CosmeticRegistry.byId(id) == null) {
         return;
      }

      // Ignore anything already finished, so a stale heartbeat cannot replay an
      // emote somebody did half a minute ago.
      if(System.currentTimeMillis() - startedAt > durationOf(id)) {
         return;
      }

      Playing e = new Playing();
      e.id = id;
      e.startedAt = startedAt;
      playing.put(name.toLowerCase(), e);
   }

   public static void stop(EntityPlayer p) {
      if(p != null) {
         playing.remove(p.getName().toLowerCase());
      }
   }

   public static boolean isPlaying(EntityPlayer p) {
      return current(p) != null;
   }

   /** The local player's active emote id, or null. */
   public static String localEmote() {
      Minecraft mc = Minecraft.getMinecraft();
      Playing e = mc.thePlayer == null ? null : current(mc.thePlayer);
      return e == null ? null : e.id;
   }

   public static long localEmoteStart() {
      Minecraft mc = Minecraft.getMinecraft();
      Playing e = mc.thePlayer == null ? null : current(mc.thePlayer);
      return e == null ? 0L : e.startedAt;
   }

   private static Playing current(EntityPlayer p) {
      if(p == null) {
         return null;
      }

      Playing e = playing.get(p.getName().toLowerCase());
      if(e == null) {
         return null;
      }

      if(System.currentTimeMillis() - e.startedAt > durationOf(e.id)) {
         playing.remove(p.getName().toLowerCase());
         return null;
      }

      return e;
   }

   // ------------------------------------------------------------------
   // posing
   // ------------------------------------------------------------------

   /**
    * Overrides the model's limbs for whatever this player is doing.
    *
    * @return true if a pose was applied, so the caller knows vanilla's was
    *         replaced rather than blended with
    */
   public static boolean pose(EntityPlayer p, ModelBiped m) {
      Playing e = current(p);
      if(e == null) {
         return false;
      }

      float t = (System.currentTimeMillis() - e.startedAt) / (float)durationOf(e.id);
      t = Math.max(0.0F, Math.min(1.0F, t));

      // Ease in and out at the edges so an emote starts and finishes from the
      // normal stance instead of snapping into it. The ramp is short -- a tenth
      // of the emote each end -- because a viewer who joins late lands in the
      // tail, and a long fade meant they saw a twitch rather than a wave.
      float blend = (float)Math.sin(Math.min(1.0D, Math.min(t, 1.0F - t) * 10.0D) * Math.PI / 2.0D);

      if("emote_wave".equals(e.id)) {
         wave(m, t, blend);
      } else if("emote_sit".equals(e.id)) {
         sit(m, t, blend);
      } else if("emote_floss".equals(e.id)) {
         floss(m, t, blend);
      } else {
         return false;
      }

      return true;
   }

   /**
    * Right arm straight up, swinging wide.
    *
    * <p>The swing was 0.45 radians, which is about 25 degrees -- barely visible
    * from more than a few blocks, and the head tilt ended up reading as the
    * whole animation. It is nearly double that now, and the arm is raised past
    * vertical so the silhouette changes rather than just the pose.
    */
   private static void wave(ModelBiped m, float t, float blend) {
      float swing = (float)Math.sin(t * Math.PI * 7.0D) * 0.80F;

      m.bipedRightArm.rotateAngleX = lerp(m.bipedRightArm.rotateAngleX, -2.90F, blend);
      m.bipedRightArm.rotateAngleZ = lerp(m.bipedRightArm.rotateAngleZ, -0.30F + swing, blend);
      m.bipedRightArm.rotateAngleY = lerp(m.bipedRightArm.rotateAngleY, 0.0F, blend);

      // Left arm settles slightly out, so the pose is not half a normal stance.
      m.bipedLeftArm.rotateAngleZ = lerp(m.bipedLeftArm.rotateAngleZ, -0.18F, blend);

      m.bipedHead.rotateAngleZ = lerp(m.bipedHead.rotateAngleZ, swing * 0.14F, blend);
   }

   /** Knees up, body settled, arms braced behind. */
   private static void sit(ModelBiped m, float t, float blend) {
      m.bipedRightLeg.rotateAngleX = lerp(m.bipedRightLeg.rotateAngleX, -1.5F, blend);
      m.bipedLeftLeg.rotateAngleX = lerp(m.bipedLeftLeg.rotateAngleX, -1.5F, blend);
      m.bipedRightLeg.rotateAngleY = lerp(m.bipedRightLeg.rotateAngleY, 0.22F, blend);
      m.bipedLeftLeg.rotateAngleY = lerp(m.bipedLeftLeg.rotateAngleY, -0.22F, blend);

      m.bipedRightArm.rotateAngleX = lerp(m.bipedRightArm.rotateAngleX, 0.55F, blend);
      m.bipedLeftArm.rotateAngleX = lerp(m.bipedLeftArm.rotateAngleX, 0.55F, blend);
      m.bipedRightArm.rotateAngleZ = lerp(m.bipedRightArm.rotateAngleZ, 0.32F, blend);
      m.bipedLeftArm.rotateAngleZ = lerp(m.bipedLeftArm.rotateAngleZ, -0.32F, blend);

      // Nothing here moves the body down -- the model has no root offset to
      // move. EmoteRenderer drops the whole player instead.
   }

   /** Arms swinging past the hips, alternating sides. */
   private static void floss(ModelBiped m, float t, float blend) {
      float beat = (float)Math.sin(t * Math.PI * 8.0D);
      float side = beat > 0.0F ? 1.0F : -1.0F;
      float sway = Math.abs(beat);

      m.bipedRightArm.rotateAngleZ = lerp(m.bipedRightArm.rotateAngleZ,
            side * (0.15F + sway * 0.75F), blend);
      m.bipedLeftArm.rotateAngleZ = lerp(m.bipedLeftArm.rotateAngleZ,
            side * (0.15F + sway * 0.75F), blend);
      m.bipedRightArm.rotateAngleX = lerp(m.bipedRightArm.rotateAngleX, side * 0.42F, blend);
      m.bipedLeftArm.rotateAngleX = lerp(m.bipedLeftArm.rotateAngleX, -side * 0.42F, blend);

      m.bipedBody.rotateAngleY = lerp(m.bipedBody.rotateAngleY, -side * 0.22F, blend);
      m.bipedHead.rotateAngleY = lerp(m.bipedHead.rotateAngleY, side * 0.14F, blend);

      m.bipedRightLeg.rotateAngleX = lerp(m.bipedRightLeg.rotateAngleX, 0.0F, blend);
      m.bipedLeftLeg.rotateAngleX = lerp(m.bipedLeftLeg.rotateAngleX, 0.0F, blend);
   }

   /** Height the whole model drops, for emotes that need it. */
   public static float bodyOffset(EntityPlayer p) {
      Playing e = current(p);
      if(e == null || !"emote_sit".equals(e.id)) {
         return 0.0F;
      }

      float t = (System.currentTimeMillis() - e.startedAt) / (float)durationOf(e.id);
      float blend = (float)Math.sin(Math.min(1.0D, Math.min(t, 1.0F - t) * 6.0D) * Math.PI / 2.0D);
      return 0.42F * blend;
   }

   private static float lerp(float from, float to, float f) {
      return from + (to - from) * f;
   }
}
