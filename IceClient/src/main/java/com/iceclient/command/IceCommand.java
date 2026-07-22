package com.iceclient.command;

import com.iceclient.cosmetic.CosmeticApi;
import com.iceclient.cosmetic.CosmeticManager;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

/**
 * {@code /ice} -- points, codes and the wardrobe.
 *
 * <p>A client-side command, so nothing here reaches the Minecraft server; it is
 * only a way to reach the Ice server without opening a GUI, which matters for
 * redeeming a code someone just posted in chat mid-raid.
 */
public class IceCommand extends CommandBase {

   @Override
   public String getCommandName() {
      return "ice";
   }

   @Override
   public String getCommandUsage(ICommandSender sender) {
      return "/ice <balance|redeem|sync|cosmetics>";
   }

   @Override
   public List<String> getCommandAliases() {
      return Arrays.asList("iceclient");
   }

   @Override
   public int getRequiredPermissionLevel() {
      return 0;
   }

   @Override
   public void processCommand(ICommandSender sender, String[] args) {
      if(args.length == 0) {
         help();
         return;
      }

      String sub = args[0].toLowerCase();

      if(sub.equals("balance") || sub.equals("bal") || sub.equals("points")) {
         chat(EnumChatFormatting.AQUA + "Balance: " + EnumChatFormatting.GOLD
               + CosmeticManager.getBalance() + EnumChatFormatting.GRAY + " frost");
         chat(EnumChatFormatting.DARK_GRAY + "(checking with the server…)");
         CosmeticApi.sync(new CosmeticApi.Callback() {
            public void done(boolean ok, String msg) {
               if(ok) {
                  chat(EnumChatFormatting.AQUA + "Balance: " + EnumChatFormatting.GOLD
                        + CosmeticManager.getBalance() + EnumChatFormatting.GRAY + " frost");
               } else {
                  chat(EnumChatFormatting.RED + msg);
               }
            }
         });
         return;
      }

      if(sub.equals("redeem")) {
         if(args.length < 2) {
            chat(EnumChatFormatting.RED + "Usage: /ice redeem <code>");
            return;
         }

         final String code = args[1];
         chat(EnumChatFormatting.GRAY + "Redeeming " + code + "…");
         CosmeticApi.redeem(code, new CosmeticApi.Callback() {
            public void done(boolean ok, String msg) {
               chat((ok ? EnumChatFormatting.GREEN : EnumChatFormatting.RED) + msg);
               if(ok) {
                  chat(EnumChatFormatting.GRAY + "Balance is now "
                        + EnumChatFormatting.GOLD + CosmeticManager.getBalance());
               }
            }
         });
         return;
      }

      if(sub.equals("sync")) {
         CosmeticApi.sync(new CosmeticApi.Callback() {
            public void done(boolean ok, String msg) {
               chat(ok
                     ? EnumChatFormatting.GREEN + "Synced. Balance " + CosmeticManager.getBalance()
                     : EnumChatFormatting.RED + msg);
            }
         });
         return;
      }

      if(sub.equals("cosmetics") || sub.equals("wardrobe") || sub.equals("shop")) {
         // Opened next tick: replacing the screen while the chat GUI is closing
         // leaves the chat box open behind it.
         Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            public void run() {
               Minecraft.getMinecraft().displayGuiScreen(new com.iceclient.gui.CosmeticsScreen());
            }
         });
         return;
      }

      help();
   }

   private void help() {
      chat(EnumChatFormatting.AQUA + "Ice Client");
      chat(EnumChatFormatting.GRAY + "  /ice balance " + EnumChatFormatting.DARK_GRAY + "- your frost");
      chat(EnumChatFormatting.GRAY + "  /ice redeem <code> " + EnumChatFormatting.DARK_GRAY + "- claim a giveaway code");
      chat(EnumChatFormatting.GRAY + "  /ice cosmetics " + EnumChatFormatting.DARK_GRAY + "- open the wardrobe");
      chat(EnumChatFormatting.GRAY + "  /ice sync " + EnumChatFormatting.DARK_GRAY + "- refresh from the server");
   }

   private static void chat(String msg) {
      Minecraft mc = Minecraft.getMinecraft();
      if(mc.thePlayer != null) {
         mc.thePlayer.addChatMessage(new ChatComponentText(msg));
      }
   }

   public static void register() {
      net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new IceCommand());
   }
}
