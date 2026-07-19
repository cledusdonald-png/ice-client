package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ModeSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

/**
 * Equipped armour as item icons with durability beside each piece.
 *
 * <p>Rendered top-down helmet-first, matching the order you read armour in, and
 * durability is colour-graded so a piece about to break is obvious at a glance
 * rather than something you have to read a number to notice.
 */
public class ArmorStatusModule extends HudModule {

   private static final int ICON = 16;
   private static final int ROW_H = 18;
   private static final int PAD = 2;

   private final ModeSetting durability = (ModeSetting)this.addSetting(new ModeSetting("Durability", "Percent", new String[]{"Percent", "Value", "Hidden"}));
   private final BooleanSetting showIcons = (BooleanSetting)this.addSetting(new BooleanSetting("Show icons", true));
   private final BooleanSetting colorByWear = (BooleanSetting)this.addSetting(new BooleanSetting("Color by durability", true));
   private final BooleanSetting helmetFirst = (BooleanSetting)this.addSetting(new BooleanSetting("Helmet first", true));
   private final BooleanSetting hideEmpty = (BooleanSetting)this.addSetting(new BooleanSetting("Hide empty slots", true));
   private final BooleanSetting includeHeld = (BooleanSetting)this.addSetting(new BooleanSetting("Include held item", false));

   public ArmorStatusModule() {
      super("Armor Status", "Shows equipped armor and durability",
            ModuleCategory.GENERAL, 0, HudModule.Anchor.BOTTOM_RIGHT);
   }

   /** Pieces to show, in display order. */
   private List<ItemStack> pieces() {
      List<ItemStack> out = new ArrayList();
      if(this.mc.thePlayer == null) {
         return out;
      }

      // armorInventory is boots-first; flip it unless asked otherwise.
      ItemStack[] armor = this.mc.thePlayer.inventory.armorInventory;
      for(int i = 0; i < armor.length; ++i) {
         ItemStack s = armor[this.helmetFirst.get() ? armor.length - 1 - i : i];
         if(s != null || !this.hideEmpty.get()) {
            out.add(s);
         }
      }

      if(this.includeHeld.get()) {
         ItemStack held = this.mc.thePlayer.getHeldItem();
         if(held != null || !this.hideEmpty.get()) {
            out.add(held);
         }
      }

      return out;
   }

   public int getWidth() {
      List<ItemStack> list = this.pieces();
      if(list.isEmpty()) {
         return 0;
      }

      int w = 0;
      for(ItemStack s : list) {
         String label = this.labelFor(s);
         if(label != null) {
            w = Math.max(w, this.mc.fontRendererObj.getStringWidth(label));
         }
      }

      return (this.showIcons.get() ? ICON + 4 : 0) + w + PAD * 2;
   }

   public int getHeight() {
      List<ItemStack> list = this.pieces();
      return list.isEmpty() ? 0 : list.size() * ROW_H + PAD * 2;
   }

   public void render(int x, int y) {
      List<ItemStack> list = this.pieces();
      if(list.isEmpty()) {
         return;
      }

      if(this.hasBackground()) {
         Gui.drawRect(x, y, x + this.getWidth(), y + this.getHeight(), this.backgroundColor());
      }

      int textX = x + PAD + (this.showIcons.get() ? ICON + 4 : 0);
      int rowY = y + PAD;

      for(ItemStack s : list) {
         if(s != null && this.showIcons.get()) {
            // Item rendering leaves lighting on; the HUD text after it would
            // come out tinted if we didn't turn it back off.
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableDepth();
            this.mc.getRenderItem().renderItemAndEffectIntoGUI(s, x + PAD, rowY);
            this.mc.getRenderItem().renderItemOverlayIntoGUI(this.mc.fontRendererObj, s, x + PAD, rowY, (String)null);
            GlStateManager.disableDepth();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         }

         String label = this.labelFor(s);
         if(label != null) {
            int col = this.textChroma.get() ? this.styledColor() : this.colorFor(s);
            int ty = rowY + (ICON - 8) / 2;
            if(this.textShadow.get()) {
               this.mc.fontRendererObj.drawStringWithShadow(label, (float)textX, (float)ty, col);
            } else {
               this.mc.fontRendererObj.drawString(label, textX, ty, col);
            }
         }

         rowY += ROW_H;
      }

   }

   private String labelFor(ItemStack s) {
      if(s == null) {
         return this.hideEmpty.get() ? null : "-";
      }

      if(this.durability.is("Hidden") || s.getMaxDamage() <= 0) {
         return null;
      }

      int remaining = s.getMaxDamage() - s.getItemDamage();
      if(this.durability.is("Value")) {
         return remaining + "/" + s.getMaxDamage();
      }

      return (int)Math.round(100.0D * (double)remaining / (double)s.getMaxDamage()) + "%";
   }

   /** Green through yellow to red as a piece wears down. */
   private int colorFor(ItemStack s) {
      if(s == null || !this.colorByWear.get() || s.getMaxDamage() <= 0) {
         return this.getColor();
      }

      float frac = (float)(s.getMaxDamage() - s.getItemDamage()) / (float)s.getMaxDamage();
      if(frac > 0.5F) {
         return -11141291;
      }

      return frac > 0.25F ? -171 : -43691;
   }
}
