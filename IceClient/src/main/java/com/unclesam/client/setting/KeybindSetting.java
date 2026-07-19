package com.unclesam.client.setting;

import com.unclesam.client.setting.Setting;
import com.unclesam.client.util.BindUtil;

public class KeybindSetting extends Setting {
   private int keyCode;

   public KeybindSetting(String name, int defaultKey) {
      super(name);
      this.keyCode = defaultKey;
   }

   public int get() {
      return this.keyCode;
   }

   public void set(int keyCode) {
      this.keyCode = keyCode;
   }

   public int getKeyCode() {
      return this.keyCode;
   }

   public void setKeyCode(int keyCode) {
      this.keyCode = keyCode;
   }

   public String getKeyName() {
      return BindUtil.getName(this.keyCode);
   }

   public boolean isDown() {
      return BindUtil.isDown(this.keyCode);
   }

   public boolean matches(int pressed) {
      return !BindUtil.isNone(this.keyCode) && !BindUtil.isMouseCode(this.keyCode) && pressed == this.keyCode;
   }

   public boolean matchesMouse(int button) {
      return !BindUtil.isNone(this.keyCode) && this.keyCode == BindUtil.fromMouseButton(button);
   }
}
