package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.module.TextHudModule;
import com.unclesam.client.setting.BooleanSetting;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.Collections;
import java.util.List;

/** Your ping to the current server. */
public class PingHud extends TextHudModule {

   private final BooleanSetting label = (BooleanSetting)this.addSetting(new BooleanSetting("Show label", true));

   public PingHud() {
      super("Ping", "Shows your ping to the server", ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 80);
   }

   protected List<String> lines() {
      int ping = this.ping();
      if(ping < 0) {
         return Collections.emptyList();
      }

      return Collections.singletonList(this.label.get() ? "Ping: " + ping + "ms" : ping + "ms");
   }

   /** -1 when we're not connected or the tab entry isn't populated yet. */
   private int ping() {
      if(this.mc.thePlayer == null || this.mc.getNetHandler() == null) {
         return -1;
      }

      NetworkPlayerInfo info = this.mc.getNetHandler().getPlayerInfo(this.mc.thePlayer.getUniqueID());
      return info == null ? -1 : Math.max(0, info.getResponseTime());
   }
}
