package com.iceclient.setting;

import com.iceclient.setting.Setting;
import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting {
   private final List<String> options;
   private int index;

   public ModeSetting(String name, String defaultValue, String... options) {
      super(name);
      this.options = Arrays.asList(options);
      int i = this.options.indexOf(defaultValue);
      this.index = i < 0?0:i;
   }

   public String get() {
      return (String)this.options.get(this.index);
   }

   public boolean is(String value) {
      return this.get().equalsIgnoreCase(value);
   }

   public void cycle() {
      this.index = (this.index + 1) % this.options.size();
   }

   public void setByName(String value) {
      int i = this.options.indexOf(value);
      if(i >= 0) {
         this.index = i;
      }

   }

   public List<String> getOptions() {
      return this.options;
   }
}
