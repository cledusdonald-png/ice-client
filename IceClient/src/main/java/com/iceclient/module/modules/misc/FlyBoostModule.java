package com.iceclient.module.modules.misc;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.BindUtil;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

/**
 * Faster creative-style flight, on servers that allow fast fly.
 *
 * <p>Only touches {@code capabilities.setFlySpeed} and the player's own motion,
 * so it does nothing where the server hasn't granted flight -- it cannot turn
 * flight on, only make granted flight faster.
 */
public class FlyBoostModule extends Module {

   private static final float BASE_FLY_SPEED = 0.05F;
   private static final double VERTICAL_BASE = 0.15D;

   private final NumberSetting horizontal =
         this.addNumber("Horizontal Boost", 2.0D, 1.0D, 10.0D, 0.1D);
   private final NumberSetting verticalBoost =
         this.addNumber("Vertical Boost", 2.0D, 1.0D, 10.0D, 0.1D);
   private final BooleanSetting boostVertical = this.addBool("Boost vertical", true);
   /** When off, flyboost only applies while the toggle key has switched it on. */
   private final BooleanSetting automatic = this.addBool("Automatic Flyboost", true);
   private final BooleanSetting holdSprint = this.addBool("Hold Sprint For Flyboost", false);
   private final BooleanSetting sameSpeed = this.addBool("Always Same Speed (No A/D Speed)", false);
   private final BooleanSetting noMomentum = this.addBool("Instant Flyboost (No Momentum)", false);
   /**
    * Re-asserts the fly capability every tick, so a server that briefly clears
    * it -- e.g. crossing a region edge -- does not drop you out of the air.
    * Off by default: on a server that never grants fly this does nothing, but
    * it is the kind of thing an anticheat looks at, so it stays opt-in.
    */
   private final BooleanSetting keepFly = this.addBool("Keep Fly", false);
   private final KeybindSetting toggleKey = this.addKeybind("Toggle Flyboost", 0);

   /** Only consulted when {@link #automatic} is off. Starts on. */
   private boolean toggledOn = true;
   private boolean toggleWasDown;

   public FlyBoostModule() {
      super("Fly Boost", "Faster flight (only where the server allows fast fly)", ModuleCategory.MECHANIC, 0);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(!this.isEnabled() || this.mc.thePlayer == null) {
         return;
      }

      // Edge-detect the toggle on either phase-once per tick is enough.
      if(event.phase == Phase.START) {
         this.pollToggle();
      }

      if(this.keepFly.get()) {
         this.mc.thePlayer.capabilities.allowFlying = true;
      }

      boolean flying = this.mc.thePlayer.capabilities.isFlying;
      if(!flying || !this.shouldBoost()) {
         if(event.phase == Phase.START) {
            this.restore();
         }

         return;
      }

      if(event.phase == Phase.START) {
         // Before movement: set the horizontal speed and drive the vertical
         // axis, which vanilla is about to read.
         this.mc.thePlayer.capabilities.setFlySpeed(BASE_FLY_SPEED * (float)this.horizontal.get());

         if(this.boostVertical.get()) {
            boolean up = this.mc.gameSettings.keyBindJump.isKeyDown();
            boolean down = this.mc.gameSettings.keyBindSneak.isKeyDown();
            if(up != down) {
               double v = VERTICAL_BASE * this.verticalBoost.get();
               this.mc.thePlayer.motionY = up ? v : -v;
            }
         }
      } else {
         // After movement: correct the horizontal velocity vanilla just
         // produced. Both of these are post-move because they act on the
         // motion vector, which does not exist until the move has run.
         this.postMove();
      }
   }

   private void pollToggle() {
      boolean down = this.toggleKey.getKeyCode() != 0
            && this.mc.currentScreen == null
            && BindUtil.isDown(this.toggleKey.getKeyCode());

      if(down && !this.toggleWasDown) {
         this.toggledOn = !this.toggledOn;
      }

      this.toggleWasDown = down;
   }

   private boolean shouldBoost() {
      if(!this.automatic.get() && !this.toggledOn) {
         return false;
      }

      return !this.holdSprint.get() || this.mc.thePlayer.isSprinting();
   }

   private void postMove() {
      boolean anyHorizontal = this.mc.gameSettings.keyBindForward.isKeyDown()
            || this.mc.gameSettings.keyBindBack.isKeyDown()
            || this.mc.gameSettings.keyBindLeft.isKeyDown()
            || this.mc.gameSettings.keyBindRight.isKeyDown();

      if(this.noMomentum.get() && !anyHorizontal) {
         // Vanilla flight glides to a stop; zeroing the residual velocity makes
         // releasing the keys an instant stop instead.
         this.mc.thePlayer.motionX = 0.0D;
         this.mc.thePlayer.motionZ = 0.0D;
         return;
      }

      if(this.sameSpeed.get() && anyHorizontal) {
         // Vanilla adds strafe and forward as vectors, so moving diagonally is
         // ~1.41x faster than straight. Renormalising the horizontal velocity
         // to the straight-line target makes every direction the same speed.
         double target = (double)(BASE_FLY_SPEED * (float)this.horizontal.get()) * 2.0D;
         double mx = this.mc.thePlayer.motionX;
         double mz = this.mc.thePlayer.motionZ;
         double speed = Math.sqrt(mx * mx + mz * mz);

         if(speed > target && speed > 1.0E-4D) {
            double scale = target / speed;
            this.mc.thePlayer.motionX = mx * scale;
            this.mc.thePlayer.motionZ = mz * scale;
         }
      }
   }

   protected void onDisable() {
      this.restore();
   }

   private void restore() {
      if(this.mc.thePlayer != null) {
         this.mc.thePlayer.capabilities.setFlySpeed(BASE_FLY_SPEED);
      }

   }
}
