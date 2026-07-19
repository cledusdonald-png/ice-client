package com.unclesam.client.module.modules.render;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.NumberSetting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

/**
 * Accumulation-buffer motion blur.
 *
 * <p>Blends each frame into the accumulation buffer and reads it back, so the
 * previous frames bleed through by {@code amount}. This is the only approach
 * that works without a framebuffer pipeline of our own on 1.8.9, but the
 * accumulation buffer is legacy GL -- plenty of modern drivers advertise zero
 * accum bits, in which case {@code glAccum} is a no-op or an error rather than a
 * crash. {@link #available} probes for that once and disables the module rather
 * than spamming GL errors every frame.
 */
public class MotionBlur extends Module {

   private final NumberSetting amount =
         this.addNumber("Amount", 0.55D, 0.05D, 0.95D, 0.05D);

   /** Tri-state: null until probed, then the answer for this GL context. */
   private Boolean available;

   /** The accum buffer holds garbage until we prime it with a full frame. */
   private boolean primed;

   public MotionBlur() {
      super("MotionBlur", "Blends previous frames for a smeared look", ModuleCategory.MECHANIC);
   }

   protected void onEnable() {
      // Re-probe on each enable: the context can change if the user toggles
      // fullscreen, and a stale "unsupported" would strand the module off.
      this.available = null;
      this.primed = false;
   }

   protected void onDisable() {
      this.primed = false;
   }

   @SubscribeEvent
   public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
      if(!this.isEnabled() || event.type != RenderGameOverlayEvent.ElementType.ALL) {
         return;
      }

      if(this.mc.theWorld == null || this.mc.gameSettings.showDebugInfo) {
         return;
      }

      if(!this.probe()) {
         return;
      }

      float blend = (float)this.amount.get();

      if(!this.primed) {
         // Load rather than accumulate for the first frame, otherwise whatever
         // was in the buffer at context creation ghosts for several seconds.
         GL11.glAccum(GL11.GL_LOAD, 1.0F);
         this.primed = true;
         return;
      }

      GL11.glAccum(GL11.GL_MULT, blend);
      GL11.glAccum(GL11.GL_ACCUM, 1.0F - blend);
      GL11.glAccum(GL11.GL_RETURN, 1.0F);
   }

   /**
    * True when this context actually has an accumulation buffer. Checked once
    * and cached; a context with zero accum red bits cannot blur.
    */
   private boolean probe() {
      if(this.available != null) {
         return this.available.booleanValue();
      }

      boolean ok;
      try {
         ok = GLContext.getCapabilities() != null
               && GL11.glGetInteger(GL11.GL_ACCUM_RED_BITS) > 0;
      } catch (Throwable t) {
         ok = false;
      }

      this.available = Boolean.valueOf(ok);
      if(!ok) {
         com.unclesam.client.notification.NotificationManager.post(
               "MotionBlur",
               "No accumulation buffer on this GPU",
               com.unclesam.client.notification.Notification.Type.ERROR);
         this.setEnabled(false);
      }

      return ok;
   }
}
