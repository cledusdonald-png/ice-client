package com.iceclient.module.modules.schematic;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.schematic.Schematic;
import com.iceclient.schematic.SchematicManager;
import com.iceclient.schematic.SchematicRenderer;
import com.iceclient.schematica.SchematicaBridge;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

/**
 * Sets a freshly loaded schematic's height, once, when it loads.
 *
 * <p>The earlier version forced the Y <em>every tick</em>, which is what made it
 * feel broken from both directions: with follow-player on it dragged the
 * schematic down as you flew, and with it off it yanked every schematic to a
 * fixed 64 the moment you loaded. Neither is useful.
 *
 * <p>It now acts exactly once per load. Loading a schematic gives it a new
 * identity token, and when that token changes this drops the schematic to your
 * current Y (or the fixed level, if you prefer that) and then leaves it alone --
 * so you can fly around, nudge it, Move Here, without it fighting you.
 */
public class AutoYLevel extends Module {

   public final NumberSetting yLevel = this.addNumber("Y Level", 64.0D, 0.0D, 256.0D, 1.0D);
   /**
    * Level a newly loaded schematic to your own Y instead of the fixed level
    * above. On by default, because "put it where I'm standing" is what you want
    * almost every time.
    */
   public final BooleanSetting useMyY = this.addBool("Use My Y On Load", true);

   /** Identity of the schematic we last levelled, so we do it once per load. */
   private int lastToken;

   public AutoYLevel() {
      super("Auto Y Level", "Levels a schematic to your Y when it loads", ModuleCategory.PRINTER);
   }

   protected void onEnable() {
      // Re-level whatever is already loaded the moment the module is switched
      // on, rather than waiting for the next load.
      this.lastToken = 0;
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(!this.isEnabled() || event.phase != Phase.END || this.mc.thePlayer == null) {
         return;
      }

      if(SchematicaBridge.isAvailable() && SchematicaBridge.hasSchematic()) {
         int token = SchematicaBridge.schematicToken();
         if(token == 0 || token == this.lastToken) {
            return; // same schematic -- already levelled, leave it be
         }

         this.lastToken = token;

         int targetY = this.useMyY.get()
               ? (int)Math.floor(this.mc.thePlayer.posY)
               : this.yLevel.getInt();

         int[] pos = SchematicaBridge.position();
         if(pos != null && pos[1] != targetY) {
            SchematicaBridge.nudge(0, targetY - pos[1], 0);
         }

         return;
      }

      // Ice's own SchematicManager, used only when Schematica is absent.
      if(SchematicManager.isLoaded()) {
         int token = System.identityHashCode(SchematicManager.getLoaded());
         if(token == this.lastToken) {
            return;
         }

         this.lastToken = token;

         Schematic s = SchematicManager.getLoaded();
         BlockPos o = s.getOrigin();
         int targetY = this.useMyY.get() ? (int)Math.floor(this.mc.thePlayer.posY) : this.yLevel.getInt();
         if(o.getY() != targetY) {
            s.setOrigin(new BlockPos(o.getX(), targetY, o.getZ()));
            SchematicRenderer.invalidate();
         }
      }
   }
}
