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
   /**
    * Manual nudge, in GUI pixels, for anyone whose GUI scale still lands the
    * crosshair a hair off -- or who wants it deliberately offset. Half-steps
    * because the whole point is sub-pixel placement.
    */
   private final NumberSetting offsetX = (NumberSetting)this.addSetting(new NumberSetting("Offset X", 0.0D, -5.0D, 5.0D, 0.5D));
   private final NumberSetting offsetY = (NumberSetting)this.addSetting(new NumberSetting("Offset Y", 0.0D, -5.0D, 5.0D, 0.5D));

   public Crosshair() {
      super("Crosshair", "Custom crosshair", ModuleCategory.MECHANIC);
   }

   /**
    * Float-precision filled rect.
    *
    * <p>{@link Gui#drawRect} takes ints, which is what forced the crosshair onto
    * whole-pixel boundaries in the first place.
    */
   private static void rect(float left, float top, float right, float bottom, int color) {
      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;

      GlStateManager.color(r, g, b, a <= 0.0F ? 1.0F : a);
      org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_QUADS);
      org.lwjgl.opengl.GL11.glVertex2f(left, bottom);
      org.lwjgl.opengl.GL11.glVertex2f(right, bottom);
      org.lwjgl.opengl.GL11.glVertex2f(right, top);
      org.lwjgl.opengl.GL11.glVertex2f(left, top);
      org.lwjgl.opengl.GL11.glEnd();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
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

      // Float centre, not scaledWidth/2. Integer coordinates address pixel
      // *corners*, so an integer-aligned bar of odd thickness is always half a
      // pixel off true centre -- which at GUI scale 3 is three real pixels and
      // is exactly the "slightly off" you can see. Working in floats lets the
      // bar straddle the centre line properly.
      float cx = (float)res.getScaledWidth() / 2.0F;
      float cy = (float)res.getScaledHeight() / 2.0F;

      float len = (float)this.size.get();
      float th = (float)this.thickness.get();
      float g = (float)this.gap.get();
      float half = th / 2.0F;
      int col = this.chroma.get() ? ColorUtil.withAlpha(ColorUtil.chroma(0), 255) : this.color.getRGB();

      cx += (float)this.offsetX.get();
      cy += (float)this.offsetY.get();

      GlStateManager.pushMatrix();
      GlStateManager.enableBlend();
      GlStateManager.disableLighting();
      GlStateManager.disableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      // Four arms, offset from centre by the gap.
      rect(cx - g - len, cy - half, cx - g, cy + half, col);
      rect(cx + g, cy - half, cx + g + len, cy + half, col);
      rect(cx - half, cy - g - len, cx + half, cy - g, col);
      rect(cx - half, cy + g, cx + half, cy + g + len, col);

      if(this.dot.get()) {
         rect(cx - half, cy - half, cx + half, cy + half, col);
      }

      GlStateManager.enableTexture2D();

      // Leave the overlay pipeline exactly as we found it -- the elements drawn
      // after this one inherit whatever state we leave behind.
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
   }
}
