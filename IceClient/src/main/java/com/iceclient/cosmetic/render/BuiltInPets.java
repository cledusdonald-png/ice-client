package com.iceclient.cosmetic.render;

import com.iceclient.cosmetic.PetModel;

import java.util.HashMap;
import java.util.Map;

/**
 * The shipped pets.
 *
 * <p>Built with the same {@link PetModel} the JSON format produces, so a pet
 * designed in {@code config/iceclient/pets} and one that ships in the jar are
 * the same kind of object and go through the same renderer. Anything worked out
 * in a file becomes a catalogue pet by moving the numbers here.
 *
 * <p><b>On variety.</b> The first roster was nine animals in the same white,
 * all at the same scale, and they were genuinely hard to tell apart at a
 * glance. Three things fix that and every pet below uses them deliberately:
 * a <em>palette</em> that is not white, a <em>scale</em> that is not 1.0, and a
 * <em>silhouette</em> that differs at the outline rather than in the detail --
 * ears, tails, stance and bulk, which is all you can read from ten blocks away.
 */
public final class BuiltInPets {

   private static final Map<String, PetModel> MODELS = new HashMap<String, PetModel>();

   private BuiltInPets() {
   }

   public static PetModel get(String id) {
      return MODELS.get(id);
   }

   static {
      // --- tiny -------------------------------------------------------
      MODELS.put("pet_slime", slime(0x92E2F6, 0xB4EEFC));
      MODELS.put("pet_slime_lava", slime(0xF2792A, 0xFFC24A));
      MODELS.put("pet_bunny", bunny(0xFEFEFF, 0xF2F2F6));
      MODELS.put("pet_bunny_brown", bunny(0xA5764C, 0x8B5F3B));
      MODELS.put("pet_chick", chick());

      // --- small ------------------------------------------------------
      MODELS.put("pet_fox_arctic", fox(0xFCFDFE, 0xE8EEF4, 0xFFFFFF));
      MODELS.put("pet_fox_red", fox(0xC85A28, 0x8E3D18, 0xF6EDE2));
      MODELS.put("pet_fox_fennec", fennec());
      MODELS.put("pet_penguin", penguin());
      MODELS.put("pet_robin", bird(0x5A4A3E, 0xD9503A, 0xF2C14A));
      MODELS.put("pet_raven", bird(0x1A1A22, 0x2A2A36, 0x6A6A7A));
      MODELS.put("pet_owl", owl());
      MODELS.put("pet_monkey", monkey());
      MODELS.put("pet_seal", seal());

      // --- medium -----------------------------------------------------
      MODELS.put("pet_redpanda", redPanda());
      MODELS.put("pet_snowman", snowman());

      // --- large ------------------------------------------------------
      MODELS.put("pet_polarbear", bear(1.45F, 0xF4F7F9, 0xEAEFF3, 0x1B1F24));
      MODELS.put("pet_brownbear", bear(1.45F, 0x7A5334, 0x664326, 0x14100C));
      MODELS.put("pet_yeti", yeti());

      // --- legendary --------------------------------------------------
      MODELS.put("pet_snowgolem", snowGolem());
      MODELS.put("pet_dragon", dragon(1.25F, 0x8FB6EE, 0xC9E4FF));
      MODELS.put("pet_dragon_ember", dragon(1.25F, 0xC4451E, 0xFFB03A));
      MODELS.put("pet_wyvern", wyvern());
   }

   // ------------------------------------------------------------------
   // shared pieces
   // ------------------------------------------------------------------

   /**
    * A pair of eyes with catchlights.
    *
    * <p>The single most useful thing in this file. A dark eye with a pale
    * highlight reads as alive from any distance; the same eye without one looks
    * like a bead pressed into a toy.
    */
   private static void eyes(PetModel.Builder b, float x, float y, float z,
                            float w, float h, int dark) {
      b.box(-x, y, z, w, h, 0.02F, dark);
      b.box(x, y, z, w, h, 0.02F, dark);
      b.box(-x - w * 0.22F, y + h * 0.45F, z + 0.005F, w * 0.42F, h * 0.38F, 0.02F, 0xFFFFFF);
      b.box(x - w * 0.22F, y + h * 0.45F, z + 0.005F, w * 0.42F, h * 0.38F, 0.02F, 0xFFFFFF);
   }

