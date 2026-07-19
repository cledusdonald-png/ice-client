package com.unclesam.client.module.modules.factions;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.KeybindSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.BindUtil;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Snaps your view to an exact pitch/yaw and holds it, so a cannon lines up the
 * same way every shot.
 *
 * <p>Yaw is snapped to the nearest 90° by default -- cannons are axis-aligned,
 * and eyeballing "straight north" is where alignment usually goes wrong.
 */
public class CannonView extends Module {

   private final KeybindSetting snapKey = (KeybindSetting)this.addSetting(new KeybindSetting("Snap key", Keyboard.KEY_X));
   private final NumberSetting pitch = (NumberSetting)this.addSetting(new NumberSetting("Pitch", 0.0D, -90.0D, 90.0D, 1.0D));
   private final BooleanSetting snapYaw = (BooleanSetting)this.addSetting(new BooleanSetting("Snap yaw to 90", true));
   private final BooleanSetting hold = (BooleanSetting)this.addSetting(new BooleanSetting("Hold view", false));
   private final BooleanSetting announce = (BooleanSetting)this.addSetting(new BooleanSetting("Announce in chat", true));

   private boolean wasDown;
   private boolean holding;
   private float heldYaw;
   private float heldPitch;

   public CannonView() {
      super("Cannon View", "Snaps your view to a fixed cannon angle", ModuleCategory.FACTIONS);
   }

   protected void onDisable() {
      this.holding = false;
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled() || this.mc.thePlayer == null) {
         return;
      }

      if(this.mc.currentScreen != null) {
         this.wasDown = false;
         return;
      }

      boolean down = BindUtil.isDown(this.snapKey.getKeyCode());
      if(down && !this.wasDown) {
         this.snap();
      }

      this.wasDown = down;

      if(this.holding && this.hold.get()) {
         this.mc.thePlayer.rotationYaw = this.heldYaw;
         this.mc.thePlayer.rotationPitch = this.heldPitch;
      }

   }

   private void snap() {
      float yaw = this.mc.thePlayer.rotationYaw;
      if(this.snapYaw.get()) {
         yaw = (float)(Math.round(yaw / 90.0F) * 90);
      }

      this.mc.thePlayer.rotationYaw = yaw;
      this.mc.thePlayer.rotationPitch = (float)this.pitch.get();

      this.heldYaw = yaw;
      this.heldPitch = (float)this.pitch.get();
      this.holding = !this.holding;

      if(this.announce.get()) {
         this.mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Ice] "
               + EnumChatFormatting.RESET + "View snapped to yaw " + (int)yaw + ", pitch " + (int)this.pitch.get()
               + (this.hold.get() ? this.holding ? " (holding)" : " (released)" : "")));
      }

   }
}
