package com.iceclient.mixin.mixins;

import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.hud.BetterF3;
import net.minecraft.client.gui.GuiOverlayDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * BetterF3: trims the debug overlay down to the lines that are actually useful
 * in a Factions context.
 *
 * <p>Vanilla builds both columns as plain string lists -- {@code call()} on the
 * left, {@code getDebugInfoRight()} on the right -- and neither is exposed to
 * Forge. Filtering the returned lists is far more robust than trying to patch
 * the rendering, because the strings keep their vanilla formatting and spacing.
 */
@Mixin(GuiOverlayDebug.class)
public class MixinGuiOverlayDebug {

    @Inject(method = "call", at = @At("RETURN"), cancellable = true)
    private void iceclient$filterLeft(CallbackInfoReturnable<List<String>> cir) {
        BetterF3 m = iceclient$module();
        if (m == null || !m.isEnabled()) return;
        cir.setReturnValue(m.filterLeft(cir.getReturnValue()));
    }

    @Inject(method = "getDebugInfoRight", at = @At("RETURN"), cancellable = true)
    private void iceclient$filterRight(CallbackInfoReturnable<List<String>> cir) {
        BetterF3 m = iceclient$module();
        if (m == null || !m.isEnabled()) return;
        cir.setReturnValue(m.filterRight(cir.getReturnValue()));
    }

    /**
     * The lagometer is drawn separately from the text columns and is the single
     * biggest frame cost of having F3 open, so it gets its own switch.
     */
    @Inject(method = "renderLagometer", at = @At("HEAD"), cancellable = true)
    private void iceclient$lagometer(CallbackInfo ci) {
        BetterF3 m = iceclient$module();
        if (m != null && m.isEnabled() && !m.showLagometer()) {
            ci.cancel();
        }
    }

    private static BetterF3 iceclient$module() {
        return (BetterF3) ModuleManager.getByName("BetterF3");
    }
}