   /** Four legs at the corners of a body. */
   private static void legs(PetModel.Builder b, float x, float z, float w, float h, int c) {
      b.box(-x, 0.0F, z, w, h, w, c);
      b.box(x, 0.0F, z, w, h, w, c);
      b.box(-x, 0.0F, -z, w, h, w, c);
      b.box(x, 0.0F, -z, w, h, w, c);
   }

   // ------------------------------------------------------------------
   // tiny
   // ------------------------------------------------------------------

   private static PetModel slime(int body, int top) {
      PetModel.Builder b = PetModel.builder("Slime", 0.85F);
      b.box(0, 0.00F, 0, 0.30F, 0.14F, 0.30F, body);
      b.box(0, 0.13F, 0, 0.24F, 0.10F, 0.24F, top);
      b.box(0, 0.22F, 0, 0.15F, 0.06F, 0.15F, top);
      eyes(b, 0.07F, 0.10F, 0.152F, 0.07F, 0.08F, 0x14202A);
      b.box(0, 0.075F, 0.155F, 0.05F, 0.02F, 0.02F, 0x14202A);
      return b.perches().build();
   }

   private static PetModel bunny(int fur, int shade) {
      PetModel.Builder b = PetModel.builder("Bunny", 0.80F);
      b.box(0, 0.04F, -0.02F, 0.20F, 0.18F, 0.24F, shade);
      b.box(0, 0.19F, 0.09F, 0.19F, 0.18F, 0.18F, fur);

      b.box(-0.055F, 0.395F, 0.06F, 0.06F, 0.20F, 0.05F, fur);
      b.box(0.055F, 0.395F, 0.06F, 0.06F, 0.20F, 0.05F, fur);
      b.box(-0.055F, 0.42F, 0.075F, 0.03F, 0.14F, 0.02F, 0xFFC2CE);
      b.box(0.055F, 0.42F, 0.075F, 0.03F, 0.14F, 0.02F, 0xFFC2CE);

      eyes(b, 0.055F, 0.25F, 0.175F, 0.07F, 0.08F, 0x141A22);
      b.box(0, 0.225F, 0.18F, 0.04F, 0.03F, 0.02F, 0xFF9FB4);
      b.box(0, 0.09F, -0.15F, 0.09F, 0.09F, 0.07F, 0xFFFFFF);
      b.box(-0.07F, 0.00F, 0.06F, 0.07F, 0.05F, 0.11F, shade);
      b.box(0.07F, 0.00F, 0.06F, 0.07F, 0.05F, 0.11F, shade);
      return b.build();
   }

   /** Small enough to be comic; almost all head. */
   private static PetModel chick() {
      PetModel.Builder b = PetModel.builder("Chick", 0.70F);
      b.box(0, 0.02F, 0, 0.20F, 0.18F, 0.18F, 0xFFD84A);
      b.box(0, 0.19F, 0.01F, 0.19F, 0.17F, 0.17F, 0xFFE470);
      eyes(b, 0.055F, 0.26F, 0.09F, 0.05F, 0.06F, 0x1A1408);
      b.spike(0, 0.235F, 0.10F, 0.045F, 0.07F, 0xFF9A2A, false);
      b.box(-0.11F, 0.06F, 0, 0.03F, 0.10F, 0.09F, 0xFFCE3A);
      b.box(0.11F, 0.06F, 0, 0.03F, 0.10F, 0.09F, 0xFFCE3A);
      b.box(-0.05F, 0.00F, 0.03F, 0.05F, 0.03F, 0.08F, 0xFF9A2A);
      b.box(0.05F, 0.00F, 0.03F, 0.05F, 0.03F, 0.08F, 0xFF9A2A);
      return b.perches().build();
   }

   // ------------------------------------------------------------------
   // small
   // ------------------------------------------------------------------

   private static PetModel fox(int coat, int shade, int tip) {
      PetModel.Builder b = PetModel.builder("Fox", 0.95F);
      b.box(0, 0.09F, -0.02F, 0.20F, 0.17F, 0.30F, coat);
      b.box(0, 0.22F, 0.14F, 0.20F, 0.18F, 0.17F, coat);
      b.box(0, 0.245F, 0.24F, 0.09F, 0.08F, 0.07F, tip);
      b.box(0, 0.245F, 0.29F, 0.04F, 0.035F, 0.02F, 0x2A2028);

      b.spike(-0.07F, 0.38F, 0.13F, 0.08F, 0.10F, coat, false);
      b.spike(0.07F, 0.38F, 0.13F, 0.08F, 0.10F, coat, false);

      eyes(b, 0.06F, 0.28F, 0.225F, 0.07F, 0.07F, 0x141A22);

      b.box(0, 0.16F, -0.20F, 0.13F, 0.13F, 0.18F, coat);
      b.box(0, 0.16F, -0.28F, 0.10F, 0.10F, 0.06F, tip);
      legs(b, 0.07F, 0.09F, 0.06F, 0.10F, shade);
      return b.build();
   }

