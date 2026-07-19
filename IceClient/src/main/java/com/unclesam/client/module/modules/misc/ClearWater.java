package com.unclesam.client.module.modules.misc;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.NumberSetting;
import net.minecraft.block.material.Material;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Removes the underwater screen overlay and thins the fog while submerged. */
public class ClearWater extends Module {

   private final BooleanSetting hideOverlay = (BooleanSetting)this.addSetting(new BooleanSetting("Hide water overlay", true));
   private final BooleanSetting clearFog = (BooleanSetting)this.addSetting(new BooleanSetting("Clear water fog", true));
   private final NumberSetting fogDensity = (NumberSetting)this.addSetting(new NumberSetting("Fog density", 0.02D, 0.0D, 0.5D, 0.01D));
   private final BooleanSetting hideFire = (BooleanSetting)this.addSetting(new BooleanSetting("Hide fire overlay", false));

   public ClearWater() {
      super("Clear Water", "Disable overlay of liquids & fog", ModuleCategory.MECHANIC);
   }

   @SubscribeEvent
   public void onBlockOverlay(RenderBlockOverlayEvent event) {
      if(!this.isEnabled()) {
         return;
      }

      // overlayType is a public field on 1.8.9, not a getter.
      if(event.overlayType == RenderBlockOverlayEvent.OverlayType.WATER && this.hideOverlay.get()) {
         event.setCanceled(true);
      } else if(event.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE && this.hideFire.get()) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
      if(!this.isEnabled() || !this.clearFog.get() || this.mc.thePlayer == null) {
         return;
      }

      if(event.block.getMaterial() == Material.water) {
         // density is a public field on 1.8.9; setting canceled makes it apply.
         event.density = (float)this.fogDensity.get();
         event.setCanceled(true);
      }

   }
}
