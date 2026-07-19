package com.iceclient.module.modules.render;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.ColorUtil;
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Replaces vanilla's block outline with a configurable box and optional fill. */
public class BlockOverlay extends Module {

   private final BooleanSetting hideVanilla = (BooleanSetting)this.addSetting(new BooleanSetting("Hide vanilla outline", true));
   private final BooleanSetting outline = (BooleanSetting)this.addSetting(new BooleanSetting("Outline", true));
   private final BooleanSetting fill = (BooleanSetting)this.addSetting(new BooleanSetting("Fill", true));
   private final NumberSetting fillAlpha = (NumberSetting)this.addSetting(new NumberSetting("Fill alpha", 60.0D, 0.0D, 255.0D, 5.0D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final BooleanSetting chroma = (BooleanSetting)this.addSetting(new BooleanSetting("Chroma", false));
   private final ColorSetting color = (ColorSetting)this.addSetting(new ColorSetting("Color", -1));

   public BlockOverlay() {
      super("Block Overlay", "Custom outline on the block you're looking at", ModuleCategory.MECHANIC);
   }

   /** Cancels vanilla's selection box so ours isn't drawn on top of it. */
   @SubscribeEvent
   public void onDrawHighlight(DrawBlockHighlightEvent event) {
      if(this.isEnabled() && this.hideVanilla.get()
            && event.target != null && event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.mc.theWorld == null) {
         return;
      }

      MovingObjectPosition hit = this.mc.objectMouseOver;
      if(hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
         return;
      }

      BlockPos pos = hit.getBlockPos();
      Block b = this.mc.theWorld.getBlockState(pos).getBlock();
      if(b == Blocks.air) {
         return;
      }

      // Use the block's real shape so slabs and fences don't get a full cube.
      b.setBlockBoundsBasedOnState(this.mc.theWorld, pos);
      AxisAlignedBB box = b.getSelectedBoundingBox(this.mc.theWorld, pos).expand(0.002D, 0.002D, 0.002D);

      int base = this.chroma.get() ? ColorUtil.chroma(0) : this.color.getRGB();
      if(this.fill.get()) {
         WorldRenderUtil.filledBox(box, ColorUtil.withAlpha(base, (int)this.fillAlpha.get()));
      }

      if(this.outline.get()) {
         WorldRenderUtil.outlineBox(box, ColorUtil.withAlpha(base, 220), (float)this.lineWidth.get());
      }

   }
}
