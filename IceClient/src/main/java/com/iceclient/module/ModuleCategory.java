package com.iceclient.module;

public enum ModuleCategory {
   HUD("HUD"),
   COMBAT("Combat"),
   FACTIONS("Factions"),
   PRINTER("Printer"),
   MECHANIC("Mechanic"),
   GENERAL("General"),
   PVP("PvP"),
   ADMIN("Admin");

   public final String label;

   /**
    * The category tabs on the Modules page.
    *
    * <p>{@link #ADMIN} is deliberately absent: it has its own sidebar entry
    * behind an unlock prompt, so listing it here would show its modules on the
    * ordinary grid and defeat the point.
    */
   public static final ModuleCategory[] TABS = new ModuleCategory[]{HUD, COMBAT, FACTIONS, PRINTER, MECHANIC};

   private ModuleCategory(String label) {
      this.label = label;
   }
}
