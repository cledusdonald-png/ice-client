package com.iceclient.schematica.orbit;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.properties.IProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * The settings {@link OrbitPrinter} reads while it runs.
 *
 * <p>The printer was written against a module object that held its options as
 * public fields, and it reads them constantly -- roughly forty times a tick.
 * Rather than thread a config object through every method, this keeps the same
 * shape: plain fields, one shared instance, pushed from the Printer module each
 * tick. It is not elegant, but it means the ported code did not have to be
 * rewritten to accommodate us, which is the whole point of porting it.
 */
public final class PrinterSettings {

   private static final PrinterSettings INSTANCE = new PrinterSettings();

   public static PrinterSettings get() {
      return INSTANCE;
   }

   private PrinterSettings() {
   }

   // ---- placement ----
   public boolean print = true;
   public int placeDistance = 5;
   public boolean placeInsantly = true;   // spelling kept: the port reads this name
   public boolean placeAdjacent = true;
   public boolean placeBanners = true;
   public int delay = 0;
   public int timeout = 2;

   // ---- throughput ----
   public boolean doLimitPackets = true;
   public int packetLimit = 64;
   public boolean fast = true;
   public boolean uberFast = false;

   // ---- clearing ----
   public boolean destroyBlocks = false;
   public boolean destroyInstantly = false;
   public boolean destroyair = false;
   public boolean removeAir = false;
   public boolean breakBadBlocksModule = false;

   // ---- redstone ----
   public boolean delayPowered = true;
   public boolean autoTick = true;
   public boolean aTickTrapdoors = true;
   public int autoTickTimeout = 2;

   // ---- print 360 ----
   /** Derive each block's facing from the schematic and aim before placing. */
   public boolean print360 = true;
   /** Skip the per-block look packet once the queue is large, for throughput. */
   public boolean fast360 = false;

   // ---- misc ----
   public boolean disableGens = false;
   public boolean scanForAir = false;
   /** Send a held-item packet before each placement. */
   public boolean setHeldItem = true;

   /**
    * Whether printing is switched on.
    *
    * <p>The port reads this through the settings object because in Orbit the
    * settings <em>were</em> the module. Kept as a field here rather than routed
    * back to our Module, so the printer has no dependency on our class layout.
    */
   private boolean enabled;

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean v) {
      this.enabled = v;
   }

   /**
    * Block pairs that count as the same thing.
    *
    * <p>Both members of each pair are the same block in two states -- lit and
    * unlit redstone lamp, powered and unpowered repeater. Without this the
    * printer treats a lit lamp as the wrong block and breaks it, which in a
    * cannon means tearing out working redstone the moment it fires.
    */
   public static final Set<Pair.EitherPair<Integer, Integer>> SAME_IDS =
         new HashSet<Pair.EitherPair<Integer, Integer>>();

   private static void addPair(int a, int b) {
      SAME_IDS.add(Pair.EitherPair.of(Integer.valueOf(a), Integer.valueOf(b)));
      SAME_IDS.add(Pair.EitherPair.of(Integer.valueOf(b), Integer.valueOf(a)));
   }

   static {
      addPair(93, 94);      // repeater, unpowered / powered
      addPair(149, 150);    // comparator
      addPair(75, 76);      // redstone torch
      addPair(61, 62);      // furnace / lit furnace
      addPair(123, 124);    // redstone lamp
      addPair(73, 74);      // redstone ore
   }

   /**
    * Properties that describe power rather than shape.
    *
    * <p>A block whose only difference is one of these is not misplaced -- it is
    * the right block reacting to a signal, and replacing it would fight the
    * redstone rather than build it.
    */
   public static boolean isPowerStateProperty(IProperty<?> property) {
      if(property == null) {
         return false;
      }

      String name = property.getName();
      return "power".equals(name)
            || "powered".equals(name)
            || "extended".equals(name)
            || "locked".equals(name)
            || "triggered".equals(name)
            || "lit".equals(name);
   }

   /**
    * Piston heads and the moving-block entity.
    *
    * <p>Never touched in either direction: an extended piston is a base plus a
    * head that no schematic contains, so it always looks wrong, and breaking one
    * mid-fire kills the cannon.
    */
   public static boolean isPistonCell(Block block) {
      return block instanceof BlockPistonExtension || block instanceof BlockPistonMoving;
   }
}
