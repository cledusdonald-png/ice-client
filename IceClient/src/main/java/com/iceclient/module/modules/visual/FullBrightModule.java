package com.iceclient.module.modules.visual;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class FullBrightModule extends Module {
   private static final float BRIGHT = 100.0F;
   private float saved = 1.0F;
   private boolean stored = false;

   public FullBrightModule() {
      super("Fullbright", "Maxes brightness so caves/night are lit", ModuleCategory.GENERAL, 0);
   }

   protected void onEnable() {
      if(this.mc.gameSettings != null) {
         if(!this.stored) {
            this.saved = this.mc.gameSettings.gammaSetting;
            this.stored = true;
         }

      }
   }

   protected void onDisable() {
      if(this.mc.gameSettings != null && this.stored) {
         this.mc.gameSettings.gammaSetting = this.saved;
         this.stored = false;
      }

   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.START) {
         if(this.mc.gameSettings != null) {
            if(this.mc.gameSettings.gammaSetting != 100.0F) {
               this.mc.gameSettings.gammaSetting = 100.0F;
            }

         }
      }
   }
}
