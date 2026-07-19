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

      ScaledResolution res = event.resolution;
      int sf = Math.max(1, res.getScaleFactor());

      // Drawn in real framebuffer pixels, not GUI pixels.
      //
      // The GUI matrix maps a scaled space onto the window, and the screen
      // centre only lands on a whole GUI pixel when the scaled width happens to
      // be even. The rest of the time a 1-wide bar spans something like 212.5 to
      // 213.5 -- half-covering two pixel columns -- and with no antialiasing the
      // rasteriser picks one, which is what made the crosshair look lopsided and
      // what no amount of arithmetic in GUI space could fix.
      //
      // Scaling the matrix down by the GUI scale makes one unit one real pixel.
      // The centre is then exactly displayWidth/2, and because every size is
      // multiplied back up by the same factor each arm is a whole number of real
      // pixels: symmetric, and crisp at any GUI scale.
      float cx = (float)this.mc.displayWidth / 2.0F;
      float cy = (float)this.mc.displayHeight / 2.0F;

      float len = (float)this.size.get() * sf;
      float th = (float)this.thickness.get() * sf;
      float g = (float)this.gap.get() * sf;
      float half = th / 2.0F;
      int col = this.chroma.get() ? ColorUtil.withAlpha(ColorUtil.chroma(0), 255) : this.color.getRGB();

      cx += (float)this.offsetX.get() * sf;
      cy += (float)this.offsetY.get() * sf;

      GlStateManager.pushMatrix();
      GlStateManager.scale(1.0F / (float)sf, 1.0F / (float)sf, 1.0F);
      GlStateManager.enableBlend();
      // Set explicitly: whatever overlay element ran before this one leaves its
      // own blend function behind, and inheriting an inverting one would tint
      // the crosshair against the background instead of using its colour.
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
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
