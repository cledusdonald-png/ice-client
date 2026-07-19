package com.unclesam.client.util;

import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class BindUtil {
   public static final int NONE = 0;

   private BindUtil() {
   }

   public static int fromMouseButton(int button) {
      return button - 101;
   }

   public static int fromKeyboard(int key) {
      return key;
   }

   public static boolean isMouseCode(int code) {
      return code < 0;
   }

   public static boolean isNone(int code) {
      return code == 0;
   }

   public static boolean isDown(int code) {
      return isNone(code)?false:(isMouseCode(code)?Mouse.isButtonDown(code + 101):Keyboard.isKeyDown(code));
   }

   public static String getName(int code) {
      if(isNone(code)) {
         return "NONE";
      } else if(isMouseCode(code)) {
         return I18n.format("key.mouseButton", new Object[]{Integer.valueOf(code + 101)});
      } else if(code >= 256) {
         return String.valueOf((char)code);
      } else {
         String name = Keyboard.getKeyName(code);
         return name == null?"KEY" + code:name;
      }
   }
}
