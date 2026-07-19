package com.iceclient.schematic;

import com.iceclient.IceClient;
import com.iceclient.schematic.Schematic;
import com.iceclient.schematic.SchematicRenderer;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public final class SchematicManager {
   private static Schematic loaded;

   private SchematicManager() {
   }

   public static File directory() {
      File dir = new File(Minecraft.getMinecraft().mcDataDir, "schematics");
      if(!dir.exists()) {
         dir.mkdirs();
      }

      return dir;
   }

   public static List<File> list() {
      List<File> out = new ArrayList();
      collect(directory(), out, 0);
      out.sort(Comparator.comparing((f) -> {
         return f.getName().toLowerCase();
      }));
      return out;
   }

   private static void collect(File dir, List<File> out, int depth) {
      if(depth <= 4) {
         File[] files = dir.listFiles();
         if(files != null) {
            for(File f : files) {
               if(f.isDirectory()) {
                  collect(f, out, depth + 1);
               } else if(f.getName().toLowerCase().endsWith(".schematic")) {
                  out.add(f);
               }
            }

         }
      }
   }

   public static Schematic getLoaded() {
      return loaded;
   }

   public static boolean isLoaded() {
      return loaded != null;
   }

   public static void unload() {
      loaded = null;
      SchematicRenderer.invalidate();
   }

   public static boolean load(File file) {
      try {
         Schematic s = Schematic.load(file);
         Minecraft mc = Minecraft.getMinecraft();
         if(mc.thePlayer != null) {
            s.setOrigin(new BlockPos(Math.floor(mc.thePlayer.posX), Math.floor(mc.thePlayer.posY), Math.floor(mc.thePlayer.posZ)));
         }

         loaded = s;
         SchematicRenderer.invalidate();
         chat(EnumChatFormatting.GREEN + "Loaded " + EnumChatFormatting.GRAY + s.getName() + EnumChatFormatting.DARK_GRAY + " (" + s.getWidth() + "x" + s.getHeight() + "x" + s.getLength() + ", " + s.getVolume() + " blocks)");
         return true;
      } catch (Exception var3) {
         IceClient.LOGGER.error("Failed to load schematic " + file, var3);
         chat(EnumChatFormatting.RED + "Failed to load " + file.getName() + ": " + var3.getMessage());
         return false;
      }
   }

   public static void moveToPlayer() {
      Minecraft mc = Minecraft.getMinecraft();
      if(loaded != null && mc.thePlayer != null) {
         loaded.setOrigin(new BlockPos(Math.floor(mc.thePlayer.posX), Math.floor(mc.thePlayer.posY), Math.floor(mc.thePlayer.posZ)));
         SchematicRenderer.invalidate();
      }
   }

   public static void move(int dx, int dy, int dz) {
      if(loaded != null) {
         loaded.setOrigin(loaded.getOrigin().add(dx, dy, dz));
         SchematicRenderer.invalidate();
      }
   }

   static void chat(String s) {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.thePlayer != null) {
         mc.thePlayer.addChatMessage(new ChatComponentText(s));
      }

   }
}
