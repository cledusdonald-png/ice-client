package com.unclesam.client.module;

import com.unclesam.client.util.BindUtil;
import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.module.modules.factions.Callouts;
import com.unclesam.client.module.modules.factions.CannonReplay;
import com.unclesam.client.module.modules.factions.FloatFinder;
import com.unclesam.client.module.modules.factions.DispenserCheck;
import com.unclesam.client.module.modules.factions.ExplosionBox;
import com.unclesam.client.module.modules.factions.RaidingSounds;
import com.unclesam.client.module.modules.groups.PingLocation;
import com.unclesam.client.module.modules.groups.ShareClipboard;
import com.unclesam.client.module.modules.groups.FocusPlayer;
import com.unclesam.client.module.modules.factions.AutoTickModule;
import com.unclesam.client.module.modules.factions.BreadcrumbsModule;
import com.unclesam.client.module.modules.factions.MissingBlockEspModule;
import com.unclesam.client.module.modules.factions.PatchCrumbsModule;
import com.unclesam.client.module.modules.factions.RallyWaypointModule;
import com.unclesam.client.module.modules.factions.SchematicModule;
import com.unclesam.client.module.modules.hud.ArmorStatusModule;
import com.unclesam.client.module.modules.hud.CompassModule;
import com.unclesam.client.module.modules.hud.CoordinatesModule;
import com.unclesam.client.module.modules.hud.DirectionHud;
import com.unclesam.client.module.modules.hud.PingHud;
import com.unclesam.client.module.modules.hud.ServerIpHud;
import com.unclesam.client.module.modules.hud.TpsHud;
import com.unclesam.client.module.modules.hud.EntityCountHud;
import com.unclesam.client.module.modules.hud.PlaytimeHud;
import com.unclesam.client.module.modules.hud.ObsidianCounterHud;
import com.unclesam.client.module.modules.hud.FactionDisplayHud;
import com.unclesam.client.module.modules.hud.ResourcePackHud;
import com.unclesam.client.module.modules.hud.SaturationHud;
import com.unclesam.client.module.modules.render.ChunkBorders;
import com.unclesam.client.module.modules.hud.SprintDisplay;
import com.unclesam.client.module.modules.pvp.Alerts;
import com.unclesam.client.module.modules.misc.ChatNotify;
import com.unclesam.client.module.modules.misc.TimeChanger;
import com.unclesam.client.module.modules.factions.CannonView;
import com.unclesam.client.module.modules.factions.Minecadia;
import com.unclesam.client.module.modules.hud.CpsModule;
import com.unclesam.client.module.modules.hud.FpsModule;
import com.unclesam.client.module.modules.hud.KeystrokesModule;
import com.unclesam.client.module.modules.hud.PotCounterModule;
import com.unclesam.client.module.modules.hud.PotionStatusModule;
import com.unclesam.client.module.modules.hud.ScoreboardModule;
import com.unclesam.client.module.modules.misc.ItemSearch;
import com.unclesam.client.module.modules.misc.FovMod;
import com.unclesam.client.module.modules.misc.ItemLock;
import com.unclesam.client.module.modules.misc.ClearWater;
import com.unclesam.client.module.modules.misc.FpsParticles;
import com.unclesam.client.module.modules.misc.Macros;
import com.unclesam.client.module.modules.misc.FlyBoostModule;
import com.unclesam.client.module.modules.misc.LunarClientApi;
import com.unclesam.client.module.modules.misc.BetterChat;
import com.unclesam.client.module.modules.misc.ScrollableTooltips;
import com.unclesam.client.module.modules.hud.BetterF3;
import com.unclesam.client.module.modules.hud.GuiScale;
import com.unclesam.client.module.modules.render.CustomMcGui;
import com.unclesam.client.module.modules.render.Minimap;
import com.unclesam.client.module.modules.schematic.EasyPlace;
import com.unclesam.client.module.modules.schematic.SchemTransform;
import com.unclesam.client.module.modules.schematic.SelectionTool;
import com.unclesam.client.module.modules.groups.PingBlock;
import com.unclesam.client.module.modules.groups.ChunkPing;
import com.unclesam.client.module.modules.groups.PingAdjust;
import com.unclesam.client.module.modules.hud.QuickDisplay;
import com.unclesam.client.ping.PingManager;
import com.unclesam.client.module.modules.misc.Notifications;
import com.unclesam.client.module.modules.misc.Screenshots;
import com.unclesam.client.module.modules.render.MotionBlur;
import com.unclesam.client.module.modules.render.LeftHand;
import com.unclesam.client.notification.NotificationManager;
import com.unclesam.client.module.modules.misc.NoHurtCamModule;
import com.unclesam.client.module.modules.render.HitBoxes;
import com.unclesam.client.module.modules.render.BlockOverlay;
import com.unclesam.client.module.modules.render.Waypoints;
import com.unclesam.client.module.modules.render.Crosshair;
import com.unclesam.client.module.modules.render.ImprovedHitBoxes;
import com.unclesam.client.module.modules.render.Trails;
import com.unclesam.client.module.modules.render.ArmorSetTags;
import com.unclesam.client.module.modules.render.ItemPhysics;
import com.unclesam.client.module.modules.render.RgbRedstone;
import com.unclesam.client.module.modules.render.RedstonePower;
import com.unclesam.client.module.modules.render.ColorSaturation;
import com.unclesam.client.module.modules.pvp.OldAnimations;
import com.unclesam.client.module.modules.render.SharpnessParticles;
import com.unclesam.client.module.modules.render.FreeLook;
import com.unclesam.client.module.modules.misc.ToggleSprintModule;
import com.unclesam.client.module.modules.pvp.ComboCounterModule;
import com.unclesam.client.module.modules.pvp.TargetHudModule;
import com.unclesam.client.module.modules.schematic.HideSchematicaGui;
import com.unclesam.client.module.modules.schematic.Printer;
import com.unclesam.client.module.modules.visual.FullBrightModule;
import com.unclesam.client.module.modules.visual.LowHpTintModule;
import com.unclesam.client.module.modules.visual.NoFireModule;
import com.unclesam.client.module.modules.visual.NoFogModule;
import com.unclesam.client.module.modules.visual.NoWeatherModule;
import com.unclesam.client.module.modules.visual.TntTimerModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ModuleManager {
   private static final List<Module> MODULES = new ArrayList();

   public ModuleManager() {
   }

   public static void init() {
      tag(register(new KeystrokesModule()), ModuleCategory.HUD, (String)null);
      tag(register(new CpsModule()), ModuleCategory.HUD, (String)null);
      tag(register(new FpsModule()), ModuleCategory.HUD, (String)null);
      tag(register(new CoordinatesModule()), ModuleCategory.HUD, (String)null);
      tag(register(new PotionStatusModule()), ModuleCategory.HUD, (String)null);
      tag(register(new ArmorStatusModule()), ModuleCategory.HUD, (String)null);
      tag(register(new CompassModule()), ModuleCategory.HUD, (String)null);
      tag(register(new ScoreboardModule()), ModuleCategory.HUD, (String)null);
      tag(register(new PotCounterModule()), ModuleCategory.HUD, (String)null);
      tag(register(new TargetHudModule()), ModuleCategory.COMBAT, (String)null);
      tag(register(new ComboCounterModule()), ModuleCategory.COMBAT, (String)null);
      tag(register(new NoHurtCamModule()), ModuleCategory.COMBAT, (String)null);
      tag(register(new ToggleSprintModule()), ModuleCategory.MECHANIC, "Movement");
      tag(register(new FlyBoostModule()), ModuleCategory.MECHANIC, "Movement");
      tag(register(new FullBrightModule()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new NoFogModule()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new NoFireModule()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new NoWeatherModule()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new LowHpTintModule()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new RallyWaypointModule()), ModuleCategory.FACTIONS, (String)null);
      tag(register(new TntTimerModule()), ModuleCategory.FACTIONS, "Faction Alerts");
      tag(register(new PatchCrumbsModule()), ModuleCategory.FACTIONS, "Faction Alerts");
      tag(register(new BreadcrumbsModule()), ModuleCategory.FACTIONS, "Faction Alerts");
      tag(register(new SchematicModule()), ModuleCategory.PRINTER, "Printer");
      tag(register(new Printer()), ModuleCategory.PRINTER, "Printer");
      tag(register(new MissingBlockEspModule()), ModuleCategory.PRINTER, "Printer");
      tag(register(new AutoTickModule()), ModuleCategory.PRINTER, "Printer");
      tag(register(new DirectionHud()), ModuleCategory.HUD, (String)null);
      tag(register(new PingHud()), ModuleCategory.HUD, (String)null);
      tag(register(new ServerIpHud()), ModuleCategory.HUD, (String)null);
      tag(register(new TpsHud()), ModuleCategory.HUD, (String)null);
      tag(register(new EntityCountHud()), ModuleCategory.HUD, (String)null);
      tag(register(new PlaytimeHud()), ModuleCategory.HUD, (String)null);
      tag(register(new ObsidianCounterHud()), ModuleCategory.HUD, (String)null);
      tag(register(new FactionDisplayHud()), ModuleCategory.HUD, (String)null);
      tag(register(new ResourcePackHud()), ModuleCategory.HUD, (String)null);
      tag(register(new SaturationHud()), ModuleCategory.HUD, (String)null);
      tag(register(new ChunkBorders()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new Callouts()), ModuleCategory.FACTIONS, (String)null);
      tag(register(new FloatFinder()), ModuleCategory.FACTIONS, (String)null);
      tag(register(new DispenserCheck()), ModuleCategory.FACTIONS, (String)null);
      tag(register(new ExplosionBox()), ModuleCategory.FACTIONS, (String)null);
      tag(register(new RaidingSounds()), ModuleCategory.FACTIONS, (String)null);
      tag(register(new PingLocation()), ModuleCategory.FACTIONS, "Groups");
      tag(register(new ShareClipboard()), ModuleCategory.FACTIONS, "Groups");
      tag(register(new FocusPlayer()), ModuleCategory.FACTIONS, "Groups");
      tag(register(new HitBoxes()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new BlockOverlay()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new Waypoints()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new Crosshair()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new ItemSearch()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new FovMod()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new ItemLock()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new ImprovedHitBoxes()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new Trails()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new ClearWater()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new FpsParticles()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new SprintDisplay()), ModuleCategory.MECHANIC, "Movement");
      tag(register(new Alerts()), ModuleCategory.COMBAT, (String)null);
      tag(register(new ChatNotify()), ModuleCategory.GENERAL, (String)null);
      tag(register(new TimeChanger()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new CannonView()), ModuleCategory.FACTIONS, (String)null);
      tag(register(new Minecadia()), ModuleCategory.FACTIONS, (String)null);
      tag(register(new HideSchematicaGui()), ModuleCategory.PRINTER, "Printer");
      tag(register(new Macros()), ModuleCategory.GENERAL, (String)null);
      tag(register(new ArmorSetTags()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new ItemPhysics()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new RgbRedstone()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new RedstonePower()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new ColorSaturation()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new OldAnimations()), ModuleCategory.COMBAT, (String)null);
      tag(register(new SharpnessParticles()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new FreeLook()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new LunarClientApi()), ModuleCategory.GENERAL, (String)null);
      tag(register(new Notifications()), ModuleCategory.GENERAL, (String)null);
      tag(register(new BetterChat()), ModuleCategory.GENERAL, (String)null);
      tag(register(new BetterF3()), ModuleCategory.HUD, (String)null);
      tag(register(new GuiScale()), ModuleCategory.HUD, (String)null);
      tag(register(new ScrollableTooltips()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new CustomMcGui()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new Minimap()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new CannonReplay()), ModuleCategory.FACTIONS, (String)null);
      tag(register(new EasyPlace()), ModuleCategory.PRINTER, "Printer");
      tag(register(new SchemTransform()), ModuleCategory.PRINTER, "Printer");
      tag(register(new SelectionTool()), ModuleCategory.PRINTER, "Printer");
      tag(register(new PingBlock()), ModuleCategory.FACTIONS, "Groups");
      tag(register(new ChunkPing()), ModuleCategory.FACTIONS, "Groups");
      tag(register(new PingAdjust()), ModuleCategory.FACTIONS, "Groups");
      tag(register(new QuickDisplay()), ModuleCategory.HUD, (String)null);
      tag(register(new Screenshots()), ModuleCategory.GENERAL, (String)null);
      tag(register(new MotionBlur()), ModuleCategory.MECHANIC, (String)null);
      tag(register(new LeftHand()), ModuleCategory.MECHANIC, (String)null);
      MinecraftForge.EVENT_BUS.register(new ModuleManager());

      // Registered after the modules so the gate lookup in its render handler
      // always resolves.
      NotificationManager.init();

      // Owns its own render hook so placed pings stay visible even if the
      // module that placed them is toggled off afterwards.
      PingManager.init();
   }

   private static void tag(Module m, ModuleCategory category, String group) {
      m.setCategory(category);
      m.setGroupName(group);
   }

   private static Module register(Module module) {
      MODULES.add(module);
      MinecraftForge.EVENT_BUS.register(module);
      return module;
   }

   public static List<Module> getModules() {
      return Collections.unmodifiableList(MODULES);
   }

   public static List<Module> getModulesByCategory(ModuleCategory category) {
      List<Module> result = new ArrayList();

      for(Module m : MODULES) {
         if(m.getCategory() == category) {
            result.add(m);
         }
      }

      return result;
   }

   public static Module getByName(String name) {
      for(Module m : MODULES) {
         if(m.getName().equalsIgnoreCase(name)) {
            return m;
         }
      }

      return null;
   }

   @SubscribeEvent
   public void onKeyInput(KeyInputEvent event) {
      for(Module m : MODULES) {
         if(m.getKeyCode() != 0 && Keyboard.getEventKey() == m.getKeyCode() && Keyboard.getEventKeyState()) {
            m.toggle();
         }
      }

   }

   /**
    * Mouse-bound module toggles. KeyInputEvent only covers the keyboard, so
    * without this a module bound to a mouse button could be set in the GUI but
    * would never actually fire.
    */
   @SubscribeEvent
   public void onMouseInput(MouseInputEvent event) {
      int button = Mouse.getEventButton();
      if(button < 0 || !Mouse.getEventButtonState()) {
         return;
      }

      int code = BindUtil.fromMouseButton(button);
      for(Module m : MODULES) {
         if(m.getKeyCode() != 0 && m.getKeyCode() == code) {
            m.toggle();
         }
      }

   }
}
