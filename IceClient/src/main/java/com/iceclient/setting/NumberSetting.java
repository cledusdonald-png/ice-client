package com.iceclient.setting;

import com.iceclient.setting.Setting;

public class NumberSetting extends Setting {
   private final double min;
   private final double max;
   private final double increment;
   private double value;

   public NumberSetting(String name, double defaultValue, double min, double max, double increment) {
      super(name);
      this.min = min;
      this.max = max;
      this.increment = increment;
      this.value = this.clamp(defaultValue);
   }

   public double get() {
      return this.value;
   }

   public int getInt() {
      return (int)Math.round(this.value);
   }

   public void set(double raw) {
      this.value = this.clamp(this.snap(raw));
   }

   public void setFromFraction(double fraction) {
      this.set(this.min + (this.max - this.min) * Math.max(0.0D, Math.min(1.0D, fraction)));
   }

   public double getFraction() {
      return this.max == this.min?0.0D:(this.value - this.min) / (this.max - this.min);
   }

   public double getMin() {
      return this.min;
   }

   public double getMax() {
      return this.max;
   }

   private double snap(double raw) {
      return this.increment <= 0.0D?raw:(double)Math.round(raw / this.increment) * this.increment;
   }

   private double clamp(double v) {
      return v < this.min?this.min:(v > this.max?this.max:v);
   }
}
