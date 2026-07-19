package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import com.iceclient.setting.BooleanSetting;

import java.util.Collections;
import java.util.List;

/**
 * How long the current session has run.
 *
 * <p>"Session" means since the client launched, not since you joined a server --
 * {@code Reset on join} switches it to the latter if you'd rather time a raid.
 */
public class PlaytimeHud extends TextHudModule {

   private final BooleanSetting label = (BooleanSetting)this.addSetting(new BooleanSetting("Show label", true));
   private final BooleanSetting resetOnJoin = (BooleanSetting)this.addSetting(new BooleanSetting("Reset on join", false));

   private final long started = System.currentTimeMillis();
   private long joined;
   private boolean wasInWorld;

   public PlaytimeHud() {
      super("Playtime", "Shows how long this session has run", ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 120);
   }

   protected List<String> lines() {
      boolean inWorld = this.mc.theWorld != null;
      if(inWorld && !this.wasInWorld) {
         this.joined = System.currentTimeMillis();
      }

      this.wasInWorld = inWorld;

      long base = this.resetOnJoin.get() && this.joined != 0L ? this.joined : this.started;
      long secs = (System.currentTimeMillis() - base) / 1000L;
      String t = String.format("%d:%02d:%02d",
            Long.valueOf(secs / 3600L), Long.valueOf(secs % 3600L / 60L), Long.valueOf(secs % 60L));
      return Collections.singletonList(this.label.get() ? "Playtime: " + t : t);
   }
}
