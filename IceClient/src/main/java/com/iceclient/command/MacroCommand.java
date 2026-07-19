package com.iceclient.command;

import com.iceclient.macro.Macro;
import com.iceclient.module.modules.misc.Macros;
import com.iceclient.util.BindUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import org.lwjgl.input.Keyboard;

import java.util.List;

/**
 * Manages macros from chat: {@code /macro}.
 *
 * <p>A command rather than only a GUI because it's the fastest way to bind
 * something mid-game, and because it works before the settings page exists.
 */
public class MacroCommand extends CommandBase {

   public String getCommandName() {
      return "macro";
   }

   public String getCommandUsage(ICommandSender sender) {
      return "/macro add <key> <text> | list | remove <n> | cooldown <n> <secs> | delay <n> <secs> | toggle <n>";
   }

   public int getRequiredPermissionLevel() {
      return 0;
   }

   public boolean canCommandSenderUseCommand(ICommandSender sender) {
      return true;
   }

   public void processCommand(ICommandSender sender, String[] args) {
      if(args.length == 0) {
         this.usage(sender);
         return;
      }

      String sub = args[0].toLowerCase();
      List<Macro> macros = Macros.getMacros();

      if("list".equals(sub)) {
         if(macros.isEmpty()) {
            this.msg(sender, EnumChatFormatting.GRAY + "No macros.");
            return;
         }

         for(int i = 0; i < macros.size(); ++i) {
            Macro m = macros.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append(EnumChatFormatting.AQUA).append(i + 1).append(". ")
              .append(EnumChatFormatting.RESET).append(m.getText())
              .append(EnumChatFormatting.GRAY).append("  [").append(m.getKeyName()).append(']');
            if(m.getCooldownSeconds() > 0.0D) {
               sb.append(" cd ").append(m.getCooldownSeconds()).append('s');
            }

            if(m.getDelaySeconds() > 0.0D) {
               sb.append(" delay ").append(m.getDelaySeconds()).append('s');
            }

            if(!m.isEnabled()) {
               sb.append(EnumChatFormatting.RED).append(" (off)");
            }

            this.msg(sender, sb.toString());
         }

         return;
      }

      if("add".equals(sub)) {
         if(args.length < 3) {
            this.msg(sender, EnumChatFormatting.RED + "Usage: /macro add <key> <text>");
            return;
         }

         int key = Keyboard.getKeyIndex(args[1].toUpperCase());
         if(key == Keyboard.KEY_NONE) {
            this.msg(sender, EnumChatFormatting.RED + "Unknown key: " + args[1]);
            return;
         }

         StringBuilder text = new StringBuilder();
         for(int i = 2; i < args.length; ++i) {
            if(text.length() > 0) {
               text.append(' ');
            }

            text.append(args[i]);
         }

         Macros.add(text.toString(), key);
         this.msg(sender, EnumChatFormatting.GREEN + "Added: " + text + EnumChatFormatting.GRAY + " [" + BindUtil.getName(key) + "]");

         // Modules start disabled, so a macro added into a switched-off Macros
         // module silently never fires. Turning it on here is what someone
         // adding a macro obviously means.
         this.ensureEnabled(sender);
         return;
      }

      // Everything below addresses an existing macro by its list number.
      Macro m = this.at(sender, macros, args.length > 1 ? args[1] : null);
      if(m == null) {
         return;
      }

      if("remove".equals(sub)) {
         Macros.remove(m);
         this.msg(sender, EnumChatFormatting.GREEN + "Removed.");
      } else if("toggle".equals(sub)) {
         m.setEnabled(!m.isEnabled());
         this.msg(sender, EnumChatFormatting.GREEN + "Macro " + (m.isEnabled() ? "enabled" : "disabled") + ".");
      } else if("cooldown".equals(sub) || "delay".equals(sub)) {
         if(args.length < 3) {
            this.msg(sender, EnumChatFormatting.RED + "Usage: /macro " + sub + " <n> <seconds>");
            return;
         }

         double secs = parseDuration(args[2]);
         if(secs < 0.0D) {
            this.msg(sender, EnumChatFormatting.RED + "Not a duration: " + args[2]
                  + EnumChatFormatting.GRAY + " (try 500ms, 20s, 2m)");
            return;
         }

         if("cooldown".equals(sub)) {
            m.setCooldownSeconds(secs);
            this.msg(sender, EnumChatFormatting.GREEN + "Cooldown set to " + secs + "s.");
         } else {
            m.setDelaySeconds(secs);
            this.msg(sender, EnumChatFormatting.GREEN + "Delay set to " + secs + "s.");
         }
      } else {
         this.usage(sender);
      }

   }

