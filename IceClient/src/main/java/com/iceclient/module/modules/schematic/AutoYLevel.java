package com.iceclient.module.modules.schematic;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.schematic.Schematic;
import com.iceclient.schematic.SchematicManager;
import com.iceclient.schematic.SchematicRenderer;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class AutoYLevel extends Module {
   public final NumberSetting yLevel = this.addNumber("Y Level", 64.0D, 0.0D, 256.0D, 1.0D);
   public final BooleanSetting doOnSchemMove = this.addBool("Do On Schem Move", true);
   private BlockPos lastOrigin = BlockPos.ORIGIN;

   public AutoYLevel() {
      super("Auto Y Level", "Pins the schematic base to a Y level", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(!this.isEnabled() || event.phase != Phase.END || this.mc.thePlayer == null) {
         return;
      }

      // Follow the player's own Y when "Do On Schem Move" is on, otherwise pin
      // to the fixed level. Following is what you want while flying up a cannon
      // stack; the fixed level is for a wall you keep re-placing at one height.
      int targetY = this.doOnSchemMove.get()
            ? (int)Math.floor(this.mc.thePlayer.posY)
            : this.yLevel.getInt();

      // Drive Schematica when it is present -- it is the schematic actually on
      // screen. Moving only Ice's own SchematicManager would leave the visible
      // schematic where it was, which reads as the module doing nothing.
      if(com.iceclient.schematica.SchematicaBridge.isAvailable()
            && com.iceclient.schematica.SchematicaBridge.hasSchematic()) {
         int[] pos = com.iceclient.schematica.SchematicaBridge.position();
         if(pos != null && pos[1] != targetY) {
            com.iceclient.schematica.SchematicaBridge.nudge(0, targetY - pos[1], 0);
         }

         return;
      }

      if(SchematicManager.isLoaded()) {
         Schematic s = SchematicManager.getLoaded();
         BlockPos o = s.getOrigin();
         if(o.getY() != targetY) {
            s.setOrigin(new BlockPos(o.getX(), targetY, o.getZ()));
            SchematicRenderer.invalidate();
            this.lastOrigin = s.getOrigin();
         }
      }
   }
}
