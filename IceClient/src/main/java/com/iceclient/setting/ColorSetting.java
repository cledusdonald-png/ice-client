package com.iceclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iceclient.setting.Setting;
import com.iceclient.util.ColorUtil;

public class ColorSetting extends Setting {
   private int rgb;
   private boolean chroma;

   public ColorSetting(String name, int defaultRgb) {
      super(name);
      this.rgb = defaultRgb;
   }

   public int getRGB() {
      return this.chroma?ColorUtil.withAlpha(ColorUtil.chroma(0), this.rgb >> 24 & 255):this.rgb;
   }

   public int getStored() {
      return this.rgb;
   }

   public void set(int rgb) {
      this.rgb = rgb;
   }

   public boolean isChroma() {
      return this.chroma;
   }

   public void setChroma(boolean chroma) {
      this.chroma = chroma;
   }

   public JsonElement save() {
      JsonObject o = new JsonObject();
      o.addProperty("rgb", Integer.valueOf(this.rgb));
      o.addProperty("chroma", Boolean.valueOf(this.chroma));
      return o;
   }

   public void load(JsonElement element) {
      if(element != null && element.isJsonObject()) {
         JsonObject o = element.getAsJsonObject();
         if(o.has("rgb")) {
            this.rgb = o.get("rgb").getAsInt();
         }

         if(o.has("chroma")) {
            this.chroma = o.get("chroma").getAsBoolean();
         }
      }

   }
}
