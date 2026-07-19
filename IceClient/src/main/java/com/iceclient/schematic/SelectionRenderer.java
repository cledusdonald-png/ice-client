package com.iceclient.schematic;

import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.schematic.SelectionTool;
import com.iceclient.util.ColorUtil;
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws the Point A/B region whenever one exists.
 *
 * <p>Deliberately not gated on {@link SelectionTool} being enabled. Corners can
 * be set from the workspace GUI's Points tab as well as from that module's
 * keybinds, and the GUI has no idea whether the module is switched on -- so
 * gating the render on it meant setting two points, seeing the coordinates and
 * block count update, and getting nothing in the world, with nothing on screen
 * explaining the gap.
 *
 * <p>Same pattern as {@code PingManager}: the state is shared, so the renderer
 * follows the state rather than any one module's toggle. Turning the drawing
 * off is what {@code Clear} and the module's own draw switches are for.
 */
public final class SelectionRenderer {

   /** Used when SelectionTool has not been registered yet. */
   private static final int DEFAULT_A = 0xFFFF3333;
   private static final int DEFAULT_B = 0xFF3388FF;
   private static final int DEFAULT_REGION = 0xFF8C5AFF;

   private SelectionRenderer() {
   }

   public static void init() {
      MinecraftForge.EVENT_BUS.register(new Handler());
   }

   public static class Handler {

      @SubscribeEvent
      public void onRenderWorld(RenderWorldLastEvent event) {
         Minecraft mc = Minecraft.getMinecraft();
         if(mc.theWorld == null || mc.thePlayer == null) {
            return;
         }

         BlockPos a = Selection.getA();
         BlockPos b = Selection.getB();
         if(a == null && b == null) {
            return;
         }

         SelectionTool tool = (SelectionTool)ModuleManager.getByName("SelectionTool");

         boolean through = tool == null || tool.drawThroughWalls();
         boolean corners = tool == null || tool.showCornerMarkers();
         boolean fill = tool == null || tool.fillCornerBlocks();
         boolean region = tool == null || tool.showRegionBox();

         int colA = tool == null ? DEFAULT_A : tool.pointAColor();
         int colB = tool == null ? DEFAULT_B : tool.pointBColor();
         int colRegion = tool == null ? DEFAULT_REGION : tool.regionColor();

         if(corners) {
            // A and B get their own colours so you can tell at a glance which
            // corner you are about to move -- with one colour they are
            // indistinguishable, and the region grows the wrong way if you
            // guess.
            drawCorner(a, colA, through, fill);
            drawCorner(b, colB, through, fill);
         }

         if(region && Selection.isComplete()) {
            BlockPos lo = Selection.min();
            BlockPos hi = Selection.max();

            // +1 on the max corner because the region is inclusive: a selection
            // from (0,0,0) to (0,0,0) is one block, not zero-sized.
            WorldRenderUtil.outlineBox(new AxisAlignedBB(
                  (double)lo.getX(), (double)lo.getY(), (double)lo.getZ(),
                  (double)(hi.getX() + 1), (double)(hi.getY() + 1), (double)(hi.getZ() + 1)),
                  colRegion, 2.0F, through);
         }

      }

      private static void drawCorner(BlockPos pos, int color, boolean through, boolean fill) {
         if(pos == null) {
            return;
         }

         // Inset very slightly so the corner marker does not z-fight with the
         // region outline, which shares its edges when the region is one block
         // deep on an axis.
         AxisAlignedBB box = new AxisAlignedBB(
               (double)pos.getX() + 0.002D, (double)pos.getY() + 0.002D, (double)pos.getZ() + 0.002D,
               (double)(pos.getX() + 1) - 0.002D, (double)(pos.getY() + 1) - 0.002D,
               (double)(pos.getZ() + 1) - 0.002D);

         if(fill) {
            // A translucent fill reads as a coloured block at distance, where a
            // wireframe cube collapses into a dot.
            WorldRenderUtil.filledBox(box, ColorUtil.withAlpha(color, 110));
         }

         WorldRenderUtil.outlineBox(box, color, 2.5F, through);
      }
   }
}
