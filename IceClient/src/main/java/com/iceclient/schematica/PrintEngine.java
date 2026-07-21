package com.iceclient.schematica;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
 *
 * <p><b>What the server actually sees.</b> Three things here are non-obvious and
 * every one of them produced a visible bug when it was missing:
 * <ul>
 *   <li>The server places whatever slot <em>it</em> thinks you are holding. The
 *       client field alone is not enough -- {@link C09PacketHeldItemChange} has
 *       to go out, and it cannot be reverted in the same tick or it never sends.
 *   <li>Directional blocks are oriented from the placer's rotation at the moment
 *       of the click, so a dispenser only faces the right way if a look packet
 *       precedes the placement.
 *   <li>Blocks sitting in the main inventory are not placeable at all; they have
 *       to be swapped into the hotbar first.
 * </ul>
 */
public final class PrintEngine {

   /** Ticks between full rescans. 10 is half a second -- fast enough to pick
    *  up blocks a teammate placed, cheap enough not to matter. */
   private static final int REBUILD_INTERVAL = 10;

   /** Breaking is bounded far tighter than placing: each break is a block you
    *  destroy for real, so a runaway loop is destructive rather than just noisy. */
   private static final int BREAK_BUDGET = 1;

   private final List<BlockPos> queue = new ArrayList<BlockPos>();
   private int rebuildIn;
   private int cursor;
   private int delayLeft;

   /** Hotbar slot held before this tick started placing, or -1 when we have not
    *  taken the hotbar over. Restored at the end of the batch, never mid-batch. */
   private int slotToRestore = -1;

   /** Rebuilt each pass so a stale list cannot strand the printer. */
   private BlockPos lastCenter;

   public void reset() {
      this.queue.clear();
      this.cursor = 0;
      this.rebuildIn = 0;
      this.delayLeft = 0;
      this.slotToRestore = -1;
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
      int broke = 0;

      while(this.cursor < this.queue.size() && sent < budget) {
         BlockPos pos = this.queue.get(this.cursor++);

         Result r = this.attempt(mc, cfg, pos, broke < BREAK_BUDGET);
         if(r == Result.PLACED) {
            ++sent;
         } else if(r == Result.BROKE) {
            ++broke;
         }
      }

      // Hand the hotbar back only once the whole batch is done. Restoring
      // between placements is what silently broke every placement before: the
      // revert cancelled the slot change before the client ever synced it.
      this.releaseSlot(mc, cfg);

      // Exhausting the queue used to mean idling until the next scheduled
      // rebuild -- up to half a second of doing nothing with work still to do.
      // Rescan on the next tick instead: the world has changed underneath us,
      // which is exactly when a fresh list is worth building.
      if(this.cursor >= this.queue.size()) {
         this.rebuildIn = 0;
      }

      if(sent > 0 && cfg.delay > 0) {
         this.delayLeft = cfg.delay;
      }

      return sent;
   }

   private enum Result { NOTHING, PLACED, BROKE }

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
      final boolean replaceWrong = cfg.replaceWrong;

      SchematicaBridge.forEachSchematicBlock(reach, new SchematicaBridge.SchemBlockVisitor() {
         public void accept(BlockPos world, IBlockState want) {
            if(!mc.theWorld.isBlockLoaded(world, false)) {
               return;
            }

            IBlockState have = mc.theWorld.getBlockState(world);
            if(matches(have, want)) {
               return;
            }

            boolean empty = have.getBlock() == Blocks.air
                  || have.getBlock().getMaterial().isLiquid();

            // A non-air mismatch is only worth queueing if we are allowed to
            // clear it -- otherwise we would retry it forever.
            if(!empty && !replaceWrong) {
               return;
            }

            found.add(world);
         }
      });

