package com.iceclient.module.modules.factions;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.ModeSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.ColorUtil;
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlights floating blocks -- solid blocks with no solid neighbour, the
 * tell-tale of a patched or half-built wall.
 *
 * <p><b>Scanning is incremental.</b> The first version swept the whole radius in
 * one pass every 500ms: at radius 24 that's 49^3 = ~118k block lookups twice a
 * second on the client thread, which stutters the game badly enough to feel like
 * mouse lag. Now one horizontal slice is scanned per tick and results are
 * swapped in only when a full sweep completes, so the per-tick cost is a
 * fraction of that and constant.
 *
 * <p>The vertical range is also separate from the horizontal one -- floats
 * matter within a few blocks of your level, not 24 up and down.
 */
public class FloatFinder extends Module {

   private final ModeSetting detect = (ModeSetting)this.addSetting(new ModeSetting("Detect", "Unsupported", new String[]{"Unsupported", "Isolated"}));
   private final BooleanSetting requireExposed = (BooleanSetting)this.addSetting(new BooleanSetting("Ignore terrain overhangs", true));
   private final NumberSetting maxSupports = (NumberSetting)this.addSetting(new NumberSetting("Max touching sides", 2.0D, 0.0D, 5.0D, 1.0D));
   private final NumberSetting radius = (NumberSetting)this.addSetting(new NumberSetting("Radius", 16.0D, 4.0D, 48.0D, 4.0D));
   private final NumberSetting height = (NumberSetting)this.addSetting(new NumberSetting("Height range", 6.0D, 1.0D, 24.0D, 1.0D));
   private final NumberSetting slicesPerTick = (NumberSetting)this.addSetting(new NumberSetting("Slices per tick", 2.0D, 1.0D, 8.0D, 1.0D));
   private final NumberSetting maxResults = (NumberSetting)this.addSetting(new NumberSetting("Max results", 128.0D, 16.0D, 512.0D, 16.0D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final BooleanSetting obsidianOnly = (BooleanSetting)this.addSetting(new BooleanSetting("Obsidian only", false));
   private final BooleanSetting ignoreNatural = (BooleanSetting)this.addSetting(new BooleanSetting("Ignore leaves & plants", true));
   private final ColorSetting color = (ColorSetting)this.addSetting(new ColorSetting("Color", -256));

   /** Published results; swapped in atomically when a sweep finishes. */
   private List<BlockPos> floats = new ArrayList();
   private List<BlockPos> building = new ArrayList();

   private BlockPos origin;
   private int sliceY;

   public FloatFinder() {
      super("Float Finder", "Highlights floating blocks", ModuleCategory.FACTIONS);
   }

   protected void onDisable() {
      this.floats = new ArrayList();
      this.building = new ArrayList();
      this.origin = null;
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled()
            || this.mc.theWorld == null || this.mc.thePlayer == null) {
         return;
      }

      int h = (int)this.height.get();

      // Restart the sweep from the player's current position each time round.
      if(this.origin == null) {
         this.origin = new BlockPos(this.mc.thePlayer);
         this.building = new ArrayList();
         this.sliceY = -h;
      }

      for(int i = 0; i < (int)this.slicesPerTick.get(); ++i) {
         if(this.sliceY > h) {
            // Sweep complete: publish and start again from where we now stand.
            this.floats = this.building;
            this.origin = null;
            return;
         }

         this.scanSlice(this.origin, this.sliceY);
         ++this.sliceY;
      }

   }

   private void scanSlice(BlockPos origin, int dy) {
      int r = (int)this.radius.get();
      int cap = (int)this.maxResults.get();
      int y = origin.getY() + dy;
      if(y < 0 || y > 255) {
         return;
      }

      for(int dx = -r; dx <= r; ++dx) {
         for(int dz = -r; dz <= r; ++dz) {
            if(this.building.size() >= cap) {
               return;
            }

            BlockPos p = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
            if(this.isFloating(p)) {
               this.building.add(p);
            }
         }
      }

   }

   /**
    * Whether this block counts as a float.
    *
    * <p>A "float" in factions is an <b>unsupported</b> block -- one with air
    * directly beneath it, marking a gap in a wall. The first version tested for
    * a block with no solid neighbour on any of six faces, which is an
    * <em>isolated</em> block, not a float: on natural terrain that lights up
    * every surface contour, which is what made it look broken.
    *
    * <p>{@code Detect} switches between that real definition and the strict
    * isolated test, which is still occasionally useful for spotting stray
    * placements.
    */
   private boolean isFloating(BlockPos p) {
      Block b = this.mc.theWorld.getBlockState(p).getBlock();
      if(b == Blocks.air) {
         return false;
      }

      Material m = b.getMaterial();
      if(!m.isSolid()) {
         return false;
      }

      if(this.ignoreNatural.get() && this.isNatural(b, m)) {
         return false;
      }

      if(this.obsidianOnly.get() && b != Blocks.obsidian) {
         return false;
      }

      if(this.detect.is("Isolated")) {
         for(EnumFacing f : EnumFacing.values()) {
            Block n = this.mc.theWorld.getBlockState(p.offset(f)).getBlock();
            if(n != Blocks.air && n.getMaterial().isSolid()
                  && !(this.ignoreNatural.get() && this.isNatural(n, n.getMaterial()))) {
               return false;
            }
         }

         return true;
      }

      // "Unsupported": air directly below.
      Block below = this.mc.theWorld.getBlockState(p.down()).getBlock();
      if(below != Blocks.air && below.getMaterial().isSolid()) {
         return false;
      }

      // Terrain overhangs are unsupported too but aren't floats. Requiring the
      // block to be man-made-ish, or to have air on most sides, filters the
      // natural cliff faces that made this unusable outdoors.
      if(this.requireExposed.get()) {
         int solidSides = 0;

         for(EnumFacing f : EnumFacing.values()) {
            if(f == EnumFacing.DOWN) {
               continue;
            }

            Block n = this.mc.theWorld.getBlockState(p.offset(f)).getBlock();
            if(n != Blocks.air && n.getMaterial().isSolid()) {
               ++solidSides;
            }
         }

         if(solidSides > (int)this.maxSupports.get()) {
            return false;
         }
      }

      return true;
   }

   private boolean isNatural(Block b, Material m) {
      return m == Material.leaves || m == Material.plants || m == Material.vine
            || m == Material.cactus || m == Material.gourd || m == Material.wood
            || b == Blocks.leaves || b == Blocks.leaves2;
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      List<BlockPos> list = this.floats;
      if(!this.isEnabled() || list.isEmpty()) {
         return;
      }

      int col = ColorUtil.withAlpha(this.color.getRGB(), 200);
      float w = (float)this.lineWidth.get();
      for(BlockPos p : list) {
         WorldRenderUtil.outlineBox(new AxisAlignedBB(p, p.add(1, 1, 1)), col, w, false);
      }

   }
}
