package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.module.TextHudModule;
import com.unclesam.client.ping.PingManager;
import com.unclesam.client.setting.BooleanSetting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * One compact panel of the stats you would otherwise enable five HUD elements
 * to see.
 *
 * <p>Each of coordinates, ping, FPS and direction already exists as its own
 * element. Turning them all on gives four separately positioned boxes that have
 * to be aligned by hand and re-aligned whenever one changes width. This is the
 * single-panel alternative -- every row optional, one thing to place.
 */
public class QuickDisplay extends TextHudModule {

   private final BooleanSetting showCoords = this.addBool("Coordinates", true);
   private final BooleanSetting showDirection = this.addBool("Direction", true);
   private final BooleanSetting showPing = this.addBool("Ping", true);
   private final BooleanSetting showFps = this.addBool("FPS", true);
   private final BooleanSetting showSpeed = this.addBool("Speed", false);
   private final BooleanSetting showPings = this.addBool("Active pings", false);
   private final BooleanSetting compact = this.addBool("Compact labels", false);

   public QuickDisplay() {
      super("QuickDisplay", "One panel with the stats you check most",
            ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 120);
   }

   protected List<String> lines() {
      List<String> out = new ArrayList<String>(6);
      if(this.mc.thePlayer == null) {
         return out;
      }

      boolean c = this.compact.get();

      if(this.showCoords.get()) {
         int x = MathHelper.floor_double(this.mc.thePlayer.posX);
         int y = MathHelper.floor_double(this.mc.thePlayer.posY);
         int z = MathHelper.floor_double(this.mc.thePlayer.posZ);
         out.add((c ? "" : "XYZ: ") + x + " " + y + " " + z);
      }

      if(this.showDirection.get()) {
         out.add((c ? "" : "Facing: ") + this.facing());
      }

      if(this.showPing.get()) {
         int ping = this.ping();
         if(ping >= 0) {
            out.add((c ? "" : "Ping: ") + ping + "ms");
         }
      }

      if(this.showFps.get()) {
         out.add((c ? "" : "FPS: ") + net.minecraft.client.Minecraft.getDebugFPS());
      }

      if(this.showSpeed.get()) {
         out.add((c ? "" : "Speed: ") + String.format("%.1f", Double.valueOf(this.speed())) + " b/s");
      }

      if(this.showPings.get() && PingManager.count() > 0) {
         out.add((c ? "" : "Pings: ") + PingManager.count());
      }

      return out;
   }

   /** Horizontal blocks per second, from this tick's movement delta. */
   private double speed() {
      double dx = this.mc.thePlayer.posX - this.mc.thePlayer.prevPosX;
      double dz = this.mc.thePlayer.posZ - this.mc.thePlayer.prevPosZ;
      return Math.sqrt(dx * dx + dz * dz) * 20.0D;
   }

   private String facing() {
      float yaw = this.mc.thePlayer.rotationYaw % 360.0F;
      if(yaw < 0.0F) {
         yaw += 360.0F;
      }

      // Offset by half a sector so each name covers 45 degrees centred on its
      // own direction rather than starting at it.
      int idx = (int)((double)(yaw + 22.5F) / 45.0D) & 7;
      return new String[]{"S", "SW", "W", "NW", "N", "NE", "E", "SE"}[idx];
   }

   /** -1 when not connected or the tab entry is not populated yet. */
   private int ping() {
      if(this.mc.thePlayer == null || this.mc.getNetHandler() == null) {
         return -1;
      }

      NetworkPlayerInfo info = this.mc.getNetHandler().getPlayerInfo(this.mc.thePlayer.getUniqueID());
      return info == null ? -1 : Math.max(0, info.getResponseTime());
   }
}