   /** Ears the size of its head. Reads instantly at any distance. */
   private static PetModel fennec() {
      PetModel.Builder b = PetModel.builder("Fennec Fox", 0.85F);
      b.box(0, 0.08F, -0.02F, 0.18F, 0.15F, 0.26F, 0xE8CFA6);
      b.box(0, 0.19F, 0.12F, 0.18F, 0.16F, 0.15F, 0xF2DEBC);
      b.box(0, 0.205F, 0.21F, 0.08F, 0.07F, 0.06F, 0xFAF0DE);
      b.box(0, 0.205F, 0.255F, 0.035F, 0.03F, 0.02F, 0x2A2028);

      // Oversized, and angled outward so they show from the front.
      b.box(-0.10F, 0.33F, 0.11F, 0.10F, 0.22F, 0.04F, 0xF2DEBC);
      b.box(0.10F, 0.33F, 0.11F, 0.10F, 0.22F, 0.04F, 0xF2DEBC);
      b.box(-0.10F, 0.35F, 0.125F, 0.06F, 0.16F, 0.02F, 0xD8A87A);
      b.box(0.10F, 0.35F, 0.125F, 0.06F, 0.16F, 0.02F, 0xD8A87A);

      eyes(b, 0.055F, 0.24F, 0.195F, 0.065F, 0.065F, 0x141A22);
      b.box(0, 0.14F, -0.17F, 0.11F, 0.11F, 0.15F, 0xF2DEBC);
      b.box(0, 0.14F, -0.24F, 0.08F, 0.08F, 0.05F, 0xFAF0DE);
      legs(b, 0.06F, 0.08F, 0.05F, 0.09F, 0xE8CFA6);
      return b.build();
   }

   private static PetModel penguin() {
      PetModel.Builder b = PetModel.builder("Penguin", 0.90F);
      b.box(0, 0.08F, 0, 0.22F, 0.30F, 0.20F, 0x1B2028);
      b.box(0, 0.10F, 0.07F, 0.16F, 0.25F, 0.08F, 0xF6F8FA);
      b.box(0, 0.38F, 0, 0.19F, 0.17F, 0.18F, 0x1B2028);
      eyes(b, 0.05F, 0.46F, 0.08F, 0.04F, 0.04F, 0xF6F8FA);
      b.spike(0, 0.42F, 0.11F, 0.05F, 0.09F, 0xFFA23D, false);
      b.box(-0.13F, 0.12F, 0, 0.04F, 0.20F, 0.10F, 0x232935);
      b.box(0.13F, 0.12F, 0, 0.04F, 0.20F, 0.10F, 0x232935);
      b.box(-0.06F, 0.00F, 0.04F, 0.08F, 0.04F, 0.12F, 0xFFA23D);
      b.box(0.06F, 0.00F, 0.04F, 0.08F, 0.04F, 0.12F, 0xFFA23D);
      return b.build();
   }

   /** Generic songbird, coloured per variant. */
   private static PetModel bird(int back, int breast, int beak) {
      PetModel.Builder b = PetModel.builder("Bird", 0.72F);
      b.box(0, 0.04F, -0.01F, 0.18F, 0.17F, 0.20F, back);
      b.box(0, 0.06F, 0.08F, 0.13F, 0.13F, 0.06F, breast);
      b.box(0, 0.19F, 0.03F, 0.16F, 0.15F, 0.15F, back);
      eyes(b, 0.05F, 0.25F, 0.10F, 0.045F, 0.05F, 0x0E0E14);
      b.spike(0, 0.225F, 0.11F, 0.04F, 0.08F, beak, false);
      b.box(-0.10F, 0.06F, -0.01F, 0.03F, 0.14F, 0.14F, back);
      b.box(0.10F, 0.06F, -0.01F, 0.03F, 0.14F, 0.14F, back);
      b.box(0, 0.06F, -0.14F, 0.09F, 0.04F, 0.12F, back);
      b.box(-0.04F, 0.00F, 0.02F, 0.03F, 0.04F, 0.06F, beak);
      b.box(0.04F, 0.00F, 0.02F, 0.03F, 0.04F, 0.06F, beak);
      return b.perches().build();
   }

