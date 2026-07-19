package com.unclesam.client.module.modules.render;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ColorSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.ColorUtil;
import com.unclesam.client.util.WorldRenderUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

/**
 * Shows the custom armour set a player is wearing, as a line above their head.
 *
 * <p>Deliberately <em>additive</em>: vanilla's nametag is left completely alone
 * and this draws above it. The previous Nametags module cancelled vanilla's
 * nameplate and redrew it a render pass later, which is what made tags flicker
 * -- one pass had a tag, the next didn't. Nothing here cancels anything, so
 * there's no window in which the name is missing.
 *
 * <p>The set is the first word shared by the player's non-vanilla armour, and
 * <b>at least two pieces must agree</b>. Accepting a single match is what
 * produced "[Head]" from one decorative player head worn with plain diamond.
 */
public class ArmorSetTags extends Module {

   private final NumberSetting range = (NumberSetting)this.addSetting(new NumberSetting("Range", 64.0D, 8.0D, 256.0D, 8.0D));
   private final NumberSetting minPieces = (NumberSetting)this.addSetting(new NumberSetting("Min matching pieces", 2.0D, 2.0D, 4.0D, 1.0D));
   private final NumberSetting scale = (NumberSetting)this.addSetting(new NumberSetting("Scale", 1.0D, 0.5D, 3.0D, 0.1D));
   private final NumberSetting heightOffset = (NumberSetting)this.addSetting(new NumberSetting("Height offset", 0.75D, 0.0D, 3.0D, 0.05D));
   private final BooleanSetting brackets = (BooleanSetting)this.addSetting(new BooleanSetting("Brackets", true));
   private final BooleanSetting throughWalls = (BooleanSetting)this.addSetting(new BooleanSetting("Through walls", false));
   private final BooleanSetting skipNpcs = (BooleanSetting)this.addSetting(new BooleanSetting("Skip NPCs", true));
   private final BooleanSetting showSelf = (BooleanSetting)this.addSetting(new BooleanSetting("Show own", false));
   private final ColorSetting color = (ColorSetting)this.addSetting(new ColorSetting("Color", -22016));

   public ArmorSetTags() {
      super("Armor Set Tags", "Shows the armor set a player is wearing", ModuleCategory.MECHANIC);
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.mc.theWorld == null || this.mc.thePlayer == null) {
         return;
      }

      double max = this.range.get();
      int col = ColorUtil.withAlpha(this.color.getRGB(), 255);

      for(EntityPlayer p : new ArrayList<EntityPlayer>(this.mc.theWorld.playerEntities)) {
         if(p == this.mc.thePlayer && !this.showSelf.get()) {
            continue;
         }

         if(p.getDistanceToEntity(this.mc.thePlayer) > max || p.isInvisible()) {
            continue;
         }

         if(this.isNpc(p)) {
            continue;
         }

         String set = this.armorSetOf(p);
         if(set == null) {
            continue;
         }

         String label = this.brackets.get() ? "[" + set + "]" : set;
         // Sits above the vanilla nametag rather than replacing it.
         WorldRenderUtil.text3d(label, p.posX,
               p.posY + (double)p.height + this.heightOffset.get(), p.posZ,
               col, 0.025F * (float)this.scale.get());
      }

   }

   /** NPCs are player entities without a tab-list entry. */
   private boolean isNpc(EntityPlayer p) {
      if(!this.skipNpcs.get() || p == this.mc.thePlayer || this.mc.getNetHandler() == null) {
         return false;
      }

      return this.mc.getNetHandler().getPlayerInfo(p.getUniqueID()) == null;
   }

   /**
    * The set name shared by the player's custom armour, or null.
    *
    * <p>Requires {@code Min matching pieces} agreeing pieces. One custom item
    * is not a set -- that's how a decorative head became "[Head]".
    */
   private String armorSetOf(EntityPlayer p) {
      String common = null;
      int matched = 0;

      for(int slot = 1; slot <= 4; ++slot) {
         ItemStack s = p.getEquipmentInSlot(slot);
         if(s == null) {
            continue;
         }

         // Only real armour counts; a skull in the helmet slot is decoration.
         if(!(s.getItem() instanceof ItemArmor)) {
            continue;
         }

         String first = this.setWordOf(s);
         if(first == null) {
            continue;
         }

         if(common == null) {
            common = first;
            matched = 1;
         } else if(common.equalsIgnoreCase(first)) {
            ++matched;
         } else {
            return null;
         }
      }

      return common != null && matched >= (int)this.minPieces.get() ? common : null;
   }

   private String setWordOf(ItemStack stack) {
      try {
         String name = EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName());
         if(name == null) {
            return null;
         }

         String trimmed = name.trim();
         String lower = trimmed.toLowerCase();
         if(lower.startsWith("diamond") || lower.startsWith("iron") || lower.startsWith("gold")
               || lower.startsWith("golden") || lower.startsWith("leather") || lower.startsWith("chain")) {
            return null;
         }

         String first = trimmed.split("\\s+")[0];
         if(first.toLowerCase().endsWith("'s")) {
            first = first.substring(0, first.length() - 2);
         }

         return first.length() < 2 ? null : first;
      } catch (Throwable var6) {
         return null;
      }
   }
}
