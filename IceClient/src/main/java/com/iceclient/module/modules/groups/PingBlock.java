package com.iceclient.module.modules.groups;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import com.iceclient.ping.Ping;
import com.iceclient.ping.PingManager;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.BindUtil;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Marks the block you are looking at with a beam, and optionally calls it out
 * in chat.
 *
 * <p>Complements {@link PingLocation}, which marks where <em>you</em> are. This
 * marks something you can see but are not standing on -- a chest, a spawner, the
 * bit of wall to cannon -- which is the case you cannot cover by walking there
 * first during a raid.
 */
public class PingBlock extends Module {

   private final KeybindSetting pingKey = this.addKeybind("Ping key", Keyboard.KEY_MULTIPLY);
   private final ColorSetting color = this.addColor("Marker colour", 0xFF55FFFF);
   private final NumberSetting lifetime = this.addNumber("Lifetime (s)", 30.0D, 0.0D, 300.0D, 5.0D);
   private final BooleanSetting announce = this.addBool("Announce in chat", false);
   private final BooleanSetting toast = this.addBool("Toast on ping", true);
   private final NumberSetting maxDistance = this.addNumber("Max distance", 128.0D, 16.0D, 256.0D, 8.0D);

   private boolean wasDown;

   public PingBlock() {
      super("PingBlock", "Marks the block you're looking at", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled()) {
         return;
      }

      if(this.mc.thePlayer == null || this.mc.currentScreen != null) {
         this.wasDown = false;
         return;
      }

      boolean down = this.pingKey.getKeyCode() != 0 && BindUtil.isDown(this.pingKey.getKeyCode());
      if(down && !this.wasDown) {
         this.ping();
      }

      this.wasDown = down;
   }

   private void ping() {
      BlockPos pos = this.target();
      if(pos == null) {
         NotificationManager.post("PingBlock", "Look at a block first", Notification.Type.WARNING);
         return;
      }

      PingManager.add(new Ping(pos, Ping.Type.BLOCK, this.color.getRGB(),
            (long)(this.lifetime.get() * 1000.0D)));

      String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();

      if(this.toast.get()) {
         NotificationManager.post("Pinged", coords, Notification.Type.SUCCESS);
      }

      if(this.announce.get()) {
         // Plain chat, not a faction command: servers differ on what /f-prefixed
         // messages are allowed, and a rejected command is worse than a message
         // that lands in the wrong channel.
         this.mc.thePlayer.sendChatMessage("Ping: " + coords);
      }

   }

   /**
    * The looked-at block, if within range.
    *
    * <p>Falls back to nothing rather than to the player's own position: a ping
    * that silently lands under your feet when you were aiming at a wall reads
    * as the feature being broken.
    */
   private BlockPos target() {
      MovingObjectPosition hit = this.mc.objectMouseOver;
      if(hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
         return null;
      }

      BlockPos pos = hit.getBlockPos();
      double max = this.maxDistance.get();
      return this.mc.thePlayer.getDistanceSq(pos) > max * max ? null : pos;
   }
}
