package com.iceclient.module.modules.admin;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import com.iceclient.setting.BooleanSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loaded entities split by type: the F3 numbers that matter while raiding.
 *
 * <p>Separate from {@code EntityCountHud} and filed under Admin because the
 * player line is the sensitive part -- a raw total says nothing, but "Players:
 * 4" tells you someone is nearby through walls and across chunks.
 *
 * <p>Counts every loaded entity, which includes ones outside render distance,
 * so the totals will not match what you can see. That is the point: TNT
 * appearing in the count is the earliest warning a cannon has fired.
 */
public class EntityBreakdownHud extends TextHudModule {

   private final BooleanSetting showPlayers = this.addBool("Players", true);
   private final BooleanSetting showItems = this.addBool("Items", true);
   private final BooleanSetting showTnt = this.addBool("TNT", true);
   private final BooleanSetting hideZero = this.addBool("Hide empty rows", false);

   public EntityBreakdownHud() {
      super("Entity Breakdown", "Loaded entities split by type",
            ModuleCategory.ADMIN, HudModule.Anchor.TOP_LEFT, 130);
   }

   protected List<String> lines() {
      if(this.mc.theWorld == null) {
         return Collections.emptyList();
      }

      List<Entity> all = new ArrayList<Entity>(this.mc.theWorld.loadedEntityList);

      int players = 0;
      int items = 0;
      int tnt = 0;

      for(Entity e : all) {
         if(e instanceof EntityPlayer) {
            ++players;
         } else if(e instanceof EntityItem) {
            ++items;
         } else if(e instanceof EntityTNTPrimed) {
            ++tnt;
         }
      }

      List<String> out = new ArrayList<String>(4);
      out.add("Entities: " + all.size());

      if(this.showPlayers.get() && (players > 0 || !this.hideZero.get())) {
         out.add("Players: " + players);
      }

      if(this.showItems.get() && (items > 0 || !this.hideZero.get())) {
         out.add("Items: " + items);
      }

      if(this.showTnt.get() && (tnt > 0 || !this.hideZero.get())) {
         out.add("TNT: " + tnt);
      }

      return out;
   }
}
