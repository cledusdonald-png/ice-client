package com.iceclient.module.modules.schematic;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.schematic.Schematic;
import com.iceclient.schematic.SchematicManager;
import com.iceclient.schematic.SchematicRenderer;
import com.iceclient.setting.KeybindSetting;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.input.Keyboard;

public class SchemArrowKeys extends Module {
   public final KeybindSetting moveBackwardKeybind = this.addKeybind("Move Backward", 208);
   public final KeybindSetting moveForwardKeybind = this.addKeybind("Move Forward", 200);
   public final KeybindSetting moveRightKeybind = this.addKeybind("Move Right", 205);
   public final KeybindSetting moveLeftKeybind = this.addKeybind("Move Left", 203);

   public SchemArrowKeys() {
      super("Schem Arrow Keys", "Move schematic with keybinds", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.END && this.mc.currentScreen == null) {
         Schematic s = SchematicManager.getLoaded();
         if(s != null) {
            int move = Keyboard.isKeyDown(42)?5:1;
            BlockPos o = s.getOrigin();
            boolean moved = false;
            if(this.isDown(this.moveBackwardKeybind)) {
               o = o.add(0, 0, move);
               moved = true;
            }

            if(this.isDown(this.moveForwardKeybind)) {
               o = o.add(0, 0, -move);
               moved = true;
            }

            if(this.isDown(this.moveLeftKeybind)) {
               o = o.add(-move, 0, 0);
               moved = true;
            }

            if(this.isDown(this.moveRightKeybind)) {
               o = o.add(move, 0, 0);
               moved = true;
            }

            if(moved) {
               s.setOrigin(o);
               SchematicRenderer.invalidate();
            }

         }
      }
   }

   private boolean isDown(KeybindSetting bind) {
      return bind.isDown();
   }
}
