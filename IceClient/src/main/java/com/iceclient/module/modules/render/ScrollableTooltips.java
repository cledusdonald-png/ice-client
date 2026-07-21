package com.iceclient.module.modules.render;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.NumberSetting;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 * Long item tooltips (kit contents, crate previews, /invsee'd gear) run off the
 * top and bottom of the screen and the rest is simply unreadable. This shows a
 * window of the lines instead and lets the scroll wheel move through it, with
 * "N more" markers so you know there's content either side.
 *
 * Deliberately done through ItemTooltipEvent -- rewriting the lines -- rather
 * than a render mixin, so it works with whatever else draws tooltips.
 */
public class ScrollableTooltips extends Module {

   private final NumberSetting maxLines = this.addNumber("Max lines", 18.0D, 6.0D, 40.0D, 1.0D);

   private int scroll;
   private ItemStack lastStack;

   public ScrollableTooltips() {
      super("Scrollable Tooltips", "Scroll through tooltips that are too tall for the screen", ModuleCategory.GENERAL);
   }

   @Override
   protected void onDisable() {
      this.scroll = 0;
      this.lastStack = null;
   }

   /** Wheel while an inventory screen is open moves the window. Not cancelled, so
    *  creative-tab scrolling and anything else still behaves normally. */
   @SubscribeEvent
   public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
      if (this.isEnabled() && this.mc.currentScreen instanceof GuiContainer) {
         int wheel = Mouse.getEventDWheel();
         if (wheel != 0) {
            this.scroll += wheel > 0 ? -1 : 1;
         }
      }
   }

   @SubscribeEvent
   public void onTooltip(ItemTooltipEvent event) {
      if (!this.isEnabled()) return;

      List<String> lines = event.toolTip;
      if (lines == null) return;

      // hovering something new starts from the top again
      if (event.itemStack != this.lastStack) {
         this.lastStack = event.itemStack;
         this.scroll = 0;
      }

      int max = (int) this.maxLines.get();
      if (lines.size() <= max) {
         this.scroll = 0;
         return;
      }

      int maxScroll = lines.size() - max;
      if (this.scroll < 0) this.scroll = 0;
      if (this.scroll > maxScroll) this.scroll = maxScroll;

      List<String> window = new ArrayList<String>(lines.subList(this.scroll, this.scroll + max));
      if (this.scroll > 0) {
         window.set(0, "§8▲ " + this.scroll + " more");
      }
      if (this.scroll < maxScroll) {
         window.set(window.size() - 1, "§8▼ " + (maxScroll - this.scroll) + " more");
      }

      lines.clear();
      lines.addAll(window);
   }
}