   private static PetModel owl() {
      PetModel.Builder b = PetModel.builder("Snowy Owl", 0.88F);
      b.box(0, 0.03F, 0, 0.24F, 0.26F, 0.20F, 0xF8FAFC);
      b.box(0, 0.26F, 0, 0.26F, 0.20F, 0.20F, 0xFDFEFF);
      b.box(-0.065F, 0.30F, 0.10F, 0.11F, 0.11F, 0.02F, 0xF0B93A);
      b.box(0.065F, 0.30F, 0.10F, 0.11F, 0.11F, 0.02F, 0xF0B93A);
      eyes(b, 0.065F, 0.315F, 0.112F, 0.07F, 0.07F, 0x141A22);
      b.spike(0, 0.28F, 0.11F, 0.05F, 0.06F, 0xF0B93A, true);
      b.spike(-0.09F, 0.42F, 0, 0.06F, 0.07F, 0xFDFEFF, false);
      b.spike(0.09F, 0.42F, 0, 0.06F, 0.07F, 0xFDFEFF, false);
      b.box(-0.13F, 0.06F, 0, 0.04F, 0.18F, 0.13F, 0xEEF3F8);
      b.box(0.13F, 0.06F, 0, 0.04F, 0.18F, 0.13F, 0xEEF3F8);
      b.box(-0.05F, 0.00F, 0.05F, 0.06F, 0.03F, 0.09F, 0xF0B93A);
      b.box(0.05F, 0.00F, 0.05F, 0.06F, 0.03F, 0.09F, 0xF0B93A);
      return b.perches().build();
   }

   /** Long arms, pale face, tail curled up behind. */
   private static PetModel monkey() {
      PetModel.Builder b = PetModel.builder("Monkey", 0.88F);
      b.box(0, 0.10F, 0, 0.20F, 0.20F, 0.16F, 0x6B4A32);
      b.box(0, 0.12F, 0.07F, 0.14F, 0.15F, 0.04F, 0xC49A72);
      b.box(0, 0.28F, 0.01F, 0.21F, 0.19F, 0.18F, 0x6B4A32);
      b.box(0, 0.30F, 0.10F, 0.14F, 0.13F, 0.04F, 0xD8B48C);

      b.box(-0.13F, 0.33F, 0.01F, 0.06F, 0.10F, 0.05F, 0xC49A72);   // ears
      b.box(0.13F, 0.33F, 0.01F, 0.06F, 0.10F, 0.05F, 0xC49A72);

      eyes(b, 0.05F, 0.35F, 0.115F, 0.05F, 0.055F, 0x14100C);
      b.box(0, 0.315F, 0.12F, 0.05F, 0.03F, 0.02F, 0x3A2A1E);

      b.box(-0.14F, 0.08F, 0.01F, 0.06F, 0.22F, 0.06F, 0x5C3E28);   // long arms
      b.box(0.14F, 0.08F, 0.01F, 0.06F, 0.22F, 0.06F, 0x5C3E28);
      b.box(-0.06F, 0.00F, 0.01F, 0.07F, 0.11F, 0.07F, 0x5C3E28);
      b.box(0.06F, 0.00F, 0.01F, 0.07F, 0.11F, 0.07F, 0x5C3E28);

      b.box(0, 0.16F, -0.11F, 0.04F, 0.04F, 0.10F, 0x5C3E28);       // curled tail
      b.box(0, 0.22F, -0.15F, 0.04F, 0.10F, 0.04F, 0x5C3E28);
      b.box(0, 0.30F, -0.13F, 0.04F, 0.04F, 0.07F, 0x5C3E28);
      return b.build();
   }