   private Macro at(ICommandSender sender, List<Macro> macros, String arg) {
      if(arg == null) {
         this.usage(sender);
         return null;
      }

      int idx;
      try {
         idx = Integer.parseInt(arg) - 1;
      } catch (NumberFormatException var6) {
         this.msg(sender, EnumChatFormatting.RED + "Not a number: " + arg);
         return null;
      }

      if(idx < 0 || idx >= macros.size()) {
         this.msg(sender, EnumChatFormatting.RED + "No macro " + arg + ". Use /macro list.");
         return null;
      }

      return macros.get(idx);
   }

   /**
    * Parses a duration into seconds, accepting a unit suffix.
    *
    * <p>{@code 500ms}, {@code 20s}, {@code 2m}, or a bare number (seconds).
    * Returns -1 when it isn't a duration at all, so callers can report it
    * rather than silently treating a typo as zero.
    */
   public static double parseDuration(String raw) {
      if(raw == null) {
         return -1.0D;
      }

      String s = raw.trim().toLowerCase().replace(" ", "");
      if(s.isEmpty()) {
         return -1.0D;
      }

      double mult = 1.0D;
      if(s.endsWith("ms")) {
         mult = 0.001D;
         s = s.substring(0, s.length() - 2);
      } else if(s.endsWith("s")) {
         s = s.substring(0, s.length() - 1);
      } else if(s.endsWith("m")) {
         mult = 60.0D;
         s = s.substring(0, s.length() - 1);
      } else if(s.endsWith("t")) {
         // Ticks, for anything timed against the server's 20/s clock.
         mult = 0.05D;
         s = s.substring(0, s.length() - 1);
      }

      try {
         double v = Double.parseDouble(s);
         return v < 0.0D ? -1.0D : v * mult;
      } catch (NumberFormatException var6) {
         return -1.0D;
      }
   }

   /** Formats seconds back into the most readable unit. */
   public static String formatDuration(double secs) {
      if(secs <= 0.0D) {
         return "-";
      }

      if(secs < 1.0D) {
         return (int)Math.round(secs * 1000.0D) + "ms";
      }

      return secs < 60.0D
            ? (secs == Math.floor(secs) ? (int)secs + "s" : String.format("%.1fs", Double.valueOf(secs)))
            : String.format("%.1fm", Double.valueOf(secs / 60.0D));
   }

   /** Switches the Macros module on, and says so, if it wasn't already. */
   private void ensureEnabled(ICommandSender sender) {
      com.iceclient.module.Module m =
            com.iceclient.module.ModuleManager.getByName("Macros");
      if(m != null && !m.isEnabled()) {
         m.setEnabled(true);
         this.msg(sender, EnumChatFormatting.GRAY + "Enabled the Macros module.");
      }

   }

   private void usage(ICommandSender sender) {
      this.msg(sender, EnumChatFormatting.GRAY + this.getCommandUsage(sender));
   }

   private void msg(ICommandSender sender, String s) {
      sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Ice] " + EnumChatFormatting.RESET + s));
   }

   public static void register() {
      ClientCommandHandler.instance.registerCommand(new MacroCommand());
   }
}
