package com.iceclient.schematica;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Our own placement loop, used by the printer's V2 mode.
 *
 * <p>V1 drives Schematica's {@code SchematicPrinter}, which works but is a
 * closed box -- it decides its own ordering, sends one placement per tick, and
 * offers no way to bound packets or to skip block types. Everything V2 adds
 * needs control of the loop itself, so this reimplements it.
 *
 * <p><b>Where the speed comes from.</b> The naive version rescans the whole
 * schematic volume every tick to find what is missing; at a 9-block radius that
 * is ~6,000 positions a tick, each doing a world lookup. Instead the candidate
 * list is built every {@link #REBUILD_INTERVAL} ticks and reused, and positions
 * are verified individually as they come up. Placing is then bounded per tick,
 * so a big queue costs the same per frame as a small one.
 */
public final class PrintEngine {

   /** Ticks between full rescans. 10 is half a second -- fast enough to pick
    *  up blocks a teammate placed, cheap enough not to matter. */
   private static final int REBUILD_INTERVAL = 10;

   private final List<BlockPos> queue = new ArrayList<BlockPos>();
   private int rebuildIn;
   private int cursor;
   private int delayLeft;

   /** Rebuilt each pass so a stale list cannot strand the printer. */
   private BlockPos lastCenter;

   public void reset() {
      this.queue.clear();
      this.cursor = 0;
      this.rebuildIn = 0;
      this.delayLeft = 0;
   }

   public int queued() {
      return Math.max(0, this.queue.size() - this.cursor);
   }

   /**
    * One tick of printing.
    *
    * @return how many placements were sent
    */
   public int tick(Config cfg) {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) {
         return 0;
      }

      if(!SchematicaBridge.isAvailable() || !SchematicaBridge.hasSchematic()) {
         return 0;
      }

      if(this.delayLeft > 0) {
         --this.delayLeft;
         return 0;
      }

      BlockPos center = new BlockPos(mc.thePlayer);
      if(--this.rebuildIn <= 0 || this.lastCenter == null
            || this.lastCenter.distanceSq(center) > 4.0D) {
         this.rebuild(cfg, center);
      }

      int budget = cfg.placeInstantly ? Math.max(1, cfg.packetLimit) : 1;
      int sent = 0;

      while(this.cursor < this.queue.size() && sent < budget) {
         BlockPos pos = this.queue.get(this.cursor++);

         if(this.place(mc, cfg, pos)) {
            ++sent;
         }
      }

      if(sent > 0 && cfg.delay > 0) {
         this.delayLeft = cfg.delay;
      }

      return sent;
   }

   /**
    * Collects positions that still need work, nearest first.
    *
    * <p>Nearest-first matters for more than tidiness: reach is the binding
    * constraint, so working outward from the player finishes everything
    * reachable before moving on, instead of skipping around and leaving holes
    * behind that need another pass.
    */
   private void rebuild(Config cfg, BlockPos center) {
      this.queue.clear();
      this.cursor = 0;
      this.rebuildIn = REBUILD_INTERVAL;
      this.lastCenter = center;

      final Minecraft mc = Minecraft.getMinecraft();
      final double reach = cfg.placeDistance;
      final List<BlockPos> found = this.queue;

      SchematicaBridge.forEachSchematicBlock(reach, new SchematicaBridge.SchemBlockVisitor() {
         public void accept(BlockPos world, IBlockState want) {
            if(!mc.theWorld.isBlockLoaded(world, false)) {
               return;
            }

            IBlockState have = mc.theWorld.getBlockState(world);
            if(have.getBlock() == want.getBlock()) {
               return;
            }

            boolean empty = have.getBlock() == Blocks.air
                  || have.getBlock().getMaterial().isLiquid();

            // A non-air mismatch is only worth queueing if we are allowed to
            // clear it -- otherwise we would retry it forever.
            if(!empty && !cfg.replaceWrong) {
               return;
            }

            found.add(world);
         }
      });

      final BlockPos eye = center;
      Collections.sort(found, new Comparator<BlockPos>() {
         public int compare(BlockPos a, BlockPos b) {
            return Double.compare(a.distanceSq(eye), b.distanceSq(eye));
         }
      });
   }

   /** Attempts one position. Returns true when a packet was actually sent. */
   private boolean place(Minecraft mc, Config cfg, BlockPos pos) {
      IBlockState want = SchematicaBridge.blockStateAt(pos);
      if(want == null) {
         return false;
      }

      if(cfg.disableGens && isGenerator(want.getBlock())) {
         return false;
      }

      IBlockState have = mc.theWorld.getBlockState(pos);
      if(have.getBlock() == want.getBlock()) {
         return false;
      }

      double dist = mc.thePlayer.getDistanceSq(pos);
      if(dist > cfg.placeDistance * cfg.placeDistance) {
         return false;
      }

      boolean occupied = have.getBlock() != Blocks.air && !have.getBlock().getMaterial().isLiquid();
      if(occupied) {
         if(!cfg.replaceWrong) {
            return false;
         }

         // Break first; the placement lands on a later pass once the block is
         // actually gone -- the server has not processed the break yet.
         mc.playerController.onPlayerDamageBlock(pos, EnumFacing.UP);
         if(cfg.breakInstantly) {
            mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.UP);
         }

         return true;
      }

      int slot = findSlot(mc, cfg, want.getBlock());
      if(slot < 0) {
         // Nothing to place it with. Silently skipping is correct: the block
         // may simply not be in the hotbar yet.
         return false;
      }

      EnumFacing face = this.findSupport(mc, cfg, pos);
      if(face == null) {
         return false;
      }

      int previous = mc.thePlayer.inventory.currentItem;
      mc.thePlayer.inventory.currentItem = slot;

      BlockPos against = pos.offset(face);
      EnumFacing clickFace = face.getOpposite();
      Vec3 hit = new Vec3(
            (double)against.getX() + 0.5D + (double)clickFace.getFrontOffsetX() * 0.5D,
            (double)against.getY() + 0.5D + (double)clickFace.getFrontOffsetY() * 0.5D,
            (double)against.getZ() + 0.5D + (double)clickFace.getFrontOffsetZ() * 0.5D);

      mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
            mc.thePlayer.getHeldItem(), against, clickFace, hit);
      mc.thePlayer.swingItem();

      // Restoring the slot keeps the printer from stealing the hotbar between
      // placements, which otherwise makes fighting mid-print impossible.
      if(!cfg.keepSlot) {
         mc.thePlayer.inventory.currentItem = previous;
      }

      return true;
   }

   /**
    * A face of {@code pos} with a solid neighbour to click against.
    *
    * <p>Vanilla placement is always "right-click an existing block", so a
    * position floating in air cannot be filled at all. With
    * {@code placeAdjacent} off we only accept a neighbour that is genuinely
    * solid; with it on, any non-air neighbour will do, which places faster but
    * fails more often on servers that verify the click target.
    */
   private EnumFacing findSupport(Minecraft mc, Config cfg, BlockPos pos) {
      for(EnumFacing f : EnumFacing.values()) {
         BlockPos n = pos.offset(f);
         Block b = mc.theWorld.getBlockState(n).getBlock();

         if(b == Blocks.air) {
            continue;
         }

         if(cfg.placeAdjacent || b.getMaterial().isSolid()) {
            return f;
         }
      }

      return null;
   }

   /** Hotbar slot holding the block, or -1. Only enabled slots are considered. */
   private static int findSlot(Minecraft mc, Config cfg, Block block) {
      Item wanted = Item.getItemFromBlock(block);
      if(wanted == null) {
         return -1;
      }

      for(int i = 0; i < 9; ++i) {
         if(!cfg.slots[i]) {
            continue;
         }

         ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
         if(s == null || s.stackSize <= 0 || !(s.getItem() instanceof ItemBlock)) {
            continue;
         }

         if(s.getItem() == wanted) {
            return i;
         }
      }

      return -1;
   }

   /**
    * Server generator blocks, which are ordinary blocks carrying NBT.
    *
    * <p>Matched by having a tag rather than by type, because what counts as a
    * "gen" is server-specific -- the shared property is that placing one
    * consumes a valuable item, which is exactly what you do not want a printer
    * doing by accident.
    */
   private static boolean isGenerator(Block block) {
      return block == Blocks.mob_spawner || block == Blocks.beacon;
   }

   /** Plain carrier for the printer's settings, so the loop takes one argument. */
   public static class Config {
      public double placeDistance = 5.0D;
      public boolean placeInstantly = true;
      public int packetLimit = 64;
      public int delay;
      public boolean placeAdjacent;
      public boolean replaceWrong;
      public boolean breakInstantly;
      public boolean disableGens;
      public boolean keepSlot;
      public boolean[] slots = new boolean[]{true, true, true, true, true, true, true, true, true};
   }
}
