package com.unclesam.client.module.modules.misc;

import com.unclesam.client.macro.Macro;
import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.BindUtil;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds keys to chat messages and commands, with a per-macro cooldown and an
 * optional send delay.
 *
 * <p>Both exist because they solve different problems: a <b>cooldown</b> stops
 * you re-firing something the server rate-limits (spamming {@code /f rally} gets
 * you muted), while a <b>delay</b> waits before sending, which is what you want
 * when a command has to land a moment after something else.
 */
public class Macros extends Module {

   private static final List<Macro> MACROS = new ArrayList();

   private final NumberSetting defaultCooldown = (NumberSetting)this.addSetting(new NumberSetting("Default cooldown (s)", 0.0D, 0.0D, 60.0D, 1.0D));
   private final BooleanSetting blockWhileTyping = (BooleanSetting)this.addSetting(new BooleanSetting("Ignore while typing", true));
   private final BooleanSetting notifyOnCooldown = (BooleanSetting)this.addSetting(new BooleanSetting("Notify when on cooldown", true));
   private final BooleanSetting echoSent = (BooleanSetting)this.addSetting(new BooleanSetting("Echo sent macro", false));

   private final List<Integer> wasDown = new ArrayList();

   public Macros() {
      super("Macros", "Bind keybinds to chat commands and messages", ModuleCategory.GENERAL);
   }

   public static List<Macro> getMacros() {
      return MACROS;
   }

   public static Macro add(String text, int keyCode) {
      Macro m = new Macro(text, keyCode);
      MACROS.add(m);
      return m;
   }

   public static void remove(Macro m) {
      MACROS.remove(m);
   }

   /** Applies the module default to any macro that hasn't set its own. */
   public double defaultCooldownSeconds() {
      return this.defaultCooldown.get();
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled() || this.mc.thePlayer == null) {
         return;
      }

      // Fire anything whose delay has elapsed, even if the GUI is now open --
      // the press already happened.
      for(Macro m : new ArrayList<Macro>(MACROS)) {
         if(m.isDue()) {
            this.send(m);
         }
      }

      if(this.blockWhileTyping.get() && this.mc.currentScreen != null) {
         this.wasDown.clear();
         return;
      }

      for(Macro m : new ArrayList<Macro>(MACROS)) {
         if(!m.isEnabled() || m.getKeyCode() == 0 || m.getText().isEmpty()) {
            continue;
         }

         boolean down = BindUtil.isDown(m.getKeyCode());
         boolean held = this.wasDown.contains(Integer.valueOf(m.getKeyCode()));

         if(down && !held) {
            this.trigger(m);
         }

         if(down && !held) {
            this.wasDown.add(Integer.valueOf(m.getKeyCode()));
         } else if(!down) {
            this.wasDown.remove(Integer.valueOf(m.getKeyCode()));
         }
      }

   }

   private void trigger(Macro m) {
      long remaining = this.effectiveCooldownRemaining(m);
      if(remaining > 0L) {
         if(this.notifyOnCooldown.get()) {
            this.message(EnumChatFormatting.RED + "On cooldown: "
                  + String.format("%.1f", Double.valueOf((double)remaining / 1000.0D)) + "s");
         }

         return;
      }

      if(m.getDelaySeconds() > 0.0D) {
         if(!m.isPending()) {
            m.schedule();
         }
      } else {
         this.send(m);
      }

   }

   /** Macro cooldown, falling back to the module-wide default when unset. */
   private long effectiveCooldownRemaining(Macro m) {
      if(m.getCooldownSeconds() > 0.0D) {
         return m.cooldownRemaining();
      }

      double def = this.defaultCooldown.get();
      if(def <= 0.0D) {
         return 0L;
      }

      m.setCooldownSeconds(def);
      long r = m.cooldownRemaining();
      m.setCooldownSeconds(0.0D);
      return r;
   }

   private void send(Macro m) {
      if(this.mc.thePlayer == null) {
         m.clearSchedule();
         return;
      }

      String text = m.getText().trim();
      if(text.isEmpty()) {
         m.clearSchedule();
         return;
      }

      this.mc.thePlayer.sendChatMessage(text);
      m.markFired();

      if(this.echoSent.get()) {
         this.message(EnumChatFormatting.GRAY + "Sent: " + text);
      }

   }

   private void message(String msg) {
      if(this.mc.thePlayer != null) {
         this.mc.thePlayer.addChatMessage(new ChatComponentText(
               EnumChatFormatting.AQUA + "[Ice] " + EnumChatFormatting.RESET + msg));
      }

   }
}
