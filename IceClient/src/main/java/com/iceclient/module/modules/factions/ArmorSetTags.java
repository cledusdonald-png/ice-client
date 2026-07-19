package com.iceclient.module.modules.factions;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.ColorUtil;
import com.iceclient.util.WorldRenderUtil;
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
   private final BooleanSetting tierFallback = (BooleanSetting)this.addSetting(new BooleanSetting("Fall back to lore tier", true));
   private final BooleanSetting skipNpcs = (BooleanSetting)this.addSetting(new BooleanSetting("Skip NPCs", true));
   private final BooleanSetting showSelf = (BooleanSetting)this.addSetting(new BooleanSetting("Show own", false));
   private final ColorSetting color = (ColorSetting)this.addSetting(new ColorSetting("Color", -22016));

   public ArmorSetTags() {
      super("Armor Set Tags", "Shows the armor set a player is wearing", ModuleCategory.FACTIONS);
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
         // Sits above the vanilla nametag rather than replacing it. Positioned
         // from the interpolated render position for the same reason the hit
         // boxes are: a label pinned to the 20Hz tick position visibly lags the
         // player it is labelling.
         float pt = event.partialTicks;
         WorldRenderUtil.text3d(label,
               WorldRenderUtil.renderX(p, pt),
               WorldRenderUtil.renderY(p, pt) + (double)p.height + this.heightOffset.get(),
               WorldRenderUtil.renderZ(p, pt),
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
            common = null;
            break;
         }
      }

      if(common != null && matched >= (int)this.minPieces.get()) {
         return common;
      }

      return this.tierFallback.get() ? this.tierOf(p) : null;
   }

   /**
    * Falls back to the tier written in the armour's lore.
    *
    * <p>Name-word matching assumes every piece of a set shares a first word.
    * That breaks the moment a server leaves one piece vanilla-named, or names
    * pieces "Tier 3 Helmet" -- then the shared word is "Tier" and the real
    * information is the number after it. Minecadia writes a tier line in lore,
    * which is both more reliable and exactly what the Minecadia module already
    * reads for its in-slot labels.
    *
    * <p>Other players' armour arrives with full NBT on the equipment packet, so
    * their lore is readable client-side just like your own.
    */
   private String tierOf(EntityPlayer p) {
      String common = null;
      int matched = 0;

      for(int slot = 1; slot <= 4; ++slot) {
         ItemStack s = p.getEquipmentInSlot(slot);
         if(s == null || !s.hasTagCompound()) {
            continue;
         }

         String tier = null;

         try {
            for(String line : s.getTooltip(this.mc.thePlayer, false)) {
               String clean = EnumChatFormatting.getTextWithoutFormattingCodes(line);
               if(clean != null && clean.toLowerCase().contains("tier")) {
                  java.util.regex.Matcher m = TIER.matcher(clean);
                  if(m.find()) {
                     tier = "T" + m.group(1);
                  }

                  break;
               }
            }
         } catch (Throwable ignored) {
            continue;
         }

         if(tier == null) {
            continue;
         }

         if(common == null) {
            common = tier;
            matched = 1;
         } else if(common.equals(tier)) {
            ++matched;
         }
      }

      return matched >= (int)this.minPieces.get() ? common : null;
   }

   private static final java.util.regex.Pattern TIER =
         java.util.regex.Pattern.compile("tier\\s*(\\d{1,2})", java.util.regex.Pattern.CASE_INSENSITIVE);

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
