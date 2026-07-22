package com.iceclient.cosmetic.render;

import com.iceclient.cosmetic.Cosmetic;
import com.iceclient.cosmetic.CosmeticManager;
import com.iceclient.cosmetic.CosmeticType;
import com.iceclient.cosmetic.CustomPets;
import com.iceclient.cosmetic.PetModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Pets that walk along the ground behind their owner.
 *
 * <p>The first version orbited the player's shoulder, which was easy -- it could
 * be drawn inside the player's own transform -- and looked like a floating
 * ornament rather than a companion. A pet that follows has to live in world
 * space with its own position, heading and gait, so it is simulated on tick and
 * drawn in {@link RenderWorldLastEvent} rather than hung off the player model.
 *
 * <p>Movement is deliberately lazy: the pet trails a point behind you and eases
 * toward it, so it falls behind when you sprint and catches up when you stop.
 * Chasing exactly would look robotic.
 */
public final class PetRenderer {

   /** How far behind the owner the pet tries to sit. */
   private static final double FOLLOW_DIST = 1.15D;
   /** Past this it gives up and teleports; otherwise it never catches a sprinting player. */
   private static final double TELEPORT_DIST = 12.0D;

   private final Map<String, PetState> pets = new HashMap<String, PetState>();

   /** Where one pet is, and how it is moving. */
   private static final class PetState {
      double x, y, z;
      double prevX, prevY, prevZ;
      float yaw, prevYaw;
      /** Drives the walk cycle; advances only while actually moving. */
      float gait;
      float prevGait;
      boolean placed;
   }

