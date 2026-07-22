package com.iceclient.cosmetic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The cosmetic catalogue.
 *
 * <p>Ships in the jar rather than being fetched, so the wardrobe works offline
 * and a server outage cannot leave someone's equipped cape rendering as nothing.
 * Ids are permanent -- an equipped cosmetic is stored by id, so renaming one is
 * fine but changing its id un-equips it for everybody.
 */
public final class CosmeticRegistry {

   private static final Map<String, Cosmetic> BY_ID = new LinkedHashMap<String, Cosmetic>();

   private CosmeticRegistry() {
   }

   static {
      // ---- capes -------------------------------------------------------
      add(new Cosmetic("cape_ice", "Ice Cape", CosmeticType.CAPE,
            Cosmetic.Rarity.COMMON, 0,
            "The house cape. Free for everyone running Ice.", "cape_ice.png"));

      add(new Cosmetic("cape_frost", "Frostbite", CosmeticType.CAPE,
            Cosmetic.Rarity.RARE, 500,
            "Pale blue, fading to white at the hem.", "cape_frost.png"));

      add(new Cosmetic("cape_aurora", "Aurora", CosmeticType.CAPE,
            Cosmetic.Rarity.EPIC, 1500,
            "Northern lights, the same green as the launcher sky.", "cape_aurora.png"));

      add(new Cosmetic("cape_obsidian", "Obsidian", CosmeticType.CAPE,
            Cosmetic.Rarity.EPIC, 1500,
            "For people who spend their evenings inside a wall.", "cape_obsidian.png"));

      add(new Cosmetic("cape_founder", "Founder", CosmeticType.CAPE,
            Cosmetic.Rarity.LEGENDARY, 0,
            "Given, not bought. Early Ice Client testers.", "cape_founder.png"));

      // ---- hats --------------------------------------------------------
      // Colour-driven rather than textured: at the size a hat renders, a solid
      // shape with a good silhouette reads better than a 4-pixel texture.
      add(new Cosmetic("hat_crown", "Frost Crown", CosmeticType.HAT,
            Cosmetic.Rarity.EPIC, 1200,
            "Five points of clear ice.", null, 0xA8F2FF));

      add(new Cosmetic("hat_beanie", "Beanie", CosmeticType.HAT,
            Cosmetic.Rarity.COMMON, 200,
            "For the cold. Practical, for once.", null, 0x4A9BC4));

      add(new Cosmetic("hat_halo", "Halo", CosmeticType.HAT,
            Cosmetic.Rarity.RARE, 800,
            "Floats above. Unearned, probably.", null, 0xFFC947));

      // ---- wings -------------------------------------------------------
      add(new Cosmetic("wings_frost", "Frost Wings", CosmeticType.WINGS,
            Cosmetic.Rarity.EPIC, 1800,
            "Sheets of ice that catch the light.", null, 0xBFE8FA));

      add(new Cosmetic("wings_shadow", "Shadow Wings", CosmeticType.WINGS,
            Cosmetic.Rarity.LEGENDARY, 3000,
            "Darker than the wall you're standing in.", null, 0x2A1E3D));

      // ---- pets --------------------------------------------------------
      // Ground companions rather than orbiting trinkets; the shapes live in
      // BuiltInPets, in the same format custom pets use.
      add(new Cosmetic("pet_snowman", "Snowman", CosmeticType.PET,
            Cosmetic.Rarity.RARE, 900,
            "Waddles along behind you. Scarf included.", null, 0xFFFFFF));

      add(new Cosmetic("pet_polarbear", "Polar Bear", CosmeticType.PET,
            Cosmetic.Rarity.EPIC, 1800,
            "Small, for a bear. Follows you everywhere.", null, 0xF4F7F9));

      // ---- trails ------------------------------------------------------
      add(new Cosmetic("trail_frost", "Frost Trail", CosmeticType.TRAIL,
            Cosmetic.Rarity.COMMON, 300,
            "Cold air where you've been.", null, 0xA8F2FF));

      add(new Cosmetic("trail_aurora", "Aurora Trail", CosmeticType.TRAIL,
            Cosmetic.Rarity.EPIC, 1400,
            "Green and blue, fading behind you.", null, 0x5AFFD2));

      // ---- emotes ------------------------------------------------------
      // Registered so the wardrobe and shop can show what is coming, but there
      // is no animation behind them yet -- see CosmeticManager#canPlayEmote.
      add(new Cosmetic("emote_wave", "Wave", CosmeticType.EMOTE,
            Cosmetic.Rarity.COMMON, 0, "A plain wave.", null));

      add(new Cosmetic("emote_sit", "Sit", CosmeticType.EMOTE,
            Cosmetic.Rarity.RARE, 400, "Sit down where you stand.", null));

      add(new Cosmetic("emote_floss", "Floss", CosmeticType.EMOTE,
            Cosmetic.Rarity.EPIC, 1200, "You know the one.", null));
   }

   private static void add(Cosmetic c) {
      BY_ID.put(c.getId(), c);
   }

   public static Cosmetic byId(String id) {
      return id == null ? null : BY_ID.get(id);
   }

   public static List<Cosmetic> all() {
      return Collections.unmodifiableList(new ArrayList<Cosmetic>(BY_ID.values()));
   }

   /** Everything in one slot, catalogue order. */
   public static List<Cosmetic> ofType(CosmeticType type) {
      List<Cosmetic> out = new ArrayList<Cosmetic>();

      for(Cosmetic c : BY_ID.values()) {
         if(c.getType() == type) {
            out.add(c);
         }
      }

      return out;
   }
}
