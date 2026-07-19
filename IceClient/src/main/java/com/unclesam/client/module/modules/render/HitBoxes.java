package com.unclesam.client.module.modules.render;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ColorSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.ColorUtil;
import com.unclesam.client.util.WorldRenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

/**
 * Player hit boxes, drawn depth-tested so they're hidden behind terrain --
 * you only see boxes on players you can actually see.
 *
 * <p>{@link ImprovedHitBoxes} is the see-through-walls variant.
 */
public class HitBoxes extends Module {

   private final ColorSetting outlineColor = (ColorSetting)this.addSetting(new ColorSetting("Outline color", -1));
   private final BooleanSetting chromaOutline = (BooleanSetting)this.addSetting(new BooleanSetting("Chroma outline", false));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final NumberSetting range = (NumberSetting)this.addSetting(new NumberSetting("Range", 64.0D, 8.0D, 128.0D, 8.0D));
   private final NumberSetting expand = (NumberSetting)this.addSetting(new NumberSetting("Expand", 0.0D, 0.0D, 0.5D, 0.05D));

   public HitBoxes() {
      super("Hit Boxes", "Displays hit boxes on players", ModuleCategory.MECHANIC);
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
         if(!(e instanceof EntityPlayer) || e == this.mc.thePlayer) {
            continue;
         }

         if(e.getDistanceToEntity(this.mc.thePlayer) > max) {
            continue;
         }

         AxisAlignedBB box = e.getEntityBoundingBox().expand(grow, grow, grow);
         // false == depth-tested, so terrain hides it.
         WorldRenderUtil.outlineBox(box, col, w, false);
      }

   }
}
