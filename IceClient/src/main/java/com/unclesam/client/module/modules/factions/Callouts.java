package com.unclesam.client.module.modules.factions;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.KeybindSetting;
import com.unclesam.client.setting.ModeSetting;
import com.unclesam.client.util.BindUtil;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Sends a coordinate callout to chat on a keypress -- either where you're
 * looking or where you're standing.
 */
public class Callouts extends Module {

   private final KeybindSetting sendKey = (KeybindSetting)this.addSetting(new KeybindSetting("Send key", Keyboard.KEY_G));
   private final ModeSetting source = (ModeSetting)this.addSetting(new ModeSetting("Source", "Looking at", new String[]{"Looking at", "My position"}));
   private final ModeSetting channel = (ModeSetting)this.addSetting(new ModeSetting("Channel", "Faction", new String[]{"Faction", "Ally", "Public", "Clipboard"}));
   private final BooleanSetting includeY = (BooleanSetting)this.addSetting(new BooleanSetting("Include Y", true));

   private boolean wasDown;

   public Callouts() {
      super("Callouts", "Sends coordinate callouts to chat", ModuleCategory.FACTIONS);
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

      boolean down = BindUtil.isDown(this.sendKey.getKeyCode());
      if(down && !this.wasDown) {
         this.send();
      }

      this.wasDown = down;
   }

   private void send() {
      BlockPos pos = this.target();
      if(pos == null) {
         return;
      }

      String coords = this.includeY.get()
            ? pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
            : pos.getX() + ", " + pos.getZ();

      String mode = this.channel.get();
      if("Clipboard".equals(mode)) {
         try {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                  .setContents(new java.awt.datatransfer.StringSelection(coords), null);
         } catch (Throwable var5) {
            // Headless or restricted clipboard -- nothing useful to do.
         }

         return;
      }

      String prefix = "Ally".equals(mode) ? "/a " : ("Public".equals(mode) ? "" : "/f c ");
      this.mc.thePlayer.sendChatMessage(prefix + coords);
   }

   /** Block you're looking at, or your own feet. */
   private BlockPos target() {
      if("My position".equals(this.source.get())) {
         return new BlockPos(this.mc.thePlayer);
      }

      MovingObjectPosition hit = this.mc.objectMouseOver;
      return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            ? hit.getBlockPos()
            : new BlockPos(this.mc.thePlayer);
   }
}
