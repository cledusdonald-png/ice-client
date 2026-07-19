package com.unclesam.client.module.modules.schematic;

import com.unclesam.client.util.BindUtil;
import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.KeybindSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.input.Keyboard;

public class KeepFly extends Module {
   public final BooleanSetting keepFlyX = this.addBool("Keep Fly X", true);
   public final KeybindSetting keepFlyKey = this.addKeybind("Keep Fly Key", 0);

   public KeepFly() {
      super("Keep Fly", "Maintains flight while working on schematics", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.END && this.mc.thePlayer != null) {
         boolean keyHeld = this.keepFlyKey.getKeyCode() == 0 || BindUtil.isDown(this.keepFlyKey.getKeyCode());
         if(keyHeld) {
            if(this.mc.thePlayer.capabilities.allowFlying) {
               this.mc.thePlayer.capabilities.isFlying = true;
            }

            if(this.keepFlyX.get()) {
               this.mc.thePlayer.motionX = 0.0D;
            }

         }
      }
   }
}
