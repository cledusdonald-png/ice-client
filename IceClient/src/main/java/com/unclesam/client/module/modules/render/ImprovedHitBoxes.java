package com.unclesam.client.module.modules.render;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ColorSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.ColorUtil;
import com.unclesam.client.util.WorldRenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

/**
 * Hit boxes that draw through terrain, plus the extras that only make sense
 * when you can see players you couldn't otherwise: distance, health and an
 * optional tracer.
 *
 * <p>Kept separate from {@link HitBoxes} rather than being a toggle on it,
 * because seeing players through walls is a materially different thing from
 * outlining the ones already in front of you -- worth switching on deliberately,
 * and worth checking your server allows.
 */
public class ImprovedHitBoxes extends Module {

   private final ColorSetting outlineColor = (ColorSetting)this.addSetting(new ColorSetting("Outline color", -22016));
   private final BooleanSetting chromaOutline = (BooleanSetting)this.addSetting(new BooleanSetting("Chroma outline", false));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final NumberSetting range = (NumberSetting)this.addSetting(new NumberSetting("Range", 96.0D, 8.0D, 256.0D, 8.0D));
   private final NumberSetting expand = (NumberSetting)this.addSetting(new NumberSetting("Expand", 0.05D, 0.0D, 0.5D, 0.05D));
   private final BooleanSetting includeItems = (BooleanSetting)this.addSetting(new BooleanSetting("Include items", false));
   private final BooleanSetting showName = (BooleanSetting)this.addSetting(new BooleanSetting("Show name", true));
   private final BooleanSetting showDistance = (BooleanSetting)this.addSetting(new BooleanSetting("Show distance", true));
   private final BooleanSetting showHealth = (BooleanSetting)this.addSetting(new BooleanSetting("Show health", false));
   private final BooleanSetting tracer = (BooleanSetting)this.addSetting(new BooleanSetting("Tracer", false));

   public ImprovedHitBoxes() {
      super("Improved Hit Boxes", "Hit boxes visible through walls, with distance and health",
            ModuleCategory.MECHANIC);
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.mc.theWorld == null || this.mc.thePlayer == null) {
         return;
      }

      int col = ColorUtil.withAlpha(this.chromaOutline.get() ? ColorUtil.chroma(0) : this.outlineColor.getRGB(), 220);
      float w = (float)this.lineWidth.get();
      double max = this.range.get();
      double grow = this.expand.get();

      for(Entity e : new ArrayList<Entity>(this.mc.theWorld.loadedEntityList)) {
         if(e == this.mc.thePlayer) {
            continue;
         }

         boolean isPlayer = e instanceof EntityPlayer;
         boolean isItem = e instanceof EntityItem;
         if(!isPlayer && !(isItem && this.includeItems.get())) {
            continue;
         }

         double dist = e.getDistanceToEntity(this.mc.thePlayer);
         if(dist > max) {
            continue;
         }

         AxisAlignedBB box = e.getEntityBoundingBox().expand(grow, grow, grow);
         // true == ignores depth, so it shows through terrain.
         WorldRenderUtil.outlineBox(box, col, w, true);

         if(this.tracer.get()) {
            WorldRenderUtil.drawLine(
                  WorldRenderUtil.camX(), WorldRenderUtil.camY() - 0.3D, WorldRenderUtil.camZ(),
                  e.posX, e.posY + e.height / 2.0D, e.posZ,
                  ColorUtil.withAlpha(col, 130), w);
         }

         if(isPlayer) {
            StringBuilder sb = new StringBuilder();
            if(this.showName.get()) {
               sb.append(e.getName());
            }

            if(this.showDistance.get()) {
               if(sb.length() > 0) {
                  sb.append(' ');
               }

               sb.append((int)dist).append('m');
            }

            if(this.showHealth.get()) {
               if(sb.length() > 0) {
                  sb.append(' ');
               }

               sb.append((int)Math.ceil((double)((EntityPlayer)e).getHealth())).append("hp");
            }

            if(sb.length() > 0) {
               WorldRenderUtil.text3d(sb.toString(), e.posX, e.posY + (double)e.height + 0.4D, e.posZ, col, 0.025F);
            }
         }
      }

   }
}
