package com.iceclient.mixin.mixins;

import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.schematic.EasyPlace;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * EasyPlace hook: gives the module a say before a block placement is sent.
 *
 * <p>Injected at HEAD of {@code onPlayerRightClick} because that runs before the
 * packet leaves -- cancelling here means the server never sees the misplace,
 * whereas anything later would only be able to break the block back out.
 *
 * <p>The hotbar swap the module may perform also has to happen at HEAD: vanilla
 * reads {@code heldStack} further down this same method, so switching slots
 * afterwards would place the old item anyway.
 */
@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Inject(method = "onPlayerRightClick", at = @At("HEAD"), cancellable = true)
    private void iceclient$easyPlace(EntityPlayerSP player, WorldClient worldIn, ItemStack heldStack,
                                    BlockPos hitPos, EnumFacing side, Vec3 hitVec,
                                    CallbackInfoReturnable<Boolean> cir) {
        EasyPlace m = (EasyPlace) ModuleManager.getByName("EasyPlace");
        if (m == null || !m.isEnabled()) return;

        // The schematic describes the block that should end up in the space
        // being placed into, which is the neighbour the clicked face points at,
        // not the block that was clicked.
        if (hitPos == null || side == null) return;

        if (m.handlePlacement(hitPos.offset(side))) {
            // False, not true: returning true tells vanilla the interaction
            // succeeded and it would swing the arm and start a place cooldown.
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
