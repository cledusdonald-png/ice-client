package com.unclesam.client.module.modules.misc;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ModeSetting;
import com.unclesam.client.setting.NumberSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Client-side world time. Purely visual -- the server's time is unchanged, so
 * mobs, crops and everything else behave exactly as before.
 */
public class TimeChanger extends Module {

   private final ModeSetting preset = (ModeSetting)this.addSetting(new ModeSetting("Time", "Day", new String[]{"Day", "Noon", "Sunset", "Night", "Custom"}));
   private final NumberSetting customTime = (NumberSetting)this.addSetting(new NumberSetting("Custom ticks", 6000.0D, 0.0D, 24000.0D, 100.0D));
   private final BooleanSetting freeze = (BooleanSetting)this.addSetting(new BooleanSetting("Freeze time", true));

   public TimeChanger() {
      super("Time Changer", "Sets the world time client-side", ModuleCategory.MECHANIC);
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled() || this.mc.theWorld == null) {
         return;
      }

      if(!this.freeze.get()) {
         // Offset once per second so time still advances but stays in the band.
         if(this.mc.theWorld.getWorldTime() % 20L != 0L) {
            return;
         }
      }

      this.mc.theWorld.setWorldTime(this.ticks());
   }

   private long ticks() {
      String p = this.preset.get();
      if("Noon".equals(p)) {
         return 6000L;
      } else if("Sunset".equals(p)) {
         return 12000L;
      } else if("Night".equals(p)) {
         return 18000L;
      } else {
         return "Custom".equals(p) ? (long)this.customTime.get() : 1000L;
      }
   }
}
