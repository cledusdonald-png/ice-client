package com.iceclient.module.modules.schematic;

import com.iceclient.util.BindUtil;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.KeybindSetting;
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
