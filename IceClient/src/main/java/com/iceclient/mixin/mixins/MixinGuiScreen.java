package com.iceclient.mixin.mixins;

import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.misc.ScrollableTooltips;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Makes oversized tooltips readable: shrinks them to fit, and lets them be
 * scrolled.
 *
 * <p>Vanilla's failure mode is bad. Forge's {@code GuiUtils.drawHoveringText}
 * clamps a tooltip to the screen, but a tooltip <em>taller</em> than the screen
 * cannot be clamped into it -- it gets cut off at the top and the bottom at
 * once. On an auction listing for a heavily enchanted relic that means the
 * enchant list, the price and the seller are all unreadable simultaneously.
 *
 * <p>Everything happens in one HEAD injection rather than in separate
 * {@code @ModifyVariable}s for x and y. Mixin does not guarantee the relative
 * order of several handlers on the same injection point, and the fit scale and
 * the scroll offset have to be applied consistently -- computing both in one
 * place removes that ordering question entirely.
 */
@Mixin(GuiScreen.class)
public class MixinGuiScreen {

    /** Matches vanilla's per-line advance inside a tooltip box. */
    private static final int LINE_H = 10;

    /** Border plus padding that the box adds on top of the text. */
    private static final int CHROME_H = 8;

    /** Never shrink past this -- below it the text stops being legible. */
    private static final float MIN_SCALE = 0.5F;

    @Inject(
            method = "drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void iceclient$fitTooltip(List<String> textLines, int x, int y, FontRenderer font,
                                     CallbackInfo ci) {
        ScrollableTooltips m = (ScrollableTooltips) ModuleManager.getByName("ScrollableTooltips");
        if (m == null || !m.isEnabled() || textLines == null || textLines.isEmpty()) return;

        GuiScreen self = (GuiScreen) (Object) this;
        int screenW = self.width;
        int screenH = self.height;
        if (screenW <= 0 || screenH <= 0) return;

        float natural = iceclient$fitScale(textLines.size(), screenH);
        m.markDrawn(natural < 1.0F);

        float scale = m.fitToScreen() ? natural : 1.0F;

        int offX = m.offsetX();
        int offY = m.offsetY();

        // Nothing to change: let vanilla draw it so unaffected tooltips take the
        // ordinary path and cost nothing.
        if (scale >= 1.0F && offX == 0 && offY == 0) return;

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);

        // Dividing the anchor by the scale keeps the tooltip under the cursor,
        // and handing GuiUtils the scaled-up screen size makes its own clamp
        // work in the same coordinate space the box is now drawn in -- without
        // that it would clamp against the unscaled screen and pin a shrunken
        // tooltip into the top-left corner.
        int sx = Math.round((float) (x + offX) / scale);
        int sy = Math.round((float) (y + offY) / scale);
        int sw = Math.round((float) screenW / scale);
        int sh = Math.round((float) screenH / scale);

        GuiUtils.drawHoveringText(textLines, sx, sy, sw, sh, -1, font);

        GlStateManager.popMatrix();
        ci.cancel();
    }

    /**
     * Scale needed to bring a tooltip of {@code lineCount} lines inside the
     * screen, or 1 when it already fits.
     *
     * <p>This uses the unwrapped line count, so a tooltip whose lines wrap ends
     * up slightly taller than predicted. That direction is the safe one: it
     * scales a little less than strictly needed rather than shrinking text that
     * was going to fit anyway.
     */
    private static float iceclient$fitScale(int lineCount, int screenH) {
        int needed = lineCount * LINE_H + CHROME_H;
        if (needed <= screenH) return 1.0F;

        float s = (float) screenH / (float) needed;
        return s < MIN_SCALE ? MIN_SCALE : s;
    }
}
