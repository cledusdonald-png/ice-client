package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import com.iceclient.setting.BooleanSetting;

import java.util.Collections;
import java.util.List;

/**
 * Movement-state readout: sprinting, sneaking, flying, standing still --
 * the "[Sprinting (Flyboost 2.0x)]" line Orbit shows.
 */
public class SprintDisplay extends TextHudModule {

   private final BooleanSetting brackets = (BooleanSetting)this.addSetting(new BooleanSetting("Brackets", true));
   private final BooleanSetting showFly = (BooleanSetting)this.addSetting(new BooleanSetting("Show flying", true));
   private final BooleanSetting showSneak = (BooleanSetting)this.addSetting(new BooleanSetting("Show sneaking", true));
   private final BooleanSetting showIdle = (BooleanSetting)this.addSetting(new BooleanSetting("Show standing still", true));

   public SprintDisplay() {
      super("Sprint Display", "Shows your current movement state",
            ModuleCategory.HUD, HudModule.Anchor.TOP_RIGHT, 20);
   }

   protected List<String> lines() {
      if(this.mc.thePlayer == null) {
         return Collections.emptyList();
      }

      String state = this.state();
      if(state == null) {
         return Collections.emptyList();
      }

      return Collections.singletonList(this.brackets.get() ? "[" + state + "]" : state);
   }

   private String state() {
      if(this.mc.thePlayer.capabilities.isFlying && this.showFly.get()) {
         return this.mc.thePlayer.isSprinting() ? "Flying (Boost)" : "Flying";
      }

      if(this.mc.thePlayer.isSneaking() && this.showSneak.get()) {
         return "Sneaking";
      }

      if(this.mc.thePlayer.isSprinting()) {
         return "Sprinting";
      }

      // Compare against last tick's position rather than motion, which stays
      // non-zero for a moment after input stops.
      boolean moving = Math.abs(this.mc.thePlayer.posX - this.mc.thePlayer.prevPosX) > 0.003D
            || Math.abs(this.mc.thePlayer.posZ - this.mc.thePlayer.prevPosZ) > 0.003D;
      if(moving) {
         return "Walking";
      }

      return this.showIdle.get() ? "Standing Still" : null;
   }
}
