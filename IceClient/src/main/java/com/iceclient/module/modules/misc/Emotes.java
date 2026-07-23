package com.iceclient.module.modules.misc;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.KeybindSetting;
import org.lwjgl.input.Keyboard;

/**
 * Where the emote keys live.
 *
 * <p>They were hard-coded to B and G, which is fine until someone has already
 * bound B to something they use constantly. A module is the natural home: it is
 * where every other bind in the client is set, so nobody has to learn a second
 * place to look.
 *
 * <p>Turning the module off unbinds both without clearing what they were set to.
 */
public class Emotes extends Module {

   private final KeybindSetting wheelKey = this.addKeybind("Emote wheel (hold)", Keyboard.KEY_B);
   private final KeybindSetting playKey = this.addKeybind("Play bound emote", Keyboard.KEY_G);

   private static Emotes instance;

   public Emotes() {
      super("Emotes", "Emote wheel and quick-play keys", ModuleCategory.GENERAL);
      instance = this;
      this.setEnabled(true);
   }

   /** 0 when emotes are off, so callers can treat it as "no key bound". */
   public static int wheelKey() {
      return instance == null || !instance.isEnabled() ? 0 : instance.wheelKey.getKeyCode();
   }

   public static int playKey() {
      return instance == null || !instance.isEnabled() ? 0 : instance.playKey.getKeyCode();
   }
}
