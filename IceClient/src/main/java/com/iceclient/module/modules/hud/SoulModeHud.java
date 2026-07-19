package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ModeSetting;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows whether Soul Mode is currently on, read from the server's own chat
 * announcements.
 *
 * <p>Soul Mode is server state with no packet a client can query, so the only
 * honest source is the message the server prints when it changes:
 * {@code Enchants » You have enabled Soul Mode.} That means the state is
 * genuinely unknown until the first toggle of a session -- shown as {@code ?}
 * rather than guessed at, because displaying a confident "OFF" that happens to
 * be wrong is worse than admitting it does not know yet.
 */
public class SoulModeHud extends TextHudModule {

   /**
    * Deliberately loose: it keys on "enabled/disabled" plus "soul mode" and
    * ignores everything around them, so a change to the prefix, the colours or
    * the trailing sentence does not silently stop the readout working.
    */
   private static final Pattern TOGGLE = Pattern.compile(
         "you have (enabled|disabled) soul mode", Pattern.CASE_INSENSITIVE);

   private final ModeSetting label = this.addMode("Label", "Souls", "Souls", "Soul Mode", "None");
   private final BooleanSetting hideWhenOff = this.addBool("Hide when off", false);
   private final BooleanSetting hideUntilKnown = this.addBool("Hide until known", true);
   private final BooleanSetting colored = this.addBool("Colour the state", true);

   /** null until the server tells us, then TRUE/FALSE. */
   private Boolean active;

   public SoulModeHud() {
      super("Soul Mode", "Shows whether Soul Mode is enabled",
            ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 100);
   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent event) {
      if(!this.isEnabled() || event.message == null) {
         return;
      }

      String msg = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
      if(msg == null) {
         return;
      }

      Matcher m = TOGGLE.matcher(msg);
      if(m.find()) {
         this.active = Boolean.valueOf("enabled".equalsIgnoreCase(m.group(1)));
      }

   }

   /**
    * Forget the state on disconnect. Carrying it across servers would show a
    * stale "ON" on a server that has no such feature.
    */
   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase == TickEvent.Phase.END && this.mc.theWorld == null) {
         this.active = null;
      }

   }

   protected List<String> lines() {
      if(this.active == null && this.hideUntilKnown.get()) {
         return Collections.emptyList();
      }

      boolean on = this.active != null && this.active.booleanValue();
      if(!on && this.active != null && this.hideWhenOff.get()) {
         return Collections.emptyList();
      }

      String state;
      if(this.active == null) {
         state = this.colored.get() ? EnumChatFormatting.GRAY + "?" : "?";
      } else if(on) {
         state = this.colored.get() ? EnumChatFormatting.GREEN + "ON" : "ON";
      } else {
         state = this.colored.get() ? EnumChatFormatting.RED + "OFF" : "OFF";
      }

      String prefix = this.label.is("None") ? "" : this.label.get() + ": ";
      return Collections.singletonList(prefix + state);
   }
}
