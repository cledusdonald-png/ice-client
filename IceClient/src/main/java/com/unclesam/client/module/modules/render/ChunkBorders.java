package com.unclesam.client.module.modules.render;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ColorSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.ColorUtil;
import com.unclesam.client.util.WorldRenderUtil;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Chunk grid overlay: the vertical chunk seams, a horizontal slice at a chosen
 * Y, and highlights for the chunk you're standing in.
 */
public class ChunkBorders extends Module {

   private final BooleanSetting vertical = (BooleanSetting)this.addSetting(new BooleanSetting("Vertical borders", true));
   private final BooleanSetting horizontal = (BooleanSetting)this.addSetting(new BooleanSetting("Horizontal borders", true));
   private final BooleanSetting staticHorizontal = (BooleanSetting)this.addSetting(new BooleanSetting("Static horizontal", false));
   private final NumberSetting staticY = (NumberSetting)this.addSetting(new NumberSetting("Static horizontal Y", 256.0D, 0.0D, 256.0D, 1.0D));
   private final BooleanSetting currentChunk = (BooleanSetting)this.addSetting(new BooleanSetting("Current chunk", true));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 1.5D, 1.0D, 5.0D, 0.5D));
   // Off by default: Orbit's borders are occluded by terrain, and a grid that
   // punches through solid ground reads as noise rather than a chunk guide.
   private final BooleanSetting throughWalls = (BooleanSetting)this.addSetting(new BooleanSetting("Through walls", false));
   /** How many chunks out the horizontal grid extends in each direction. */
   private final NumberSetting gridRadius = (NumberSetting)this.addSetting(new NumberSetting("Grid radius (chunks)", 8.0D, 1.0D, 16.0D, 1.0D));

   private final ColorSetting horizontalColor = (ColorSetting)this.addSetting(new ColorSetting("Horizontal color", -16737793));
   private final BooleanSetting horizontalRainbow = (BooleanSetting)this.addSetting(new BooleanSetting("Horizontal rainbow", false));
   private final ColorSetting verticalColor = (ColorSetting)this.addSetting(new ColorSetting("Vertical color", -65536));
   private final BooleanSetting verticalRainbow = (BooleanSetting)this.addSetting(new BooleanSetting("Vertical rainbow", false));
   private final ColorSetting currentChunkColor = (ColorSetting)this.addSetting(new ColorSetting("Current chunk color", -16711936));

   public ChunkBorders() {
      super("Chunk Borders", "Renders outlines of chunks", ModuleCategory.MECHANIC);
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.mc.thePlayer == null) {
         return;
      }

      BlockPos pos = new BlockPos(this.mc.thePlayer.posX, 0.0D, this.mc.thePlayer.posZ);
      int cx = pos.getX() >> 4 << 4;
      int cz = pos.getZ() >> 4 << 4;
      float w = (float)this.lineWidth.get();

      if(this.currentChunk.get()) {
         AxisAlignedBB box = new AxisAlignedBB((double)cx, 0.0D, (double)cz, (double)(cx + 16), 256.0D, (double)(cz + 16));
         WorldRenderUtil.outlineBox(box, ColorUtil.withAlpha(this.currentChunkColor.getRGB(), 160), w, this.throughWalls.get());
      }

      if(this.vertical.get()) {
         int col = this.verticalRainbow.get() ? ColorUtil.chroma(0) : this.verticalColor.getRGB();
         col = ColorUtil.withAlpha(col, 160);
         // The four seams of the chunk you're in, floor to build height.
         WorldRenderUtil.drawLine((double)cx, 0.0D, (double)cz, (double)cx, 256.0D, (double)cz, col, w, this.throughWalls.get());
         WorldRenderUtil.drawLine((double)(cx + 16), 0.0D, (double)cz, (double)(cx + 16), 256.0D, (double)cz, col, w, this.throughWalls.get());
         WorldRenderUtil.drawLine((double)cx, 0.0D, (double)(cz + 16), (double)cx, 256.0D, (double)(cz + 16), col, w, this.throughWalls.get());
         WorldRenderUtil.drawLine((double)(cx + 16), 0.0D, (double)(cz + 16), (double)(cx + 16), 256.0D, (double)(cz + 16), col, w, this.throughWalls.get());
      }

      if(this.horizontal.get()) {
         int col = this.horizontalRainbow.get() ? ColorUtil.chroma(0) : this.horizontalColor.getRGB();
         col = ColorUtil.withAlpha(col, 160);
         // Either pinned at a fixed Y or tracking the player's feet.
         double y = this.staticHorizontal.get() ? this.staticY.get() : Math.floor(this.mc.thePlayer.posY);

         // A full grid spanning `Grid radius` chunks in each direction, not just
         // the chunk underfoot -- the point is to read the chunk alignment of
         // the terrain around you from a distance.
         int rad = (int)this.gridRadius.get();
         int minX = cx - rad * 16;
         int maxX = cx + (rad + 1) * 16;
         int minZ = cz - rad * 16;
         int maxZ = cz + (rad + 1) * 16;

         for(int i = -rad; i <= rad + 1; ++i) {
            double lineX = (double)(cx + i * 16);
            WorldRenderUtil.drawLine(lineX, y, (double)minZ, lineX, y, (double)maxZ, col, w, this.throughWalls.get());
            double lineZ = (double)(cz + i * 16);
            WorldRenderUtil.drawLine((double)minX, y, lineZ, (double)maxX, y, lineZ, col, w, this.throughWalls.get());
         }
      }

   }
}
