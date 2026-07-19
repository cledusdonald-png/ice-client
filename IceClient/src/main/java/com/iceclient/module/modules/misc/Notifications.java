package com.iceclient.module.modules.misc;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;

/**
 * Gate and settings for the toast queue.
 *
 * <p>Deliberately thin: {@link NotificationManager} owns the queue and the
 * drawing, because toasts are posted from all over the client and routing every
 * one of those through a module instance would mean a lookup and a null check at
 * each call site.
 */
public class Notifications extends Module {

   private final BooleanSetting onToggle =
         this.addBool("Module toggles", true);
   private final NumberSetting duration =
         this.addNumber("Duration (s)", 3.0D, 1.0D, 10.0D, 0.5D);

   public Notifications() {
      super("Notifications", "Corner toasts for client events", ModuleCategory.GENERAL);
      this.setEnabled(true);
   }

   public boolean showsModuleToggles() {
      return this.onToggle.get();
   }

   public long durationMs() {
      return (long)(this.duration.get() * 1000.0D);
   }

   protected void onEnable() {
      NotificationManager.post("Notifications", "Toasts enabled", Notification.Type.SUCCESS, this.durationMs());
   }

   protected void onDisable() {
      // Nothing left on screen should outlive the switch being flipped off.
      NotificationManager.clear();
   }
}
