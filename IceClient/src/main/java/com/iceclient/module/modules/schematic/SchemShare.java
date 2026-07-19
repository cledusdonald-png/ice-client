package com.iceclient.module.modules.schematic;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.schematic.Schematic;
import com.iceclient.schematic.SchematicManager;
import com.iceclient.setting.BooleanSetting;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class SchemShare extends Module {
   private final BooleanSetting includeOrigin = this.addBool("Include Origin", true);

   public SchemShare() {
      super("Schem Share", "Share schematic details via clipboard", ModuleCategory.FACTIONS);
   }

   public void shareToClipboard() {
      Schematic s = SchematicManager.getLoaded();
      if(s == null) {
         this.chat(EnumChatFormatting.RED + "No schematic loaded.");
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append(s.getName()).append(" ").append(s.getWidth()).append("x").append(s.getHeight()).append("x").append(s.getLength());
         if(this.includeOrigin.get()) {
            BlockPos o = s.getOrigin();
            sb.append(" @ ").append(o.getX()).append(",").append(o.getY()).append(",").append(o.getZ());
         }

         try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), (ClipboardOwner)null);
            this.chat(EnumChatFormatting.GREEN + "Copied schematic info to clipboard.");
         } catch (Exception var4) {
            this.chat(EnumChatFormatting.RED + "Clipboard unavailable.");
         }

      }
   }

   private void chat(String msg) {
      if(this.mc.thePlayer != null) {
         this.mc.thePlayer.addChatMessage(new ChatComponentText(msg));
      }

   }
}
