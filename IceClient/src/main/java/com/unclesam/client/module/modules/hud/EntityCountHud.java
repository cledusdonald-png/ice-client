package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.module.TextHudModule;
import com.unclesam.client.setting.BooleanSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loaded entity counts -- the F3 numbers that actually matter while raiding. */
public class EntityCountHud extends TextHudModule {

   private final BooleanSetting split = (BooleanSetting)this.addSetting(new BooleanSetting("Split by type", true));

   public EntityCountHud() {
      super("Entity Count", "Counts loaded entities", ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 110);
   }

   protected List<String> lines() {
      if(this.mc.theWorld == null) {
         return Collections.emptyList();
      }

      List<Entity> all = new ArrayList(this.mc.theWorld.loadedEntityList);
      if(!this.split.get()) {
         return Collections.singletonList("Entities: " + all.size());
      }

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

      List<String> out = new ArrayList(4);
      out.add("Entities: " + all.size());
      out.add("Players: " + players);
      out.add("Items: " + items);
      out.add("TNT: " + tnt);
      return out;
   }
}
