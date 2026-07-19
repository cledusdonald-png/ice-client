package com.unclesam.client.mixin.mixins;

import com.unclesam.client.module.modules.render.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swaps in FreeLook's camera rotation for the duration of camera setup, then
 * puts the real rotation back before anything else in the frame reads it.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    private float ice$yaw;
    private float ice$pitch;
    private float ice$prevYaw;
    private float ice$prevPitch;
    private boolean ice$swapped;

    /**
     * prev* is set to match so the interpolation inside orientCamera doesn't
     * smear between the real rotation and the camera one.
     */
    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void ice$freeLookIn(float partialTicks, CallbackInfo ci) {
        ice$swapped = false;
        if (!FreeLook.isActive()) return;
        Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (view == null) return;

        ice$yaw = view.rotationYaw;
        ice$pitch = view.rotationPitch;
        ice$prevYaw = view.prevRotationYaw;
        ice$prevPitch = view.prevRotationPitch;

        view.rotationYaw = view.prevRotationYaw = FreeLook.getCamYaw();
        view.rotationPitch = view.prevRotationPitch = FreeLook.getCamPitch();
        ice$swapped = true;
    }

    @Inject(method = "orientCamera", at = @At("RETURN"))
    private void ice$freeLookOut(float partialTicks, CallbackInfo ci) {
        if (!ice$swapped) return;
        Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (view != null) {
            view.rotationYaw = ice$yaw;
            view.rotationPitch = ice$pitch;
            view.prevRotationYaw = ice$prevYaw;
            view.prevRotationPitch = ice$prevPitch;
        }
        ice$swapped = false;
    }
}
