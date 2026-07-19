package com.unclesam.client.mixin.mixins;

import com.unclesam.client.module.modules.render.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Diverts mouse look into FreeLook's camera while it's active, so the player's
 * own rotation never changes.
 */
@Mixin(Entity.class)
public class MixinEntity {

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void ice$freeLook(float yaw, float pitch, CallbackInfo ci) {
        if (!FreeLook.isActive()) return;
        if ((Object) this != Minecraft.getMinecraft().thePlayer) return;

        // Vanilla applies these as (yaw * 0.15) and (-pitch * 0.15); mirror that
        // scaling so FreeLook feels identical to normal looking.
        if (FreeLook.consumeMouse(yaw * 0.15F, -pitch * 0.15F)) {
            ci.cancel();
        }
    }
}
