package com.iceclient.module.modules.factions;

import com.iceclient.module.modules.factions.SchematicModule;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class SchemCommand extends CommandBase {
   public SchemCommand() {
   }

   public String getCommandName() {
      return "schem";
   }

   public String getCommandUsage(ICommandSender sender) {
      return ".schem load <name> | setpos | next | switch <n> | list | folder | test | share | importclipboard";
   }

   public void processCommand(ICommandSender sender, String[] args) {
      SchematicModule module = SchematicModule.getInstance();
      if(module != null && args.length != 0) {
         String var4 = args[0].toLowerCase();
         byte var5 = -1;
         switch(var4.hashCode()) {
         case -1268966290:
            if(var4.equals("folder")) {
               var5 = 6;
            }
            break;
         case -905772366:
            if(var4.equals("setpos")) {
               var5 = 2;
            }
            break;
         case -889473228:
            if(var4.equals("switch")) {
               var5 = 4;
            }
            break;
         case 3322014:
            if(var4.equals("list")) {
               var5 = 5;
            }
            break;
         case 3327206:
            if(var4.equals("load")) {
               var5 = 1;
            }
            break;
         case 3377907:
            if(var4.equals("next")) {
               var5 = 3;
            }
            break;
         case 3556498:
            if(var4.equals("test")) {
               var5 = 0;
            }
            break;
         case 109400031:
            if(var4.equals("share")) {
               var5 = 7;
            }
            break;
         case 1434563473:
            if(var4.equals("importclipboard")) {
               var5 = 8;
            }
         }

         switch(var5) {
         case 0:
            module.createTestSchematic();
            break;
         case 1:
            if(args.length < 2) {
               return;
            }

            module.loadSchematic(args[1]);
            break;
         case 2:
            module.setOriginToPlayer();
            break;
         case 3:
            module.cycle();
            break;
         case 4:
            if(args.length < 2) {
               return;
            }

            try {
               module.switchTo(Integer.parseInt(args[1]));
            } catch (NumberFormatException var7) {
               ;
            }
            break;
         case 5:
            module.listLoaded();
            break;
         case 6:
            module.openFolder();
            break;
         case 7:
            module.shareToClipboard();
            break;
         case 8:
            module.importFromClipboard();
         }

      }
   }

   public int getRequiredPermissionLevel() {
      return 0;
   }
}
