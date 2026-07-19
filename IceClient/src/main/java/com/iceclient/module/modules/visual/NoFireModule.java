package com.iceclient.module.modules.visual;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent.OverlayType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoFireModule extends Module {
   public NoFireModule() {
      super("No Fire", "Hides the fire overlay while burning", ModuleCategory.GENERAL, 0);
   }

   @SubscribeEvent
   public void onBlockOverlay(RenderBlockOverlayEvent event) {
      if(this.isEnabled()) {
         if(event.overlayType == OverlayType.FIRE) {
            event.setCanceled(true);
         }

      }
   }
}
