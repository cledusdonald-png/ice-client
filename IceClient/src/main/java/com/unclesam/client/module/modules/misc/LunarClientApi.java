package com.unclesam.client.module.modules.misc;

import com.unclesam.client.lunar.LunarApi;
import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ColorSetting;
import com.unclesam.client.util.WorldRenderUtil;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Surfaces the cooldowns, waypoints and teammate positions a server pushes over
 * the Lunar / Apollo plugin channel.
 */
public class LunarClientApi extends HudModule {

   private final BooleanSetting cooldowns = (BooleanSetting)this.addSetting(new BooleanSetting("Cooldowns", true));
   private final BooleanSetting cooldownHud = (BooleanSetting)this.addSetting(new BooleanSetting("Cooldown HUD", true));
   private final BooleanSetting horizontal = (BooleanSetting)this.addSetting(new BooleanSetting("Horizontal mode", false));
   private final ColorSetting cooldownColor = (ColorSetting)this.addSetting(new ColorSetting("Cooldown color", -1));

   private final BooleanSetting waypoints = (BooleanSetting)this.addSetting(new BooleanSetting("Waypoints", true));
   private final ColorSetting rallyColor = (ColorSetting)this.addSetting(new ColorSetting("F rally waypoint color", -65536));

   private final BooleanSetting team = (BooleanSetting)this.addSetting(new BooleanSetting("Team", true));
   private final BooleanSetting textShadow = (BooleanSetting)this.addSetting(new BooleanSetting("Text shadow", true));
   private final BooleanSetting debugPackets = (BooleanSetting)this.addSetting(new BooleanSetting("Debug packets", false));

   private static final int PAD = 4;
   private static final int LINE_H = 12;

   public LunarClientApi() {
      super("LunarClientApi", "Supports Lunar Client and Apollo plugin-message features",
            ModuleCategory.GENERAL, 0, HudModule.Anchor.TOP_LEFT, 40);
   }

   protected void onEnable() {
      LunarApi.init();
      LunarApi.setDebug(this.debugPackets.get());
   }

   protected void onDisable() {
      LunarApi.clear();
      LunarApi.setDebug(false);
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase == TickEvent.Phase.END) {
         LunarApi.setDebug(this.isEnabled() && this.debugPackets.get());
         // Leaving a server invalidates everything the old one told us.
         if(this.mc.theWorld == null) {
            LunarApi.clear();
         }
      }
   }

   // ---------------- HUD ----------------

   private List<LunarApi.Cooldown> visible() {
      return LunarApi.cooldowns();
   }

   private String lineFor(LunarApi.Cooldown c) {
      return c.message + " " + format(c.remainingMs());
   }

   public int getWidth() {
      if(!this.cooldowns.get() || !this.cooldownHud.get()) {
         return 0;
      }
      List<LunarApi.Cooldown> active = this.visible();
      if(active.isEmpty()) {
         return 0;
      }
      int w = 0;
      if(this.horizontal.get()) {
         StringBuilder sb = new StringBuilder();
         for(LunarApi.Cooldown c : active) {
            if(sb.length() > 0) sb.append("  ");
            sb.append(this.lineFor(c));
         }
         w = this.mc.fontRendererObj.getStringWidth(sb.toString());
      } else {
         for(LunarApi.Cooldown c : active) {
            w = Math.max(w, this.mc.fontRendererObj.getStringWidth(this.lineFor(c)));
         }
      }
      return w + PAD * 2;
   }

   public int getHeight() {
      if(!this.cooldowns.get() || !this.cooldownHud.get()) {
         return 0;
      }
      List<LunarApi.Cooldown> active = this.visible();
      if(active.isEmpty()) {
         return 0;
      }
      return this.horizontal.get() ? LINE_H + PAD : active.size() * LINE_H + PAD;
   }

   public void render(int x, int y) {
      if(!this.cooldowns.get() || !this.cooldownHud.get()) {
         return;
      }
      List<LunarApi.Cooldown> active = this.visible();
      if(active.isEmpty()) {
         return;
      }

      int col = this.cooldownColor.getRGB();
      if(this.horizontal.get()) {
         StringBuilder sb = new StringBuilder();
         for(LunarApi.Cooldown c : active) {
            if(sb.length() > 0) sb.append("  ");
            sb.append(this.lineFor(c));
         }
         this.drawText(sb.toString(), x + PAD, y + PAD, col);
      } else {
         int ly = y + PAD;
         for(LunarApi.Cooldown c : active) {
            this.drawText(this.lineFor(c), x + PAD, ly, col);
            ly += LINE_H;
         }
      }
   }

   private void drawText(String s, int x, int y, int col) {
      if(this.textShadow.get()) {
         this.mc.fontRendererObj.drawStringWithShadow(s, (float)x, (float)y, col);
      } else {
         this.mc.fontRendererObj.drawString(s, x, y, col);
      }
   }

   private static String format(long ms) {
      double s = (double)ms / 1000.0D;
      return s >= 10.0D ? (int)Math.ceil(s) + "s" : String.format("%.1fs", Double.valueOf(s));
   }

   // ---------------- world ----------------

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.mc.theWorld == null) {
         return;
      }

      if(this.waypoints.get()) {
         for(LunarApi.Waypoint w : LunarApi.waypoints()) {
            if(!w.visible) continue;
            // Rally points are the ones a server marks forced; they get their
            // own colour so a callout stands out from ordinary waypoints.
            int c = w.forced ? this.rallyColor.getRGB() : (-16777216 | w.color);
            AxisAlignedBB box = new AxisAlignedBB((double)w.x, (double)w.y, (double)w.z,
                  (double)w.x + 1.0D, (double)w.y + 1.0D, (double)w.z + 1.0D);
            WorldRenderUtil.outlineBox(box, c, 2.0F);
            WorldRenderUtil.text3d(w.name, (double)w.x + 0.5D, (double)w.y + 1.4D, (double)w.z + 0.5D, c, 0.03F);
         }
      }

      if(this.team.get()) {
         for(Map.Entry<UUID, double[]> e : LunarApi.teammates().entrySet()) {
            double[] p = e.getValue();
            AxisAlignedBB box = new AxisAlignedBB(
                  p[0] - 0.4D, p[1], p[2] - 0.4D,
                  p[0] + 0.4D, p[1] + 1.8D, p[2] + 0.4D);
            WorldRenderUtil.outlineBox(box, -12517632, 1.5F);
         }
      }
   }
}
