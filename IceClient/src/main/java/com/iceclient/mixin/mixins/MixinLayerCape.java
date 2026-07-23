package com.iceclient.mixin.mixins;

import com.iceclient.cosmetic.CosmeticManager;
import com.iceclient.cosmetic.CosmeticType;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the vanilla cape while an Ice cape is worn.
 *
 * <p>Both capes occupy the same plane, so leaving vanilla's in place makes the
 * two z-fight -- the textures flicker through each other as the camera moves,
 * which looks like a rendering bug in ours. Only one cape can be in that space,
 * and if someone has picked one of ours that is the one they asked for.
 *
 * <p>Cancelled rather than made transparent so vanilla does no work at all, and
 * so a Mojang or Optifine cape reappears intact the moment the Ice cape is
 * unequipped.
 */
@Mixin(LayerCape.class)
public class MixinLayerCape {

   @Inject(method = "doRenderLayer", at = @At("HEAD"), cancellable = true)
   private void iceclient$hideVanillaCape(AbstractClientPlayer player, float limbSwing,
                                          float limbSwingAmount, float partialTicks,
                                          float ageInTicks, float netHeadYaw,
                                          float headPitch, float scale, CallbackInfo ci) {
      if (player == null) {
         return;
      }

      boolean self = player == net.minecraft.client.Minecraft.getMinecraft().thePlayer;

      boolean wearingIce = self
            ? CosmeticManager.getEquippedItem(CosmeticType.CAPE) != null
                  || CosmeticManager.getCustomCape() != null
            : CosmeticManager.getRemote(player.getName(), CosmeticType.CAPE) != null;

      // Only your own vanilla cape can be hidden by the toggle: it is a local
      // preference, and blanking other people's would be deciding for them.
      if (wearingIce || (self && CosmeticManager.isVanillaCapeHidden())) {
         ci.cancel();
      }
   }
}
