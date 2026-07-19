package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.module.TextHudModule;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Collections;
import java.util.List;

/** The address of the server you're on. Hidden in singleplayer. */
public class ServerIpHud extends TextHudModule {

   public ServerIpHud() {
      super("Server IP", "Shows the current server address", ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 90);
   }

   protected List<String> lines() {
      ServerData data = this.mc.getCurrentServerData();
      if(data == null || data.serverIP == null || data.serverIP.isEmpty()) {
         return Collections.emptyList();
      }

      return Collections.singletonList(data.serverIP);
   }
}
