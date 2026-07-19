package com.iceclient.module.modules.factions;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.util.ColorUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * Minecadia-specific inventory labels.
 *
 * <p>Soul gems and armour pieces carry their tier in the item lore, which means
 * squinting at a tooltip to tell two stacks apart. This stamps the useful number
 * straight onto the slot.
 */
public class Minecadia extends Module {

   private final BooleanSetting soulGemLabel = (BooleanSetting)this.addSetting(new BooleanSetting("Soul gem label", true));
   private final BooleanSetting armorLabel = (BooleanSetting)this.addSetting(new BooleanSetting("Armor label", true));
   private final BooleanSetting soulsEnabled = (BooleanSetting)this.addSetting(new BooleanSetting("Souls", true));
   private final BooleanSetting soulsOnArmor = (BooleanSetting)this.addSetting(new BooleanSetting("Souls on armor", true));

   public Minecadia() {
      super("Minecadia", "Features for Minecadia", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
      if(!this.isEnabled() || !(event.gui instanceof GuiContainer)) {
         return;
      }

      GuiContainer gui = (GuiContainer)event.gui;
      int left = this.intField(gui, "guiLeft", "field_147003_i");
      int top = this.intField(gui, "guiTop", "field_147009_r");

      // GuiContainer draws item icons at z=200 with GUI item lighting still on.
      // Drawing the label at the default z=0 puts it *behind* the icon, which is
      // why these tags looked like they simply did not work -- they were being
      // rendered, just underneath the item they label.
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0F, 0.0F, 300.0F);
      GlStateManager.disableDepth();
      GlStateManager.disableLighting();
      RenderHelper.disableStandardItemLighting();
      GlStateManager.enableBlend();

      for(Slot slot : gui.inventorySlots.inventorySlots) {
         ItemStack stack = slot.getStack();
         if(stack == null) {
            continue;
         }

         String label = this.labelFor(stack);
         if(label == null) {
            continue;
         }

         int x = left + slot.xDisplayPosition;
         int y = top + slot.yDisplayPosition;
         Gui.drawRect(x, y, x + 16, y + 8, ColorUtil.withAlpha(0, 150));
         this.mc.fontRendererObj.drawStringWithShadow(label, (float)(x + 1), (float)(y + 1), -1);
      }

      GlStateManager.disableBlend();
      GlStateManager.enableDepth();
      GlStateManager.popMatrix();
   }

   /**
    * Short marker for a Minecadia item, from its display name and lore.
    *
    * <p>Everything here reads name/lore the server already sends, so it works
    * with or without the Minecadia resource pack installed -- that pack is
    * OptiFine CIT and only changes how custom armour is *drawn*, not how it's
    * identified.
    */
   private String labelFor(ItemStack stack) {
      try {
         String name = EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName());
         if(name == null) {
            return null;
         }

         String lower = name.toLowerCase();

         if(this.soulsEnabled.get() && this.soulGemLabel.get() && lower.contains("soul gem")) {
            String n = this.firstNumber(name);
            return n == null ? "SG" : n;
         }

         List<String> tip = stack.getTooltip(this.mc.thePlayer, false);

         // Souls stored on a piece of gear, e.g. "Souls: 1,204".
         if(this.soulsEnabled.get() && this.soulsOnArmor.get()) {
            for(String line : tip) {
               String clean = EnumChatFormatting.getTextWithoutFormattingCodes(line);
               if(clean != null && clean.toLowerCase().contains("soul")) {
                  String n = this.firstNumber(clean.replace(",", ""));
                  if(n != null) {
                     return this.compact(Integer.parseInt(n));
                  }
               }
            }
         }

         if(this.armorLabel.get()) {
            for(String line : tip) {
               String clean = EnumChatFormatting.getTextWithoutFormattingCodes(line);
               if(clean != null && clean.toLowerCase().contains("tier")) {
                  String n = this.firstNumber(clean);
                  if(n != null) {
                     return "T" + n;
                  }
               }
            }
         }
      } catch (Throwable var8) {
         // A server-built stack with odd NBT shouldn't break inventory rendering.
      }

      return null;
   }

   /** 1204 -> "1.2k", so a soul count fits in a 16px slot. */
   private String compact(int v) {
      if(v < 1000) {
         return String.valueOf(v);
      }

      return v < 1000000
            ? String.format("%.1fk", Float.valueOf((float)v / 1000.0F))
            : String.format("%.1fm", Float.valueOf((float)v / 1000000.0F));
   }

   private String firstNumber(String s) {
      java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,4})").matcher(s);
      return m.find() ? m.group(1) : null;
   }

   private int intField(GuiContainer gui, String mcpName, String srgName) {
      try {
         return ((Integer)net.minecraftforge.fml.relauncher.ReflectionHelper
               .getPrivateValue(GuiContainer.class, gui, new String[]{mcpName, srgName})).intValue();
      } catch (Throwable var5) {
         return 0;
      }
   }
}
