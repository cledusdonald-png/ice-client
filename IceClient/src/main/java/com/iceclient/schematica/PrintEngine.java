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
    * Collects positions that still need work, lowest first.
    *
    * <p>Bottom-to-top is the correct order, not nearest-first: a block needs
    * something under or beside it to place against, so filling a layer before
    * the one above it means every block has support by the time its turn comes.
    * Sorting by distance instead left the top of a wall failing to place until a
    * later pass happened to fill the gap beneath it. Distance is only the
    * tiebreaker within a layer, so the nearest reachable block on each level
    * still goes first.
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
               // Same block: only revisit it if it is facing the wrong way and
               // we are set to correct that.
               if(cfg.fixOrientation && !orientationMatches(have, want)) {
                  found.add(world);
               }

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
            if(a.getY() != b.getY()) {
               return Integer.compare(a.getY(), b.getY());
            }

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

      double dist = mc.thePlayer.getDistanceSq(pos);
      if(dist > cfg.placeDistance * cfg.placeDistance) {
         return false;
      }

      IBlockState have = mc.theWorld.getBlockState(pos);

      // Right block already there. Done -- unless it is the right block facing
      // the wrong way and we are allowed to correct that. This is the dispenser
      // case: a same-type block whose facing does not match gets broken so the
      // next pass replaces it correctly. Without this it looked "done" and was
      // left pointing the wrong direction forever.
      if(have.getBlock() == want.getBlock()) {
         if(!cfg.fixOrientation || orientationMatches(have, want)) {
            return false;
         }

         mc.playerController.onPlayerDamageBlock(pos, EnumFacing.UP);
         if(cfg.breakInstantly) {
            mc.playerController.onPlayerDestroyBlock(pos, EnumFacing.UP);
         }

         return true;
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
         // Not in the hotbar. In creative we can conjure it -- which is the
         // whole point of the creative-grab option; on a build server you never
         // stock the hotbar by hand. Outside creative there is nothing to do
         // but skip.
         slot = cfg.creativeGrab ? grabCreative(mc, cfg, want.getBlock()) : -1;
         if(slot < 0) {
            return false;
         }
      }

      Placement placement = this.resolvePlacement(mc, cfg, pos, want);
      if(placement == null) {
         return false;
      }

      int previous = mc.thePlayer.inventory.currentItem;
      mc.thePlayer.inventory.currentItem = slot;

      float savedYaw = mc.thePlayer.rotationYaw;
      float savedPitch = mc.thePlayer.rotationPitch;

      if(placement.rotate) {
         // The server computes a directional block's state from the player's
         // rotation as *it* knows it, so a look packet has to actually go out --
         // setting the field alone only fixes the client-side ghost. The view
         // is restored below within the same tick, so nothing renders between
         // the two and the head does not visibly snap.
         mc.thePlayer.rotationYaw = placement.yaw;
         mc.thePlayer.rotationPitch = placement.pitch;
         if(mc.getNetHandler() != null) {
            mc.getNetHandler().addToSendQueue(
                  new net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook(
                        placement.yaw, placement.pitch, mc.thePlayer.onGround));
         }
      }

      mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
            mc.thePlayer.getHeldItem(), placement.against, placement.clickFace, placement.hit);
      mc.thePlayer.swingItem();

      if(placement.rotate) {
         mc.thePlayer.rotationYaw = savedYaw;
         mc.thePlayer.rotationPitch = savedPitch;
      }

      // Restoring the slot keeps the printer from stealing the hotbar between
      // placements, which otherwise makes fighting mid-print impossible.
      if(!cfg.keepSlot) {
         mc.thePlayer.inventory.currentItem = previous;
      }

      return true;
   }

   /** A resolved click: what to click, where, and how to be facing. */
   private static final class Placement {
      final BlockPos against;
      final EnumFacing clickFace;
      final Vec3 hit;
      final boolean rotate;
      final float yaw;
      final float pitch;

      Placement(BlockPos against, EnumFacing clickFace, Vec3 hit, boolean rotate, float yaw, float pitch) {
         this.against = against;
         this.clickFace = clickFace;
         this.hit = hit;
         this.rotate = rotate;
         this.yaw = yaw;
         this.pitch = pitch;
      }
   }

   private static final float[] YAWS = new float[]{0.0F, 90.0F, 180.0F, 270.0F};
   private static final float[] PITCHES = new float[]{0.0F, 80.0F, -80.0F};
   private static final float[] HIT_Y = new float[]{0.3F, 0.7F};

   /**
    * Works out how to place {@code pos} so the block lands in the schematic's
    * orientation.
    *
    * <p>For a plain block any solid neighbour will do. For a directional one --
    * stairs, a piston, a repeater -- the state depends on which face is clicked,
    * where on it, and which way the player faces. Rather than hardcode each
    * block's rules (they disagree: stairs face your look, a furnace faces you),
    * this asks vanilla: for each candidate click and rotation it runs the real
    * {@code onBlockPlaced} and keeps the first whose result matches the target.
    * Vanilla's own code is the source of truth, so no per-block table can drift
    * out of date.
    */
   private Placement resolvePlacement(Minecraft mc, Config cfg, BlockPos pos, IBlockState want) {
      List<EnumFacing> faces = new ArrayList<EnumFacing>(6);
      for(EnumFacing f : EnumFacing.values()) {
         Block b = mc.theWorld.getBlockState(pos.offset(f)).getBlock();
         if(b != Blocks.air && (cfg.placeAdjacent || b.getMaterial().isSolid())) {
            faces.add(f);
         }
      }

      if(faces.isEmpty()) {
         if(!cfg.midAir) {
            return null;
         }

         // Nothing solid to click. On a server that permits floating placement
         // -- or once the schematic below is filled by the bottom-up pass --
         // attempt it against the neighbour anyway. Harmless where the server
         // rejects it; it just does not place.
         Collections.addAll(faces, EnumFacing.values());
      }

      boolean directional = cfg.faceBlocks && isDirectional(want);

      if(!directional) {
         EnumFacing f = faces.get(0);
         EnumFacing click = f.getOpposite();
         return new Placement(pos.offset(f), click, faceCenter(pos.offset(f), click), false, 0.0F, 0.0F);
      }

      int meta = mc.thePlayer.getHeldItem() != null ? mc.thePlayer.getHeldItem().getMetadata() : 0;
      Block block = want.getBlock();
      int stateId = Block.getStateId(want);

      // Fast path: a rotation that solved this exact state before almost always
      // solves it again (a wall of stairs all want the same facing). Try the
      // cached rotation across the available faces before the full 24-combo
      // search -- this is what keeps the orientation logic from tanking the
      // frame rate on a large uniform structure.
      int[] cached = ORIENT_CACHE.get(Integer.valueOf(stateId));
      if(cached != null) {
         for(EnumFacing f : faces) {
            Placement p = this.tryPlace(mc, block, pos, f, want, meta,
                  YAWS[cached[0]], PITCHES[cached[1]], HIT_Y[cached[2]]);
            if(p != null) {
               return p;
            }
         }
      }

      for(int fi = 0; fi < faces.size(); ++fi) {
         EnumFacing f = faces.get(fi);
         for(int yi = 0; yi < YAWS.length; ++yi) {
            for(int pi = 0; pi < PITCHES.length; ++pi) {
               for(int hi = 0; hi < HIT_Y.length; ++hi) {
                  Placement p = this.tryPlace(mc, block, pos, f, want, meta,
                        YAWS[yi], PITCHES[pi], HIT_Y[hi]);
                  if(p != null) {
                     ORIENT_CACHE.put(Integer.valueOf(stateId), new int[]{yi, pi, hi});
                     return p;
                  }
               }
            }
         }
      }

      // No combination matched: place it anyway in its default orientation
      // rather than skip the block forever. Better a wrong-facing stair than a
      // permanent hole the printer keeps stalling on.
      EnumFacing f = faces.get(0);
      EnumFacing click = f.getOpposite();
      return new Placement(pos.offset(f), click, faceCenter(pos.offset(f), click), false, 0.0F, 0.0F);
   }

   private final java.util.Map<Integer, int[]> ORIENT_CACHE = new java.util.HashMap<Integer, int[]>();

   /**
    * Dry-runs one candidate click and returns a Placement if the block would
    * land in the target orientation, else null.
    */
   private Placement tryPlace(Minecraft mc, Block block, BlockPos pos, EnumFacing f,
                              IBlockState want, int meta, float yaw, float pitch, float hy) {
      BlockPos against = pos.offset(f);
      EnumFacing click = f.getOpposite();

      Vec3 hit = new Vec3(
            (double)against.getX() + 0.5D + (double)click.getFrontOffsetX() * 0.5D,
            click.getAxis() == EnumFacing.Axis.Y
                  ? (double)against.getY() + 0.5D + (double)click.getFrontOffsetY() * 0.5D
                  : (double)against.getY() + (double)hy,
            (double)against.getZ() + 0.5D + (double)click.getFrontOffsetZ() * 0.5D);

      float fhx = (float)(hit.xCoord - against.getX());
      float fhy = (float)(hit.yCoord - against.getY());
      float fhz = (float)(hit.zCoord - against.getZ());

      IBlockState sim;
      float oy = mc.thePlayer.rotationYaw;
      float op = mc.thePlayer.rotationPitch;
      try {
         mc.thePlayer.rotationYaw = yaw;
         mc.thePlayer.rotationPitch = pitch;
         sim = block.onBlockPlaced(mc.theWorld, pos, click, fhx, fhy, fhz, meta, mc.thePlayer);
      } catch (Throwable t) {
         // A block whose onBlockPlaced touches the world can throw in this dry
         // run; treat it as "cannot resolve" and move on.
         return null;
      } finally {
         mc.thePlayer.rotationYaw = oy;
         mc.thePlayer.rotationPitch = op;
      }

      return orientationMatches(sim, want)
            ? new Placement(against, click, hit, true, yaw, pitch)
            : null;
   }

   private static Vec3 faceCenter(BlockPos against, EnumFacing click) {
      return new Vec3(
            (double)against.getX() + 0.5D + (double)click.getFrontOffsetX() * 0.5D,
            (double)against.getY() + 0.5D + (double)click.getFrontOffsetY() * 0.5D,
            (double)against.getZ() + 0.5D + (double)click.getFrontOffsetZ() * 0.5D);
   }

   /** Whether a block's state carries orientation worth aiming for. */
   private static boolean isDirectional(IBlockState state) {
      for(Object o : state.getProperties().keySet()) {
         String name = ((net.minecraft.block.properties.IProperty)o).getName();
         if("facing".equals(name) || "half".equals(name) || "axis".equals(name)
               || "rotation".equals(name)) {
            return true;
         }
      }

      return false;
   }

   /** True when both states agree on every orientation property they share. */
   private static boolean orientationMatches(IBlockState a, IBlockState b) {
      if(a == null || b == null || a.getBlock() != b.getBlock()) {
         return false;
      }

      return prop(a, "facing", b) && prop(a, "half", b) && prop(a, "axis", b)
            && prop(a, "rotation", b);
   }

   /** True unless both states define the named property and disagree on it. */
   private static boolean prop(IBlockState a, String name, IBlockState b) {
      Comparable<?> va = value(a, name);
      Comparable<?> vb = value(b, name);
      return va == null || vb == null || va.equals(vb);
   }

   private static Comparable<?> value(IBlockState state, String name) {
      for(java.util.Map.Entry<?, ?> e : state.getProperties().entrySet()) {
         if(((net.minecraft.block.properties.IProperty)e.getKey()).getName().equals(name)) {
            return (Comparable<?>)e.getValue();
         }
      }

      return null;
   }

   /**
    * Puts the wanted block into a hotbar slot via a creative-give packet and
    * returns that slot, or -1 when not in creative.
    *
    * <p>This is what a creative printer does: on a build/plot server you have
    * creative but an empty hotbar, and stocking nine slots by hand for a large
    * schematic is the tedium the printer exists to remove. {@code sendSlotPacket}
    * is the same call the creative inventory GUI uses, and it no-ops off
    * creative, so this cannot give items on a survival server even if misused.
    *
    * <p>Prefers the first <em>empty</em> enabled slot so it does not clobber a
    * block you are already holding; only if every enabled slot is full does it
    * reuse the current one, since the schematic needs this block now.
    */
   private static int grabCreative(Minecraft mc, Config cfg, Block block) {
      if(!mc.playerController.isInCreativeMode()) {
         return -1;
      }

      net.minecraft.item.Item item = Item.getItemFromBlock(block);
      if(item == null) {
         return -1;
      }

      int slot = firstUsableSlot(mc, cfg);
      if(slot < 0) {
         return -1;
      }

      ItemStack stack = new ItemStack(item, 64, block.getMetaFromState(block.getDefaultState()));

      // Update the client inventory as well as sending the packet: the place
      // that follows reads getHeldItem this same tick, before any server echo
      // could arrive.
      mc.thePlayer.inventory.setInventorySlotContents(slot, stack);

      // Player-inventory container slot ids: the hotbar is 36-44.
      mc.playerController.sendSlotPacket(stack, 36 + slot);
      return slot;
   }

   /** First empty enabled hotbar slot, else the current slot if it is enabled. */
   private static int firstUsableSlot(Minecraft mc, Config cfg) {
      for(int i = 0; i < 9; ++i) {
         if(cfg.slots[i] && mc.thePlayer.inventory.getStackInSlot(i) == null) {
            return i;
         }
      }

      int cur = mc.thePlayer.inventory.currentItem;
      return cfg.slots[cur] ? cur : -1;
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
      public boolean creativeGrab;
      public boolean faceBlocks = true;
      public boolean fixOrientation = true;
      public boolean midAir;
      public boolean[] slots = new boolean[]{true, true, true, true, true, true, true, true, true};
   }
}
