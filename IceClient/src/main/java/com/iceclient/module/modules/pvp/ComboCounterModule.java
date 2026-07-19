package com.iceclient.module.modules.pvp;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.RenderUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ComboCounterModule extends Module {
   private final NumberSetting resetSeconds = (NumberSetting)this.addSetting(new NumberSetting("Reset (s)", 4.0D, 1.0D, 15.0D, 0.5D));
   private int combo;
   private long lastHit;

   public ComboCounterModule() {
      super("Combo Counter", "Counts your consecutive hits on a target", ModuleCategory.PVP, 0);
   }

   @SubscribeEvent
   public void onLivingHurt(LivingHurtEvent event) {
      if(this.isEnabled() && this.mc.thePlayer != null) {
         if(event.entity == this.mc.thePlayer) {
            this.combo = 0;
         } else {
            if(event.source != null && event.source.getEntity() == this.mc.thePlayer) {
               ++this.combo;
               this.lastHit = System.currentTimeMillis();
            }

         }
      }
   }

   @SubscribeEvent
   public void onRenderOverlay(Post event) {
      if(this.isEnabled()) {
         if(event.type == ElementType.ALL) {
            if(this.combo > 0) {
               if(System.currentTimeMillis() - this.lastHit > (long)(this.resetSeconds.get() * 1000.0D)) {
                  this.combo = 0;
               } else {
                  String label = this.combo + " combo";
                  ScaledResolution res = new ScaledResolution(this.mc);
                  int x = res.getScaledWidth() / 2 - this.mc.fontRendererObj.getStringWidth(label) / 2;
                  int y = res.getScaledHeight() / 2 - 40;
                  RenderUtil.text(this.mc.fontRendererObj, label, x, y, -4048054);
               }
            }
         }
      }
   }
}
