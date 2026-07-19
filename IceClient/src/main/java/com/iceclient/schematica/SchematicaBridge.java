package com.iceclient.schematica;

import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RenderSchematic;
import com.github.lunatrius.schematica.client.util.FlipHelper;
import com.github.lunatrius.schematica.client.util.RotationHelper;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.Loader;

public final class SchematicaBridge {
   private SchematicaBridge() {
   }

   public static boolean isAvailable() {
      return Loader.isModLoaded("Schematica");
   }

   private static Minecraft mc() {
      return Minecraft.getMinecraft();
   }

   public static File directory() {
      return ConfigurationHandler.schematicDirectory;
   }

   public static List<String> listFiles() {
      List<String> names = new ArrayList();
      File dir = directory();
      if(dir == null) {
         return names;
      } else {
         File[] files = dir.listFiles((d, n) -> {
            return n.toLowerCase().endsWith(".schematic");
         });
         if(files != null) {
            Arrays.sort(files, (a, b) -> {
               return a.getName().compareToIgnoreCase(b.getName());
            });

            for(File f : files) {
               names.add(f.getName());
            }
         }

         return names;
      }
   }

   public static boolean load(String filename) {
      if(mc().thePlayer == null) {
         return false;
      } else {
         boolean ok = ((ClientProxy)Schematica.proxy).loadSchematic(mc().thePlayer, directory(), filename);
         if(ok && ClientProxy.schematic != null) {
            ClientProxy.schematic.isRendering = true;
         }

         return ok;
      }
   }

   public static void unload() {
      ((ClientProxy)Schematica.proxy).unloadSchematic();
   }

   public static boolean hasSchematic() {
      return ClientProxy.schematic != null;
   }

   public static String name() {
      SchematicWorld s = ClientProxy.schematic;
      return s == null?null:s.getWidth() + "x" + s.getHeight() + "x" + s.getLength();
   }

   public static int[] position() {
      SchematicWorld s = ClientProxy.schematic;
      return s == null?null:new int[]{s.position.getX(), s.position.getY(), s.position.getZ()};
   }

   public static void moveHere() {
      SchematicWorld s = ClientProxy.schematic;
      if(s != null && mc().thePlayer != null) {
         ClientProxy.setPlayerData(mc().thePlayer, 1.0F);
         ClientProxy.moveSchematicToPlayer(s);
         RenderSchematic.INSTANCE.refresh();
      }
   }

   public static void nudge(int dx, int dy, int dz) {
      SchematicWorld s = ClientProxy.schematic;
      if(s != null) {
         s.position.set(s.position.getX() + dx, s.position.getY() + dy, s.position.getZ() + dz);
         RenderSchematic.INSTANCE.refresh();
      }
   }

   public static boolean isRendering() {
      SchematicWorld s = ClientProxy.schematic;
      return s != null && s.isRendering;
   }

   /**
    * Whether any schematic overlay should draw at all.
    *
    * <p>Every module that draws something schematic-shaped must gate on this,
    * not on its own enabled flag alone. "Render: OFF" is one switch to the
    * user, but the drawing is spread across Schematica's own renderer, Ice's
    * preview, and both missing-block ESPs. A module that skips this check
    * leaves wireframes on screen while the toggle reads OFF -- which reads as
    * the toggle being broken, not as that module being on.
    *
    * <p>Returns true when Schematica is absent, so the standalone renderers
    * still work without it installed.
    */
   public static boolean shouldRenderOverlays() {
      return !isAvailable() || isRendering();
   }

   public static void setRendering(boolean on) {
      SchematicWorld s = ClientProxy.schematic;
      if(s != null) {
         s.isRendering = on;
         RenderSchematic.INSTANCE.refresh();
      }
   }

   public static boolean isPrinting() {
      return SchematicPrinter.INSTANCE.isPrinting();
   }

   public static void setPrinting(boolean on) {
      if(ClientProxy.schematic == null) {
         SchematicPrinter.INSTANCE.setPrinting(false);
      } else {
         SchematicPrinter.INSTANCE.setPrinting(on);
      }
   }

