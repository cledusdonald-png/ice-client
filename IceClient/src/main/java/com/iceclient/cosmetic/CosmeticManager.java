package com.iceclient.cosmetic;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the local player owns and wears, and what everyone else is wearing.
 *
 * <p><b>Local state</b> -- owned ids and the equipped item per slot -- is
 * persisted by {@code ConfigManager} and is the source of truth for your own
 * appearance. <b>Remote state</b> is whatever the presence service last reported
 * about other players; it is a cache, is allowed to be stale, and is never
 * written to disk.
 *
 * <p>Cosmetics are only ever visible to other people running Ice Client. Nothing
 * here reaches Mojang's cape system or another client's, so a cape shows to your
 * faction and to nobody else. That is worth being plain about before anyone
 * pays for one.
 */
public final class CosmeticManager {

   private static final Set<String> owned = new HashSet<String>();
   private static final Map<CosmeticType, String> equipped = new EnumMap<CosmeticType, String>(CosmeticType.class);

   /** Lower-cased player name -> slot -> cosmetic id, from the presence service. */
   private static final Map<String, Map<CosmeticType, String>> remote =
         new ConcurrentHashMap<String, Map<CosmeticType, String>>();

   /** Spendable balance. Earned in-client; see the note on the shop screen. */
   private static int balance = 0;

   /**
    * A custom cape from the user's own folder, or null.
    *
    * <p>Held separately from {@link #equipped} because it is not a catalogue id
    * -- it is a filename on this machine, means nothing to anyone else, and must
    * not be written into the equipped map where a later load would try to
    * resolve it against the registry and drop it.
    */
   private static String customCape;

   /**
    * Whether to hide the Mojang/Optifine cape even with no Ice cape on.
    *
    * <p>Separate from equipping, because "wear an Ice cape" and "stop wearing
    * the vanilla one" are different wishes. Hiding was previously only a side
    * effect of equipping, which left no way to simply take the vanilla cape off.
    */
   private static boolean hideVanillaCape;

   public static boolean isVanillaCapeHidden() {
      return hideVanillaCape;
   }

   public static void setVanillaCapeHidden(boolean v) {
      hideVanillaCape = v;
   }

   /**
    * Whether the pet rides your shoulder instead of walking behind you.
    *
    * <p>A preference rather than a property of the pet, because it depends on
    * what you are doing: a companion trailing you looks better standing around
    * a base, and is a liability in a corridor fight where it clips through
    * everything. Some pets default to perching -- an owl on the ground is a
    * strange sight -- but it is always yours to change.
    */
   private static boolean petOnShoulder;

   public static boolean isPetOnShoulder() {
      return petOnShoulder;
   }

   public static void setPetOnShoulder(boolean v) {
      petOnShoulder = v;
   }

   /** A pet from the user's own folder, or null. Same reasoning as customCape. */
   private static String customPet;

   public static String getCustomPet() {
      return customPet;
   }

   public static void setCustomPet(String name) {
      customPet = name;
      if(name != null) {
         equipped.remove(CosmeticType.PET);
      }
   }

   public static String getCustomCape() {
      return customCape;
   }

   /** Selecting a custom cape clears any catalogue cape, since only one renders. */
   public static void setCustomCape(String name) {
      customCape = name;
      if(name != null) {
         equipped.remove(CosmeticType.CAPE);
      }
   }

   private CosmeticManager() {
   }

   // ------------------------------------------------------------------
   // ownership
   // ------------------------------------------------------------------

   /**
    * Whether the item is available to wear.
    *
    * <p>Free items are owned implicitly rather than being granted at first run,
    * so a config that predates a new free cosmetic still gets it.
    */
   public static boolean owns(String id) {
      Cosmetic c = CosmeticRegistry.byId(id);
      if(c == null) {
         return false;
      }

      return c.isDefault() || owned.contains(id);
   }

   public static void grant(String id) {
      if(CosmeticRegistry.byId(id) != null) {
         owned.add(id);
      }
   }

   public static void revoke(String id) {
      owned.remove(id);
   }

   public static Set<String> getOwned() {
      return Collections.unmodifiableSet(owned);
   }

   /** Used by config loading; replaces the whole set. */
   public static void setOwned(Set<String> ids) {
      owned.clear();

      for(String id : ids) {
         if(CosmeticRegistry.byId(id) != null) {
            owned.add(id);
         }
      }
   }

   // ------------------------------------------------------------------
   // equipping
   // ------------------------------------------------------------------

   public static String getEquipped(CosmeticType type) {
      return equipped.get(type);
   }

   public static Cosmetic getEquippedItem(CosmeticType type) {
      return CosmeticRegistry.byId(equipped.get(type));
   }

   /**
    * Wears an item, or clears the slot when {@code id} is null.
    *
    * @return true if the slot changed
    */
   public static boolean equip(String id) {
      if(id == null) {
         return false;
      }

      Cosmetic c = CosmeticRegistry.byId(id);
      if(c == null || !owns(id)) {
         return false;
      }

      if(c.getType() == CosmeticType.CAPE) {
         customCape = null;
      }

      equipped.put(c.getType(), id);
      return true;
   }

   public static void unequip(CosmeticType type) {
      equipped.remove(type);
   }

   /** Used by config loading; ignores ids that no longer exist. */
   public static void setEquipped(CosmeticType type, String id) {
      Cosmetic c = CosmeticRegistry.byId(id);
      if(c != null && c.getType() == type) {
         equipped.put(type, id);
      }
   }

   // ------------------------------------------------------------------
   // balance
   // ------------------------------------------------------------------

   public static int getBalance() {
      return balance;
   }

   public static void setBalance(int v) {
      balance = Math.max(0, v);
   }

   public static void addBalance(int v) {
      balance = Math.max(0, balance + v);
   }

   /**
    * Buys an item.
    *
    * @return null on success, or why it failed
    */
   public static String purchase(String id) {
      Cosmetic c = CosmeticRegistry.byId(id);
      if(c == null) {
         return "That item doesn't exist.";
      }

      if(owns(id)) {
         return "You already own that.";
      }

      if(c.getPrice() <= 0) {
         // Priced at zero but not owned means it is a granted item -- a founder
         // cape, say -- and buying it is not how you get one.
         return "That one can't be bought.";
      }

      if(balance < c.getPrice()) {
         return "Not enough — you need " + (c.getPrice() - balance) + " more.";
      }

      balance -= c.getPrice();
      owned.add(id);
      return null;
   }

   // ------------------------------------------------------------------
   // other players
   // ------------------------------------------------------------------

   /** What another player is wearing in a slot, or null. */
   public static Cosmetic getRemote(String playerName, CosmeticType type) {
      if(playerName == null) {
         return null;
      }

      Map<CosmeticType, String> worn = remote.get(playerName.toLowerCase());
      if(worn == null) {
         return null;
      }

      return CosmeticRegistry.byId(worn.get(type));
   }

   public static void setRemote(String playerName, CosmeticType type, String id) {
      if(playerName == null) {
         return;
      }

      String key = playerName.toLowerCase();
      Map<CosmeticType, String> worn = remote.get(key);
      if(worn == null) {
         worn = new EnumMap<CosmeticType, String>(CosmeticType.class);
         remote.put(key, worn);
      }

      if(id == null) {
         worn.remove(type);
      } else {
         worn.put(type, id);
      }
   }

   public static void clearRemote() {
      remote.clear();
   }

   /** Emotes animate the model via MixinModelBiped and sync through /worn. */
   public static boolean canPlayEmote() {
      return true;
   }
}
