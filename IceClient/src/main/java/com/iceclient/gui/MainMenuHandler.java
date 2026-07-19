package com.iceclient.gui;

import com.iceclient.gui.IceMainMenuScreen;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MainMenuHandler {
   public MainMenuHandler() {
   }

   @SubscribeEvent
   public void onGuiOpen(GuiOpenEvent event) {
      if(event.gui instanceof GuiMainMenu) {
         event.gui = new IceMainMenuScreen();
      }

   }
}
