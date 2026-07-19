package com.unclesam.client.module.modules.visual;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.RenderUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LowHpTintModule extends Module {
   private static final int EDGE = 40;
   private final NumberSetting threshold = (NumberSetting)this.addSetting(new NumberSetting("Below HP", 8.0D, 1.0D, 20.0D, 1.0D));
   private final NumberSetting strength = (NumberSetting)this.addSetting(new NumberSetting("Strength", 60.0D, 10.0D, 150.0D, 5.0D));

   public LowHpTintModule() {
      super("Low HP Tint", "Red screen edges when your health is low", ModuleCategory.GENERAL, 0);
   }

   @SubscribeEvent
   public void onRenderOverlay(Post event) {
      if(this.isEnabled() && event.type == ElementType.ALL) {
         if(this.mc.thePlayer != null && !this.mc.thePlayer.isDead) {
            float hp = this.mc.thePlayer.getHealth();
            float limit = (float)this.threshold.get();
            if(hp < limit) {
               float severity = Math.min(1.0F, Math.max(0.0F, (limit - hp) / limit));
               int alpha = (int)((double)severity * this.strength.get());
               if(alpha > 0) {
                  int color = Math.min(alpha, 255) << 24 | 16711680;
                  ScaledResolution res = new ScaledResolution(this.mc);
                  int w = res.getScaledWidth();
                  int h = res.getScaledHeight();
                  RenderUtil.rect(0, 0, w, 40, color);
                  RenderUtil.rect(0, h - 40, w, h, color);
                  RenderUtil.rect(0, 40, 40, h - 40, color);
                  RenderUtil.rect(w - 40, 40, w, h - 40, color);
               }
            }
         }
      }
   }
}
