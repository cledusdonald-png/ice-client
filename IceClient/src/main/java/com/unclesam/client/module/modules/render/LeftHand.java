package com.unclesam.client.module.modules.render;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;

/**
 * Renders the held item on the left side of the screen.
 *
 * <p>1.8.9 has no off-hand and no main-hand setting -- the hand position is baked
 * into {@code ItemRenderer.renderItemInFirstPerson}. The actual mirroring
 * therefore lives in {@code MixinItemRenderer}, which wraps that method in a
 * flipped matrix; this class exists only to own the toggle so the feature shows
 * up in the GUI like any other module.
 */
public class LeftHand extends Module {

   public LeftHand() {
      super("LeftHand", "Renders your held item on the left", ModuleCategory.MECHANIC);
   }
}
