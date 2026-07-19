package com.unclesam.client.schematic;

import com.unclesam.client.schematic.Schematic;
import com.unclesam.client.schematic.SchematicManager;
import com.unclesam.client.util.WorldRenderUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

public final class SchematicRenderer {
   private static int displayList = -1;
   private static boolean dirty = true;
   private static int layer = -1;
   private static boolean renderInWater = true;
   private static boolean doBadBlock = true;
   private static boolean doAirBlock = false;
   private static boolean doMissingBlock = true;
   private static boolean doFacingBlock = true;
   private static int badBlockColor = -43691;
   private static int airColor = 1442840575;
   private static int missingBlockColor = -11141291;
   private static int invalidStateColor = -22016;
   private static int renderDist = 64;

   private SchematicRenderer() {
   }

   public static void invalidate() {
      dirty = true;
   }

   public static int getLayer() {
      return layer;
   }

   public static void setLayer(int l) {
      layer = l;
      dirty = true;
   }

   public static void setColors(int missing, int wrong, int a, boolean miss, boolean wr, int dist) {
      missingBlockColor = missing;
      badBlockColor = wrong;
      doMissingBlock = miss;
      doBadBlock = wr;
      renderDist = dist;
      dirty = true;
   }

   public static void setOrbitSettings(boolean inWater, boolean bad, boolean air, boolean missing, boolean facing, int badCol, int airCol, int missCol, int invalidCol, int dist) {
      renderInWater = inWater;
      doBadBlock = bad;
      doAirBlock = air;
      doMissingBlock = missing;
      doFacingBlock = facing;
      badBlockColor = badCol;
      airColor = airCol;
      missingBlockColor = missCol;
      invalidStateColor = invalidCol;
      renderDist = dist;
      dirty = true;
   }

   public static void resetColors() {
      renderInWater = true;
      doBadBlock = true;
      doAirBlock = false;
      doMissingBlock = true;
      doFacingBlock = true;
      badBlockColor = -43691;
      airColor = 1442840575;
      missingBlockColor = -11141291;
      invalidStateColor = -22016;
      renderDist = 64;
      dirty = true;
   }

   public static void render() {
      Schematic s = SchematicManager.getLoaded();
      Minecraft mc = Minecraft.getMinecraft();
      if(s != null && mc.theWorld != null) {
         if(mc.thePlayer == null || !mc.thePlayer.isInsideOfMaterial(Material.water) || renderInWater) {
            if(dirty || displayList == -1) {
               rebuild(s);
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(-WorldRenderUtil.camX(), -WorldRenderUtil.camY(), -WorldRenderUtil.camZ());
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.depthMask(false);
            GL11.glDisable(2884);
            if(displayList != -1) {
               GL11.glCallList(displayList);
            }

            GL11.glEnable(2884);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
         }
      }
   }

   private static void rebuild(Schematic s) {
      Minecraft mc = Minecraft.getMinecraft();
      if(displayList == -1) {
         displayList = GL11.glGenLists(1);
      }

      GL11.glNewList(displayList, 4864);
      GL11.glBegin(7);

      for(int y = 0; y < s.getHeight(); ++y) {
         if(layer < 0 || y == layer) {
            for(int z = 0; z < s.getLength(); ++z) {
               for(int x = 0; x < s.getWidth(); ++x) {
                  if(s.isAir(x, y, z)) {
                     if(doAirBlock) {
                        BlockPos world = s.toWorld(x, y, z);
                        if(inRange(mc, world) && mc.theWorld.getBlockState(world).getBlock().getMaterial() == Material.air) {
                           emitColoredCube(world, airColor);
                        }
                     }
                  } else {
                     BlockPos world = s.toWorld(x, y, z);
                     if(inRange(mc, world)) {
                        IBlockState existing = mc.theWorld.getBlockState(world);
                        IBlockState want = s.getBlockState(x, y, z);
                        boolean occupied = existing.getBlock().getMaterial() != Material.air;
                        if(existing.getBlock() == want.getBlock()) {
                           if(occupied && existing != want && doFacingBlock) {
                              emitColoredCube(world, invalidStateColor);
                           }
                        } else if(occupied) {
                           if(doBadBlock) {
                              emitColoredCube(world, badBlockColor);
                           }
                        } else if(doMissingBlock) {
                           emitColoredCube(world, missingBlockColor);
                        }
                     }
                  }
               }
            }
         }
      }

      GL11.glEnd();
      GL11.glEndList();
      dirty = false;
   }

   private static boolean inRange(Minecraft mc, BlockPos world) {
      double dx = (double)world.getX() + 0.5D - mc.getRenderViewEntity().posX;
      double dy = (double)world.getY() + 0.5D - mc.getRenderViewEntity().posY;
      double dz = (double)world.getZ() + 0.5D - mc.getRenderViewEntity().posZ;
      return dx * dx + dy * dy + dz * dz <= (double)(renderDist * renderDist);
   }

   private static void emitColoredCube(BlockPos world, int col) {
      float r = (float)(col >> 16 & 255) / 255.0F;
      float g = (float)(col >> 8 & 255) / 255.0F;
      float b = (float)(col & 255) / 255.0F;
      float a = (float)(col >> 24 & 255) / 255.0F;
      if(a <= 0.0F) {
         a = 0.35F;
      }

      GL11.glColor4f(r, g, b, a);
      emitCube((double)world.getX(), (double)world.getY(), (double)world.getZ());
   }

   private static void emitCube(double wx, double wy, double wz) {
      double i = 0.001D;
      double x1 = wx + i;
      double y1 = wy + i;
      double z1 = wz + i;
      double x2 = wx + 1.0D - i;
      double y2 = wy + 1.0D - i;
      double z2 = wz + 1.0D - i;
      GL11.glVertex3d(x1, y2, z1);
      GL11.glVertex3d(x1, y2, z2);
      GL11.glVertex3d(x2, y2, z2);
      GL11.glVertex3d(x2, y2, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x2, y1, z1);
      GL11.glVertex3d(x2, y1, z2);
      GL11.glVertex3d(x1, y1, z2);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x1, y2, z1);
      GL11.glVertex3d(x2, y2, z1);
      GL11.glVertex3d(x2, y1, z1);
      GL11.glVertex3d(x1, y1, z2);
      GL11.glVertex3d(x2, y1, z2);
      GL11.glVertex3d(x2, y2, z2);
      GL11.glVertex3d(x1, y2, z2);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x1, y1, z2);
      GL11.glVertex3d(x1, y2, z2);
      GL11.glVertex3d(x1, y2, z1);
      GL11.glVertex3d(x2, y1, z1);
      GL11.glVertex3d(x2, y2, z1);
      GL11.glVertex3d(x2, y2, z2);
      GL11.glVertex3d(x2, y1, z2);
   }
}
