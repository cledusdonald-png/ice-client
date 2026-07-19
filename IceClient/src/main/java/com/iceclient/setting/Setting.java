package com.iceclient.setting;

import com.google.gson.JsonElement;
import java.util.function.BooleanSupplier;

public abstract class Setting {
   private final String name;
   private BooleanSupplier visibleWhen = () -> {
      return true;
   };
   private String section;

   protected Setting(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public Setting inSection(String section) {
      this.section = section;
      return this;
   }

   public String getSection() {
      return this.section;
   }

   public Setting visibleWhen(BooleanSupplier condition) {
      this.visibleWhen = condition;
      return this;
   }

   public boolean isVisible() {
      return this.visibleWhen.getAsBoolean();
   }

   public JsonElement save() {
      return null;
   }

   public void load(JsonElement element) {
   }
}
