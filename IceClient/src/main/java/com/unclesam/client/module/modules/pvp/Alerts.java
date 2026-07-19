package com.unclesam.client.module.modules.pvp;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ColorSetting;
import com.unclesam.client.setting.NumberSetting;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Warns when armour is about to break or pots run low.
 *
 * <p>Each alert latches: it fires once when the threshold is crossed and won't
 * fire again until the value recovers above it. Without that, a helmet sitting
 * at 32% would spam every tick.
 */
public class Alerts extends Module {

   private final BooleanSetting helmet = (BooleanSetting)this.addSetting(new BooleanSetting("Low helmet alert", true));
   private final NumberSetting helmetPct = (NumberSetting)this.addSetting(new NumberSetting("Helmet % to alert", 33.0D, 1.0D, 99.0D, 1.0D));
   private final BooleanSetting chest = (BooleanSetting)this.addSetting(new BooleanSetting("Low chestplate alert", true));
   private final NumberSetting chestPct = (NumberSetting)this.addSetting(new NumberSetting("Chestplate % to alert", 33.0D, 1.0D, 99.0D, 1.0D));
   private final BooleanSetting legs = (BooleanSetting)this.addSetting(new BooleanSetting("Low leggings alert", true));
   private final NumberSetting legsPct = (NumberSetting)this.addSetting(new NumberSetting("Leggings % to alert", 33.0D, 1.0D, 99.0D, 1.0D));
   private final BooleanSetting boots = (BooleanSetting)this.addSetting(new BooleanSetting("Low boots alert", true));
   private final NumberSetting bootsPct = (NumberSetting)this.addSetting(new NumberSetting("Boots % to alert", 33.0D, 1.0D, 99.0D, 1.0D));
   private final BooleanSetting pots = (BooleanSetting)this.addSetting(new BooleanSetting("Low pots alert", true));
   private final NumberSetting potCount = (NumberSetting)this.addSetting(new NumberSetting("Amount of pots to alert", 10.0D, 1.0D, 64.0D, 1.0D));
   private final NumberSetting cooldown = (NumberSetting)this.addSetting(new NumberSetting("Alert cooldown (seconds)", 3.0D, 1.0D, 60.0D, 1.0D));
   private final BooleanSetting sound = (BooleanSetting)this.addSetting(new BooleanSetting("Play sound", true));
   private final ColorSetting alertColor = (ColorSetting)this.addSetting(new ColorSetting("Alert text color", -1));

   /** Latch per slot so an alert fires on the crossing, not every tick. */
   private final boolean[] armorLatched = new boolean[4];
   private boolean potsLatched;
   private long lastAlert;

   public Alerts() {
      super("Alerts", "Alerts that pop up on your screen when an event happens", ModuleCategory.COMBAT);
   }

   protected void onDisable() {
      java.util.Arrays.fill(this.armorLatched, false);
      this.potsLatched = false;
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled() || this.mc.thePlayer == null) {
         return;
      }

      // armorInventory: 0 boots, 1 leggings, 2 chestplate, 3 helmet
      this.checkArmor(3, this.helmet.get(), this.helmetPct.get(), "Helmet");
      this.checkArmor(2, this.chest.get(), this.chestPct.get(), "Chestplate");
      this.checkArmor(1, this.legs.get(), this.legsPct.get(), "Leggings");
      this.checkArmor(0, this.boots.get(), this.bootsPct.get(), "Boots");

      if(this.pots.get()) {
         int count = this.countPots();
         if(count <= (int)this.potCount.get()) {
            if(!this.potsLatched) {
               this.potsLatched = true;
               this.alert("Low pots: " + count + " left");
            }
         } else {
            this.potsLatched = false;
         }
      }

   }

   private void checkArmor(int slot, boolean on, double threshold, String name) {
      if(!on) {
         this.armorLatched[slot] = false;
         return;
      }

      ItemStack s = this.mc.thePlayer.inventory.armorInventory[slot];
      if(s == null || s.getMaxDamage() <= 0) {
         this.armorLatched[slot] = false;
         return;
      }

      double pct = 100.0D * (double)(s.getMaxDamage() - s.getItemDamage()) / (double)s.getMaxDamage();
      if(pct <= threshold) {
         if(!this.armorLatched[slot]) {
            this.armorLatched[slot] = true;
            this.alert(name + " at " + (int)pct + "%");
         }
      } else {
         this.armorLatched[slot] = false;
      }

   }

   private int countPots() {
      int total = 0;
      for(ItemStack s : this.mc.thePlayer.inventory.mainInventory) {
         if(s != null && s.getItem() == Items.potionitem) {
            total += s.stackSize;
         }
      }

      return total;
   }

   private void alert(String message) {
      long now = System.currentTimeMillis();
      if(now - this.lastAlert < (long)(this.cooldown.get() * 1000.0D)) {
         return;
      }

      this.lastAlert = now;
      if(this.mc.thePlayer != null) {
         this.mc.thePlayer.addChatMessage(new ChatComponentText(
               EnumChatFormatting.RED + "[Alert] " + EnumChatFormatting.RESET + message));
         if(this.sound.get()) {
            this.mc.thePlayer.playSound("note.pling", 1.0F, 0.5F);
         }
      }

   }
}
