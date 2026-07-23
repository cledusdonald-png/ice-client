package com.iceclient.mixin.mixins;

import com.iceclient.cosmetic.EmoteManager;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets emotes pose the player model.
 *
 * <p>Injected at the TAIL of {@code setRotationAngles} specifically: vanilla
 * computes the walk cycle, head tracking and item-holding pose inside this
 * method, so anything applied earlier is simply overwritten. Running last means
 * an emote wins for the limbs it sets and leaves the rest alone.
 */
@Mixin(ModelBiped.class)
public class MixinModelBiped {

   @Inject(method = "setRotationAngles", at = @At("TAIL"))
   private void iceclient$applyEmote(float limbSwing, float limbSwingAmount, float ageInTicks,
                                     float netHeadYaw, float headPitch, float scale,
                                     Entity entity, CallbackInfo ci) {
      if (entity instanceof EntityPlayer) {
         EmoteManager.pose((EntityPlayer) entity, (ModelBiped) (Object) this);
      }
   }
}
