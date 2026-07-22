package com.iceclient.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Capes loaded from PNGs the user drops in {@code config/iceclient/capes}.
 *
 * <p>Kept apart from {@link CosmeticRegistry} because these are per-machine and
 * unowned: they are not in the catalogue, cannot be bought, and -- since the
 * file exists only on your disk -- are visible only to you. Sharing one with
 * the faction means it going in the jar as a real cosmetic.
 *
 * <p>Files are read once at startup and on demand from the wardrobe, not
 * watched, so adding a cape needs a Refresh rather than an alt-tab.
 */
public final class CustomCapes {

   /** Vanilla's cape sheet size. Anything else is rejected rather than stretched. */
   private static final int SHEET_W = 64;
   private static final int SHEET_H = 32;

   private static final Map<String, ResourceLocation> loaded = new HashMap<String, ResourceLocation>();
   private static final List<String> names = new ArrayList<String>();
   private static String lastError = "";

   private CustomCapes() {
   }

   public static File folder() {
      File dir = new File(new File(Minecraft.getMinecraft().mcDataDir, "config"), "iceclient");
      dir = new File(dir, "capes");
      if(!dir.exists()) {
         dir.mkdirs();
      }

      return dir;
   }

   public static List<String> names() {
      return names;
   }

   public static String getLastError() {
      return lastError;
   }

   public static ResourceLocation textureFor(String name) {
      return loaded.get(name);
   }

   /**
    * Rereads the folder.
    *
    * <p>Textures already uploaded are kept rather than re-uploaded, so hitting
    * Refresh repeatedly does not leak GL texture handles.
    */
   public static void reload() {
      names.clear();
      lastError = "";

      File dir = folder();
      File[] files = dir.listFiles();
      if(files == null) {
         return;
      }

      for(File f : files) {
         String fn = f.getName();
         if(!fn.toLowerCase().endsWith(".png")) {
            continue;
         }

         String name = fn.substring(0, fn.length() - 4);

         if(loaded.containsKey(name)) {
            names.add(name);
            continue;
         }

         try {
            BufferedImage img = ImageIO.read(f);
            if(img == null) {
               lastError = fn + ": not a readable PNG";
               continue;
            }

            // 64x32 is the format the cape model's UVs expect. A 22x17 crop or a
            // HD sheet would render as garbage rather than fail, so it is worth
            // saying no clearly instead.
            if(img.getWidth() != SHEET_W || img.getHeight() != SHEET_H) {
               lastError = fn + ": must be " + SHEET_W + "x" + SHEET_H
                     + " (this is " + img.getWidth() + "x" + img.getHeight() + ")";
               continue;
            }

            ResourceLocation rl = Minecraft.getMinecraft().getTextureManager()
                  .getDynamicTextureLocation("iceclient_cape_" + name, new DynamicTexture(img));

            loaded.put(name, rl);
            names.add(name);
         } catch (Exception e) {
            lastError = fn + ": " + e.getMessage();
         }
      }
   }
}
