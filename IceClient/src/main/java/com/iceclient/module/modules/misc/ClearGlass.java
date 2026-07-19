package com.iceclient.module.modules.misc;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;

/**
 * Makes glass render as nothing at all.
 *
 * <p>The work happens in {@code MixinBlockBreakable}; this owns the toggle and
 * the two block-class switches. Skipping the faces outright is cheaper than
 * drawing transparent ones, which is why it sits on the FPS page as well as
 * being a visibility aid -- a glass box stops costing anything to look through.
 *
 * <p>Note this is purely visual. The blocks are still solid, so you cannot walk
 * or shoot through the gap you can now see.
 */
public class ClearGlass extends Module {

   private final BooleanSetting blocks = this.addBool("Hide glass blocks", true);
   private final BooleanSetting panes = this.addBool("Hide glass panes", true);

   public ClearGlass() {
      super("Clear Glass", "Renders glass fully invisible", ModuleCategory.MECHANIC);
   }

   public boolean hidesBlocks() {
      return this.blocks.get();
   }

   public boolean hidesPanes() {
      return this.panes.get();
   }
}
