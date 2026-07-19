package com.unclesam.client.module.modules.visual;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class NoWeatherModule extends Module {
   public NoWeatherModule() {
      super("No Weather", "Removes rain, snow and thunder (client-side)", ModuleCategory.MECHANIC, 0);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.START) {
         if(this.mc.theWorld != null) {
            this.mc.theWorld.setRainStrength(0.0F);
            this.mc.theWorld.setThunderStrength(0.0F);
         }
      }
   }
}
