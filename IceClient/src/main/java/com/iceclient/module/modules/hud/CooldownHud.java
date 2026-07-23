package com.iceclient.module.modules.hud;

import com.iceclient.lunar.LunarApi;
import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.RenderUtil;
import net.minecraft.client.gui.Gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import java.util.List;


/**
 * Server-sent cooldowns: pet summons, ability timers, kit re-use.
 *
 * <p>The values come from the server over Apollo rather than being worked out
 * here, so this shows exactly what a Lunar user sees and nothing the server has
 * not chosen to send. On a server that does not use Apollo the list is simply
 * empty, which is why the module draws nothing at all rather than an empty box.
 *
 * <p>Sorted by time remaining, so whatever is about to come off sits at the top
 * -- in a fight that is the only line worth looking at.
 */
public class CooldownHud extends HudModule {

   private static final int LINE_H = 13;

   private final BooleanSetting showBar = (BooleanSetting)this.addSetting(
         new BooleanSetting("Progress bar", true));
   private final BooleanSetting showSeconds = (BooleanSetting)this.addSetting(
         new BooleanSetting("Seconds", true));
   private final NumberSetting maxShown = (NumberSetting)this.addSetting(
         new NumberSetting("Max shown", 5.0D, 1.0D, 10.0D, 1.0D));

   public CooldownHud() {
      super("Cooldowns", "Ability and pet cooldowns sent by the server",
            ModuleCategory.HUD, 0, HudModule.Anchor.TOP_LEFT);
   }

   public int getWidth() {
      return 104;
   }

   public int getHeight() {
      int n = Math.min((int)this.maxShown.get(), LunarApi.cooldowns().size());
      return Math.max(1, n) * LINE_H + 4;
   }

   public void render(int x, int y) {
      List<LunarApi.Cooldown> live = LunarApi.cooldowns();

      if(live.isEmpty()) {
         // An element with nothing in it cannot be picked up in the HUD editor,
         // so it shows a placeholder there and stays invisible in play.
         if(this.mc.currentScreen instanceof com.iceclient.gui.HudEditorScreen) {
            this.drawStyled("Cooldowns (none)", x + 2, y + 2);
         }

         return;
      }

      if(this.hasBackground()) {
         RenderUtil.panel(x, y, x + this.getWidth(), y + this.getHeight(),
               this.backgroundColor(), -14013902);
      }

      // Soonest first: in a fight the one about to come off is the only line
      // actually being watched.
      List<LunarApi.Cooldown> sorted = new ArrayList<LunarApi.Cooldown>(live);
      Collections.sort(sorted, new Comparator<LunarApi.Cooldown>() {
         public int compare(LunarApi.Cooldown a, LunarApi.Cooldown b) {
            return Long.compare(a.expiresAt, b.expiresAt);
         }
      });

      int limit = (int)this.maxShown.get();
      int row = 0;

      for(LunarApi.Cooldown c : sorted) {
         if(row >= limit) {
            break;
         }

         long left = c.remainingMs();
         int ry = y + 2 + row * LINE_H;

         this.drawStyled(this.showSeconds.get() ? c.message + "  " + format(left) : c.message,
               x + 2, ry);

         if(this.showBar.get()) {
            float frac = c.totalMs <= 0L
                  ? 0.0F
                  : Math.max(0.0F, Math.min(1.0F, (float)left / (float)c.totalMs));

            Gui.drawRect(x + 2, ry + 9, x + 100, ry + 11, 1610612736);
            Gui.drawRect(x + 2, ry + 9, x + 2 + (int)(98.0F * frac), ry + 11, this.getColor());
         }

         ++row;
      }
   }

   /** Tenths below ten seconds, whole seconds above -- precision where it matters. */
   private static String format(long ms) {
      return ms >= 10000L
            ? (ms / 1000L) + "s"
            : String.format("%.1fs", Double.valueOf(ms / 1000.0D));
   }
}
