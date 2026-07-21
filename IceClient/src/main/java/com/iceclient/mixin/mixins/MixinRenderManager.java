package com.iceclient.mixin.mixins;

import com.iceclient.module.modules.render.FreeLook;
import net.minecraft.client.renderer.entity.RenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Points billboarded rendering at the FreeLook camera.
 *
 * <p>Nametags, floating labels and particles all rotate to face
 * {@code playerViewY/X}, which {@code cacheActiveRenderInfo} fills in from the
 * <em>player's</em> rotation. FreeLook deliberately leaves the body still, so
 * everything billboarded keeps facing where your character points while you look
 * from somewhere else -- swing round behind and they turn edge-on, then vanish.
 *
 * <p>This has to be a mixin rather than an event handler. The values are set
 * during the world render, and vanilla draws its nameplates in the entity pass
 * immediately afterwards -- long before {@code RenderWorldLastEvent}, where our
 * own modules draw. Overwriting them at the tail of the method that sets them is
 * the only point that catches both.
 */
@Mixin(RenderManager.class)
public class MixinRenderManager {

   @Inject(method = "cacheActiveRenderInfo", at = @At("TAIL"))
   private void iceclient$aimAtFreeLookCamera(CallbackInfo ci) {
      if (FreeLook.isActive()) {
         RenderManager self = (RenderManager) (Object) this;
         self.playerViewY = FreeLook.getCamYaw();
         self.playerViewX = FreeLook.getCamPitch();
      }
   }
}
