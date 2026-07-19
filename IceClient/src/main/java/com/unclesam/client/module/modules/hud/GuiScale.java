package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.NumberSetting;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Scales individual HUD elements independently of the global GUI scale.
 *
 * <p>Originally slated for a {@code GuiIngame} mixin, but Forge already brackets
 * each overlay element with {@code RenderGameOverlayEvent.Pre}/{@code Post},
 * which is a matched pair -- exactly what a push/pop needs. Wrapping those is
 * both simpler and safer than patching {@code renderGameOverlay}, where the
 * hotbar is drawn inline with no method boundary to inject against.
 *
 * <p>Scaling about the element's own anchor keeps a bigger hotbar centred and a
 * bigger health bar rooted to the bottom-left, rather than drifting toward the
 * screen corner as a plain {@code scale()} would.
 */
public class GuiScale extends Module {

   private final NumberSetting hotbar = this.addNumber("Hotbar", 1.0D, 0.5D, 2.0D, 0.05D);
   private final NumberSetting health = this.addNumber("Health & hunger", 1.0D, 0.5D, 2.0D, 0.05D);
   private final NumberSetting experience = this.addNumber("Experience bar", 1.0D, 0.5D, 2.0D, 0.05D);

   /** Non-zero only between a Pre we scaled and its matching Post. */
   private RenderGameOverlayEvent.ElementType scaling;

   public GuiScale() {
      super("GUI Scale", "Independent scale for hotbar, health and XP", ModuleCategory.HUD);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPre(RenderGameOverlayEvent.Pre event) {
      if(!this.isEnabled() || this.scaling != null) {
         return;
      }

      float s = this.scaleFor(event.type);
      if(s == 1.0F) {
         return;
      }

      ScaledResolution res = new ScaledResolution(this.mc);
      float px = this.anchorX(event.type, res);
      float py = this.anchorY(event.type, res);

      this.scaling = event.type;
      GlStateManager.pushMatrix();
      // Translate to the anchor, scale, translate back -- otherwise the element
      // grows toward the origin instead of in place.
      GlStateManager.translate(px, py, 0.0F);
      GlStateManager.scale(s, s, 1.0F);
      GlStateManager.translate(-px, -py, 0.0F);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onPost(RenderGameOverlayEvent.Post event) {
      // Compare against the latched type, not the settings: if the user changes
      // a slider mid-frame the Pre already pushed and this must still pop.
      if(this.scaling != null && this.scaling == event.type) {
         this.scaling = null;
         GlStateManager.popMatrix();
      }

   }

   protected void onDisable() {
      // A pending push would be orphaned if the module went off between Pre and
      // Post; the Post handler still runs, so only the latch needs clearing.
      this.scaling = null;
   }

   private float scaleFor(RenderGameOverlayEvent.ElementType type) {
      if(type == RenderGameOverlayEvent.ElementType.HOTBAR) {
         return (float)this.hotbar.get();
      } else if(type == RenderGameOverlayEvent.ElementType.HEALTH
            || type == RenderGameOverlayEvent.ElementType.FOOD
            || type == RenderGameOverlayEvent.ElementType.ARMOR) {
         return (float)this.health.get();
      } else if(type == RenderGameOverlayEvent.ElementType.EXPERIENCE) {
         return (float)this.experience.get();
      } else {
         return 1.0F;
      }
   }

   /** Hotbar and XP are centred; the stat bars grow from their own corner. */
   private float anchorX(RenderGameOverlayEvent.ElementType type, ScaledResolution res) {
      if(type == RenderGameOverlayEvent.ElementType.HEALTH
            || type == RenderGameOverlayEvent.ElementType.ARMOR) {
         return (float)res.getScaledWidth() / 2.0F - 91.0F;
      } else if(type == RenderGameOverlayEvent.ElementType.FOOD) {
         return (float)res.getScaledWidth() / 2.0F + 91.0F;
      } else {
         return (float)res.getScaledWidth() / 2.0F;
      }
   }

   private float anchorY(RenderGameOverlayEvent.ElementType type, ScaledResolution res) {
      return (float)res.getScaledHeight();
   }
}
