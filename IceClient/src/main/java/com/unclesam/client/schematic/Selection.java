package com.unclesam.client.schematic;

import net.minecraft.util.BlockPos;

/**
 * The Point A / Point B region shared between the GUI page and the in-world
 * renderer.
 *
 * <p>Static because the two consumers never coexist: the workspace page reads it
 * while the GUI is open, the render module draws it while the GUI is closed.
 * Threading a single instance between a screen and a module would mean the
 * screen owning state that has to outlive it.
 */
public final class Selection {

   private static BlockPos pointA;
   private static BlockPos pointB;

   private Selection() {
   }

   public static BlockPos getA() {
      return pointA;
   }

   public static BlockPos getB() {
      return pointB;
   }

   public static void setA(BlockPos pos) {
      pointA = pos;
   }

   public static void setB(BlockPos pos) {
      pointB = pos;
   }

   public static void clear() {
      pointA = null;
      pointB = null;
   }

   public static boolean isComplete() {
      return pointA != null && pointB != null;
   }

   /** Block counts along each axis, inclusive of both corners, or null. */
   public static int[] size() {
      if(!isComplete()) {
         return null;
      }

      return new int[]{
            Math.abs(pointA.getX() - pointB.getX()) + 1,
            Math.abs(pointA.getY() - pointB.getY()) + 1,
            Math.abs(pointA.getZ() - pointB.getZ()) + 1
      };
   }

   public static long volume() {
      int[] s = size();
      return s == null ? 0L : (long)s[0] * (long)s[1] * (long)s[2];
   }

   /** Lower corner, so callers do not each have to normalise the pair. */
   public static BlockPos min() {
      if(!isComplete()) {
         return null;
      }

      return new BlockPos(
            Math.min(pointA.getX(), pointB.getX()),
            Math.min(pointA.getY(), pointB.getY()),
            Math.min(pointA.getZ(), pointB.getZ()));
   }

   public static BlockPos max() {
      if(!isComplete()) {
         return null;
      }

      return new BlockPos(
            Math.max(pointA.getX(), pointB.getX()),
            Math.max(pointA.getY(), pointB.getY()),
            Math.max(pointA.getZ(), pointB.getZ()));
   }
}
