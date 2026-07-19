package com.iceclient.module.modules.factions;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Audio cues for raid events -- TNT lighting, sand landing, players entering
 * range -- so you don't have to be looking the right way to notice.
 *
 * <p>Each cue is rate-limited: a cannon volley spawns dozens of entities per
 * second and an unthrottled ping per entity is unusable.
 */
public class RaidingSounds extends Module {

   private final BooleanSetting onTnt = (BooleanSetting)this.addSetting(new BooleanSetting("TNT primed", true));
   private final BooleanSetting onSand = (BooleanSetting)this.addSetting(new BooleanSetting("Sand spawned", false));
   private final BooleanSetting onPlayer = (BooleanSetting)this.addSetting(new BooleanSetting("Player nearby", true));
   private final NumberSetting playerRange = (NumberSetting)this.addSetting(new NumberSetting("Player range", 48.0D, 8.0D, 128.0D, 8.0D));
   private final NumberSetting cooldown = (NumberSetting)this.addSetting(new NumberSetting("Cooldown (ms)", 400.0D, 50.0D, 3000.0D, 50.0D));
   private final NumberSetting volume = (NumberSetting)this.addSetting(new NumberSetting("Volume", 1.0D, 0.1D, 2.0D, 0.1D));
   private final NumberSetting pitch = (NumberSetting)this.addSetting(new NumberSetting("Pitch", 1.0D, 0.5D, 2.0D, 0.1D));

   private long lastTnt;
   private long lastSand;
   private long lastPlayer;

   public RaidingSounds() {
      super("Raiding Sounds", "Plays audio cues for raid events", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onEntityJoin(EntityJoinWorldEvent event) {
      if(!this.isEnabled() || this.mc.thePlayer == null || event.world != this.mc.theWorld) {
         return;
      }

      Entity e = event.entity;
      long now = System.currentTimeMillis();
      long gap = (long)this.cooldown.get();

      if(e instanceof EntityTNTPrimed && this.onTnt.get()) {
         if(now - this.lastTnt >= gap) {
            this.lastTnt = now;
            this.play("random.orb");
         }
      } else if(e instanceof EntityFallingBlock && this.onSand.get()) {
         if(now - this.lastSand >= gap) {
            this.lastSand = now;
            this.play("random.click");
         }
      } else if(e instanceof EntityPlayer && e != this.mc.thePlayer && this.onPlayer.get()) {
         if(e.getDistanceToEntity(this.mc.thePlayer) <= this.playerRange.get() && now - this.lastPlayer >= gap) {
            this.lastPlayer = now;
            this.play("note.pling");
         }
      }

   }

   private void play(String sound) {
      if(this.mc.thePlayer == null) {
         return;
      }

      this.mc.thePlayer.playSound(sound, (float)this.volume.get(), (float)this.pitch.get());
   }
}
