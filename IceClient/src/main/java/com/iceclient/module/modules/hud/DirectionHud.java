package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import com.iceclient.setting.BooleanSetting;
import net.minecraft.util.MathHelper;

import java.util.Collections;
import java.util.List;

/** Facing direction, optionally with the raw yaw. */
public class DirectionHud extends TextHudModule {

   private static final String[] NAMES = {"South", "South West", "West", "North West",
                                          "North", "North East", "East", "South East"};
   private static final String[] AXES = {"+Z", "-X +Z", "-X", "-X -Z",
                                         "-Z", "+X -Z", "+X", "+X +Z"};

   private final BooleanSetting showAxis = (BooleanSetting)this.addSetting(new BooleanSetting("Show axis", true));
   private final BooleanSetting showYaw = (BooleanSetting)this.addSetting(new BooleanSetting("Show yaw", false));

   public DirectionHud() {
      super("Direction", "Shows which way you're facing", ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 70);
   }

   protected List<String> lines() {
      if(this.mc.thePlayer == null) {
         return Collections.emptyList();
      }

      // Vanilla's own 8-way bucket: +0.5 then floor, wrapped to 0..7.
      int i = MathHelper.floor_double((double)(this.mc.thePlayer.rotationYaw * 8.0F / 360.0F) + 0.5D) & 7;
      StringBuilder sb = new StringBuilder(NAMES[i]);
      if(this.showAxis.get()) {
         sb.append(" [").append(AXES[i]).append(']');
      }

      if(this.showYaw.get()) {
         sb.append(' ').append((int)MathHelper.wrapAngleTo180_float(this.mc.thePlayer.rotationYaw)).append((char)176);
      }

      return Collections.singletonList(sb.toString());
   }
}
