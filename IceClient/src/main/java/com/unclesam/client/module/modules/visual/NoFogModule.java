package com.unclesam.client.module.modules.visual;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoFogModule extends Module {
   public NoFogModule() {
      super("No Fog", "Removes render fog", ModuleCategory.GENERAL, 0);
   }

   @SubscribeEvent
   public void onFogDensity(FogDensity event) {
      if(this.isEnabled()) {
         event.density = 0.0F;
         event.setCanceled(true);
      }
   }
}
