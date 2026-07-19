package com.iceclient.module.modules.schematic;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import com.iceclient.schematica.SchematicaBridge;
import com.iceclient.setting.BooleanSetting;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

/**
 * Places the block the schematic asks for, instead of whatever is in your hand.
 *
 * <p>The interception lives in {@code MixinPlayerControllerMP}; this holds the
 * settings and the decision logic. Two things happen on a right-click against a
 * position the schematic covers: the hotbar is switched to the required block,
 * and -- if {@link #strict} is on and you do not have that block -- the place is
 * cancelled outright.
 *
 * <p>Cancelling matters more than the auto-swap does. Misplacing one wrong block
 * inside a wall means breaking it back out, and on a server with block-place
 * logging that is the difference between a clean print and a mess.
 */
public class EasyPlace extends Module {

   private final BooleanSetting autoSwap = this.addBool("Auto-swap hotbar", true);
   private final BooleanSetting strict = this.addBool("Block wrong placements", true);
   private final BooleanSetting notifyMissing = this.addBool("Notify when missing", true);

   /** Rate-limits the "missing block" toast; a held right-click fires fast. */
   private long lastNotify;

   public EasyPlace() {
      super("EasyPlace", "Places the schematic's block, not the one you're holding", ModuleCategory.PRINTER);
   }

   /**
    * Decides what to do about a right-click at {@code pos}.
    *
    * @return true when the placement should be cancelled
    */
   public boolean handlePlacement(BlockPos pos) {
      if(!this.isEnabled() || pos == null || !SchematicaBridge.isAvailable() || !SchematicaBridge.hasSchematic()) {
         return false;
      }

      IBlockState wanted = SchematicaBridge.blockStateAt(pos);
      if(wanted == null) {
         // Outside the schematic, or it wants air here -- ordinary building,
         // leave the player alone.
         return false;
      }

      Item needed = Item.getItemFromBlock(wanted.getBlock());
      if(needed == null) {
         return false;
      }

      if(this.holding(needed)) {
         return false;
      }

      int slot = this.findSlot(needed);
      if(slot >= 0 && this.autoSwap.get()) {
         this.mc.thePlayer.inventory.currentItem = slot;
         return false;
      }

      if(this.strict.get()) {
         this.warnMissing(wanted);
         return true;
      }

      return false;
   }

   private boolean holding(Item needed) {
      ItemStack held = this.mc.thePlayer == null ? null : this.mc.thePlayer.getHeldItem();
      return held != null && held.getItem() == needed;
   }

   /** Hotbar only -- swapping from the main inventory needs a server packet. */
   private int findSlot(Item needed) {
      if(this.mc.thePlayer == null) {
         return -1;
      }

      for(int i = 0; i < 9; ++i) {
         ItemStack s = this.mc.thePlayer.inventory.getStackInSlot(i);
         if(s != null && s.getItem() == needed && s.stackSize > 0) {
            return i;
         }
      }

      return -1;
   }

   private void warnMissing(IBlockState wanted) {
      if(!this.notifyMissing.get()) {
         return;
      }

      long now = System.currentTimeMillis();
      if(now - this.lastNotify < 1500L) {
         return;
      }

      this.lastNotify = now;
      String name = wanted.getBlock().getLocalizedName();
      NotificationManager.post("EasyPlace", "Need " + name, Notification.Type.WARNING);
   }
}
