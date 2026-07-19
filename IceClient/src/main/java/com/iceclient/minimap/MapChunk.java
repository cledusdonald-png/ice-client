package com.iceclient.minimap;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;

/**
 * One 16x16 chunk of sampled minimap colour.
 *
 * <p>Sampling means walking down every column to find the first opaque block,
 * which is far too expensive per frame. These are built once, cached by
 * {@link MinimapCache}, and rebuilt only when marked dirty.
 *
 * <p>Colours are stored as packed ARGB rather than block references so the
 * render path never touches the world -- rendering happens on the render thread
 * while chunks can unload underneath it.
 */
public class MapChunk {

   public static final int SIZE = 16;

   /** How far above the player cave mode starts looking for a ceiling. */
   private static final int CAVE_UP = 3;

   /** How far below the player cave mode will search for a floor. */
   private static final int CAVE_DOWN = 24;

   private final int chunkX;
   private final int chunkZ;
   private final int[] colors = new int[SIZE * SIZE];
   private boolean built;

   /**
    * The Y this was sampled at in cave mode, so the cache can tell when the
    * player has climbed far enough that the slice is stale. Unused for surface
    * sampling, where the heightmap is Y-independent.
    */
   private int sliceY = Integer.MIN_VALUE;

   public MapChunk(int chunkX, int chunkZ) {
      this.chunkX = chunkX;
      this.chunkZ = chunkZ;
   }

   public int getChunkX() {
      return this.chunkX;
   }

   public int getChunkZ() {
      return this.chunkZ;
   }

   public boolean isBuilt() {
      return this.built;
   }

   public int getSliceY() {
      return this.sliceY;
   }

   public int colorAt(int localX, int localZ) {
      return this.colors[(localZ & 15) * SIZE + (localX & 15)];
   }

   /**
    * Samples the chunk out of the world.
    *
    * @param caveMode  sample a slice around {@code playerY} instead of the surface
    * @return false when the chunk is not loaded, so the caller can retry rather
    *         than caching a block of empty pixels
    */
   public boolean build(WorldClient world, boolean caveMode, int playerY) {
      if(world == null) {
         return false;
      }

      Chunk chunk = world.getChunkFromChunkCoords(this.chunkX, this.chunkZ);
      if(chunk == null || !chunk.isLoaded()) {
         return false;
      }

      int baseX = this.chunkX << 4;
      int baseZ = this.chunkZ << 4;

      for(int lz = 0; lz < SIZE; ++lz) {
         for(int lx = 0; lx < SIZE; ++lx) {
            int wx = baseX + lx;
            int wz = baseZ + lz;
            this.colors[lz * SIZE + lx] = caveMode
                  ? this.sampleCave(world, wx, wz, playerY)
                  : this.sampleSurface(world, chunk, wx, wz);
         }
      }

      this.built = true;
      this.sliceY = caveMode ? playerY : Integer.MIN_VALUE;
      return true;
   }

   /**
    * Colour of the topmost visible block in one column, shaded by height.
    *
    * <p>The height shading is what makes terrain readable -- without it a hilly
    * area is a flat green rectangle and you cannot see the ridge you are on.
    */
   private int sampleSurface(WorldClient world, Chunk chunk, int worldX, int worldZ) {
      int top = chunk.getHeightValue(worldX & 15, worldZ & 15);

      for(int y = Math.min(top, world.getHeight() - 1); y > 0; --y) {
         int c = this.colorOf(world, worldX, y, worldZ);
         if(c != 0) {
            return shade(c, y, world.getSeaLevel());
         }
      }

      return 0xFF000000;
   }

   /**
    * Cave mode: the first solid block at or below the player's level.
    *
    * <p>Surface sampling is useless underground -- every column returns the
    * mountain above your head and the map is a single flat colour. Searching
    * downward from just above the player instead shows the floor you are
    * walking on and leaves open shafts dark, which is what makes tunnels
    * legible.
    */
   private int sampleCave(WorldClient world, int worldX, int worldZ, int playerY) {
      int from = Math.min(world.getHeight() - 1, playerY + CAVE_UP);
      int to = Math.max(1, playerY - CAVE_DOWN);

      for(int y = from; y >= to; --y) {
         int c = this.colorOf(world, worldX, y, worldZ);
         if(c != 0) {
            // Shaded against the player's own level, so the floor directly
            // around you is brightest and drops away with depth.
            return shade(c, y, playerY);
         }
      }

      // Nothing within the slice: open air or an unlit void below. Drawn dark
      // rather than black so it reads as "no data" and not as solid stone.
      return 0xFF0C0C10;
   }

   /** Packed colour of a block, or 0 when it should not be drawn. */
   private int colorOf(WorldClient world, int x, int y, int z) {
      IBlockState state = world.getBlockState(new BlockPos(x, y, z));
      if(state.getBlock() == Blocks.air) {
         return 0;
      }

      MapColor mc = state.getBlock().getMapColor(state);
      if(mc == null || mc == MapColor.airColor) {
         return 0;
      }

      return mc.colorValue | 0xFF000000;
   }

   /**
    * Darkens below the reference level and lightens above it, on a curve gentle
    * enough that the block's own colour still dominates.
    */
   private static int shade(int argb, int y, int reference) {
      float delta = (float)(y - reference) / 64.0F;
      float factor = 1.0F + Math.max(-0.45F, Math.min(0.35F, delta * 0.5F));

      int r = clamp(Math.round((float)(argb >> 16 & 255) * factor));
      int g = clamp(Math.round((float)(argb >> 8 & 255) * factor));
      int b = clamp(Math.round((float)(argb & 255) * factor));

      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private static int clamp(int v) {
      return v < 0 ? 0 : (v > 255 ? 255 : v);
   }
}
