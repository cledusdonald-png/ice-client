package com.unclesam.client.module.modules.render;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ColorSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.ColorUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Container-screen styling: a tinted backdrop and a pop-open animation.
 *
 * <p>Uses Forge's screen events rather than a {@code GuiContainer} mixin.
 * {@code DrawScreenEvent.Pre}/{@code Post} bracket the entire screen draw, so a
 * matrix pushed in one and popped in the other scales the whole container
 * without needing to know how it lays itself out -- and it then applies to
 * modded containers too, which a mixin on the vanilla draw path would miss.
 */
public class CustomMcGui extends Module {

   /** Long enough to read as motion, short enough not to delay a raid click. */
   private static final long ANIM_MS = 160L;

   private final BooleanSetting animate = this.addBool("Open animation", true);
   private final BooleanSetting tint = this.addBool("Tinted background", true);
   private final ColorSetting tintColor = this.addColor("Tint colour", 0xFF120C1F);
   private final NumberSetting tintAlpha = this.addNumber("Tint alpha", 140.0D, 0.0D, 255.0D, 5.0D);

   private long openedAt;
   private boolean pushed;

   public CustomMcGui() {
      super("Custom MC Gui", "Tinted backdrop and open animation for containers", ModuleCategory.MECHANIC);
   }

   @SubscribeEvent
   public void onOpen(GuiScreenEvent.InitGuiEvent.Post event) {
      if(event.gui instanceof GuiContainer) {
         this.openedAt = System.currentTimeMillis();
      }

   }

   /** Drawn after vanilla's dim so it layers on top rather than under it. */
   @SubscribeEvent
   public void onBackground(GuiScreenEvent.BackgroundDrawnEvent event) {
      if(!this.isEnabled() || !this.tint.get() || !(event.gui instanceof GuiContainer)) {
         return;
      }

      ScaledResolution res = new ScaledResolution(this.mc);
      GlStateManager.enableBlend();
      Gui.drawRect(0, 0, res.getScaledWidth(), res.getScaledHeight(),
            ColorUtil.withAlpha(this.tintColor.getRGB(), (int)this.tintAlpha.get()));
      GlStateManager.disableBlend();
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
      if(!this.isEnabled() || !this.animate.get() || !(event.gui instanceof GuiContainer)) {
         return;
      }

      float t = this.progress();
      if(t >= 1.0F) {
         return;
      }

      ScaledResolution res = new ScaledResolution(this.mc);
      float cx = (float)res.getScaledWidth() / 2.0F;
      float cy = (float)res.getScaledHeight() / 2.0F;

      // Start at 80% and ease up to full size, growing from the screen centre
      // where the container itself is anchored.
      float s = 0.8F + 0.2F * ease(t);

      this.pushed = true;
      GlStateManager.pushMatrix();
      GlStateManager.translate(cx, cy, 0.0F);
      GlStateManager.scale(s, s, 1.0F);
      GlStateManager.translate(-cx, -cy, 0.0F);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
      // Gated on the latch, not on the settings, so toggling the module while a
      // container is open cannot strand a pushed matrix.
      if(this.pushed) {
         this.pushed = false;
         GlStateManager.popMatrix();
      }

   }

   protected void onDisable() {
      this.pushed = false;
   }

   private float progress() {
      if(this.openedAt == 0L) {
         return 1.0F;
      }

      long age = System.currentTimeMillis() - this.openedAt;
      return age >= ANIM_MS ? 1.0F : (float)age / (float)ANIM_MS;
   }

   private static float ease(float t) {
      float inv = 1.0F - t;
      return 1.0F - inv * inv * inv;
   }
}
