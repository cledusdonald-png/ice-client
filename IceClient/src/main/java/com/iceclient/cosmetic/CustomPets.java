package com.iceclient.cosmetic;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pets loaded from {@code config/iceclient/pets}.
 *
 * <p>Same arrangement as {@link CustomCapes}: files on your machine, reloadable
 * from the wardrobe, visible only to you. The difference is that a pet is
 * described rather than drawn, so a design here can be turned into a real
 * catalogue pet by pasting the JSON into the registry -- which makes this the
 * natural place to work one out before it ships to everyone.
 */
public final class CustomPets {

   private static final Map<String, PetModel> loaded = new LinkedHashMap<String, PetModel>();
   private static final List<String> errors = new ArrayList<String>();

   private CustomPets() {
   }

   public static File folder() {
      File dir = new File(new File(Minecraft.getMinecraft().mcDataDir, "config"), "iceclient");
      dir = new File(dir, "pets");
      if(!dir.exists()) {
         dir.mkdirs();
         writeExample(dir);
      }

      return dir;
   }

   public static List<String> names() {
      return new ArrayList<String>(loaded.keySet());
   }

   public static PetModel get(String name) {
      return loaded.get(name);
   }

   public static List<String> getErrors() {
      return errors;
   }

   /** Rereads every file. Models are cheap, so this rebuilds rather than diffs. */
   public static void reload() {
      loaded.clear();
      errors.clear();

      File[] files = folder().listFiles();
      if(files == null) {
         return;
      }

      for(File f : files) {
         if(!f.getName().toLowerCase().endsWith(".json")) {
            continue;
         }

         try {
            PetModel m = PetModel.load(f);
            loaded.put(m.name, m);
         } catch (Exception e) {
            // Named, because a typo in a pet you just wrote should be findable.
            errors.add(f.getName() + ": " + e.getMessage());
         }
      }
   }

   /**
    * Drops a worked example in the folder the first time it is created.
    *
    * <p>An empty folder and a format description is a worse start than one file
    * you can open, change a number in, and see the result.
    */
   private static void writeExample(File dir) {
      String example = "{\n"
            + "  \"name\": \"Snow Golem\",\n"
            + "  \"scale\": 1.0,\n"
            + "  \"spin\": true,\n"
            + "  \"bob\": true,\n"
            + "\n"
            + "  \"_help\": [\n"
            + "    \"pos is [x, y, z] in blocks from the pet's centre; +y up, +z forward.\",\n"
            + "    \"box  size is [width, height, depth].\",\n"
            + "    \"spike size is [base, length] and points up unless down:true.\",\n"
            + "    \"color is hex, no #. alpha 0-1 for see-through parts.\",\n"
            + "    \"Edit, then hit Refresh on the Custom tab in the wardrobe.\"\n"
            + "  ],\n"
            + "\n"
            + "  \"parts\": [\n"
            + "    { \"shape\": \"box\",   \"pos\": [0, 0.00, 0],    \"size\": [0.22, 0.20, 0.22], \"color\": \"E8F4FA\" },\n"
            + "    { \"shape\": \"box\",   \"pos\": [0, 0.22, 0],    \"size\": [0.15, 0.15, 0.15], \"color\": \"FFFFFF\" },\n"
            + "    { \"shape\": \"spike\", \"pos\": [0, 0.30, 0.07], \"size\": [0.05, 0.13],       \"color\": \"FF8A3D\", \"down\": true },\n"
            + "    { \"shape\": \"box\",   \"pos\": [-0.05, 0.25, 0.08], \"size\": [0.03, 0.03, 0.02], \"color\": \"1A1A1A\" },\n"
            + "    { \"shape\": \"box\",   \"pos\": [0.05, 0.25, 0.08],  \"size\": [0.03, 0.03, 0.02], \"color\": \"1A1A1A\" }\n"
            + "  ]\n"
            + "}\n";

      try {
         Writer w = new OutputStreamWriter(
               new FileOutputStream(new File(dir, "snow_golem.json")), StandardCharsets.UTF_8);
         w.write(example);
         w.close();
      } catch (Exception ignored) {
         // Not being able to write the example is not worth failing over.
      }
   }
}
