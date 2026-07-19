package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import com.iceclient.setting.BooleanSetting;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

/** Counts obsidian (and optionally sand/gravel) carried -- the raiding staples. */
public class ObsidianCounterHud extends TextHudModule {

   private final BooleanSetting countSand = (BooleanSetting)this.addSetting(new BooleanSetting("Count sand", true));
   private final BooleanSetting hideEmpty = (BooleanSetting)this.addSetting(new BooleanSetting("Hide when empty", true));

   public ObsidianCounterHud() {
      super("Obsidian Counter", "Counts obsidian in your inventory", ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 130);
   }

   protected List<String> lines() {
      if(this.mc.thePlayer == null) {
         return Collections.emptyList();
      }

      int obby = this.count(Item.getItemFromBlock(Blocks.obsidian));
      int sand = this.countSand.get()
            ? this.count(Item.getItemFromBlock(Blocks.sand)) + this.count(Item.getItemFromBlock(Blocks.gravel))
            : 0;

      if(this.hideEmpty.get() && obby == 0 && sand == 0) {
         return Collections.emptyList();
      }

      if(!this.countSand.get()) {
         return Collections.singletonList("Obby: " + obby);
      }

      List<String> out = new java.util.ArrayList(2);
      out.add("Obby: " + obby);
      out.add("Sand: " + sand);
      return out;
   }

   private int count(Item item) {
      if(item == null) {
         return 0;
      }

      int total = 0;
      for(ItemStack s : this.mc.thePlayer.inventory.mainInventory) {
         if(s != null && s.getItem() == item && s.getItem() instanceof ItemBlock) {
            total += s.stackSize;
         }
      }

      return total;
   }
}
