package com.iceclient.cosmetic;

import net.minecraft.util.ResourceLocation;

/**
 * One cosmetic item.
 *
 * <p>Definitions are immutable and live in {@link CosmeticRegistry}; what a
 * given player owns and has equipped is {@link CosmeticManager}'s business. The
 * split matters because ownership is per-player state that has to survive a
 * restart and eventually come from a server, while the catalogue is the same for
 * everyone and ships in the jar.
 */
public final class Cosmetic {

   private final String id;
   private final String name;
   private final CosmeticType type;
   private final Rarity rarity;
   private final int price;
   private final String description;
   private final ResourceLocation texture;

   public Cosmetic(String id, String name, CosmeticType type, Rarity rarity,
                   int price, String description, String texturePath) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.rarity = rarity;
      this.price = price;
      this.description = description;
      this.texture = texturePath == null
            ? null
            : new ResourceLocation("iceclient", "textures/cosmetics/" + texturePath);
   }

   public String getId() { return this.id; }
   public String getName() { return this.name; }
   public CosmeticType getType() { return this.type; }
   public Rarity getRarity() { return this.rarity; }
   public int getPrice() { return this.price; }
   public String getDescription() { return this.description; }
   public ResourceLocation getTexture() { return this.texture; }

   /** Free items are owned from the start rather than bought. */
   public boolean isDefault() {
      return this.price <= 0;
   }

   /**
    * Rarity, which is purely presentational -- it colours the name and sorts the
    * shop. It deliberately carries no gameplay weight: a cosmetic is a cosmetic.
    */
   public enum Rarity {
      COMMON("Common", 0xFFB0BEC5),
      RARE("Rare", 0xFF5CC6FF),
      EPIC("Epic", 0xFFB388FF),
      LEGENDARY("Legendary", 0xFFFFC947);

      private final String label;
      private final int color;

      Rarity(String label, int color) {
         this.label = label;
         this.color = color;
      }

      public String getLabel() { return this.label; }
      public int getColor() { return this.color; }
   }
}
