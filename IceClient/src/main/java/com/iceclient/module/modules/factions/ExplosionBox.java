package com.iceclient.module.modules.factions;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.ColorUtil;
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

/**
 * Draws each primed TNT's blast radius, so you can see what a volley is about
 * to reach before it lands.
 */
public class ExplosionBox extends Module {

   private final NumberSetting blastRadius = (NumberSetting)this.addSetting(new NumberSetting("Blast radius", 4.0D, 1.0D, 12.0D, 0.5D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 1.5D, 1.0D, 5.0D, 0.5D));
   private final BooleanSetting fill = (BooleanSetting)this.addSetting(new BooleanSetting("Fill", false));
   private final NumberSetting fillAlpha = (NumberSetting)this.addSetting(new NumberSetting("Fill alpha", 40.0D, 0.0D, 255.0D, 5.0D));
   private final BooleanSetting showFuse = (BooleanSetting)this.addSetting(new BooleanSetting("Show fuse", true));
   private final ColorSetting color = (ColorSetting)this.addSetting(new ColorSetting("Color", -1147124));

   public ExplosionBox() {
      super("Explosion Box", "Shows the blast radius of primed TNT", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.mc.theWorld == null) {
         return;
      }

      double r = this.blastRadius.get();
      int base = this.color.getRGB();
      float w = (float)this.lineWidth.get();

      for(Entity e : new ArrayList<Entity>(this.mc.theWorld.loadedEntityList)) {
         if(!(e instanceof EntityTNTPrimed)) {
            continue;
         }

         EntityTNTPrimed tnt = (EntityTNTPrimed)e;
         AxisAlignedBB box = new AxisAlignedBB(
               tnt.posX - r, tnt.posY - r, tnt.posZ - r,
               tnt.posX + r, tnt.posY + r, tnt.posZ + r);

         if(this.fill.get()) {
            WorldRenderUtil.filledBox(box, ColorUtil.withAlpha(base, (int)this.fillAlpha.get()));
         }

         WorldRenderUtil.outlineBox(box, ColorUtil.withAlpha(base, 220), w);

         if(this.showFuse.get()) {
            // 1.8.9 exposes fuse as a public field, not a getter.
            int ticks = tnt.fuse;
            WorldRenderUtil.text3d(String.format("%.1fs", Float.valueOf((float)ticks / 20.0F)),
                  tnt.posX, tnt.posY + 0.8D, tnt.posZ, ColorUtil.withAlpha(base, 255), 0.02F);
         }
      }

   }
}
