package com.iceclient.setting;

import com.iceclient.setting.Setting;

public class BooleanSetting extends Setting {
   private boolean value;

   public BooleanSetting(String name, boolean defaultValue) {
      super(name);
      this.value = defaultValue;
   }

   public boolean get() {
      return this.value;
   }

   public void set(boolean value) {
      this.value = value;
   }

   public void toggle() {
      this.value = !this.value;
   }
}