   public static void setPrinterEnabled(boolean on) {
      SchematicPrinter.INSTANCE.setEnabled(on);
      ConfigurationHandler.printerEnabled = on;
   }

   public static void applyPrinterConfig(int placeDistance, boolean placeInstantly, int placeDelay, int timeout, boolean placeAdjacent, boolean destroyBlocks, boolean destroyInstantly, boolean[] slots) {
      applyPrinterScalars(placeDistance, placeInstantly, placeDelay, timeout, placeAdjacent, destroyBlocks, destroyInstantly);
      applySwapSlots(slots);
   }

   public static void applyPrinterScalars(int placeDistance, boolean placeInstantly, int placeDelay, int timeout, boolean placeAdjacent, boolean destroyBlocks, boolean destroyInstantly) {
      ConfigurationHandler.placeDistance = placeDistance;
      ConfigurationHandler.placeInstantly = placeInstantly;
      ConfigurationHandler.placeDelay = placeDelay;
      ConfigurationHandler.timeout = timeout;
      ConfigurationHandler.placeAdjacent = placeAdjacent;
      ConfigurationHandler.destroyBlocks = destroyBlocks;
      ConfigurationHandler.destroyInstantly = destroyInstantly;
   }

   private static void applySwapSlots(boolean[] slots) {
      if(slots != null && slots.length == ConfigurationHandler.swapSlots.length) {
         System.arraycopy(slots, 0, ConfigurationHandler.swapSlots, 0, slots.length);
         ConfigurationHandler.swapSlotsQueue.clear();

         for(int i = 0; i < slots.length; ++i) {
            if(slots[i]) {
               ConfigurationHandler.swapSlotsQueue.offer(Integer.valueOf(i));
            }
         }

      }
   }

   public static void setFastPrinting(boolean fast) {
      ConfigurationHandler.placeInstantly = fast;
      ConfigurationHandler.placeDelay = fast?0:1;
      ConfigurationHandler.placeDistance = 5;
   }

   public static boolean isFastPrinting() {
      return ConfigurationHandler.placeInstantly;
   }

   public static void setAutoBreak(boolean on) {
      ConfigurationHandler.destroyBlocks = on;
      ConfigurationHandler.destroyInstantly = on;
   }

   public static boolean isAutoBreak() {
      return ConfigurationHandler.destroyBlocks;
   }

   public static void forEachMissing(double maxDist, SchematicaBridge.MissingVisitor v) {
      SchematicWorld s = ClientProxy.schematic;
      EntityPlayerSP p = mc().thePlayer;
      if(s != null && p != null && mc().theWorld != null) {
         int ox = s.position.getX();
         int oy = s.position.getY();
         int oz = s.position.getZ();
         int[] b = localBounds(s, p, maxDist);
         double maxSq = maxDist * maxDist;

         for(int y = b[1]; y <= b[4]; ++y) {
            for(int z = b[2]; z <= b[5]; ++z) {
               for(int x = b[0]; x <= b[3]; ++x) {
                  if(!(s.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BlockAir)) {
                     int wx = ox + x;
                     int wy = oy + y;
                     int wz = oz + z;
                     BlockPos wp = new BlockPos(wx, wy, wz);
                     if(mc().theWorld.isAirBlock(wp)) {
                        double ddx = (double)wx + 0.5D - p.posX;
                        double ddy = (double)wy + 0.5D - p.posY;
                        double ddz = (double)wz + 0.5D - p.posZ;
                        if(ddx * ddx + ddy * ddy + ddz * ddz <= maxSq) {
                           v.accept((double)wx, (double)wy, (double)wz);
                        }
                     }
                  }
               }
            }
         }

      }
   }

