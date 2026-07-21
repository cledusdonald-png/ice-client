package com.iceclient.module.modules.render;

import com.iceclient.util.BindUtil;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.ModeSetting;
import com.iceclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Hold a key to look around without turning your character.
 *
 * <p>Rather than rotating the player and rotating them back (which desyncs your
 * facing with the server and makes your body spin for everyone else), this
 * leaves the player's rotation untouched and keeps a separate camera yaw/pitch.
 * Mouse movement is diverted into that pair by {@code MixinEntity#setAngles},
 * and the camera swap happens in {@code MixinEntityRenderer#orientCamera}.
 *
 * <p>Your aim, movement and the rotation sent to the server all stay exactly
 * where they were when you pressed the key.
 */
public class FreeLook extends Module {

   // Deliberately unbound by default. The old default was Left Alt, which
   // Alt+Tab leaves stuck down in LWJGL -- FreeLook then latches on, setAngles
   // stays cancelled, and you walk in a direction you aren't facing. Bind it to
   // something you actually press on purpose.
   private final KeybindSetting key = (KeybindSetting)this.addSetting(new KeybindSetting("Hold key", 0));
   private final ModeSetting view = (ModeSetting)this.addSetting(new ModeSetting("View", "Third Person", new String[]{"Third Person", "Front", "First Person"}));
   private final NumberSetting sensitivity = (NumberSetting)this.addSetting(new NumberSetting("Sensitivity", 1.0D, 0.1D, 3.0D, 0.1D));
   private final BooleanSetting clampPitch = (BooleanSetting)this.addSetting(new BooleanSetting("Clamp pitch", true));
   private final BooleanSetting toggleMode = (BooleanSetting)this.addSetting(new BooleanSetting("Toggle mode", false));

   /** Live state read by the mixins. Static so they need no module lookup per frame. */
   private static boolean active;
   private static float camYaw;
   private static float camPitch;
   private static FreeLook instance;

   private int savedView;
   private boolean wasDown;

   public FreeLook() {
      super("FreeLook", "Hold a key to look around without turning", ModuleCategory.GENERAL);
      instance = this;
   }

   /**
    * Minecraft only rebuilds its visible-chunk set when the PLAYER's position or
    * rotation changes. FreeLook deliberately leaves the body still, so the moment
    * you swing the camera that set goes stale: chunks behind you stay culled, and
    * since entities are only drawn inside visible chunks, their nametags vanish
    * along with them. Marking the render global dirty each frame forces it to
    * re-evaluate against the camera we're actually looking through.
    */
   @SubscribeEvent
   public void onRenderTick(TickEvent.RenderTickEvent event) {
      if(!active) {
         return;
      }

      if(this.mc.renderGlobal != null) {
         this.mc.renderGlobal.setDisplayListEntitiesDirty();
      }

   }

   /**
    * Re-aims billboarded rendering at the FreeLook camera.
    *
    * <p>Everything billboarded -- nametags, waypoint labels, patch crumb coords
    * -- turns to face {@code RenderManager.playerViewY/X}. Those are taken from
    * the PLAYER's rotation, which FreeLook deliberately does not move, so labels
    * keep facing where your body points while you look from somewhere else.
    * Swing round behind and they go edge-on, then invisible: "nametags disappear
    * in freelook".
    *
    * <p>This has to run <em>here</em> rather than on a render tick. The values
    * are rewritten by {@code RenderManager.cacheActiveRenderInfo} during the
    * world render, so anything set earlier in the frame is overwritten before a
    * single label is drawn -- which is exactly why the previous attempt changed
    * nothing. HIGHEST priority so it lands before the modules that draw labels
    * in this same event.
    */
   @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST)
   public void onRenderWorldLast(net.minecraftforge.client.event.RenderWorldLastEvent event) {
      if(active && this.mc.getRenderManager() != null) {
         this.mc.getRenderManager().playerViewY = camYaw;
         this.mc.getRenderManager().playerViewX = camPitch;
      }
   }

   public static boolean isActive() {
      return active;
   }

   public static float getCamYaw() {
      return camYaw;
   }

   public static float getCamPitch() {
      return camPitch;
   }

   /**
    * Called from the mouse-input mixin with the raw per-frame deltas. Returns
    * true when FreeLook consumed them, in which case the player must not turn.
    */
   public static boolean consumeMouse(float deltaYaw, float deltaPitch) {
      if(!active || instance == null) {
         return false;
      } else {
         float scale = (float)instance.sensitivity.get();
         camYaw += deltaYaw * scale;
         camPitch += deltaPitch * scale;
         if(instance.clampPitch.get()) {
            camPitch = Math.max(-90.0F, Math.min(90.0F, camPitch));
         }

         return true;
      }
   }

   protected void onDisable() {
      if(active) {
         this.stop();
      }
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase == TickEvent.Phase.END) {
         Minecraft mc = Minecraft.getMinecraft();
         if(this.isEnabled() && mc.thePlayer != null) {
            // Alt-tabbing away can leave the hold key latched down in LWJGL.
            // If the window isn't focused we can't be holding anything, so
            // force-release rather than staying stuck with rotation cancelled.
            if(!org.lwjgl.opengl.Display.isActive()) {
               if(active) {
                  this.stop();
               }

               this.wasDown = false;
               return;
            }

            // Releasing into a GUI would otherwise strand the camera off-axis.
            if(mc.currentScreen != null) {
               if(active) {
                  this.stop();
               }

               this.wasDown = false;
            } else {
               boolean down = this.key.getKeyCode() != 0 && BindUtil.isDown(this.key.getKeyCode());
               if(this.toggleMode.get()) {
                  if(down && !this.wasDown) {
                     if(active) {
                        this.stop();
                     } else {
                        this.start();
                     }
                  }
               } else if(down && !active) {
                  this.start();
               } else if(!down && active) {
                  this.stop();
               }

               this.wasDown = down;
            }
         } else if(active) {
            this.stop();
         }
      }
   }

   private void start() {
      Minecraft mc = Minecraft.getMinecraft();
      // Start the camera where the player is looking so there's no jump.
      camYaw = mc.thePlayer.rotationYaw;
      camPitch = mc.thePlayer.rotationPitch;
      this.savedView = mc.gameSettings.thirdPersonView;
      mc.gameSettings.thirdPersonView = this.viewId();
      active = true;
   }

   private void stop() {
      Minecraft.getMinecraft().gameSettings.thirdPersonView = this.savedView;
      active = false;
   }

   private int viewId() {
      String v = this.view.get();
      return "First Person".equals(v)?0:("Front".equals(v)?2:1);
   }
}
