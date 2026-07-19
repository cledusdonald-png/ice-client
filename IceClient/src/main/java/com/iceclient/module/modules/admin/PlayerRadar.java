package com.iceclient.module.modules.admin;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.ModuleManager;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;

/**
 * Entity markers on the minimap: player heads, names, and mob dots.
 *
 * <p>Split out of {@link com.iceclient.module.modules.render.Minimap} so it can
 * live in the Admin section. Categories apply to modules rather than to
 * individual settings, so the only way to hide these six was to give them a
 * module of their own -- the minimap reads them back through
 * {@link #get()} and falls back to "draw nothing" when this is off.
 *
 * <p>The map's own arrow stays on the Minimap, since marking where <em>you</em>
 * are gives nothing away.
 */
public class PlayerRadar extends Module {

   private final BooleanSetting players = this.addBool("Player radar", true);
   private final NumberSetting headSize = this.addNumber("Player head size", 1.0D, 0.5D, 2.5D, 0.1D);
   private final BooleanSetting names = this.addBool("Player names", true);
   private final NumberSetting nameSize = this.addNumber("Player name size", 0.5D, 0.25D, 1.0D, 0.05D);
   private final BooleanSetting hostiles = this.addBool("Hostile mobs", false);
   private final BooleanSetting passives = this.addBool("Passive mobs", false);

   public PlayerRadar() {
      super("Player Radar", "Player and mob markers on the minimap", ModuleCategory.ADMIN);
   }

   /** The registered instance, or null before registration. */
   public static PlayerRadar get() {
      Module m = ModuleManager.getByName("Player Radar");
      return m instanceof PlayerRadar ? (PlayerRadar)m : null;
   }

   public boolean showPlayers() {
      return this.isEnabled() && this.players.get();
   }

   public boolean showNames() {
      return this.isEnabled() && this.names.get();
   }

   public boolean showHostiles() {
      return this.isEnabled() && this.hostiles.get();
   }

   public boolean showPassives() {
      return this.isEnabled() && this.passives.get();
   }

   public double headSize() {
      return this.headSize.get();
   }

   public double nameSize() {
      return this.nameSize.get();
   }
}