   private static PetModel seal() {
      PetModel.Builder b = PetModel.builder("Baby Seal", 0.95F);
      b.box(0, 0.04F, -0.04F, 0.26F, 0.20F, 0.34F, 0xC9D6E0);
      b.box(0, 0.20F, 0.10F, 0.24F, 0.21F, 0.22F, 0xE2ECF3);
      eyes(b, 0.06F, 0.27F, 0.21F, 0.08F, 0.09F, 0x141A22);
      b.box(0, 0.235F, 0.225F, 0.05F, 0.04F, 0.02F, 0x3A2630);
      b.box(-0.14F, 0.05F, 0.04F, 0.07F, 0.04F, 0.13F, 0xB8C8D4);
      b.box(0.14F, 0.05F, 0.04F, 0.07F, 0.04F, 0.13F, 0xB8C8D4);
      b.box(-0.05F, 0.02F, -0.22F, 0.08F, 0.04F, 0.09F, 0xB8C8D4);
      b.box(0.05F, 0.02F, -0.22F, 0.08F, 0.04F, 0.09F, 0xB8C8D4);
      return b.build();
   }

   // ------------------------------------------------------------------
   // medium
   // ------------------------------------------------------------------

   private static PetModel redPanda() {
      PetModel.Builder b = PetModel.builder("Red Panda", 1.05F);
      b.box(0, 0.09F, -0.02F, 0.22F, 0.18F, 0.30F, 0xB35A2A);
      b.box(0, 0.22F, 0.14F, 0.24F, 0.20F, 0.18F, 0xC46A34);
      b.box(0, 0.24F, 0.24F, 0.10F, 0.08F, 0.06F, 0xF6EDE2);   // muzzle
      b.box(0, 0.24F, 0.28F, 0.04F, 0.03F, 0.02F, 0x2A1810);

      b.box(-0.09F, 0.245F, 0.235F, 0.07F, 0.09F, 0.03F, 0xF6EDE2);   // cheek marks
      b.box(0.09F, 0.245F, 0.235F, 0.07F, 0.09F, 0.03F, 0xF6EDE2);
      eyes(b, 0.065F, 0.29F, 0.235F, 0.06F, 0.06F, 0x140C08);

      b.box(-0.09F, 0.40F, 0.13F, 0.09F, 0.08F, 0.05F, 0xF6EDE2);     // round ears
      b.box(0.09F, 0.40F, 0.13F, 0.09F, 0.08F, 0.05F, 0xF6EDE2);

      // Banded tail -- the one thing everyone recognises.
      for(int i = 0; i < 4; ++i) {
         b.box(0, 0.15F - i * 0.005F, -0.19F - i * 0.09F, 0.11F, 0.11F, 0.09F,
               i % 2 == 0 ? 0x8E4520 : 0xE8D2BC);
      }

      legs(b, 0.08F, 0.10F, 0.07F, 0.10F, 0x4A2614);
      return b.build();
   }

   private static PetModel snowman() {
      PetModel.Builder b = PetModel.builder("Snowman", 1.10F);
      b.box(0, 0.00F, 0, 0.34F, 0.28F, 0.34F, 0xF2F8FC);
      b.box(0, 0.28F, 0, 0.26F, 0.22F, 0.26F, 0xFFFFFF);
      b.box(0, 0.50F, 0, 0.20F, 0.18F, 0.20F, 0xFFFFFF);
      eyes(b, 0.05F, 0.60F, 0.10F, 0.03F, 0.03F, 0x14161A);
      b.spike(0, 0.56F, 0.12F, 0.045F, 0.13F, 0xFF8A3D, false);
      b.box(-0.16F, 0.36F, 0, 0.10F, 0.03F, 0.03F, 0x6B4A2A);
      b.box(0.16F, 0.36F, 0, 0.10F, 0.03F, 0.03F, 0x6B4A2A);
      b.box(0, 0.34F, 0, 0.28F, 0.02F, 0.28F, 0x2A3038);
      return b.build();
   }

   // ------------------------------------------------------------------
   // large
   // ------------------------------------------------------------------

   /** One shape, two coats. Big enough that the bulk is the read. */
   private static PetModel bear(float scale, int coat, int shade, int dark) {
      PetModel.Builder b = PetModel.builder("Bear", scale);
      b.box(0, 0.16F, 0, 0.34F, 0.26F, 0.56F, coat);
      b.box(0, 0.30F, 0.34F, 0.26F, 0.24F, 0.24F, coat);
      b.box(0, 0.32F, 0.47F, 0.14F, 0.11F, 0.09F, shade);
      b.box(0, 0.31F, 0.53F, 0.06F, 0.05F, 0.03F, dark);

      b.box(-0.07F, 0.46F, 0.38F, 0.07F, 0.06F, 0.05F, shade);   // ears
      b.box(0.07F, 0.46F, 0.38F, 0.07F, 0.06F, 0.05F, shade);
      eyes(b, 0.055F, 0.37F, 0.465F, 0.035F, 0.035F, dark);

      legs(b, 0.12F, 0.19F, 0.10F, 0.18F, shade);
      b.box(0, 0.28F, -0.29F, 0.08F, 0.07F, 0.05F, coat);
      return b.build();
   }

