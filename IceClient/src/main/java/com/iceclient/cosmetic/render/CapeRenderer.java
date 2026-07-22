package com.iceclient.cosmetic.render;

import com.iceclient.cosmetic.Cosmetic;
import com.iceclient.cosmetic.CosmeticManager;
import com.iceclient.cosmetic.CosmeticType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws Ice Client capes on players wearing one.
 *
 * <p>Rendered in {@link RenderPlayerEvent.Post} rather than as a render layer,
 * because adding a layer means replacing the player renderer -- which fights
 * every other mod that does the same. The cost is having to rebuild the model
 * transform by hand: the Post event fires after {@code RendererLivingEntity}
 * has popped its matrix, so the translate/rotate/scale below reproduces what
 * vanilla did before rendering the body. Those numbers are copied from
 * {@code RendererLivingEntity.prepareScale} and {@code RenderPlayer}, and have
 * to stay in step with them.
 *
 * <p>The sway maths is vanilla's own from {@code LayerCape}: the cape trails a
 * "chasing" position that lags the player, so it lifts when you run and settles
 * when you stop, rather than being welded to your back.
 */
public final class CapeRenderer {

   /** Vanilla's player model scale, from {@code RenderPlayer.preRenderCallback}. */
   private static final float PLAYER_SCALE = 0.9375F;
   /** Vanilla's model-space offset, from {@code RendererLivingEntity.prepareScale}. */
   private static final float MODEL_Y = -1.5078125F;

   private final ModelRenderer cape;

   public CapeRenderer() {
      ModelBase owner = new ModelBase() {
      };
      owner.textureWidth = 64;
      owner.textureHeight = 32;

      this.cape = new ModelRenderer(owner, 0, 0);
      this.cape.setTextureSize(64, 32);
      this.cape.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1, 0.0F);
   }

   @SubscribeEvent
   public void onRenderPlayer(RenderPlayerEvent.Post event) {
      EntityPlayer p = event.entityPlayer;
      if(p == null || p.isInvisible()) {
         return;
      }

      Minecraft mc = Minecraft.getMinecraft();

      // A custom cape is yours only and takes precedence -- picking one clears
      // the catalogue slot, so both can never be set at once.
      net.minecraft.util.ResourceLocation tex = null;

      if(p == mc.thePlayer && CosmeticManager.getCustomCape() != null) {
         tex = com.iceclient.cosmetic.CustomCapes.textureFor(CosmeticManager.getCustomCape());
      }

      if(tex == null) {
         Cosmetic worn = capeFor(p);
         if(worn == null || worn.getTexture() == null) {
            return;
         }

         tex = worn.getTexture();
      }

      float pt = event.partialRenderTick;

      GlStateManager.pushMatrix();
      GlStateManager.enableRescaleNormal();

      // Rebuild the model transform the Post event has already unwound.
      float bodyYaw = interpolateRotation(p.prevRenderYawOffset, p.renderYawOffset, pt);
      GlStateManager.translate(event.x, event.y, event.z);
      GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.scale(-1.0F, -1.0F, 1.0F);
      GlStateManager.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
      GlStateManager.translate(0.0F, MODEL_Y, 0.0F);

      // ---- vanilla LayerCape sway ----
      GlStateManager.translate(0.0F, 0.0F, 0.125F);

      double dx = lerp(p.prevChasingPosX, p.chasingPosX, pt) - lerp(p.prevPosX, p.posX, pt);
      double dy = lerp(p.prevChasingPosY, p.chasingPosY, pt) - lerp(p.prevPosY, p.posY, pt);
      double dz = lerp(p.prevChasingPosZ, p.chasingPosZ, pt) - lerp(p.prevPosZ, p.posZ, pt);

      float yawRad = p.prevRenderYawOffset + (p.renderYawOffset - p.prevRenderYawOffset) * pt;
      double sin = MathHelper.sin(yawRad * (float)Math.PI / 180.0F);
      double cos = -MathHelper.cos(yawRad * (float)Math.PI / 180.0F);

      float lift = MathHelper.clamp_float((float)dy * 10.0F, -6.0F, 32.0F);
      float back = (float)(dx * sin + dz * cos) * 100.0F;
      float side = (float)(dx * cos - dz * sin) * 100.0F;

      if(back < 0.0F) {
         back = 0.0F;
      }

      float bob = p.prevCameraYaw + (p.cameraYaw - p.prevCameraYaw) * pt;
      float walked = p.prevDistanceWalkedModified
            + (p.distanceWalkedModified - p.prevDistanceWalkedModified) * pt;
      lift += MathHelper.sin(walked * 6.0F) * 32.0F * bob;

      if(p.isSneaking()) {
         lift += 25.0F;
      }

      GlStateManager.rotate(6.0F + back / 2.0F + lift, 1.0F, 0.0F, 0.0F);
      GlStateManager.rotate(side / 2.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager.rotate(-side / 2.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      mc.getTextureManager().bindTexture(tex);
      this.cape.render(0.0625F);

      GlStateManager.disableRescaleNormal();
      GlStateManager.popMatrix();
   }

   /**
    * The cape this player should be wearing.
    *
    * <p>Yours comes from local state so the wardrobe previews instantly; anyone
    * else's comes from the presence cache, and is null until they have been
    * reported -- which is why a friend's cape can take a heartbeat to appear.
    */
   private static Cosmetic capeFor(EntityPlayer p) {
      if(p == Minecraft.getMinecraft().thePlayer) {
         return CosmeticManager.getEquippedItem(CosmeticType.CAPE);
      }

      return CosmeticManager.getRemote(p.getName(), CosmeticType.CAPE);
   }

   private static double lerp(double prev, double now, float pt) {
      return prev + (now - prev) * (double)pt;
   }

   /** Vanilla's angle interpolation, which handles the 360-degree wrap. */
   private static float interpolateRotation(float prev, float now, float pt) {
      float d = now - prev;

      while(d < -180.0F) {
         d += 360.0F;
      }

      while(d >= 180.0F) {
         d -= 360.0F;
      }

      return prev + pt * d;
   }
}
