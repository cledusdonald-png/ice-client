package com.iceclient.cosmetic.render;

import com.iceclient.cosmetic.PetModel;

import java.util.HashMap;
import java.util.Map;

/**
 * The shipped pets, built with the same {@link PetModel} the JSON format
 * produces.
 *
 * <p>Deliberately not a special case: a pet designed in
 * {@code config/iceclient/pets} and one that ships in the jar are the same kind
 * of object and go through the same renderer. That means anything you can build
 * in a file can become a catalogue pet by moving the numbers here, with no new
 * code -- which is the point of having the format at all.
 */
public final class BuiltInPets {

   private static final Map<String, PetModel> MODELS = new HashMap<String, PetModel>();

   private BuiltInPets() {
   }

   public static PetModel get(String id) {
      return MODELS.get(id);
   }

   static {
      MODELS.put("pet_snowman", snowman());
      MODELS.put("pet_polarbear", polarBear());
   }

   /** Two stacked snowballs, coal eyes, a carrot, and stick arms. */
   private static PetModel snowman() {
      PetModel.Builder b = PetModel.builder("Snowman", 1.0F);

      b.box(0, 0.00F, 0, 0.34F, 0.28F, 0.34F, 0xF2F8FC);      // base
      b.box(0, 0.28F, 0, 0.26F, 0.22F, 0.26F, 0xFFFFFF);      // middle
      b.box(0, 0.50F, 0, 0.20F, 0.18F, 0.20F, 0xFFFFFF);      // head

      b.box(-0.05F, 0.60F, 0.10F, 0.03F, 0.03F, 0.02F, 0x14161A);   // eyes
      b.box(0.05F, 0.60F, 0.10F, 0.03F, 0.03F, 0.02F, 0x14161A);
      b.spike(0, 0.56F, 0.12F, 0.045F, 0.13F, 0xFF8A3D, false);     // carrot

      b.box(-0.16F, 0.36F, 0, 0.10F, 0.03F, 0.03F, 0x6B4A2A);       // arms
      b.box(0.16F, 0.36F, 0, 0.10F, 0.03F, 0.03F, 0x6B4A2A);

      b.box(0, 0.34F, 0, 0.28F, 0.02F, 0.28F, 0x2A3038);            // scarf
      return b.build();
   }

   /** Low body, four legs, blunt snout -- reads as a bear at pet scale. */
   private static PetModel polarBear() {
      PetModel.Builder b = PetModel.builder("Polar Bear", 1.0F);

      b.box(0, 0.16F, 0, 0.30F, 0.24F, 0.52F, 0xF4F7F9);      // body
      b.box(0, 0.28F, 0.32F, 0.24F, 0.22F, 0.22F, 0xFAFCFD);  // head
      b.box(0, 0.30F, 0.44F, 0.13F, 0.10F, 0.08F, 0xE4EAEE);  // snout
      b.box(0, 0.29F, 0.49F, 0.06F, 0.05F, 0.03F, 0x1B1F24);  // nose

      b.box(-0.06F, 0.43F, 0.36F, 0.06F, 0.05F, 0.04F, 0xEDF2F5);   // ears
      b.box(0.06F, 0.43F, 0.36F, 0.06F, 0.05F, 0.04F, 0xEDF2F5);
      b.box(-0.05F, 0.35F, 0.44F, 0.03F, 0.03F, 0.02F, 0x1B1F24);   // eyes
      b.box(0.05F, 0.35F, 0.44F, 0.03F, 0.03F, 0.02F, 0x1B1F24);

      b.box(-0.11F, 0.00F, 0.18F, 0.09F, 0.17F, 0.09F, 0xEAEFF3);   // legs
      b.box(0.11F, 0.00F, 0.18F, 0.09F, 0.17F, 0.09F, 0xEAEFF3);
      b.box(-0.11F, 0.00F, -0.16F, 0.09F, 0.17F, 0.09F, 0xEAEFF3);
      b.box(0.11F, 0.00F, -0.16F, 0.09F, 0.17F, 0.09F, 0xEAEFF3);

      b.box(0, 0.26F, -0.27F, 0.07F, 0.06F, 0.05F, 0xF4F7F9);       // tail
      return b.build();
   }
}