   /**
    * A yeti, not a white fox.
    *
    * <p>The previous one shared its proportions with the fox and simply lost.
    * Everything here is about bulk: a barrel chest wider than it is deep, arms
    * that reach the ground, a heavy brow, and blue-grey fur rather than white so
    * it does not read as another snow animal.
    */
   private static PetModel yeti() {
      PetModel.Builder b = PetModel.builder("Yeti", 1.55F);

      int fur = 0xC7D8E6;
      int furDark = 0xA8BDD0;
      int skin = 0xE8F0F6;

      b.box(0, 0.16F, 0, 0.44F, 0.34F, 0.30F, fur);          // barrel chest
      b.box(0, 0.10F, 0, 0.40F, 0.10F, 0.28F, furDark);      // shaggy belly
      b.box(0, 0.46F, 0.01F, 0.34F, 0.26F, 0.28F, fur);      // head, sunk into shoulders

      b.box(0, 0.60F, 0.13F, 0.30F, 0.06F, 0.05F, furDark);  // heavy brow
      b.box(0, 0.49F, 0.14F, 0.18F, 0.10F, 0.04F, skin);     // muzzle
      b.box(0, 0.475F, 0.165F, 0.06F, 0.04F, 0.02F, 0x3A4652);
      eyes(b, 0.075F, 0.545F, 0.145F, 0.06F, 0.055F, 0x141A22);

      b.box(-0.055F, 0.455F, 0.16F, 0.03F, 0.04F, 0.02F, 0xFFFFFF);  // tusks
      b.box(0.055F, 0.455F, 0.16F, 0.03F, 0.04F, 0.02F, 0xFFFFFF);

      // Arms reaching the ground -- the gorilla stance is most of the read.
      b.box(-0.27F, 0.14F, 0.01F, 0.13F, 0.36F, 0.14F, fur);
      b.box(0.27F, 0.14F, 0.01F, 0.13F, 0.36F, 0.14F, fur);
      b.box(-0.27F, 0.02F, 0.03F, 0.14F, 0.12F, 0.16F, furDark);
      b.box(0.27F, 0.02F, 0.03F, 0.14F, 0.12F, 0.16F, furDark);

      b.box(-0.11F, 0.00F, 0, 0.16F, 0.17F, 0.18F, furDark);   // short legs
      b.box(0.11F, 0.00F, 0, 0.16F, 0.17F, 0.18F, furDark);
      return b.build();
   }

   // ------------------------------------------------------------------
   // legendary
   // ------------------------------------------------------------------

   /** Tall, armless, and faintly menacing. */
   private static PetModel snowGolem() {
      PetModel.Builder b = PetModel.builder("Snow Golem", 1.70F);

      b.box(0, 0.00F, 0, 0.40F, 0.30F, 0.36F, 0xEFF5FA);
      b.box(0, 0.30F, 0, 0.34F, 0.28F, 0.30F, 0xF8FBFD);
      b.box(0, 0.58F, 0, 0.28F, 0.24F, 0.26F, 0xFFFFFF);

      b.box(0, 0.80F, 0, 0.32F, 0.04F, 0.30F, 0x3A2A1E);      // bucket
      b.box(0, 0.82F, 0, 0.26F, 0.10F, 0.24F, 0x4A3628);

      eyes(b, 0.07F, 0.66F, 0.135F, 0.06F, 0.07F, 0x0E1A24);
      b.box(0, 0.625F, 0.14F, 0.05F, 0.03F, 0.02F, 0x14202A);
      b.box(-0.07F, 0.615F, 0.14F, 0.03F, 0.02F, 0.02F, 0x14202A);
      b.box(0.07F, 0.615F, 0.14F, 0.03F, 0.02F, 0.02F, 0x14202A);

      b.box(-0.24F, 0.34F, 0, 0.13F, 0.04F, 0.04F, 0x6B4A2A);   // stick arms
      b.box(0.24F, 0.34F, 0, 0.13F, 0.04F, 0.04F, 0x6B4A2A);
      b.box(-0.30F, 0.40F, 0, 0.04F, 0.10F, 0.04F, 0x6B4A2A);
      b.box(0.30F, 0.40F, 0, 0.04F, 0.10F, 0.04F, 0x6B4A2A);
      return b.build();
   }

