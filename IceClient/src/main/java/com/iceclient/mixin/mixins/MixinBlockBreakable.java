package com.iceclient.mixin.mixins;

import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.misc.ClearGlass;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBreakable;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ClearGlass: stops glass drawing its faces, leaving a hole you can see and
 * shoot through while the block itself still exists.
 *
 * <p>Glass has no Forge render hook in 1.8.9 -- the decision is
 * {@code shouldSideBeRendered} on {@link BlockBreakable}, which glass inherits.
 * Returning false there skips the quad entirely, which is cheaper than drawing
 * a transparent one and is why this doubles as an FPS option.
 *
 * <p>Gated on the block type rather than applied to all of BlockBreakable: ice
 * and leaves also extend it, and turning those invisible is not what anyone
 * means by clear glass.
 */
@Mixin(BlockBreakable.class)
public class MixinBlockBreakable {

    @Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true)
    private void iceclient$clearGlass(IBlockAccess world, BlockPos pos, EnumFacing side,
                                      CallbackInfoReturnable<Boolean> cir) {
        ClearGlass m = (ClearGlass) ModuleManager.getByName("Clear Glass");
        if (m == null || !m.isEnabled()) return;

        Block self = (Block) (Object) this;

        boolean pane = self instanceof BlockPane || self instanceof BlockStainedGlassPane;
        boolean solid = self instanceof BlockGlass || self instanceof BlockStainedGlass;

        if ((solid && m.hidesBlocks()) || (pane && m.hidesPanes())) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