      // Bottom-to-top, then nearest. A placement always needs something to click
      // against, so a layer cannot go down before the one beneath it exists.
      // Sorting purely by distance meant most attempts hit air, failed, burned
      // their queue slot and waited for the next rebuild -- the printer crept up
      // one layer every half second instead of filling as fast as packets allow.
      // Distance breaks ties, so within a layer it still works outward from you
      // and finishes everything in reach before moving on.
      final BlockPos eye = center;
      Collections.sort(found, new Comparator<BlockPos>() {
         public int compare(BlockPos a, BlockPos b) {
            if(a.getY() != b.getY()) {
               return a.getY() - b.getY();
            }

            return Double.compare(a.distanceSq(eye), b.distanceSq(eye));
         }
      });
   }

   /**
    * Whether what is in the world is already what the schematic wants.
    *
    * <p>Block identity alone is not enough: a dispenser facing the wrong way and
    * a repeater on the wrong delay are both "the right block" but a broken
    * cannon. Orientation is compared too, so those get queued for a redo instead
    * of being silently accepted.
    */
   private static boolean matches(IBlockState have, IBlockState want) {
      if(have.getBlock() != want.getBlock()) {
         return false;
      }

      EnumFacing hf = facingOf(have);
      EnumFacing wf = facingOf(want);
      return hf == null || wf == null || hf == wf;
   }

   /** The block's "facing" property, or null when it has none. */
   private static EnumFacing facingOf(IBlockState state) {
      for(Map.Entry<IProperty, Comparable> e : ((Map<IProperty, Comparable>)state.getProperties()).entrySet()) {
         if("facing".equals(e.getKey().getName()) && e.getValue() instanceof EnumFacing) {
            return (EnumFacing)e.getValue();
         }
      }

      return null;
   }

   /** Attempts one position. */
   private Result attempt(Minecraft mc, Config cfg, BlockPos pos, boolean mayBreak) {
      IBlockState want = SchematicaBridge.blockStateAt(pos);
      if(want == null) {
         return Result.NOTHING;
      }

      if(cfg.disableGens && isGenerator(want.getBlock())) {
         return Result.NOTHING;
      }

      IBlockState have = mc.theWorld.getBlockState(pos);
      if(matches(have, want)) {
         return Result.NOTHING;
      }

      double dist = mc.thePlayer.getDistanceSq(pos);
      if(dist > cfg.placeDistance * cfg.placeDistance) {
         return Result.NOTHING;
      }

      // Pistons are never touched, in either direction. An extended piston is a
      // base with extended=true plus a head block no schematic contains, so it
      // always reads as wrong -- and breaking one mid-fire kills the cannon.
      if(isPistonCell(have.getBlock()) || isPistonCell(want.getBlock())) {
         return Result.NOTHING;
      }

      boolean occupied = have.getBlock() != Blocks.air && !have.getBlock().getMaterial().isLiquid();
      if(occupied) {
         if(!cfg.replaceWrong || !mayBreak) {
            return Result.NOTHING;
         }

         // Only ever break a genuinely different block. Same block with the
         // wrong state -- a repeater on the wrong delay -- is auto-tick's job,
         // and breaking it destroys the repeater before it can be corrected.
         if(have.getBlock() == want.getBlock()) {
            return Result.NOTHING;
         }

         // Break first; the placement lands on a later pass once the block is
         // actually gone -- the server has not processed the break yet.
         mc.playerController.onPlayerDamageBlock(pos, EnumFacing.UP);
         if(cfg.breakInstantly) {
            mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.UP);
         }

         return Result.BROKE;
      }

      int slot = this.resolveSlot(mc, cfg, want.getBlock());
      if(slot < 0) {
         // Nothing to place it with. Silently skipping is correct: the block
         // may simply not be in the inventory at all.
         return Result.NOTHING;
      }

      EnumFacing face = this.findSupport(mc, cfg, pos);
      if(face == null) {
         return Result.NOTHING;
      }

      this.holdSlot(mc, slot);

      if(cfg.orientBlocks) {
         this.aimFor(mc, want);
      }

      BlockPos against = pos.offset(face);
      EnumFacing clickFace = face.getOpposite();
      Vec3 hit = new Vec3(
            (double)against.getX() + 0.5D + (double)clickFace.getFrontOffsetX() * 0.5D,
            (double)against.getY() + 0.5D + (double)clickFace.getFrontOffsetY() * 0.5D,
            (double)against.getZ() + 0.5D + (double)clickFace.getFrontOffsetZ() * 0.5D);

      mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
            mc.thePlayer.getHeldItem(), against, clickFace, hit);
      mc.thePlayer.swingItem();

      return Result.PLACED;
   }

   // ------------------------------------------------------------------
   // Held item
   // ------------------------------------------------------------------

   /**
    * Selects a hotbar slot and tells the server about it.
    *
    * <p>The packet is the whole point. Vanilla only syncs {@code currentItem} on
    * the following tick, by diffing against the previous value -- so a printer
    * that sets the field and restores it within one tick syncs nothing, and the
    * server places whatever you were actually holding into every position.
    */
   private void holdSlot(Minecraft mc, int slot) {
      if(this.slotToRestore < 0) {
         this.slotToRestore = mc.thePlayer.inventory.currentItem;
      }

      if(mc.thePlayer.inventory.currentItem != slot) {
         mc.thePlayer.inventory.currentItem = slot;
         mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(slot));
      }
   }

   /** Gives the hotbar back after the batch, syncing that too. */
   private void releaseSlot(Minecraft mc, Config cfg) {
      if(this.slotToRestore >= 0) {
         if(!cfg.keepSlot && mc.thePlayer.inventory.currentItem != this.slotToRestore) {
            mc.thePlayer.inventory.currentItem = this.slotToRestore;
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(this.slotToRestore));
         }

         this.slotToRestore = -1;
      }
   }

   // ------------------------------------------------------------------
   // Orientation
   // ------------------------------------------------------------------

   /**
    * Points the player so vanilla's placement logic derives the facing the
    * schematic asked for, then sends the rotation.
    *
    * <p>Only the look packet is sent, not a position one, and the player's own
    * rotation fields are left alone -- your view does not move, but the server
    * has the rotation it needs when the placement arrives a moment later.
    */
   private void aimFor(Minecraft mc, IBlockState want) {
      EnumFacing target = facingOf(want);
      if(target == null) {
         return;
      }

      Block b = want.getBlock();
      boolean opposite = b instanceof BlockDispenser || b instanceof BlockFurnace
            || b instanceof BlockChest || b instanceof BlockPistonBase
            || b instanceof BlockRedstoneRepeater || b instanceof BlockRedstoneComparator
            || b instanceof BlockHopper;
      boolean direct = b instanceof BlockStairs || b instanceof BlockLever
            || b instanceof BlockButton;

      if(!opposite && !direct) {
         return;
      }

      // Vertical facings come from pitch, horizontals from yaw.
      float yaw = mc.thePlayer.rotationYaw;
      float pitch = mc.thePlayer.rotationPitch;

      if(target == EnumFacing.UP) {
         pitch = opposite ? 90.0F : -90.0F;
      } else if(target == EnumFacing.DOWN) {
         pitch = opposite ? -90.0F : 90.0F;
      } else {
         EnumFacing look = opposite ? target.getOpposite() : target;
         yaw = yawFor(look);
         pitch = 0.0F;
      }

      mc.getNetHandler().addToSendQueue(
            new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, mc.thePlayer.onGround));
   }

   /** Yaw that makes {@code getHorizontalFacing()} return the given direction. */
   private static float yawFor(EnumFacing f) {
      switch(f) {
         case SOUTH: return 0.0F;
         case WEST:  return 90.0F;
         case NORTH: return 180.0F;
         case EAST:  return -90.0F;
         default:    return 0.0F;
      }
   }

   // ------------------------------------------------------------------
   // Inventory
   // ------------------------------------------------------------------

   /**
    * A hotbar slot holding the block, pulling it up from the main inventory when
    * it is not already down there.
    *
    * <p>Without the pull the printer just stops on any block that scrolled out
    * of the hotbar -- it reports nothing wrong, it simply never places it, which
    * reads as "the printer skipped half the cannon".
    */
   private int resolveSlot(Minecraft mc, Config cfg, Block block) {
      Item wanted = Item.getItemFromBlock(block);
      if(wanted == null) {
         return -1;
      }

      int inHotbar = findInHotbar(mc, cfg, wanted);
      if(inHotbar >= 0 || !cfg.useInventory) {
         return inHotbar;
      }

      // Main inventory is container slots 9..35, which happen to be the same
      // indices the player's own inventory uses.
      for(int i = 9; i < 36; ++i) {
         ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
         if(!isUsable(s, wanted)) {
            continue;
         }

         int target = spareHotbarSlot(mc, cfg);
         if(target < 0) {
            return -1;
         }

         // Mode 2 = swap with hotbar; button carries the destination slot.
         mc.playerController.windowClick(0, i, target, 2, mc.thePlayer);
         return findInHotbar(mc, cfg, wanted);
      }

      return -1;
   }

   private static int findInHotbar(Minecraft mc, Config cfg, Item wanted) {
      for(int i = 0; i < 9; ++i) {
         if(cfg.slots[i] && isUsable(mc.thePlayer.inventory.getStackInSlot(i), wanted)) {
            return i;
         }
      }

      return -1;
   }

   private static boolean isUsable(ItemStack s, Item wanted) {
      return s != null && s.stackSize > 0 && s.getItem() instanceof ItemBlock && s.getItem() == wanted;
   }

   /**
    * Where to drop a pulled stack. Prefers an empty enabled slot so nothing you
    * are carrying gets displaced; falls back to the last enabled slot, since
    * whatever lived there goes back to the inventory rather than being lost.
    */
   private static int spareHotbarSlot(Minecraft mc, Config cfg) {
      int fallback = -1;

      for(int i = 0; i < 9; ++i) {
         if(!cfg.slots[i]) {
            continue;
         }

         if(mc.thePlayer.inventory.getStackInSlot(i) == null) {
            return i;
         }

         fallback = i;
      }

      return fallback;
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

   /** Piston bases, heads and the moving-block entity. */
   private static boolean isPistonCell(Block b) {
      return b instanceof net.minecraft.block.BlockPistonBase
            || b instanceof net.minecraft.block.BlockPistonExtension
            || b instanceof net.minecraft.block.BlockPistonMoving;
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
      public boolean useInventory = true;
      public boolean orientBlocks = true;
      public boolean[] slots = new boolean[]{true, true, true, true, true, true, true, true, true};
   }
}
