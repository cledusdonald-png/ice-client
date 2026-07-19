package com.unclesam.client.module.modules.misc;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.NumberSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class FlyBoostModule extends Module {
   private static final float BASE_FLY_SPEED = 0.05F;
   private static final double VERTICAL_BASE = 0.15D;
   private final NumberSetting multiplier = (NumberSetting)this.addSetting(new NumberSetting("Speed", 3.0D, 1.0D, 10.0D, 0.5D));
   private final BooleanSetting vertical = (BooleanSetting)this.addSetting(new BooleanSetting("Boost vertical", true));
   private final BooleanSetting sprintingOnly = (BooleanSetting)this.addSetting(new BooleanSetting("Only when sprinting", false));

   public FlyBoostModule() {
      super("Fly Boost", "Faster flight (only where the server allows fast fly)", ModuleCategory.GENERAL, 0);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.START) {
         if(this.mc.thePlayer != null) {
            boolean flying = this.mc.thePlayer.capabilities.isFlying;
            boolean gated = this.sprintingOnly.get() && !this.mc.thePlayer.isSprinting();
            if(flying && !gated) {
               this.mc.thePlayer.capabilities.setFlySpeed(0.05F * (float)this.multiplier.get());
               if(this.vertical.get()) {
                  boolean up = this.mc.gameSettings.keyBindJump.isKeyDown();
                  boolean down = this.mc.gameSettings.keyBindSneak.isKeyDown();
                  if(up != down) {
                     double v = 0.15D * this.multiplier.get();
                     this.mc.thePlayer.motionY = up?v:-v;
                  }
               }
            } else {
               this.restore();
            }

         }
      }
   }

   protected void onDisable() {
      this.restore();
   }

   private void restore() {
      if(this.mc.thePlayer != null) {
         this.mc.thePlayer.capabilities.setFlySpeed(0.05F);
      }

   }
}
