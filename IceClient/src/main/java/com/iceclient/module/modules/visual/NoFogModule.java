package com.iceclient.module.modules.visual;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
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
