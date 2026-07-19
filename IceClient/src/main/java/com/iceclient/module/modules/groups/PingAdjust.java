package com.iceclient.module.modules.groups;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import com.iceclient.ping.Ping;
import com.iceclient.ping.PingManager;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.BindUtil;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Corrects the most recent ping after it has been placed.
 *
 * <p>Pings get placed under pressure and land a block or two off, or expire
 * while the fight is still going. Re-pinging means aiming again from wherever
 * you now are, which is usually somewhere worse -- nudging the existing marker,
 * extending it or clearing it is the recovery path.
 *
 * <p>Operates on {@link PingManager#newest()} so it works on markers placed by
 * either {@link PingBlock} or {@link ChunkPing}.
 */
public class PingAdjust extends Module {

   private final KeybindSetting upKey = this.addKeybind("Nudge up", Keyboard.KEY_PRIOR);
   private final KeybindSetting downKey = this.addKeybind("Nudge down", Keyboard.KEY_NEXT);
   private final KeybindSetting towardKey = this.addKeybind("Nudge toward me", 0);
   private final KeybindSetting extendKey = this.addKeybind("Extend lifetime", 0);
   private final KeybindSetting clearKey = this.addKeybind("Clear newest", Keyboard.KEY_SUBTRACT);
   private final KeybindSetting clearAllKey = this.addKeybind("Clear all", 0);
   private final NumberSetting step = this.addNumber("Nudge step", 1.0D, 1.0D, 8.0D, 1.0D);
   private final NumberSetting extendBy = this.addNumber("Extend by (s)", 30.0D, 5.0D, 300.0D, 5.0D);

   private boolean upWas;
   private boolean downWas;
   private boolean towardWas;
   private boolean extendWas;
   private boolean clearWas;
   private boolean clearAllWas;

   public PingAdjust() {
      super("PingAdjust", "Nudge, extend or clear the most recent ping", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled()) {
         return;
      }

      if(this.mc.thePlayer == null || this.mc.currentScreen != null) {
         this.upWas = false;
         this.downWas = false;
         this.towardWas = false;
         this.extendWas = false;
         this.clearWas = false;
         this.clearAllWas = false;
         return;
      }

      if(this.pressed(this.upKey, this.upWas)) {
         this.nudge(0, (int)this.step.get(), 0);
      }

      this.upWas = this.down(this.upKey);

      if(this.pressed(this.downKey, this.downWas)) {
         this.nudge(0, -(int)this.step.get(), 0);
      }

      this.downWas = this.down(this.downKey);

      if(this.pressed(this.towardKey, this.towardWas)) {
         this.nudgeToward();
      }

      this.towardWas = this.down(this.towardKey);

      if(this.pressed(this.extendKey, this.extendWas)) {
         this.extend();
      }

      this.extendWas = this.down(this.extendKey);

      if(this.pressed(this.clearKey, this.clearWas)) {
         this.clearNewest();
      }

      this.clearWas = this.down(this.clearKey);

      if(this.pressed(this.clearAllKey, this.clearAllWas)) {
         PingManager.clear();
         NotificationManager.post("Pings", "All cleared", Notification.Type.INFO);
      }

      this.clearAllWas = this.down(this.clearAllKey);
   }

   private boolean down(KeybindSetting k) {
      return k.getKeyCode() != 0 && BindUtil.isDown(k.getKeyCode());
   }

   private boolean pressed(KeybindSetting k, boolean was) {
      return this.down(k) && !was;
   }

   private Ping require() {
      Ping p = PingManager.newest();
      if(p == null) {
         NotificationManager.post("PingAdjust", "No pings placed", Notification.Type.WARNING);
      }

      return p;
   }

   private void nudge(int dx, int dy, int dz) {
      Ping p = this.require();
      if(p == null) {
         return;
      }

      p.setPos(p.getPos().add(dx, dy, dz));
      BlockPos np = p.getPos();
      NotificationManager.post("Ping moved",
            np.getX() + ", " + np.getY() + ", " + np.getZ(), Notification.Type.INFO);
   }

   /**
    * Slides the marker one step along the horizontal line from it to you.
    *
    * <p>Horizontal only: pulling a marker toward you in 3D would drag it off
    * the wall it is marking as soon as your Y differed, which is most of the
    * time when you are flying.
    */
   private void nudgeToward() {
      Ping p = this.require();
      if(p == null) {
         return;
      }

      BlockPos pos = p.getPos();
      double dx = this.mc.thePlayer.posX - ((double)pos.getX() + 0.5D);
      double dz = this.mc.thePlayer.posZ - ((double)pos.getZ() + 0.5D);
      double len = Math.sqrt(dx * dx + dz * dz);

      if(len < 1.0D) {
         return;
      }

      int s = (int)this.step.get();
      int mx = (int)Math.round(dx / len * (double)s);
      int mz = (int)Math.round(dz / len * (double)s);
      this.nudge(mx, 0, mz);
   }

   private void extend() {
      Ping p = this.require();
      if(p == null) {
         return;
      }

      if(p.getLifetimeMs() <= 0L) {
         NotificationManager.post("PingAdjust", "That ping never expires", Notification.Type.INFO);
         return;
      }

      p.extend((long)(this.extendBy.get() * 1000.0D));
      NotificationManager.post("Ping extended",
            "+" + (int)this.extendBy.get() + "s", Notification.Type.SUCCESS);
   }

   private void clearNewest() {
      if(PingManager.removeNewest()) {
         NotificationManager.post("Ping cleared", PingManager.count() + " left", Notification.Type.INFO);
      } else {
         NotificationManager.post("PingAdjust", "No pings placed", Notification.Type.WARNING);
      }

   }
}
