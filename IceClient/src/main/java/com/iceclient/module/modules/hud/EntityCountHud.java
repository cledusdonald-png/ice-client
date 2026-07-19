package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import com.iceclient.setting.ModeSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loaded entity counts -- the F3 numbers that actually matter while raiding. */
public class EntityCountHud extends TextHudModule {

   /**
    * The per-type breakdown moved to {@code EntityBreakdownHud} in the Admin
    * section -- its player count is the part worth hiding, and splitting them
    * means this one can be positioned and sized on its own.
    */
   private final ModeSetting format = (ModeSetting)this.addSetting(
         new ModeSetting("Format", "Count first", new String[]{"Count first", "Label first"}));

   public EntityCountHud() {
      super("Entity Count", "Counts loaded entities", ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 110);
   }

   protected List<String> lines() {
      if(this.mc.theWorld == null) {
         return Collections.emptyList();
      }

      int n = this.mc.theWorld.loadedEntityList.size();
      return Collections.singletonList(this.format.is("Label first")
            ? "Entities: " + n
            : n + " Entities");
   }
}
