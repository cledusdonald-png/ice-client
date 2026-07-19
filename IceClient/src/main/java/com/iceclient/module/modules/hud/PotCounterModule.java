package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.util.RenderUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

public class PotCounterModule extends HudModule {
   private static final int PAD = 4;
   private static final int LINE_H = 10;
   private static final int[] COMBAT_IDS = new int[]{6, 1, 12, 5, 10, 11, 8};
   private static final Map<Integer, String> LABELS = new LinkedHashMap();
   private final BooleanSetting combatOnly = (BooleanSetting)this.addSetting(new BooleanSetting("Combat pots only", true));

   public PotCounterModule() {
      super("Pot Counter", "Counts splash potions in your inventory", ModuleCategory.HUD, 0, HudModule.Anchor.BOTTOM_RIGHT);
   }

   private Map<Integer, Integer> counts() {
      Map<Integer, Integer> map = new LinkedHashMap();
      if(this.mc.thePlayer == null) {
         return map;
      } else {
         for(ItemStack stack : this.mc.thePlayer.inventory.mainInventory) {
            if(stack != null && stack.getItem() instanceof ItemPotion && ItemPotion.isSplash(stack.getMetadata())) {
               ItemPotion potion = (ItemPotion)stack.getItem();
               List<PotionEffect> effects = potion.getEffects(stack);
               if(effects != null && !effects.isEmpty()) {
                  int id = ((PotionEffect)effects.get(0)).getPotionID();
                  if(!this.combatOnly.get() || LABELS.containsKey(Integer.valueOf(id))) {
                     map.merge(Integer.valueOf(id), Integer.valueOf(stack.stackSize), Integer::sum);
                  }
               }
            }
         }

         return map;
      }
   }

   private List<String> lines() {
      Map<Integer, Integer> counts = this.counts();
      List<String> lines = new ArrayList();

      for(int id : COMBAT_IDS) {
         if(counts.containsKey(Integer.valueOf(id))) {
            lines.add((String)LABELS.get(Integer.valueOf(id)) + " x" + counts.remove(Integer.valueOf(id)));
         }
      }

      for(Entry<Integer, Integer> e : counts.entrySet()) {
         String label = (String)LABELS.getOrDefault(e.getKey(), "Pot");
         lines.add(label + " x" + e.getValue());
      }

      return lines;
   }

   public boolean isEmpty() {
      return this.lines().isEmpty();
   }

   public int getWidth() {
      int widest = 0;

      for(String line : this.lines()) {
         widest = Math.max(widest, this.mc.fontRendererObj.getStringWidth(line));
      }

      return widest == 0?0:widest + 8;
   }

   public int getHeight() {
      int count = this.lines().size();
      return count == 0?0:count * 10 + 8;
   }

   public void render(int x, int y) {
      List<String> lines = this.lines();
      if(!lines.isEmpty()) {
         if(this.hasBackground()) {
            RenderUtil.panel(x, y, x + this.getWidth(), y + this.getHeight(), this.backgroundColor(), -14013902);
         }

         for(int j = 0; j < lines.size(); ++j) {
            this.drawStyled((String)lines.get(j), x + 4, y + 4 + j * 10);
         }

      }
   }

   static {
      LABELS.put(Integer.valueOf(6), "Heal");
      LABELS.put(Integer.valueOf(1), "Speed");
      LABELS.put(Integer.valueOf(12), "Fire Res");
      LABELS.put(Integer.valueOf(5), "Str");
      LABELS.put(Integer.valueOf(10), "Regen");
      LABELS.put(Integer.valueOf(11), "Resist");
      LABELS.put(Integer.valueOf(8), "Jump");
   }
}
