package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import com.iceclient.setting.BooleanSetting;

import java.util.Collections;
import java.util.List;

/** The resource pack you're currently using. */
public class ResourcePackHud extends TextHudModule {

   private final BooleanSetting label = (BooleanSetting)this.addSetting(new BooleanSetting("Show label", true));
   private final BooleanSetting stripExt = (BooleanSetting)this.addSetting(new BooleanSetting("Strip extension", true));

   public ResourcePackHud() {
      super("Resource Pack", "Shows your active resource pack", ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 140);
   }

   protected List<String> lines() {
      List<String> packs = this.mc.gameSettings.resourcePacks;
      // Vanilla's list is empty when only the default pack is active.
      String name = packs == null || packs.isEmpty() ? "Default" : packs.get(packs.size() - 1);
      if(this.stripExt.get() && name.toLowerCase().endsWith(".zip")) {
         name = name.substring(0, name.length() - 4);
      }

      return Collections.singletonList(this.label.get() ? "Pack: " + name : name);
   }
}
