package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters for the vanilla debug overlay, applied by {@code MixinGuiOverlayDebug}.
 *
 * <p>Matching is done on the line's leading label rather than by index: the
 * order and count of debug lines shift with dimension, reduced-debug mode and
 * whether a chunk is loaded, so index-based filtering silently hides the wrong
 * rows.
 */
public class BetterF3 extends Module {

   private final BooleanSetting rightColumn = this.addBool("Right column", false);
   private final BooleanSetting lagometer = this.addBool("Lagometer", false);
   private final BooleanSetting versionLines = this.addBool("Version / Java lines", false);
   private final BooleanSetting chunkLines = this.addBool("Chunk & light lines", true);
   private final BooleanSetting entityLines = this.addBool("Entity / particle counts", true);
   private final BooleanSetting biomeLine = this.addBool("Biome & difficulty", true);

   public BetterF3() {
      super("BetterF3", "Trims the F3 debug overlay", ModuleCategory.HUD);
   }

   public boolean showLagometer() {
      return this.lagometer.get();
   }

   public List<String> filterLeft(List<String> original) {
      if(original == null) {
         return original;
      }

      List<String> out = new ArrayList<String>(original.size());
      for(String line : original) {
         if(this.keepLeft(line)) {
            out.add(line);
         }
      }

      return out;
   }

   public List<String> filterRight(List<String> original) {
      if(!this.rightColumn.get()) {
         // An empty list rather than null: vanilla iterates the result without
         // a null check and would NPE every frame.
         return new ArrayList<String>();
      }

      return original;
   }

   private boolean keepLeft(String line) {
      if(line == null || line.isEmpty()) {
         return true;
      }

      // Vanilla's first line is the "Minecraft 1.8.9 (...)" banner, and the
      // Java/mod lines follow it; all are noise once you know what you run.
      if(!this.versionLines.get()
            && (line.startsWith("Minecraft 1.") || line.startsWith("Java: ") || line.contains("fps"))) {
         // Keep the fps line -- it is the one genuinely useful part of that
         // group -- and drop only the build banner and Java version.
         return line.contains("fps");
      }

      if(!this.chunkLines.get()
            && (line.startsWith("C: ") || line.startsWith("Chunk-Cache:") || line.startsWith("CH ")
                  || line.startsWith("Client Light") || line.startsWith("Server L"))) {
         return false;
      }

      if(!this.entityLines.get()
            && (line.startsWith("E: ") || line.startsWith("P: ") || line.startsWith("Particles:"))) {
         return false;
      }

      if(!this.biomeLine.get()
            && (line.startsWith("Biome:") || line.startsWith("Local Difficulty:"))) {
         return false;
      }

      return true;
   }
}