   public static void forEachSchematicBlock(double maxDist, SchematicaBridge.SchemBlockVisitor v) {
      SchematicWorld s = ClientProxy.schematic;
      EntityPlayerSP p = mc().thePlayer;
      if(s != null && p != null) {
         int ox = s.position.getX();
         int oy = s.position.getY();
         int oz = s.position.getZ();
         int[] b = localBounds(s, p, maxDist);
         double maxSq = maxDist * maxDist;

         for(int y = b[1]; y <= b[4]; ++y) {
            for(int z = b[2]; z <= b[5]; ++z) {
               for(int x = b[0]; x <= b[3]; ++x) {
                  IBlockState st = s.getBlockState(new BlockPos(x, y, z));
                  if(!(st.getBlock() instanceof BlockAir)) {
                     int wx = ox + x;
                     int wy = oy + y;
                     int wz = oz + z;
                     double ddx = (double)wx + 0.5D - p.posX;
                     double ddy = (double)wy + 0.5D - p.posY;
                     double ddz = (double)wz + 0.5D - p.posZ;
                     if(ddx * ddx + ddy * ddy + ddz * ddz <= maxSq) {
                        v.accept(new BlockPos(wx, wy, wz), st);
                     }
                  }
               }
            }
         }

      }
   }

   /**
    * The schematic's block at a <em>world</em> position, or null when there is
    * no schematic, the position is outside it, or the schematic has air there.
    *
    * <p>Callers work in world coordinates because that is what a raytrace hit
    * gives you; the schematic stores everything relative to {@code position},
    * so the offset is subtracted here rather than at each call site.
    */
   public static IBlockState blockStateAt(BlockPos worldPos) {
      SchematicWorld s = ClientProxy.schematic;
      if(s == null || worldPos == null) {
         return null;
      }

      int lx = worldPos.getX() - s.position.getX();
      int ly = worldPos.getY() - s.position.getY();
      int lz = worldPos.getZ() - s.position.getZ();

      if(lx < 0 || ly < 0 || lz < 0 || lx >= s.getWidth() || ly >= s.getHeight() || lz >= s.getLength()) {
         return null;
      }

      IBlockState st = s.getBlockState(new BlockPos(lx, ly, lz));
      return st != null && !(st.getBlock() instanceof BlockAir) ? st : null;
   }

   /**
    * Rotates the loaded schematic about the vertical axis.
    *
    * <p>Schematica's helper rebuilds the block array and remaps directional
    * block states, which is why this delegates rather than transposing
    * ourselves -- getting stairs and chests to face the right way after a
    * rotation is most of the work, and it already does it.
    */
   public static boolean rotate(boolean clockwise) {
      SchematicWorld s = ClientProxy.schematic;
      if(s == null) {
         return false;
      }

      boolean ok = RotationHelper.INSTANCE.rotate(s, EnumFacing.UP, !clockwise);
      if(ok) {
         RenderSchematic.INSTANCE.refresh();
      }

      return ok;
   }

   /** Mirrors the schematic across the axis of the given facing. */
   public static boolean flip(EnumFacing axis) {
      SchematicWorld s = ClientProxy.schematic;
      if(s == null) {
         return false;
      }

      boolean ok = FlipHelper.INSTANCE.flip(s, axis, false);
      if(ok) {
         RenderSchematic.INSTANCE.refresh();
      }

      return ok;
   }

   private static int[] localBounds(SchematicWorld s, EntityPlayerSP p, double maxDist) {
      int w = s.getWidth();
      int h = s.getHeight();
      int l = s.getLength();
      int ox = s.position.getX();
      int oy = s.position.getY();
      int oz = s.position.getZ();
      int r = (int)Math.ceil(maxDist) + 1;
      int px = MathHelper.floor_double(p.posX) - ox;
      int py = MathHelper.floor_double(p.posY) - oy;
      int pz = MathHelper.floor_double(p.posZ) - oz;
      return new int[]{Math.max(0, px - r), Math.max(0, py - r), Math.max(0, pz - r), Math.min(w - 1, px + r), Math.min(h - 1, py + r), Math.min(l - 1, pz + r)};
   }

