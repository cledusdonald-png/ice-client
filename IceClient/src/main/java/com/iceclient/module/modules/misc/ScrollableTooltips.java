package com.iceclient.module.modules.misc;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Accumulates a scroll offset that {@code MixinGuiScreen} applies to tooltips.
 *
 * <p>The offset auto-resets once no tooltip has been drawn for a moment, which
 * is what makes this feel right in practice: scroll a long enchanted-book
 * tooltip, move to a different item, and the new one starts at its natural
 * position instead of inheriting the previous scroll.
 */
public class ScrollableTooltips extends Module {

   /** Moving off an item stops the draw calls; this is how long we wait. */
   private static final long IDLE_RESET_MS = 150L;

   private final BooleanSetting fitToScreen = this.addBool("Shrink to fit screen", true);
   private final NumberSetting speed = this.addNumber("Scroll speed", 10.0D, 1.0D, 40.0D, 1.0D);
   private final BooleanSetting horizontal = this.addBool("Shift scrolls sideways", true);
   private final BooleanSetting invert = this.addBool("Invert", false);

   private int offX;
   private int offY;
   private long lastDrawn;

   /** Whether the tooltip last drawn was actually too tall for the screen. */
   private boolean lastOversized;

   public ScrollableTooltips() {
      super("ScrollableTooltips", "Shrinks and scrolls tooltips too tall to fit", ModuleCategory.MECHANIC);
      // On by default: an auction tooltip taller than the screen is unreadable
      // at both ends in vanilla, and this is the thing that fixes it.
      this.setEnabled(true);
   }

   public boolean fitToScreen() {
      return this.fitToScreen.get();
   }

   protected void onDisable() {
      this.offX = 0;
      this.offY = 0;
   }

   public int offsetX() {
      return this.offX;
   }

   public int offsetY() {
      return this.offY;
   }

   /**
    * Called from the mixin so we know a tooltip is currently on screen, and
    * whether it was one that did not fit.
    */
   public void markDrawn(boolean oversized) {
      this.lastDrawn = System.currentTimeMillis();
      this.lastOversized = oversized;
   }

   /**
    * Uses the GUI mouse event rather than polling {@code Mouse.getDWheel()}:
    * polling consumes the delta, which would break scrolling in every
    * inventory-like screen that reads the wheel itself.
    */
   @SubscribeEvent
   public void onGuiMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
      if(!this.isEnabled()) {
         return;
      }

      if(System.currentTimeMillis() - this.lastDrawn > IDLE_RESET_MS) {
         this.offX = 0;
         this.offY = 0;
         return;
      }

      // Only take the wheel for tooltips that genuinely do not fit. Screens
      // like the Auction House scroll their own pages, and swallowing every
      // wheel turn while any tooltip happened to be visible would break that.
      if(!this.lastOversized) {
         return;
      }

      int wheel = Mouse.getEventDWheel();
      if(wheel == 0) {
         return;
      }

      int step = (int)this.speed.get() * (wheel > 0 ? 1 : -1) * (this.invert.get() ? -1 : 1);

      if(this.horizontal.get()
            && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
         this.offX += step;
      } else {
         this.offY += step;
      }

      // Swallow the event so the underlying screen does not also act on a
      // wheel turn the user meant for the tooltip.
      event.setCanceled(true);
   }
}
