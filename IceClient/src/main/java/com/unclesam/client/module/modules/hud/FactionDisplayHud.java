package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.module.TextHudModule;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ModeSetting;
import com.unclesam.client.setting.NumberSetting;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Nearby players with distance and health -- who's around while you're raiding. */
public class FactionDisplayHud extends TextHudModule {

   private final NumberSetting range = (NumberSetting)this.addSetting(new NumberSetting("Range", 64.0D, 8.0D, 256.0D, 8.0D));
   private final NumberSetting maxShown = (NumberSetting)this.addSetting(new NumberSetting("Max shown", 8.0D, 1.0D, 20.0D, 1.0D));
   private final BooleanSetting showHealth = (BooleanSetting)this.addSetting(new BooleanSetting("Show health", false));
   private final BooleanSetting showDistance = (BooleanSetting)this.addSetting(new BooleanSetting("Show distance", true));
   private final BooleanSetting hideEmpty = (BooleanSetting)this.addSetting(new BooleanSetting("Hide when empty", true));
   private final BooleanSetting header = (BooleanSetting)this.addSetting(new BooleanSetting("Show header", true));
   private final ModeSetting headerLabel = (ModeSetting)this.addSetting(new ModeSetting("Header label", "Near", new String[]{"Near", "Nearby", "Players", "Enemies", "Count only"}));

   public FactionDisplayHud() {
      super("Nearby", "Lists nearby players with distance",
            ModuleCategory.HUD, HudModule.Anchor.TOP_RIGHT, 40);
   }

   protected List<String> lines() {
      if(this.mc.theWorld == null || this.mc.thePlayer == null) {
         return Collections.emptyList();
      }

      double max = this.range.get();
      List<EntityPlayer> near = new ArrayList();
      for(EntityPlayer p : this.mc.theWorld.playerEntities) {
         if(p != this.mc.thePlayer && p.getDistanceToEntity(this.mc.thePlayer) <= max) {
            near.add(p);
         }
      }

      if(near.isEmpty() && this.hideEmpty.get()) {
         return Collections.emptyList();
      }

      // Closest first -- the one about to hit you matters most.
      Collections.sort(near, new Comparator<EntityPlayer>() {
         public int compare(EntityPlayer a, EntityPlayer b) {
            return Double.compare(a.getDistanceToEntity(FactionDisplayHud.this.mc.thePlayer),
                                  b.getDistanceToEntity(FactionDisplayHud.this.mc.thePlayer));
         }
      });

      List<String> out = new ArrayList();
      if(this.header.get()) {
         String label = this.headerLabel.get();
         out.add("Count only".equals(label) ? String.valueOf(near.size()) : label + ": " + near.size());
      }

      int limit = (int)this.maxShown.get();
      for(int i = 0; i < near.size() && i < limit; ++i) {
         EntityPlayer p = near.get(i);
         StringBuilder sb = new StringBuilder(p.getName());
         if(this.showDistance.get()) {
            sb.append(' ').append((int)p.getDistanceToEntity(this.mc.thePlayer)).append('m');
         }

         if(this.showHealth.get()) {
            // Plain "hp" rather than a heart glyph -- the ♥ renders as a box in
            // some resource packs, which is what made the list look broken.
            sb.append(' ').append((int)Math.ceil((double)p.getHealth())).append("hp");
         }

         out.add(sb.toString());
      }

      return out;
   }
}
