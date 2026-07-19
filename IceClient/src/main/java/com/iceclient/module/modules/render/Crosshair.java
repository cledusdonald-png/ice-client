package com.iceclient.module.modules.render;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.ColorUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** A custom crosshair drawn over (or instead of) the vanilla one. */
public class Crosshair extends Module {

   private final BooleanSetting hideVanilla = (BooleanSetting)this.addSetting(new BooleanSetting("Hide vanilla", true));
   private final NumberSetting size = (NumberSetting)this.addSetting(new NumberSetting("Size", 5.0D, 1.0D, 20.0D, 1.0D));
   private final NumberSetting thickness = (NumberSetting)this.addSetting(new NumberSetting("Thickness", 1.0D, 1.0D, 5.0D, 1.0D));
   private final NumberSetting gap = (NumberSetting)this.addSetting(new NumberSetting("Gap", 2.0D, 0.0D, 10.0D, 1.0D));
   private final BooleanSetting dot = (BooleanSetting)this.addSetting(new BooleanSetting("Center dot", false));
   private final BooleanSetting chroma = (BooleanSetting)this.addSetting(new BooleanSetting("Chroma", false));
   private final ColorSetting color = (ColorSetting)this.addSetting(new ColorSetting("Color", -1));

   public Crosshair() {
      super("Crosshair", "Custom crosshair", ModuleCategory.MECHANIC);
   }

   /**
    * Draws in place of vanilla's crosshair, at the exact point in the overlay
    * pipeline vanilla would have drawn.
    *
    * <p>Previously this drew on Post for both CROSSHAIRS and ALL -- two draws a
    * frame, the second after every other overlay element had left its own GL
    * state behind, which is what made it render wrong. One draw, one well-defined
    * place, and the GL state is pushed and restored around it.
    */
   @SubscribeEvent
   public void onPre(RenderGameOverlayEvent.Pre event) {
      if(!this.isEnabled() || event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
         return;
      }

      if(this.mc.thePlayer == null || this.mc.gameSettings.thirdPersonView != 0) {
         return;
      }

      if(this.hideVanilla.get()) {
         event.setCanceled(true);
      }

      // The event carries the resolution already scaled for this frame; building
      // our own can disagree with the active matrix.
      ScaledResolution res = event.resolution;
      int cx = res.getScaledWidth() / 2;
      int cy = res.getScaledHeight() / 2;
      int len = (int)this.size.get();
      int th = (int)this.thickness.get();
      int g = (int)this.gap.get();
      int col = this.chroma.get() ? ColorUtil.withAlpha(ColorUtil.chroma(0), 255) : this.color.getRGB();

      // Integer division was the bug: with thickness 1, `th / 2` is 0, so every
      // arm sat one pixel off-centre and the whole crosshair looked lopsided.
      // Work out the two edges separately so odd thicknesses straddle the centre
      // evenly instead of rounding the same way twice.
      int lo = th / 2;
      int hi = th - lo;

      GlStateManager.pushMatrix();
      GlStateManager.enableBlend();
      GlStateManager.disableLighting();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      // Four arms, offset from centre by the gap.
      Gui.drawRect(cx - g - len, cy - lo, cx - g, cy + hi, col);
      Gui.drawRect(cx + g, cy - lo, cx + g + len, cy + hi, col);
      Gui.drawRect(cx - lo, cy - g - len, cx + hi, cy - g, col);
      Gui.drawRect(cx - lo, cy + g, cx + hi, cy + g + len, col);

      if(this.dot.get()) {
         Gui.drawRect(cx - lo, cy - lo, cx + hi, cy + hi, col);
      }

      // Leave the overlay pipeline exactly as we found it -- the elements drawn
      // after this one inherit whatever state we leave behind.
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
   }
}
