package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ModeSetting;
import com.iceclient.setting.NumberSetting;
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
   private final BooleanSetting hideNpcs = (BooleanSetting)this.addSetting(new BooleanSetting("Hide NPCs", true));
   private final BooleanSetting header = (BooleanSetting)this.addSetting(new BooleanSetting("Show header", true));
   private final ModeSetting headerLabel = (ModeSetting)this.addSetting(new ModeSetting("Header label", "Near", new String[]{"Near", "Nearby", "Players", "Enemies", "Count only"}));

   public FactionDisplayHud() {
      super("Nearby", "Lists nearby players with distance",
            ModuleCategory.HUD, HudModule.Anchor.TOP_RIGHT, 40);
   }

   /**
    * Whether an entity is a server NPC rather than a real player.
    *
    * <p>Shop and warp NPCs are spawned as player entities, so they land in
    * {@code playerEntities} and pad the list with names that are never a threat
    * -- the exact thing this HUD exists to show.
    *
    * <p>Three independent signals, because no single one is reliable:
    * <ul>
    *   <li><b>Absent from the tab list.</b> Real players always have an entry;
    *       NPCs are spawned without one. Strongest signal.</li>
    *   <li><b>UUID version 2.</b> Citizens -- what most servers use -- mints
    *       version-2 UUIDs, where Mojang accounts are version 4.</li>
    *   <li><b>Impossible name.</b> Colour codes, spaces or punctuation cannot
    *       occur in a real Minecraft name.</li>
    * </ul>
    */
   private boolean isNpc(EntityPlayer p) {
      java.util.UUID id = p.getUniqueID();

      if(id != null && id.version() == 2) {
         return true;
      }

      String name = p.getName();
      if(name == null || name.isEmpty() || name.length() > 16 || !NAME_OK.matcher(name).matches()) {
         return true;
      }

      // Checked last: it needs the net handler, which is null in singleplayer
      // and briefly during a server switch. Treat "cannot tell" as "real", so a
      // transient null never hides an actual player.
      if(this.mc.getNetHandler() == null || id == null) {
         return false;
      }

      return this.mc.getNetHandler().getPlayerInfo(id) == null;
   }

   /** Legal Minecraft names: 3-16 of letters, digits and underscore. */
   private static final java.util.regex.Pattern NAME_OK =
         java.util.regex.Pattern.compile("[A-Za-z0-9_]{1,16}");

   protected List<String> lines() {
      if(this.mc.theWorld == null || this.mc.thePlayer == null) {
         return Collections.emptyList();
      }

      double max = this.range.get();
      List<EntityPlayer> near = new ArrayList();
      for(EntityPlayer p : this.mc.theWorld.playerEntities) {
         if(p != this.mc.thePlayer && p.getDistanceToEntity(this.mc.thePlayer) <= max) {
            if(this.hideNpcs.get() && this.isNpc(p)) {
               continue;
            }

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