   public static String writeTestSchematic() {
      try {
         File dir = directory();
         if(dir == null) {
            return null;
         } else {
            if(!dir.exists()) {
               dir.mkdirs();
            }

            short w = 5;
            short h = 4;
            short l = 5;
            byte[] blocks = new byte[w * h * l];

            for(int y = 0; y < h; ++y) {
               for(int z = 0; z < l; ++z) {
                  for(int x = 0; x < w; ++x) {
                     boolean shell = x == 0 || x == w - 1 || z == 0 || z == l - 1 || y == 0;
                     blocks[(y * l + z) * w + x] = (byte)(shell?1:0);
                  }
               }
            }

            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setShort("Width", w);
            nbt.setShort("Height", h);
            nbt.setShort("Length", l);
            nbt.setString("Materials", "Alpha");
            nbt.setByteArray("Blocks", blocks);
            nbt.setByteArray("Data", new byte[w * h * l]);
            nbt.setTag("Entities", new NBTTagList());
            nbt.setTag("TileEntities", new NBTTagList());
            File out = new File(dir, "test_box.schematic");
            CompressedStreamTools.writeCompressed(nbt, new FileOutputStream(out));
            return out.getName();
         }
      } catch (Exception var9) {
         return null;
      }
   }

   /**
    * Writes the world region between two corners out as a .schematic.
    *
    * <p>Corners are given in any order and normalised here, because they come
    * from two separate player clicks and there is no reason B should be the
    * higher one.
    *
    * <p>Block IDs are written to the classic single-byte {@code Blocks} array.
    * 1.8.9 vanilla IDs all fit in a byte, so the {@code AddBlocks} extension
    * that modded IDs above 255 would need is deliberately not emitted -- a
    * modded block is stored as air rather than silently truncated into a
    * different block, which would be worse than a hole.
    *
    * @return the written file name, or null on failure
    */
   public static String saveRegion(BlockPos a, BlockPos b, String name) {
      if(a == null || b == null || mc().theWorld == null) {
         return null;
      }

      try {
         File dir = directory();
         if(dir == null) {
            return null;
         }

         if(!dir.exists()) {
            dir.mkdirs();
         }

         int minX = Math.min(a.getX(), b.getX());
         int minY = Math.min(a.getY(), b.getY());
         int minZ = Math.min(a.getZ(), b.getZ());
         int maxX = Math.max(a.getX(), b.getX());
         int maxY = Math.max(a.getY(), b.getY());
         int maxZ = Math.max(a.getZ(), b.getZ());

         int w = maxX - minX + 1;
         int h = maxY - minY + 1;
         int l = maxZ - minZ + 1;

         // 32767 is the format's signed-short ceiling per axis; the volume cap
         // is ours, to stop a stray click allocating a gigabyte.
         if(w > 32767 || h > 32767 || l > 32767 || (long)w * (long)h * (long)l > 4000000L) {
            return null;
         }

         byte[] blocks = new byte[w * h * l];
         byte[] data = new byte[w * h * l];

         for(int y = 0; y < h; ++y) {
            for(int z = 0; z < l; ++z) {
               for(int x = 0; x < w; ++x) {
                  IBlockState st = mc().theWorld.getBlockState(
                        new BlockPos(minX + x, minY + y, minZ + z));

                  int id = net.minecraft.block.Block.getIdFromBlock(st.getBlock());
                  int idx = (y * l + z) * w + x;

                  if(id < 0 || id > 255) {
                     blocks[idx] = 0;
                     data[idx] = 0;
                  } else {
                     blocks[idx] = (byte)id;
                     data[idx] = (byte)st.getBlock().getMetaFromState(st);
                  }
               }
            }
         }

         NBTTagCompound nbt = new NBTTagCompound();
         nbt.setShort("Width", (short)w);
         nbt.setShort("Height", (short)h);
         nbt.setShort("Length", (short)l);
         nbt.setString("Materials", "Alpha");
         nbt.setByteArray("Blocks", blocks);
         nbt.setByteArray("Data", data);
         nbt.setTag("Entities", new NBTTagList());
         nbt.setTag("TileEntities", new NBTTagList());

         String fileName = name.toLowerCase().endsWith(".schematic") ? name : name + ".schematic";
         File out = new File(dir, fileName);
         CompressedStreamTools.writeCompressed(nbt, new FileOutputStream(out));
         return out.getName();
      } catch (Exception e) {
         return null;
      }
   }

   public interface MissingVisitor {
      void accept(double var1, double var3, double var5);
   }

   public interface SchemBlockVisitor {
      void accept(BlockPos var1, IBlockState var2);
   }
}
