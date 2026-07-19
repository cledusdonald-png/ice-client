package com.iceclient.module.modules.schematic;

import com.iceclient.gui.ClickGuiScreen;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.ModuleManager;
import com.iceclient.schematic.Schematic;
import com.iceclient.schematic.SchematicManager;
import com.iceclient.schematic.SchematicRenderer;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.NumberSetting;
import java.io.File;
import java.util.List;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import org.lwjgl.input.Keyboard;

public class SchemKeybinds extends Module {
   public final KeybindSetting keybindLoad = this.addKeybind("Load", 0);
   public final KeybindSetting keybindControl = this.addKeybind("Control", 0);
   public final KeybindSetting keybindMoveHere = this.addKeybind("Move Here", 0);
   public final KeybindSetting keybindIncLayer = this.addKeybind("Inc Layer", 0);
   public final KeybindSetting keybindDecLayer = this.addKeybind("Dec Layer", 0);
   public final KeybindSetting keyBindSave = this.addKeybind("Save", 0);
   public final KeybindSetting keybindToggleLayers = this.addKeybind("Toggle Layers", 0);
   public final KeybindSetting keybindTogglePrinter = this.addKeybind("Toggle Printer", 0);
   public final KeybindSetting keybindToggleRender = this.addKeybind("Toggle Render", 0);
   // Directional nudges. Forward/back/left/right are relative to the way you're
   // facing, which is how you actually think about shifting a schematic in
   // place; up/down stay on the world Y axis.
   public final KeybindSetting keybindMoveForward = this.addKeybind("Move Forward", 0);
   public final KeybindSetting keybindMoveBackward = this.addKeybind("Move Backwards", 0);
   public final KeybindSetting keybindMoveLeft = this.addKeybind("Move Left", 0);
   public final KeybindSetting keybindMoveRight = this.addKeybind("Move Right", 0);
   public final KeybindSetting keybindMoveUp = this.addKeybind("Move Up", 0);
   public final KeybindSetting keybindMoveDown = this.addKeybind("Move Down", 0);
   public final NumberSetting moveStep = this.addNumber("Move Step", 1.0D, 1.0D, 16.0D, 1.0D);
   public final BooleanSetting showEnabled = this.addBool("Show Enabled", true);
   private boolean layersOn;

   public SchemKeybinds() {
      super("Schem Keybinds", "Hotkeys for schematic controls", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onKey(KeyInputEvent event) {
      if(this.isEnabled() && this.mc.currentScreen == null) {
         if(Keyboard.getEventKeyState()) {
            int key = Keyboard.getEventKey();
            if(this.match(key, this.keybindMoveHere)) {
               SchematicManager.moveToPlayer();
               this.msg("Moved schematic to player");
            } else if(this.match(key, this.keybindIncLayer)) {
               this.adjustLayer(1);
            } else if(this.match(key, this.keybindDecLayer)) {
               this.adjustLayer(-1);
            } else if(this.match(key, this.keybindToggleLayers)) {
               this.layersOn = !this.layersOn;
               SchematicRenderer.setLayer(this.layersOn?0:-1);
               this.msg(this.layersOn?"Layer mode ON":"Layer mode OFF");
            } else if(this.match(key, this.keybindTogglePrinter)) {
               Module m = ModuleManager.getByName("Printer");
               if(m != null) {
                  m.toggle();
                  this.msg("Printer " + (m.isEnabled()?"ON":"OFF"));
               }
            } else if(this.match(key, this.keybindToggleRender)) {
               Module m = ModuleManager.getByName("Schematic Preview");
               if(m != null) {
                  m.toggle();
                  this.msg("Render " + (m.isEnabled()?"ON":"OFF"));
               }
            } else if(this.match(key, this.keybindControl)) {
               this.mc.displayGuiScreen(new ClickGuiScreen());
            } else if(this.match(key, this.keyBindSave)) {
               this.msg("Schematic position saved");
            } else if(this.match(key, this.keybindLoad)) {
               List<File> files = SchematicManager.list();
               if(!files.isEmpty()) {
                  SchematicManager.load((File)files.get(0));
               }
            } else if(this.match(key, this.keybindMoveForward)) {
               this.nudgeFacing(1, 0);
            } else if(this.match(key, this.keybindMoveBackward)) {
               this.nudgeFacing(-1, 0);
            } else if(this.match(key, this.keybindMoveLeft)) {
               this.nudgeFacing(0, -1);
            } else if(this.match(key, this.keybindMoveRight)) {
               this.nudgeFacing(0, 1);
            } else if(this.match(key, this.keybindMoveUp)) {
               this.nudge(0, (int)this.moveStep.get(), 0);
            } else if(this.match(key, this.keybindMoveDown)) {
               this.nudge(0, -(int)this.moveStep.get(), 0);
            }

         }
      }
   }

   /**
    * Nudge relative to where the player is looking.
    *
    * <p>Yaw is bucketed to the nearest cardinal direction, so "forward" means
    * the axis you're facing rather than a diagonal drift.
    */
   private void nudgeFacing(int forward, int right) {
      if(this.mc.thePlayer == null) {
         return;
      }

      int step = (int)this.moveStep.get();
      int facing = net.minecraft.util.MathHelper.floor_double(
            (double)(this.mc.thePlayer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;

      // facing: 0 = south (+Z), 1 = west (-X), 2 = north (-Z), 3 = east (+X)
      int fx = facing == 1 ? -1 : (facing == 3 ? 1 : 0);
      int fz = facing == 0 ? 1 : (facing == 2 ? -1 : 0);
      // Right is forward rotated 90 degrees clockwise.
      int rx = -fz;
      int rz = fx;

      this.nudge((fx * forward + rx * right) * step, 0, (fz * forward + rz * right) * step);
   }

   private void nudge(int dx, int dy, int dz) {
      if(SchematicManager.isLoaded()) {
         Schematic s = SchematicManager.getLoaded();
         s.setOrigin(s.getOrigin().add(dx, dy, dz));
         SchematicRenderer.invalidate();
      }

      // Keep Schematica's own copy in step when it's the active renderer.
      if(com.iceclient.schematica.SchematicaBridge.isAvailable()) {
         com.iceclient.schematica.SchematicaBridge.nudge(dx, dy, dz);
      }

      BlockPos o = SchematicManager.isLoaded() ? SchematicManager.getLoaded().getOrigin() : null;
      this.msg("Moved schematic" + (o == null ? "" : " to " + o.getX() + ", " + o.getY() + ", " + o.getZ()));
   }

   private void adjustLayer(int delta) {
      if(SchematicManager.isLoaded()) {
         Schematic s = SchematicManager.getLoaded();
         BlockPos o = s.getOrigin();
         s.setOrigin(o.add(0, delta, 0));
         SchematicRenderer.invalidate();
         this.msg("Layer Y=" + s.getOrigin().getY());
      }
   }

   private boolean match(int key, KeybindSetting bind) {
      return bind.getKeyCode() != 0 && key == bind.getKeyCode();
   }

   private void msg(String s) {
      if(this.showEnabled.get() && this.mc.thePlayer != null) {
         this.mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[Schematic] " + EnumChatFormatting.GRAY + s));
      }
   }
}
