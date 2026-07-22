package com.iceclient.cosmetic;

/**
 * Cosmetic slots.
 *
 * <p>One item may be equipped per slot. Slots exist rather than a flat list so
 * the wardrobe can show "what am I wearing" as a fixed set of choices, and so
 * equipping a second cape replaces the first instead of stacking two.
 */
public enum CosmeticType {

   CAPE("Cape", "Worn on the back"),
   HAT("Hat", "Sits above the head"),
   WINGS("Wings", "Worn on the back, above the cape"),
   /**
    * Emotes are a slot in the same sense -- one bound at a time -- but they are
    * played on a keypress rather than worn, and they animate the player model
    * rather than adding geometry to it.
    */
   EMOTE("Emote", "Played on a keypress");

   private final String label;
   private final String blurb;

   CosmeticType(String label, String blurb) {
      this.label = label;
      this.blurb = blurb;
   }

   public String getLabel() { return this.label; }
   public String getBlurb() { return this.blurb; }
}
