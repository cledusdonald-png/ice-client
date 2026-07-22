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
   version = IceClient.VERSION,
   acceptedMinecraftVersions = "[1.8.9]"
)
public class IceClient {
   public static final String MODID = "iceclient";
   public static final String NAME = "Ice Client";

   /**
    * Build version.
    *
    * <p>An annotation value has to be a compile-time constant, so this cannot
    * simply be read from the jar -- which is why the number lived in four
    * hard-coded places and the main menu still claimed v0.1.0 several releases
    * later. It is declared once here and everything else reads
    * {@link #displayVersion()}, so the only edit when releasing is this line
    * and {@code mod_version} in gradle.properties.
    */
   public static final String VERSION = "1.8.0";

   public static final Logger LOGGER = LogManager.getLogger("Ice Client");

   /** Cached once; Forge's mod list does not change after load. */
   private static String resolvedVersion;

   /**
    * The version to show a user.
    *
    * <p>Prefers what Forge actually loaded, which comes from mcmod.info and so
    * from the Gradle build -- if this ever disagrees with {@link #VERSION} the
    * jar is the truth, and the menu should say what is really running.
    */
   public static String displayVersion() {
      if(resolvedVersion == null) {
         resolvedVersion = VERSION;

         try {
            net.minecraftforge.fml.common.ModContainer c =
                  net.minecraftforge.fml.common.Loader.instance().getIndexedModList().get(MODID);
            if(c != null && c.getVersion() != null && !c.getVersion().isEmpty()
                  && !c.getVersion().equals("${version}")) {
               resolvedVersion = c.getVersion();
            }
         } catch (Throwable t) {
            // Dev environment without a built mcmod.info -- the constant is fine.
         }
      }

      return resolvedVersion;
   }
   /**
    * Publishes worn cosmetics to the Ice server and reads back everyone else's.
    *
    * <p>Held here rather than created inline so the wardrobe can poke it the
    * moment you equip something, instead of your friends waiting out the
    * interval to see the change.
    */
   public static final com.iceclient.cosmetic.CosmeticSync COSMETIC_SYNC =
         new com.iceclient.cosmetic.CosmeticSync();

   private static final int GUI_KEY = 54;          // Right Shift
   private static final int HUD_EDITOR_KEY = 157;  // Right Ctrl
   private static final int COSMETICS_KEY = 184;   // Right Alt

   public IceClient() {
   }

   @EventHandler
   public void init(FMLInitializationEvent event) {
      LOGGER.info("Booting up Ice Client v" + displayVersion());
      clearStuckSmoothCamera();
      ModuleManager.init();
      MinecraftForge.EVENT_BUS.register(this);
      MinecraftForge.EVENT_BUS.register(new MainMenuHandler());
      MinecraftForge.EVENT_BUS.register(new com.iceclient.cosmetic.render.CapeRenderer());
      MinecraftForge.EVENT_BUS.register(new com.iceclient.cosmetic.render.AccessoryRenderer());
      MinecraftForge.EVENT_BUS.register(new com.iceclient.cosmetic.render.PetRenderer());
      MinecraftForge.EVENT_BUS.register(COSMETIC_SYNC);
      ClientCommandHandler.instance.registerCommand(new SchemCommand());
      com.iceclient.command.MacroCommand.register();
      com.iceclient.command.IceCommand.register();
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
            if(Keyboard.getEventKey() == GUI_KEY) {
               mc.displayGuiScreen(new ClickGuiScreen());
            } else if(Keyboard.getEventKey() == HUD_EDITOR_KEY) {
               mc.displayGuiScreen(new HudEditorScreen());
            } else if(Keyboard.getEventKey() == COSMETICS_KEY) {
               mc.displayGuiScreen(new com.iceclient.gui.CosmeticsScreen());
            }

         }
      }
   }
}
