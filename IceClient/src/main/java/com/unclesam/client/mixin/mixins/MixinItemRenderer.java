package com.unclesam.client.mixin.mixins;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7-style item animation.
 *
 * <p>{@code equippedProgress} is private on {@link ItemRenderer} and there is
 * no Forge event for it, which is exactly why this needs a mixin rather than a
 * normal module. Pinning it to 1.0 removes the lower/raise animation you get
 * when switching items, which is the most visible part of the 1.7 feel.
 *
 * <p>The module itself ({@code OldAnimations}) still owns the toggle -- this
 * only gates on it.
 */
@Mixin(ItemRenderer.class)
public class MixinItemRenderer {

    @Inject(method = "updateEquippedItem", at = @At("HEAD"), cancellable = true)
    private void unclesam$oldAnimations(CallbackInfo ci) {
        Module m = ModuleManager.getByName("OldAnimations");
        if (m == null || !m.isEnabled()) return;
        // Skipping the vanilla update leaves equippedProgress where it is, so
        // the item never plays the swap dip.
        ci.cancel();
    }

    /**
     * LeftHand: mirror the first-person hand across the screen's vertical axis.
     *
     * <p>1.8.9 hard-codes the hand to the right inside this method, so there is
     * nothing to configure -- the only lever is the matrix it renders under.
     * Scaling X by -1 flips it, but that also reverses triangle winding, which
     * would make the arm and item render inside-out. Swapping the cull face to
     * FRONT for the duration compensates; {@link #unclesam$leftHandEnd} puts
     * both back.
     */
    /**
     * Latches what HEAD decided so RETURN pops if and only if HEAD pushed.
     *
     * <p>Re-reading the module at RETURN would be a bug: a toggle landing
     * between the two injections leaves an unbalanced {@code pushMatrix}, and a
     * leaked matrix corrupts every subsequent frame, not just this one.
     */
    private boolean unclesam$leftHandActive;

    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"))
    private void unclesam$leftHandStart(float partialTicks, CallbackInfo ci) {
        Module m = ModuleManager.getByName("LeftHand");
        this.unclesam$leftHandActive = m != null && m.isEnabled();
        if (!this.unclesam$leftHandActive) return;

        GlStateManager.pushMatrix();
        // Mirroring reverses triangle winding, so the arm would render
        // inside-out without also flipping which face gets culled.
        GlStateManager.scale(-1.0F, 1.0F, 1.0F);
        // 1.8.9's GlStateManager has no cull-face wrapper, so this goes
        // straight to GL.
        GL11.glCullFace(GL11.GL_FRONT);
    }

    @Inject(method = "renderItemInFirstPerson", at = @At("RETURN"))
    private void unclesam$leftHandEnd(float partialTicks, CallbackInfo ci) {
        if (!this.unclesam$leftHandActive) return;
        this.unclesam$leftHandActive = false;
        GL11.glCullFace(GL11.GL_BACK);
        GlStateManager.popMatrix();
    }
}
