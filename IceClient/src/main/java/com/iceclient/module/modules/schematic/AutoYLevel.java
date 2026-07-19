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
      if(this.isEnabled() && event.phase == Phase.END) {
         if(this.mc.thePlayer != null && SchematicManager.isLoaded()) {
            Schematic s = SchematicManager.getLoaded();
            BlockPos o = s.getOrigin();
            int targetY = this.yLevel.getInt();
            if(this.doOnSchemMove.get() && this.mc.thePlayer != null) {
               targetY = (int)Math.floor(this.mc.thePlayer.posY);
            }

            if(o.getY() != targetY || !o.equals(this.lastOrigin)) {
               this.lastOrigin = o;
               if(o.getY() != targetY) {
                  s.setOrigin(new BlockPos(o.getX(), targetY, o.getZ()));
                  SchematicRenderer.invalidate();
               }

            }
         }
      }
   }
}
