package com.unclesam.client.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;

public class Schematic {
   private final String name;
   private final int width;
   private final int height;
   private final int length;
   private final int[] blockIds;
   private final byte[] data;
   private BlockPos origin = BlockPos.ORIGIN;
   private int rotation;
   private boolean flipX;
   private boolean flipZ;

   private Schematic(String name, int width, int height, int length, int[] blockIds, byte[] data) {
      this.name = name;
      this.width = width;
      this.height = height;
      this.length = length;
      this.blockIds = blockIds;
      this.data = data;
   }

   public static Schematic load(File file) throws Exception {
      InputStream in = new FileInputStream(file);
      Throwable var2 = null;

      Schematic var24;
      try {
         NBTTagCompound tag = CompressedStreamTools.readCompressed(in);
         int w = tag.getShort("Width");
         int h = tag.getShort("Height");
         int l = tag.getShort("Length");
         byte[] blocks = tag.getByteArray("Blocks");
         byte[] meta = tag.getByteArray("Data");
         if(w <= 0 || h <= 0 || l <= 0 || blocks.length < w * h * l) {
            throw new IllegalArgumentException("Malformed schematic: " + file.getName());
         }

         byte[] add = tag.hasKey("AddBlocks")?tag.getByteArray("AddBlocks"):null;
         int[] ids = new int[w * h * l];

         for(int i = 0; i < ids.length; ++i) {
            int id = blocks[i] & 255;
            if(add != null && i >> 1 < add.length) {
               int high = (i & 1) == 0?add[i >> 1] >> 4 & 15:add[i >> 1] & 15;
               id |= high << 8;
            }

            ids[i] = id;
         }

         String n = file.getName();
         if(n.toLowerCase().endsWith(".schematic")) {
            n = n.substring(0, n.length() - 10);
         }

         var24 = new Schematic(n, w, h, l, ids, meta);
      } catch (Throwable var21) {
         var2 = var21;
         throw var21;
      } finally {
         if(in != null) {
            if(var2 != null) {
               try {
                  in.close();
               } catch (Throwable var20) {
                  var2.addSuppressed(var20);
               }
            } else {
               in.close();
            }
         }

      }

      return var24;
   }

   public String getName() {
      return this.name;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public int getLength() {
      return this.length;
   }

   public int getVolume() {
      return this.width * this.height * this.length;
   }

   public BlockPos getOrigin() {
      return this.origin;
   }

   public void setOrigin(BlockPos origin) {
      this.origin = origin;
   }

   public int getRotation() {
      return this.rotation;
   }

   public void rotate() {
      this.rotation = this.rotation + 1 & 3;
   }

   public boolean isFlipX() {
      return this.flipX;
   }

   public boolean isFlipZ() {
      return this.flipZ;
   }

   public void flipX() {
      this.flipX = !this.flipX;
   }

   public void flipZ() {
      this.flipZ = !this.flipZ;
   }

   private int index(int x, int y, int z) {
      return (y * this.length + z) * this.width + x;
   }

   public IBlockState getBlockState(int x, int y, int z) {
      if(x >= 0 && y >= 0 && z >= 0 && x < this.width && y < this.height && z < this.length) {
         int i = this.index(x, y, z);
         Block block = Block.getBlockById(this.blockIds[i]);
         if(block == null) {
            return Blocks.air.getDefaultState();
         } else {
            int m = i < this.data.length?this.data[i] & 15:0;

            try {
               return block.getStateFromMeta(m);
            } catch (Exception var8) {
               return block.getDefaultState();
            }
         }
      } else {
         return Blocks.air.getDefaultState();
      }
   }

   public boolean isAir(int x, int y, int z) {
      return this.getBlockState(x, y, z).getBlock() == Blocks.air;
   }

   public BlockPos toWorld(int x, int y, int z) {
      int lx = this.flipX?this.width - 1 - x:x;
      int lz = this.flipZ?this.length - 1 - z:z;
      int rx;
      int rz;
      switch(this.rotation) {
      case 1:
         rx = this.length - 1 - lz;
         rz = lx;
         break;
      case 2:
         rx = this.width - 1 - lx;
         rz = this.length - 1 - lz;
         break;
      case 3:
         rx = lz;
         rz = this.width - 1 - lx;
         break;
      default:
         rx = lx;
         rz = lz;
      }

      return this.origin.add(rx, y, rz);
   }

   public int getRenderWidth() {
      return (this.rotation & 1) == 0?this.width:this.length;
   }

   public int getRenderLength() {
      return (this.rotation & 1) == 0?this.length:this.width;
   }
}
