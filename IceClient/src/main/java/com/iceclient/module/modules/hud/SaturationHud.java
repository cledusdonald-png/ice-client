package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ModeSetting;
import net.minecraft.util.FoodStats;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AppleSkin-style hunger readout: saturation, exhaustion and food level.
 *
 * <p>Exhaustion has no getter in 1.8.9 -- {@code FoodStats.foodExhaustionLevel}
 * is private -- so it's read reflectively by both its MCP and SRG names. The
 * previous version printed a "Sat/Food" ratio under an "Exhaustion" label, which
 * was simply the wrong number wearing the right name.
 */
public class SaturationHud extends TextHudModule {

   private final BooleanSetting saturation = (BooleanSetting)this.addSetting(new BooleanSetting("Saturation", true));
   private final ModeSetting saturationScale = (ModeSetting)this.addSetting(new ModeSetting("Saturation scale", "Out of 20", new String[]{"Out of 20", "Out of 10", "Out of food", "Raw"}));
   private final BooleanSetting exhaustion = (BooleanSetting)this.addSetting(new BooleanSetting("Exhaustion", true));
   private final BooleanSetting hunger = (BooleanSetting)this.addSetting(new BooleanSetting("Hunger value", false));
   private final BooleanSetting decimals = (BooleanSetting)this.addSetting(new BooleanSetting("Show decimals", true));
   private final BooleanSetting labels = (BooleanSetting)this.addSetting(new BooleanSetting("Show labels", true));

   public SaturationHud() {
      super("Saturation", "Displays AppleSkin-style saturation and exhaustion",
            ModuleCategory.HUD, HudModule.Anchor.BOTTOM_LEFT, 0);
   }

   protected List<String> lines() {
      if(this.mc.thePlayer == null) {
         return Collections.emptyList();
      }

      FoodStats food = this.mc.thePlayer.getFoodStats();
      List<String> out = new ArrayList(3);

      if(this.hunger.get()) {
         out.add(this.line("Hunger", (float)food.getFoodLevel(), false) + "/20");
      }

      if(this.saturation.get()) {
         // Saturation can never exceed food level -- that's why it reads low
         // even on a full hunger bar until you eat something good. Showing it
         // against a maximum makes that relationship obvious.
         String s = this.line("Saturation", food.getSaturationLevel(), true);
         String scale = this.saturationScale.get();
         if("Out of 20".equals(scale)) {
            s = s + "/20";
         } else if("Out of 10".equals(scale)) {
            s = s + "/10";
         } else if("Out of food".equals(scale)) {
            s = s + "/" + food.getFoodLevel();
         }

         out.add(s);
      }

      if(this.exhaustion.get()) {
         float ex = this.exhaustionLevel(food);
         // Vanilla drains 1 saturation each time exhaustion passes 4.0.
         out.add(ex < 0.0F ? "Exhaustion: n/a" : this.line("Exhaustion", ex, true) + "/4.0");
      }

      return out;
   }

   private String line(String label, float value, boolean allowDecimals) {
      String v = allowDecimals && this.decimals.get()
            ? String.format("%.1f", Float.valueOf(value))
            : String.valueOf((int)value);
      return this.labels.get() ? label + ": " + v : v;
   }

   /** @return exhaustion, or -1 if the field couldn't be read. */
   private float exhaustionLevel(FoodStats food) {
      try {
         return ((Float)ReflectionHelper.getPrivateValue(FoodStats.class, food,
               new String[]{"foodExhaustionLevel", "field_75126_c"})).floatValue();
      } catch (Throwable var3) {
         return -1.0F;
      }
   }
}
