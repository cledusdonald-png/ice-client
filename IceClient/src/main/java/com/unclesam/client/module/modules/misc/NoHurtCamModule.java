package com.unclesam.client.module.modules.misc;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;

public class NoHurtCamModule extends Module {
   public NoHurtCamModule() {
      super("No Hurt Cam", "Removes the screen shake from taking damage", ModuleCategory.GENERAL, 0);
   }
}
