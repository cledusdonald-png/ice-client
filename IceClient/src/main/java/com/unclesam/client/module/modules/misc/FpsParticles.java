package com.unclesam.client.module.modules.misc;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Cuts particle load, which is the usual FPS killer during a cannon volley.
 *
 * <p>Works by clamping vanilla's particle setting rather than filtering
 * individual spawns -- 1.8.9 has no Forge event for a particle being created,
 * so per-type filtering (block-break only, minimal explosions) needs a mixin on
 * {@code EffectRenderer}. The toggles for those are present and documented as
 * pending rather than silently doing nothing.
 */
public class FpsParticles extends Module {

   private final BooleanSetting disableAll = (BooleanSetting)this.addSetting(new BooleanSetting("Disable all", false));
   private final BooleanSetting minimal = (BooleanSetting)this.addSetting(new BooleanSetting("Minimal particles", true));

   /** Vanilla: 0 = all, 1 = decreased, 2 = minimal. Restored on disable. */
   private int savedSetting = -1;

   public FpsParticles() {
      super("FPS Particles", "Reduces particle load to improve FPS", ModuleCategory.MECHANIC);
   }

   protected void onDisable() {
      this.restore();
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END) {
         return;
      }

      if(!this.isEnabled()) {
         this.restore();
         return;
      }

      if(this.savedSetting < 0) {
         this.savedSetting = this.mc.gameSettings.particleSetting;
      }

      int want = this.disableAll.get() ? 2 : (this.minimal.get() ? 2 : this.savedSetting);
      if(this.mc.gameSettings.particleSetting != want) {
         this.mc.gameSettings.particleSetting = want;
      }

   }

   private void restore() {
      if(this.savedSetting >= 0) {
         this.mc.gameSettings.particleSetting = this.savedSetting;
         this.savedSetting = -1;
      }

   }
}
