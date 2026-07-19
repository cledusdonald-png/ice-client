package com.iceclient.minimap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.MathHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Cache of sampled {@link MapChunk}s, with a bounded rebuild budget per tick.
 *
 * <p>The budget is the important part. Sampling every visible chunk in the frame
 * the player crosses a boundary would drop several frames at once -- exactly
 * when you are moving fast and least want a stutter. Instead a handful are
 * rebuilt per tick and the map fills in over a few frames.
 *
 * <p>Keyed on a packed long rather than a point object to avoid allocating a key
 * on every lookup; the render path does one lookup per visible chunk per frame.
 */
public class MinimapCache {

   /** Chunks sampled per tick. Four is imperceptible; sixteen is not. */
   private static final int BUDGET_PER_TICK = 4;

   /** Dropped once this far outside the view, in chunks. */
   private static final int EVICT_MARGIN = 4;

   /**
    * How far the player may move vertically before cave slices are stale.
    * Rebuilding on every Y change would thrash the cache on stairs.
    */
   private static final int CAVE_RESAMPLE_DY = 4;

   private final Map<Long, MapChunk> chunks = new HashMap<Long, MapChunk>();
   private int dimension = Integer.MIN_VALUE;
   private boolean lastCaveMode;

   private static long key(int cx, int cz) {
      return ((long)cx & 0xFFFFFFFFL) << 32 | ((long)cz & 0xFFFFFFFFL);
   }

   /** Already-sampled chunk, or null when it has not been built yet. */
   public MapChunk get(int cx, int cz) {
      MapChunk c = this.chunks.get(Long.valueOf(key(cx, cz)));
      return c != null && c.isBuilt() ? c : null;
   }

   public void clear() {
      this.chunks.clear();
   }

   /**
    * Builds up to the per-tick budget of missing chunks in the given radius and
    * evicts anything well outside it.
    */
   public void update(Minecraft mc, int centerChunkX, int centerChunkZ, int radiusChunks,
                      boolean caveMode) {
      WorldClient world = mc.theWorld;
      if(world == null || mc.thePlayer == null) {
         this.clear();
         return;
      }

      // Nether stone and overworld grass at the same coordinates would
      // otherwise blend into each other after a portal.
      int dim = mc.thePlayer.dimension;
      if(dim != this.dimension || caveMode != this.lastCaveMode) {
         this.dimension = dim;
         this.lastCaveMode = caveMode;
         this.clear();
      }

      int playerY = MathHelper.floor_double(mc.thePlayer.posY);

      this.evictFarChunks(centerChunkX, centerChunkZ, radiusChunks);

      int spent = 0;
      // Spiral outward from the player so the centre of the map -- the part
      // being looked at -- resolves first.
      for(int ring = 0; ring <= radiusChunks && spent < BUDGET_PER_TICK; ++ring) {
         for(int dz = -ring; dz <= ring && spent < BUDGET_PER_TICK; ++dz) {
            for(int dx = -ring; dx <= ring && spent < BUDGET_PER_TICK; ++dx) {
               if(Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                  continue;
               }

               int cx = centerChunkX + dx;
               int cz = centerChunkZ + dz;
               Long k = Long.valueOf(key(cx, cz));

               MapChunk existing = this.chunks.get(k);
               if(existing != null && existing.isBuilt() && !this.isStale(existing, caveMode, playerY)) {
                  continue;
               }

               MapChunk c = existing != null ? existing : new MapChunk(cx, cz);
               if(c.build(world, caveMode, playerY)) {
                  this.chunks.put(k, c);
                  ++spent;
               } else {
                  // Not loaded yet: keep the stub so the retry does not
                  // reallocate, but do not spend budget on it.
                  this.chunks.put(k, c);
               }
            }
         }
      }

   }

   /** Cave slices go stale as the player changes level; surface ones do not. */
   private boolean isStale(MapChunk c, boolean caveMode, int playerY) {
      return caveMode && Math.abs(c.getSliceY() - playerY) >= CAVE_RESAMPLE_DY;
   }

   /**
    * Marks a chunk for resampling, so a placed obsidian wall shows up without
    * waiting for a reload.
    */
   public void invalidate(int cx, int cz) {
      this.chunks.remove(Long.valueOf(key(cx, cz)));
   }

   private void evictFarChunks(int centerChunkX, int centerChunkZ, int radiusChunks) {
      int limit = radiusChunks + EVICT_MARGIN;
      Iterator<Map.Entry<Long, MapChunk>> it = this.chunks.entrySet().iterator();

      while(it.hasNext()) {
         MapChunk c = it.next().getValue();
         if(Math.abs(c.getChunkX() - centerChunkX) > limit
               || Math.abs(c.getChunkZ() - centerChunkZ) > limit) {
            it.remove();
         }
      }

   }
}
