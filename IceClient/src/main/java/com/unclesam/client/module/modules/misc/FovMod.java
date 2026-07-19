package com.unclesam.client.module.modules.misc;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.NumberSetting;
import net.minecraft.potion.Potion;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Replaces the FOV kick from Speed with your own per-amplifier offsets, and
 * optionally suppresses the sprinting FOV change.
 *
 * <p>Vanilla defaults are roughly 6/12/18/24/30 degrees for Speed I..V.
 */
public class FovMod extends Module {

   private final NumberSetting speed1 = (NumberSetting)this.addSetting(new NumberSetting("Speed I degrees", 6.0D, 0.0D, 60.0D, 1.0D));
   private final NumberSetting speed2 = (NumberSetting)this.addSetting(new NumberSetting("Speed II degrees", 12.0D, 0.0D, 60.0D, 1.0D));
   private final NumberSetting speed3 = (NumberSetting)this.addSetting(new NumberSetting("Speed III degrees", 18.0D, 0.0D, 60.0D, 1.0D));
   private final NumberSetting speed4 = (NumberSetting)this.addSetting(new NumberSetting("Speed IV degrees", 24.0D, 0.0D, 60.0D, 1.0D));
   private final NumberSetting speed5 = (NumberSetting)this.addSetting(new NumberSetting("Speed V+ degrees", 30.0D, 0.0D, 60.0D, 1.0D));
   private final BooleanSetting applySprinting = (BooleanSetting)this.addSetting(new BooleanSetting("Apply sprinting FOV", true));

   public FovMod() {
      super("FOV Customizer", "Customize the FOV offset applied by the Speed effect", ModuleCategory.MECHANIC);
   }

   @SubscribeEvent
   public void onFov(EntityViewRenderEvent.FOVModifier event) {
      if(!this.isEnabled() || this.mc.thePlayer == null) {
         return;
      }

      float fov = event.getFOV();

      if(!this.applySprinting.get() && this.mc.thePlayer.isSprinting()) {
         // Vanilla adds roughly 10% while sprinting; take it back off.
         fov /= 1.1F;
      }

      if(this.mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
         int amp = this.mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
         fov += (float)this.degreesFor(amp);
      }

      event.setFOV(fov);
   }

   private double degreesFor(int amplifier) {
      switch(amplifier) {
      case 0:
         return this.speed1.get();
      case 1:
         return this.speed2.get();
      case 2:
         return this.speed3.get();
      case 3:
         return this.speed4.get();
      default:
         return this.speed5.get();
      }
   }
}
