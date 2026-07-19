package com.iceclient;

import com.iceclient.config.ConfigManager;
import com.iceclient.gui.ClickGuiScreen;
import com.iceclient.gui.HudEditorScreen;
import com.iceclient.gui.MainMenuHandler;
import com.iceclient.module.ModuleManager;
import com.iceclient.module.modules.factions.SchemCommand;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

@Mod(
   modid = "iceclient",
   name = "Ice Client",
   version = "0.1.0",
   acceptedMinecraftVersions = "[1.8.9]"
)
public class IceClient {
   public static final String MODID = "iceclient";
   public static final String NAME = "Ice Client";
   public static final String VERSION = "0.1.0";
   public static final Logger LOGGER = LogManager.getLogger("Ice Client");
   private static final int GUI_KEY = 54;
   private static final int HUD_EDITOR_KEY = 157;

   public IceClient() {
   }

   @EventHandler
   public void init(FMLInitializationEvent event) {
      LOGGER.info("Booting up Ice Client v0.1.0");
      clearStuckSmoothCamera();
      ModuleManager.init();
      MinecraftForge.EVENT_BUS.register(this);
      MinecraftForge.EVENT_BUS.register(new MainMenuHandler());
      ClientCommandHandler.instance.registerCommand(new SchemCommand());
      com.iceclient.command.MacroCommand.register();
      ConfigManager.load();
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
         public void run() {
            ConfigManager.save();
         }
      }, "IceClient-config-save"));
   }

   /**
    * Clears vanilla's cinematic camera if it was left switched on.
    *
    * <p>The removed Zoom module used to enable {@code smoothCamera} and could
    * leave it set if the game crashed or lost focus mid-zoom. It makes the mouse
    * crawl and keep gliding after you stop moving it, and 1.8.9 has no options
    * button to turn it back off -- so a stuck value was effectively unfixable
    * from inside the game. This undoes that damage once at startup.
    */
   private static void clearStuckSmoothCamera() {
      try {
         net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
         if(mc != null && mc.gameSettings != null && mc.gameSettings.smoothCamera) {
            mc.gameSettings.smoothCamera = false;
            mc.gameSettings.saveOptions();
            LOGGER.info("Cleared stuck smoothCamera (cinematic camera) left over from Zoom");
         }
      } catch (Throwable t) {
         LOGGER.warn("Could not check smoothCamera: " + t);
      }

   }

   @SubscribeEvent
   public void onKeyInput(KeyInputEvent event) {
      if(Keyboard.getEventKeyState()) {
         Minecraft mc = Minecraft.getMinecraft();
         if(mc.currentScreen == null) {
            if(Keyboard.getEventKey() == 54) {
               mc.displayGuiScreen(new ClickGuiScreen());
            } else if(Keyboard.getEventKey() == 157) {
               mc.displayGuiScreen(new HudEditorScreen());
            }

         }
      }
   }
}
