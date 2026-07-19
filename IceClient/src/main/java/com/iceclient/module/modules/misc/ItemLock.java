package com.iceclient.module.modules.misc;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.util.BindUtil;
import com.iceclient.util.ColorUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Marks hotbar slots as locked so you don't drop or throw their contents by
 * accident.
 *
 * <p>Locks the *slot*, not the item: it blocks the drop key while a locked slot
 * is selected. Blocking inventory-click movement as well needs a container-click
 * hook, which is a separate change.
 */
public class ItemLock extends Module {

   private final KeybindSetting lockKey = (KeybindSetting)this.addSetting(new KeybindSetting("Lock slot key", 0));
   private final BooleanSetting blockDrop = (BooleanSetting)this.addSetting(new BooleanSetting("Block drop key", true));
   private final BooleanSetting showOverlay = (BooleanSetting)this.addSetting(new BooleanSetting("Show overlay", true));
   private final BooleanSetting announce = (BooleanSetting)this.addSetting(new BooleanSetting("Announce in chat", true));
   private final ColorSetting lockColor = (ColorSetting)this.addSetting(new ColorSetting("Lock color", -47872));

   private static final Set<Integer> LOCKED = new HashSet();
   private boolean wasDown;

   public ItemLock() {
      super("Item Lock", "Locks hotbar slots against dropping", ModuleCategory.MECHANIC);
   }

   public static boolean isLocked(int slot) {
      return LOCKED.contains(Integer.valueOf(slot));
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

      boolean down = BindUtil.isDown(this.lockKey.getKeyCode());
      if(down && !this.wasDown) {
         int slot = this.mc.thePlayer.inventory.currentItem;
         boolean nowLocked;
         if(LOCKED.contains(Integer.valueOf(slot))) {
            LOCKED.remove(Integer.valueOf(slot));
            nowLocked = false;
         } else {
            LOCKED.add(Integer.valueOf(slot));
            nowLocked = true;
         }

         if(this.announce.get()) {
            this.mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Ice] "
                  + EnumChatFormatting.RESET + "Slot " + (slot + 1) + (nowLocked ? " locked" : " unlocked")));
         }
      }

      this.wasDown = down;

      // Vanilla reads the drop key itself, so the only reliable block without a
      // mixin is to clear the key state while a locked slot is selected.
      if(this.blockDrop.get() && LOCKED.contains(Integer.valueOf(this.mc.thePlayer.inventory.currentItem))) {
         int dropCode = this.mc.gameSettings.keyBindDrop.getKeyCode();
         if(dropCode != 0) {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(dropCode, false);
         }
      }

   }

   @SubscribeEvent
   public void onOverlay(RenderGameOverlayEvent.Post event) {
      if(!this.isEnabled() || !this.showOverlay.get() || LOCKED.isEmpty()) {
         return;
      }

      if(event.type != RenderGameOverlayEvent.ElementType.HOTBAR || this.mc.thePlayer == null) {
         return;
      }

      ScaledResolution res = new ScaledResolution(this.mc);
      // Vanilla hotbar: 182px wide, centred, each slot 20px, 22px above bottom.
      int left = res.getScaledWidth() / 2 - 91;
      int top = res.getScaledHeight() - 22;
      int col = ColorUtil.withAlpha(this.lockColor.getRGB(), 120);

      for(Integer slot : LOCKED) {
         int i = slot.intValue();
         if(i >= 0 && i < 9) {
            int x = left + i * 20;
            Gui.drawRect(x + 1, top + 1, x + 19, top + 19, col);
         }
      }

   }
}
