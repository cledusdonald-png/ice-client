package com.iceclient.mixin.mixins;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.render.RgbRedstone;
import com.iceclient.util.ColorUtil;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces vanilla redstone wire tint with a chroma cycle when
 * {@link RgbRedstone} is enabled.
 */
@Mixin(BlockRedstoneWire.class)
public class MixinBlockRedstoneWire {

    @Inject(method = "colorMultiplier", at = @At("HEAD"), cancellable = true)
    private void iceclient$rgbRedstone(IBlockAccess world, BlockPos pos, int renderPass,
                                      CallbackInfoReturnable<Integer> cir) {
        Module m = ModuleManager.getByName("RGB Redstone");
        if (m == null || !m.isEnabled()) return;

        RgbRedstone rgb = (RgbRedstone) m;
        IBlockState state = world.getBlockState(pos);
        int power = (Integer) state.getValue(BlockRedstoneWire.POWER);
        if (rgb.isPoweredOnly() && power == 0) return;

        long t = System.currentTimeMillis();
        int offset = (pos.getX() * 31 + pos.getZ() * 17) * 7;
        float sat = rgb.getSaturation();
        int chroma = ColorUtil.chroma((int) (3000 / rgb.getSpeed()), offset, sat, 1f);
        // Blend a little vanilla red in at low power so lines still read as redstone
        float blend = power / 15f;
        int vanilla = 0xFF0000 | (power << 16);
        cir.setReturnValue(ColorUtil.interpolate(vanilla, chroma, 0.35f + blend * 0.65f));
    }
}
