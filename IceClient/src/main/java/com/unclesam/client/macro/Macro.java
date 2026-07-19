package com.unclesam.client.macro;

import com.unclesam.client.util.BindUtil;

/**
 * One bound message or command, with an optional cooldown.
 *
 * <p>The cooldown is per-macro and measured from the last successful fire, which
 * is what makes it useful for things like {@code /f rally} that a server will
 * rate-limit or punish you for spamming.
 */
public class Macro {

   private String text;
   private int keyCode;
   private boolean enabled = true;
   /** Seconds between fires; 0 disables the cooldown. */
   private double cooldownSeconds;
   /** Seconds to wait after the key press before sending; 0 sends immediately. */
   private double delaySeconds;

   private transient long lastFired;
   private transient long fireAt;

   public Macro(String text, int keyCode) {
      this.text = text == null ? "" : text;
      this.keyCode = keyCode;
   }

   public String getText() {
      return this.text;
   }

   public void setText(String text) {
      this.text = text == null ? "" : text;
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

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public double getCooldownSeconds() {
      return this.cooldownSeconds;
   }

   public void setCooldownSeconds(double seconds) {
      this.cooldownSeconds = Math.max(0.0D, seconds);
   }

   public double getDelaySeconds() {
      return this.delaySeconds;
   }

   public void setDelaySeconds(double seconds) {
      this.delaySeconds = Math.max(0.0D, seconds);
   }

   /** Milliseconds still to wait, or 0 when ready. */
   public long cooldownRemaining() {
      if(this.cooldownSeconds <= 0.0D) {
         return 0L;
      }

      long elapsed = System.currentTimeMillis() - this.lastFired;
      long total = (long)(this.cooldownSeconds * 1000.0D);
      return elapsed >= total ? 0L : total - elapsed;
   }

   public boolean isReady() {
      return this.enabled && !this.text.isEmpty() && this.cooldownRemaining() == 0L;
   }

   /** True when a delayed send is queued and still pending. */
   public boolean isPending() {
      return this.fireAt != 0L;
   }

   public void schedule() {
      this.fireAt = System.currentTimeMillis() + (long)(this.delaySeconds * 1000.0D);
   }

   public boolean isDue() {
      return this.fireAt != 0L && System.currentTimeMillis() >= this.fireAt;
   }

   public void clearSchedule() {
      this.fireAt = 0L;
   }

   public void markFired() {
      this.lastFired = System.currentTimeMillis();
      this.fireAt = 0L;
   }
}
