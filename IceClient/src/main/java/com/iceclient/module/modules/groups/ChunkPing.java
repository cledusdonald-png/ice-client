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
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Marks a whole chunk column rather than a single block.
 *
 * <p>Factions claims are chunk-aligned, so "they are in this chunk" is the unit
 * callouts actually get made in -- a one-block marker is the wrong granularity
 * for pointing at a base, and a full column stays visible from outside the
 * walls where a block outline does not.
 */
public class ChunkPing extends Module {

   private final KeybindSetting pingKey = this.addKeybind("Ping chunk key", Keyboard.KEY_DIVIDE);
   private final ColorSetting color = this.addColor("Marker colour", 0xFFFFAA00);
   private final NumberSetting lifetime = this.addNumber("Lifetime (s)", 60.0D, 0.0D, 300.0D, 5.0D);
   private final BooleanSetting lookedAt = this.addBool("Use looked-at chunk", true);
   private final BooleanSetting announce = this.addBool("Announce in chat", false);

   private boolean wasDown;

   public ChunkPing() {
      super("ChunkPing", "Marks a whole chunk column", ModuleCategory.FACTIONS);
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
      PingManager.add(new Ping(pos, Ping.Type.CHUNK, this.color.getRGB(),
            (long)(this.lifetime.get() * 1000.0D)));

      int cx = pos.getX() >> 4;
      int cz = pos.getZ() >> 4;
      String label = "chunk " + cx + ", " + cz;

      NotificationManager.post("Chunk pinged", label, Notification.Type.SUCCESS);

      if(this.announce.get()) {
         // Chunk coordinates alone are hard to act on, so the block coordinates
         // of the chunk's corner go out too.
         this.mc.thePlayer.sendChatMessage("Ping: " + label + " (" + (cx << 4) + ", " + (cz << 4) + ")");
      }

   }

   /**
    * Falls back to the player's own chunk when nothing is targeted, which --
    * unlike {@link PingBlock} -- is genuinely useful here: "I am in this chunk"
    * is a normal callout.
    */
   private BlockPos target() {
      if(this.lookedAt.get()) {
         MovingObjectPosition hit = this.mc.objectMouseOver;
         if(hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return hit.getBlockPos();
         }
      }

      return new BlockPos(
            MathHelper.floor_double(this.mc.thePlayer.posX),
            MathHelper.floor_double(this.mc.thePlayer.posY),
            MathHelper.floor_double(this.mc.thePlayer.posZ));
   }
}
