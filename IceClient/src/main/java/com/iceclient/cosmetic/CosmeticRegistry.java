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

      add(new Cosmetic("cape_glacier", "Glacier", CosmeticType.CAPE,
            Cosmetic.Rarity.EPIC, 2000,
            "Deep blue, split by a pale crevasse.", "cape_glacier.png"));

      add(new Cosmetic("cape_ember", "Ember", CosmeticType.CAPE,
            Cosmetic.Rarity.LEGENDARY, 2400,
            "The one warm thing in the wardrobe.", "cape_ember.png"));

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

      add(new Cosmetic("hat_tophat", "Top Hat", CosmeticType.HAT,
            Cosmetic.Rarity.RARE, 700,
            "Absurdly formal for a cannon war.", null, 0x14161C));

      add(new Cosmetic("hat_horns", "Ice Horns", CosmeticType.HAT,
            Cosmetic.Rarity.EPIC, 1400,
            "Two curved shards, swept back.", null, 0xBFE8FA));

      add(new Cosmetic("hat_antlers", "Antlers", CosmeticType.HAT,
            Cosmetic.Rarity.EPIC, 1600,
            "Branched, and slightly too large.", null, 0x8A6A44));

      add(new Cosmetic("hat_visor", "Visor", CosmeticType.HAT,
            Cosmetic.Rarity.COMMON, 250,
            "A brim and nothing else.", null, 0x2F7FA6));

      // ---- wings -------------------------------------------------------
      // Six shapes, not one shape in six colours. There used to be three
      // feathered pairs that differed only by tint, which made the shop look
      // padded and gave nobody a reason to pick between them.
      add(new Cosmetic("wings_angel", "Angel Wings", CosmeticType.WINGS,
            Cosmetic.Rarity.EPIC, 1800,
            "Three rows of long white primaries.", null, 0xF2FAFF));

      add(new Cosmetic("wings_dragon", "Dragon Wings", CosmeticType.WINGS,
            Cosmetic.Rarity.EPIC, 2000,
            "Membrane webbed over long clawed fingers.", null, 0x4A2E3E));

      add(new Cosmetic("wings_crystal", "Crystal Wings", CosmeticType.WINGS,
            Cosmetic.Rarity.EPIC, 2100,
            "Hard angular shards, no feathers at all.", null, 0x9FE8FF));

      add(new Cosmetic("wings_butterfly", "Butterfly Wings", CosmeticType.WINGS,
            Cosmetic.Rarity.RARE, 1500,
            "Four broad panels, spotted and slow.", null, 0x6FD6FF));

      add(new Cosmetic("wings_mech", "Mechanical Wings", CosmeticType.WINGS,
            Cosmetic.Rarity.LEGENDARY, 3000,
            "Hard plates on a jointed frame.", null, 0x8A99AA));

      add(new Cosmetic("wings_ethereal", "Ethereal Wings", CosmeticType.WINGS,
            Cosmetic.Rarity.LEGENDARY, 3400,
            "Layers of light with no solid edge.", null, 0x7FF0E0));

      // ---- pets --------------------------------------------------------
      // Ordered small to large. Price tracks size and rarity rather than effort,
      // because a legendary that is the same size as a common does not feel
      // legendary however good it looks.
      add(new Cosmetic("pet_chick", "Chick", CosmeticType.PET,
            Cosmetic.Rarity.COMMON, 300,
            "Almost entirely head. Rides your shoulder.", null, 0xFFD84A));

      add(new Cosmetic("pet_slime", "Frost Slime", CosmeticType.PET,
            Cosmetic.Rarity.COMMON, 400,
            "A blob with a face. Deeply content.", null, 0x92E2F6));

      add(new Cosmetic("pet_slime_lava", "Lava Slime", CosmeticType.PET,
            Cosmetic.Rarity.RARE, 700,
            "The same blob, considerably warmer.", null, 0xF2792A));

      add(new Cosmetic("pet_bunny", "Snow Bunny", CosmeticType.PET,
            Cosmetic.Rarity.COMMON, 450,
            "Long ears, pink nose, cotton tail.", null, 0xFEFEFF));

      add(new Cosmetic("pet_bunny_brown", "Brown Bunny", CosmeticType.PET,
            Cosmetic.Rarity.COMMON, 450,
            "The same, in a sensible coat.", null, 0xA5764C));

      add(new Cosmetic("pet_robin", "Robin", CosmeticType.PET,
            Cosmetic.Rarity.COMMON, 500,
            "Red breast, yellow beak, perches.", null, 0xD9503A));

      add(new Cosmetic("pet_raven", "Raven", CosmeticType.PET,
            Cosmetic.Rarity.RARE, 900,
            "Black on black. Sits and judges.", null, 0x2A2A36));

      add(new Cosmetic("pet_penguin", "Penguin", CosmeticType.PET,
            Cosmetic.Rarity.RARE, 800,
            "Waddles. Orange feet. Deeply serious.", null, 0x1B2028));

      add(new Cosmetic("pet_fox_arctic", "Arctic Fox", CosmeticType.PET,
            Cosmetic.Rarity.RARE, 1100,
            "White coat, tail twice its size.", null, 0xFCFDFE));

      add(new Cosmetic("pet_fox_red", "Red Fox", CosmeticType.PET,
            Cosmetic.Rarity.RARE, 1100,
            "Rust and cream, black stockings.", null, 0xC85A28));

      add(new Cosmetic("pet_fox_fennec", "Fennec Fox", CosmeticType.PET,
            Cosmetic.Rarity.EPIC, 1400,
            "Sand-coloured, and mostly ears.", null, 0xE8CFA6));

      add(new Cosmetic("pet_owl", "Snowy Owl", CosmeticType.PET,
            Cosmetic.Rarity.RARE, 1200,
            "Mostly eyes. Rides your shoulder.", null, 0xFDFEFF));

      add(new Cosmetic("pet_monkey", "Monkey", CosmeticType.PET,
            Cosmetic.Rarity.RARE, 1300,
            "Long arms, curled tail, no manners.", null, 0x6B4A32));

      add(new Cosmetic("pet_seal", "Baby Seal", CosmeticType.PET,
            Cosmetic.Rarity.RARE, 1000,
            "Round, flippered, permanently pleased.", null, 0xC9D6E0));

      add(new Cosmetic("pet_redpanda", "Red Panda", CosmeticType.PET,
            Cosmetic.Rarity.EPIC, 1800,
            "Banded tail, white cheeks, faintly smug.", null, 0xC46A34));

      add(new Cosmetic("pet_snowman", "Snowman", CosmeticType.PET,
            Cosmetic.Rarity.RARE, 900,
            "Waddles along behind you. Scarf included.", null, 0xFFFFFF));

      add(new Cosmetic("pet_polarbear", "Polar Bear", CosmeticType.PET,
            Cosmetic.Rarity.EPIC, 2200,
            "Large, white, unhurried.", null, 0xF4F7F9));

      add(new Cosmetic("pet_brownbear", "Brown Bear", CosmeticType.PET,
            Cosmetic.Rarity.EPIC, 2200,
            "The same bear, browner and no friendlier.", null, 0x7A5334));

      add(new Cosmetic("pet_yeti", "Yeti", CosmeticType.PET,
            Cosmetic.Rarity.LEGENDARY, 3200,
            "Barrel chest, tusks, arms to the floor.", null, 0xC7D8E6));

      add(new Cosmetic("pet_snowgolem", "Snow Golem", CosmeticType.PET,
            Cosmetic.Rarity.LEGENDARY, 3400,
            "Taller than you are. Wears a bucket.", null, 0xF8FBFD));

      add(new Cosmetic("pet_dragon", "Ice Dragon", CosmeticType.PET,
            Cosmetic.Rarity.LEGENDARY, 3800,
            "Ridged back, folded wings, real size.", null, 0x8FB6EE));

      add(new Cosmetic("pet_dragon_ember", "Ember Dragon", CosmeticType.PET,
            Cosmetic.Rarity.LEGENDARY, 3800,
            "The same beast, banked in coals.", null, 0xC4451E));

      add(new Cosmetic("pet_wyvern", "Frost Wyvern", CosmeticType.PET,
            Cosmetic.Rarity.LEGENDARY, 4500,
            "The biggest thing in here. Wings spread.", null, 0x4A6FA8));

      // ---- trails ------------------------------------------------------
      add(new Cosmetic("trail_frost", "Frost Trail", CosmeticType.TRAIL,
            Cosmetic.Rarity.COMMON, 300,
            "Cold air where you've been.", null, 0xA8F2FF));

      add(new Cosmetic("trail_aurora", "Aurora Trail", CosmeticType.TRAIL,
            Cosmetic.Rarity.EPIC, 1400,
            "Green and blue, fading behind you.", null, 0x5AFFD2));

      add(new Cosmetic("trail_ember", "Ember Trail", CosmeticType.TRAIL,
            Cosmetic.Rarity.RARE, 900,
            "Warm orange, cooling as it fades.", null, 0xFF7A2A));

      add(new Cosmetic("trail_void", "Void Trail", CosmeticType.TRAIL,
            Cosmetic.Rarity.EPIC, 1600,
            "A dark ribbon that swallows the light.", null, 0x2A1240));

      add(new Cosmetic("trail_rainbow", "Rainbow Trail", CosmeticType.TRAIL,
            Cosmetic.Rarity.LEGENDARY, 2600,
            "Cycles the whole way round as it goes.", null, 0xFF4444));

      add(new Cosmetic("trail_snow", "Snow Trail", CosmeticType.TRAIL,
            Cosmetic.Rarity.COMMON, 400,
            "Plain white, and honest about it.", null, 0xFFFFFF));

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
