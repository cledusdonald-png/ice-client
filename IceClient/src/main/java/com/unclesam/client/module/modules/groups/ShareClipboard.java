package com.unclesam.client.module.modules.groups;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.KeybindSetting;
import com.unclesam.client.util.BindUtil;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/** Copies coordinates to the clipboard, and pastes them back as a chat message. */
public class ShareClipboard extends Module {

   private final KeybindSetting copyKey = (KeybindSetting)this.addSetting(new KeybindSetting("Copy key", Keyboard.KEY_INSERT));
   private final KeybindSetting pasteKey = (KeybindSetting)this.addSetting(new KeybindSetting("Paste key", 0));
   private final BooleanSetting copyLookedAt = (BooleanSetting)this.addSetting(new BooleanSetting("Copy looked-at block", true));
   private final BooleanSetting confirmInChat = (BooleanSetting)this.addSetting(new BooleanSetting("Confirm in chat", true));

   private boolean copyWasDown;
   private boolean pasteWasDown;

   public ShareClipboard() {
      super("Share Clipboard", "Copies and pastes coordinates via the clipboard", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled()) {
         return;
      }

      if(this.mc.thePlayer == null || this.mc.currentScreen != null) {
         this.copyWasDown = false;
         this.pasteWasDown = false;
         return;
      }

      boolean copy = BindUtil.isDown(this.copyKey.getKeyCode());
      if(copy && !this.copyWasDown) {
         this.copy();
      }

      this.copyWasDown = copy;

      boolean paste = BindUtil.isDown(this.pasteKey.getKeyCode());
      if(paste && !this.pasteWasDown) {
         this.paste();
      }

      this.pasteWasDown = paste;
   }

   private void copy() {
      BlockPos p = new BlockPos(this.mc.thePlayer);
      if(this.copyLookedAt.get()) {
         MovingObjectPosition hit = this.mc.objectMouseOver;
         if(hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            p = hit.getBlockPos();
         }
      }

      String text = p.getX() + ", " + p.getY() + ", " + p.getZ();
      try {
         Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
         this.info("Copied: " + text);
      } catch (Throwable var4) {
         this.info("Clipboard unavailable");
      }

   }

   private void paste() {
      try {
         Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
         if(t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            String s = String.valueOf(t.getTransferData(DataFlavor.stringFlavor)).trim();
            if(!s.isEmpty()) {
               // Deliberately not auto-sending to chat: pasting unknown clipboard
               // contents straight into public chat is a good way to leak things.
               this.info("Clipboard: " + s);
            }
         }
      } catch (Throwable var3) {
         this.info("Clipboard unavailable");
      }

   }

   private void info(String msg) {
      if(this.confirmInChat.get() && this.mc.thePlayer != null) {
         this.mc.thePlayer.addChatMessage(new ChatComponentText(
               EnumChatFormatting.AQUA + "[Ice] " + EnumChatFormatting.RESET + msg));
      }

   }
}
