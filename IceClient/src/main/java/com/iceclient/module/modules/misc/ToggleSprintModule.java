package com.iceclient.module.modules.misc;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class ToggleSprintModule extends Module {
   private final NumberSetting minHunger = (NumberSetting)this.addSetting(new NumberSetting("Min hunger", 6.0D, 0.0D, 20.0D, 1.0D));
   private final BooleanSetting allowSideways = (BooleanSetting)this.addSetting(new BooleanSetting("Sprint sideways too", false));

   public ToggleSprintModule() {
      super("Toggle Sprint", "Sprint automatically without holding the key", ModuleCategory.GENERAL, 19);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.START) {
         if(this.mc.thePlayer != null) {
            boolean moving = this.mc.gameSettings.keyBindForward.isKeyDown() || this.allowSideways.get() && (this.mc.gameSettings.keyBindLeft.isKeyDown() || this.mc.gameSettings.keyBindRight.isKeyDown());
            boolean hungerOk = this.mc.thePlayer.getFoodStats().getFoodLevel() > this.minHunger.getInt();
            if(moving && hungerOk && !this.mc.thePlayer.isSprinting()) {
               this.mc.thePlayer.setSprinting(true);
            }

         }
      }
   }
}
