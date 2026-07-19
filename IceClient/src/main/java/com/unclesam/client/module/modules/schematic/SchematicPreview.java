package com.unclesam.client.module.modules.schematic;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.schematic.Schematic;
import com.unclesam.client.schematic.SchematicManager;
import com.unclesam.client.schematic.SchematicRenderer;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.WorldRenderUtil;
import java.awt.Color;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class SchematicPreview extends Module {
   private final BooleanSetting outline = this.addBool("Bounding Box", true);
   private final NumberSetting rebuildTicks = this.addNumber("Refresh (ticks)", 20.0D, 5.0D, 100.0D, 5.0D);
   private int ticks;

   public SchematicPreview() {
      super("Schematic Preview", "Ghost overlay of the loaded schematic", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.END) {
         if(SchematicManager.isLoaded()) {
            if(++this.ticks >= this.rebuildTicks.getInt()) {
               this.ticks = 0;
               SchematicRenderer.invalidate();
            }

         }
      }
   }

   @SubscribeEvent
   public void onRender(RenderWorldLastEvent event) {
      // "Render: OFF" has to silence this too. Ice draws its own preview
      // independently of Schematica's renderer, so honouring only Schematica's
      // flag left this wireframe on screen with the toggle reading OFF -- the
      // toggle looked broken because it was only turning off half the drawing.
      if(com.unclesam.client.schematica.SchematicaBridge.isAvailable()
            && !com.unclesam.client.schematica.SchematicaBridge.isRendering()) {
         return;
      }

      if(this.isEnabled() && SchematicManager.isLoaded() && this.mc.theWorld != null) {
         SchematicRenderer.render();
         if(this.outline.get()) {
            Schematic s = SchematicManager.getLoaded();
            BlockPos o = s.getOrigin();
            AxisAlignedBB bb = new AxisAlignedBB((double)o.getX(), (double)o.getY(), (double)o.getZ(), (double)(o.getX() + s.getRenderWidth()), (double)(o.getY() + s.getHeight()), (double)(o.getZ() + s.getRenderLength()));
            WorldRenderUtil.outlineBox(bb, (new Color(90, 170, 255)).getRGB(), 2.0F);
         }

      }
   }
}
