package com.iceclient.module;

public enum ModuleCategory {
   HUD("HUD"),
   COMBAT("Combat"),
   FACTIONS("Factions"),
   PRINTER("Printer"),
   MECHANIC("Mechanic"),
   GENERAL("General"),
   PVP("PvP");

   public final String label;
   public static final ModuleCategory[] TABS = new ModuleCategory[]{HUD, COMBAT, FACTIONS, PRINTER, MECHANIC};

   private ModuleCategory(String label) {
      this.label = label;
   }
}
