package com.unclesam.client.module.modules.groups;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ColorSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.ColorUtil;
import com.unclesam.client.util.WorldRenderUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Highlights one named player so you can keep track of a focus target in a
 * crowded fight. Set the name with the in-game command or the setting.
 */
public class FocusPlayer extends Module {

   private final NumberSetting range = (NumberSetting)this.addSetting(new NumberSetting("Range", 128.0D, 16.0D, 256.0D, 16.0D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final BooleanSetting tracer = (BooleanSetting)this.addSetting(new BooleanSetting("Tracer", true));
   private final BooleanSetting showName = (BooleanSetting)this.addSetting(new BooleanSetting("Show name", true));
   private final BooleanSetting showHealth = (BooleanSetting)this.addSetting(new BooleanSetting("Show health", true));
   private final ColorSetting color = (ColorSetting)this.addSetting(new ColorSetting("Color", -65281));

   /** Set at runtime; empty means "no focus target". */
   private static String focused = "";

   public FocusPlayer() {
      super("Focus Player", "Highlights a chosen player", ModuleCategory.FACTIONS);
   }

   public static void setFocus(String name) {
      focused = name == null ? "" : name.trim();
   }

   public static String getFocus() {
      return focused;
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.mc.theWorld == null || this.mc.thePlayer == null || focused.isEmpty()) {
         return;
      }

      int col = ColorUtil.withAlpha(this.color.getRGB(), 220);
      float w = (float)this.lineWidth.get();
      double max = this.range.get();

      for(EntityPlayer p : this.mc.theWorld.playerEntities) {
         if(p == this.mc.thePlayer || !p.getName().equalsIgnoreCase(focused)) {
            continue;
         }

         if(p.getDistanceToEntity(this.mc.thePlayer) > max) {
            continue;
         }

         AxisAlignedBB box = new AxisAlignedBB(
               p.posX - 0.4D, p.posY, p.posZ - 0.4D,
               p.posX + 0.4D, p.posY + 1.8D, p.posZ + 0.4D);
         WorldRenderUtil.outlineBox(box, col, w);

         if(this.tracer.get()) {
            // From just under the camera so the line doesn't fill the screen.
            WorldRenderUtil.drawLine(
                  WorldRenderUtil.camX(), WorldRenderUtil.camY() - 0.3D, WorldRenderUtil.camZ(),
                  p.posX, p.posY + 0.9D, p.posZ, ColorUtil.withAlpha(this.color.getRGB(), 140), w);
         }

         if(this.showName.get() || this.showHealth.get()) {
            StringBuilder sb = new StringBuilder();
            if(this.showName.get()) {
               sb.append(p.getName());
            }

            if(this.showHealth.get()) {
               if(sb.length() > 0) {
                  sb.append(' ');
               }

               sb.append((int)Math.ceil((double)p.getHealth())).append("hp");
            }

            WorldRenderUtil.text3d(sb.toString(), p.posX, p.posY + 2.2D, p.posZ, col, 0.025F);
         }
      }

   }
}