   private static PetModel dragon(float scale, int body, int accent) {
      PetModel.Builder b = PetModel.builder("Dragon", scale);

      b.box(0, 0.10F, -0.02F, 0.26F, 0.24F, 0.36F, body);
      b.box(0, 0.10F, 0.10F, 0.18F, 0.18F, 0.12F, accent);      // belly
      b.box(0, 0.32F, 0.20F, 0.26F, 0.23F, 0.22F, body);
      b.box(0, 0.345F, 0.34F, 0.14F, 0.11F, 0.08F, body);       // snout
      b.box(-0.04F, 0.40F, 0.37F, 0.025F, 0.02F, 0.02F, 0x2A3448);
      b.box(0.04F, 0.40F, 0.37F, 0.025F, 0.02F, 0.02F, 0x2A3448);
      eyes(b, 0.07F, 0.40F, 0.325F, 0.06F, 0.065F, 0x141A22);

      b.spike(-0.08F, 0.53F, 0.16F, 0.06F, 0.12F, accent, false);   // horns
      b.spike(0.08F, 0.53F, 0.16F, 0.06F, 0.12F, accent, false);

      // Back ridge -- what makes it read as a dragon from behind.
      for(int i = 0; i < 4; ++i) {
         b.spike(0, 0.34F - i * 0.01F, 0.08F - i * 0.10F, 0.05F, 0.09F - i * 0.012F,
               accent, false);
      }

      b.box(-0.19F, 0.24F, -0.02F, 0.06F, 0.20F, 0.22F, accent);   // folded wings
      b.box(0.19F, 0.24F, -0.02F, 0.06F, 0.20F, 0.22F, accent);

      b.box(0, 0.12F, -0.26F, 0.10F, 0.10F, 0.14F, body);          // tail
      b.box(0, 0.12F, -0.36F, 0.06F, 0.06F, 0.10F, body);
      b.spike(0, 0.13F, -0.44F, 0.07F, 0.11F, accent, false);

      legs(b, 0.10F, 0.12F, 0.09F, 0.11F, body);
      return b.build();
   }

   /** Longer and leaner than the dragon, with wings spread rather than folded. */
   private static PetModel wyvern() {
      PetModel.Builder b = PetModel.builder("Frost Wyvern", 1.85F);

      int body = 0x4A6FA8;
      int accent = 0x9FD8FF;

      b.box(0, 0.18F, -0.04F, 0.22F, 0.22F, 0.42F, body);
      b.box(0, 0.42F, 0.24F, 0.22F, 0.20F, 0.20F, body);
      b.box(0, 0.435F, 0.37F, 0.13F, 0.10F, 0.09F, body);
      eyes(b, 0.065F, 0.485F, 0.345F, 0.055F, 0.06F, 0xF0F8FF);

      b.spike(-0.07F, 0.60F, 0.21F, 0.055F, 0.16F, accent, false);
      b.spike(0.07F, 0.60F, 0.21F, 0.055F, 0.16F, accent, false);

      // Wings held out, stepped so the silhouette is a wing rather than a slab.
      for(int s = -1; s <= 1; s += 2) {
         b.box(s * 0.20F, 0.36F, 0.02F, 0.16F, 0.05F, 0.26F, accent);
         b.box(s * 0.36F, 0.42F, 0.00F, 0.16F, 0.05F, 0.20F, accent);
         b.box(s * 0.50F, 0.48F, -0.02F, 0.14F, 0.04F, 0.14F, accent);
      }

      b.box(0, 0.20F, -0.30F, 0.10F, 0.10F, 0.18F, body);
      b.box(0, 0.20F, -0.42F, 0.06F, 0.06F, 0.12F, body);
      b.spike(0, 0.21F, -0.52F, 0.08F, 0.14F, accent, false);

      b.box(-0.10F, 0.00F, 0.10F, 0.09F, 0.19F, 0.10F, body);
      b.box(0.10F, 0.00F, 0.10F, 0.09F, 0.19F, 0.10F, body);
      return b.build();
   }
}
