package com.unclesam.client.module.modules.pvp;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.RenderUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class TargetHudModule extends Module {
   private static final int MARGIN_TOP = 30;
   private static final int WIDTH = 120;
   private static final int HEIGHT = 34;
   private static final int PAD = 5;
   private final NumberSetting lingerSeconds = (NumberSetting)this.addSetting(new NumberSetting("Linger (s)", 3.0D, 0.5D, 10.0D, 0.5D));
   private EntityPlayer target;
   private long lastSeen;

   public TargetHudModule() {
      super("Target HUD", "Shows the health of the player you\'re looking at", ModuleCategory.PVP, 0);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.START) {
         if(this.mc.thePlayer != null && this.mc.objectMouseOver != null) {
            if(this.mc.objectMouseOver.entityHit instanceof EntityPlayer) {
               this.target = (EntityPlayer)this.mc.objectMouseOver.entityHit;
               this.lastSeen = System.currentTimeMillis();
            }

         }
      }
   }

   @SubscribeEvent
   public void onRenderOverlay(Post event) {
      if(this.isEnabled()) {
         if(event.type == ElementType.ALL) {
            if(this.target != null) {
               if(System.currentTimeMillis() - this.lastSeen <= (long)(this.lingerSeconds.get() * 1000.0D) && this.target.getHealth() > 0.0F) {
                  ScaledResolution res = new ScaledResolution(this.mc);
                  int left = res.getScaledWidth() / 2 - 60;
                  int top = 30;
                  RenderUtil.panel(left, top, left + 120, top + 34, -1879048192, -14013902);
                  RenderUtil.text(this.mc.fontRendererObj, this.target.getName(), left + 5, top + 5, -1250068);
                  float max = this.target.getMaxHealth();
                  float hp = Math.max(0.0F, Math.min(this.target.getHealth(), max));
                  float frac = max > 0.0F?hp / max:0.0F;
                  int barLeft = left + 5;
                  int barRight = left + 120 - 5;
                  int barTop = top + 34 - 5 - 6;
                  int barBottom = top + 34 - 5;
                  RenderUtil.rect(barLeft, barTop, barRight, barBottom, -12961214);
                  int fillRight = barLeft + (int)((float)(barRight - barLeft) * frac);
                  RenderUtil.rect(barLeft, barTop, fillRight, barBottom, this.healthColor(frac));
                  String hpText = String.format("%.1f", new Object[]{Float.valueOf(hp)});
                  int hpX = barRight - this.mc.fontRendererObj.getStringWidth(hpText);
                  RenderUtil.text(this.mc.fontRendererObj, hpText, hpX, barTop - 10, -1250068);
               } else {
                  this.target = null;
               }
            }
         }
      }
   }

   private int healthColor(float frac) {
      return frac > 0.5F?-11751600:(frac > 0.25F?-16121:-4048054);
   }
}
