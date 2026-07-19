package com.unclesam.client.module.modules.misc;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.NumberSetting;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays a sound when your name, or one of your phrases, appears in chat.
 *
 * <p>"Ping own messages" is off by default -- your own message echoes back from
 * the server and contains your name, so leaving it on pings you every time you
 * talk.
 */
public class ChatNotify extends Module {

   private final BooleanSetting notifyUsername = (BooleanSetting)this.addSetting(new BooleanSetting("Notify on username", true));
   private final BooleanSetting pingOwn = (BooleanSetting)this.addSetting(new BooleanSetting("Ping own messages", false));
   private final NumberSetting volume = (NumberSetting)this.addSetting(new NumberSetting("Volume", 1.0D, 0.1D, 2.0D, 0.1D));
   private final NumberSetting pitch = (NumberSetting)this.addSetting(new NumberSetting("Pitch", 1.0D, 0.5D, 2.0D, 0.1D));
   private final NumberSetting cooldown = (NumberSetting)this.addSetting(new NumberSetting("Cooldown (ms)", 500.0D, 0.0D, 5000.0D, 100.0D));

   /** Extra trigger words. Managed at runtime; the GUI list editor is pending. */
   private static final List<String> PHRASES = new ArrayList();

   private long lastPing;

   public ChatNotify() {
      super("Chat Notify", "Plays a sound when you're mentioned in chat", ModuleCategory.GENERAL);
   }

   public static List<String> getPhrases() {
      return PHRASES;
   }

   public static void addPhrase(String p) {
      if(p != null && !p.trim().isEmpty()) {
         PHRASES.add(p.trim().toLowerCase());
      }

   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent event) {
      if(!this.isEnabled() || this.mc.thePlayer == null || event.message == null) {
         return;
      }

      String raw = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
      if(raw == null || raw.isEmpty()) {
         return;
      }

      String msg = raw.toLowerCase();
      String self = this.mc.thePlayer.getName().toLowerCase();

      // The server echoes your own message back with your name in it.
      if(!this.pingOwn.get() && msg.contains("<" + self + ">")) {
         return;
      }

      boolean hit = this.notifyUsername.get() && msg.contains(self);
      if(!hit) {
         for(String phrase : PHRASES) {
            if(msg.contains(phrase)) {
               hit = true;
               break;
            }
         }
      }

      if(!hit) {
         return;
      }

      long now = System.currentTimeMillis();
      if(now - this.lastPing < (long)this.cooldown.get()) {
         return;
      }

      this.lastPing = now;
      this.mc.thePlayer.playSound("random.orb", (float)this.volume.get(), (float)this.pitch.get());
   }
}