   // ------------------------------------------------------------------
   // simulation
   // ------------------------------------------------------------------

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END) {
         return;
      }

      Minecraft mc = Minecraft.getMinecraft();
      if(mc.theWorld == null || mc.thePlayer == null) {
         this.pets.clear();
         return;
      }

      // Drop state for anyone who left, or the map grows for a whole session.
      for(Iterator<Map.Entry<String, PetState>> it = this.pets.entrySet().iterator(); it.hasNext();) {
         if(mc.theWorld.getPlayerEntityByName(it.next().getKey()) == null) {
            it.remove();
         }
      }

      for(EntityPlayer p : mc.theWorld.playerEntities) {
         if(petFor(p) == null && customFor(p) == null) {
            this.pets.remove(p.getName());
            continue;
         }

         step(mc, p);
      }
   }

   private void step(Minecraft mc, EntityPlayer p) {
      PetState s = this.pets.get(p.getName());
      if(s == null) {
         s = new PetState();
         this.pets.put(p.getName(), s);
      }

      // Target: behind the owner, offset to one side so it is not hidden by them.
      double yawRad = Math.toRadians(p.renderYawOffset);
      double bx = p.posX - Math.sin(-yawRad) * -FOLLOW_DIST - Math.cos(-yawRad) * 0.35D;
      double bz = p.posZ - Math.cos(-yawRad) * FOLLOW_DIST + Math.sin(-yawRad) * 0.35D;
      double by = p.posY;

      if(!s.placed) {
         s.x = s.prevX = bx;
         s.y = s.prevY = by;
         s.z = s.prevZ = bz;
         s.yaw = s.prevYaw = p.renderYawOffset;
         s.placed = true;
         return;
      }

      s.prevX = s.x;
      s.prevY = s.y;
      s.prevZ = s.z;
      s.prevYaw = s.yaw;
      s.prevGait = s.gait;

      double dx = bx - s.x;
      double dz = bz - s.z;
      double dist = Math.sqrt(dx * dx + dz * dz);

      if(dist > TELEPORT_DIST) {
         // Owner teleported or outran it entirely; walking there would look absurd.
         s.x = bx;
         s.z = bz;
         s.y = by;
         return;
      }

      // Dead zone, so it settles instead of jittering when you stand still.
      if(dist > 0.28D) {
         double speed = Math.min(0.42D, 0.14D + dist * 0.16D);
         s.x += dx / dist * speed;
         s.z += dz / dist * speed;
         s.gait += (float)speed * 5.0F;

         // Face where it is going.
         float want = (float)Math.toDegrees(Math.atan2(-dx, dz));
         s.yaw = s.yaw + wrap(want - s.yaw) * 0.25F;
      }

      // Settle onto whatever it is standing over, so it walks up stairs rather
      // than through them.
      s.y += (groundBelow(mc, s.x, by, s.z) - s.y) * 0.35D;
   }

   /** Top of the first solid block at or below the owner's feet. */
   private static double groundBelow(Minecraft mc, double x, double fromY, double z) {
      int ix = net.minecraft.util.MathHelper.floor_double(x);
      int iz = net.minecraft.util.MathHelper.floor_double(z);
      int iy = net.minecraft.util.MathHelper.floor_double(fromY + 0.5D);

      for(int i = 0; i < 6; ++i) {
         BlockPos pos = new BlockPos(ix, iy - i, iz);
         if(mc.theWorld.getBlockState(pos).getBlock().getMaterial().isSolid()) {
            return iy - i + 1;
         }
      }

      return fromY;
   }

   private static float wrap(float deg) {
      while(deg < -180.0F) {
         deg += 360.0F;
      }

      while(deg >= 180.0F) {
         deg -= 360.0F;
      }

      return deg;
   }

   // ------------------------------------------------------------------
   // rendering
   // ------------------------------------------------------------------

   @SubscribeEvent
   public void onRenderWorldLast(RenderWorldLastEvent event) {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.theWorld == null || mc.thePlayer == null || this.pets.isEmpty()) {
         return;
      }

      float pt = event.partialTicks;
      double camX = mc.getRenderManager().viewerPosX;
      double camY = mc.getRenderManager().viewerPosY;
      double camZ = mc.getRenderManager().viewerPosZ;

      for(EntityPlayer p : mc.theWorld.playerEntities) {
         PetState s = this.pets.get(p.getName());
         if(s == null || !s.placed) {
            continue;
         }

         PetModel model = customFor(p);
         Cosmetic shipped = null;

         if(model == null) {
            shipped = petFor(p);
            if(shipped == null) {
               continue;
            }
            model = BuiltInPets.get(shipped.getId());
            if(model == null) {
               continue;
            }
         }

         double x = s.prevX + (s.x - s.prevX) * pt - camX;
         double y = s.prevY + (s.y - s.prevY) * pt - camY;
         double z = s.prevZ + (s.z - s.prevZ) * pt - camZ;
         float yaw = s.prevYaw + wrap(s.yaw - s.prevYaw) * pt;
         float gait = s.prevGait + (s.gait - s.prevGait) * pt;

         draw(model, x, y, z, yaw, gait);
      }
   }

   private void draw(PetModel m, double x, double y, double z, float yaw, float gait) {
      GlStateManager.pushMatrix();
      GlStateManager.disableTexture2D();
      GlStateManager.disableLighting();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.enableCull();

      GlStateManager.translate(x, y, z);
      GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.scale(m.scale, m.scale, m.scale);

      // A slight rock and hop, so it reads as walking rather than sliding.
      if(m.bob) {
         GlStateManager.translate(0.0F, Math.abs(Math.sin(gait)) * 0.055F, 0.0F);
         GlStateManager.rotate((float)Math.sin(gait) * 4.0F, 0.0F, 0.0F, 1.0F);
      }

      for(PetModel.Part part : m.parts) {
         GlStateManager.pushMatrix();
         GlStateManager.translate(part.x, part.y, part.z);
         GlStateManager.color((part.color >> 16 & 255) / 255.0F,
               (part.color >> 8 & 255) / 255.0F, (part.color & 255) / 255.0F, part.alpha);

         if("spike".equals(part.shape)) {
            spike(part.w, part.h, part.down);
         } else {
            box(part.w, part.h, part.d);
         }

         GlStateManager.popMatrix();
      }

      GlStateManager.disableBlend();
      GlStateManager.enableLighting();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   // ------------------------------------------------------------------

   private static Cosmetic petFor(EntityPlayer p) {
      if(p == Minecraft.getMinecraft().thePlayer) {
         return CosmeticManager.getEquippedItem(CosmeticType.PET);
      }

      return CosmeticManager.getRemote(p.getName(), CosmeticType.PET);
   }

   private static PetModel customFor(EntityPlayer p) {
      if(p != Minecraft.getMinecraft().thePlayer || CosmeticManager.getCustomPet() == null) {
         return null;
      }

      return CustomPets.get(CosmeticManager.getCustomPet());
   }

   /**
    * World-space box, sitting on the ground and centred horizontally.
    *
    * <p>Unlike the model-space one in {@code AccessoryRenderer}, +y is up here,
    * which is why pet JSON reads the way you would expect.
    */
   private static void box(float w, float h, float d) {
      float x = w / 2.0F;
      float z = d / 2.0F;
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION);

      float[][] faces = new float[][]{
            {-x, 0, -z, -x, h, -z, x, h, -z, x, 0, -z},
            {-x, 0, z, x, 0, z, x, h, z, -x, h, z},
            {-x, 0, -z, -x, 0, z, -x, h, z, -x, h, -z},
            {x, 0, -z, x, h, -z, x, h, z, x, 0, z},
            {-x, h, -z, -x, h, z, x, h, z, x, h, -z},
            {-x, 0, -z, x, 0, -z, x, 0, z, -x, 0, z}};

      for(float[] f : faces) {
         for(int i = 0; i < 12; i += 3) {
            wr.pos(f[i], f[i + 1], f[i + 2]).endVertex();
         }
      }

      tess.draw();
   }

   private static void spike(float base, float height, boolean down) {
      float b = base / 2.0F;
      float tip = down ? -height : height;
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(4, DefaultVertexFormats.POSITION);

      float[][] corners = new float[][]{{-b, -b}, {b, -b}, {b, b}, {-b, b}};

      for(int i = 0; i < 4; ++i) {
         float[] c1 = corners[i];
         float[] c2 = corners[(i + 1) % 4];
         wr.pos(c1[0], 0, c1[1]).endVertex();
         wr.pos(c2[0], 0, c2[1]).endVertex();
         wr.pos(0, tip, 0).endVertex();
      }

      tess.draw();
   }
}
