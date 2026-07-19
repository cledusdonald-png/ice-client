package com.unclesam.client.module.modules.factions;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.module.modules.factions.SchematicModule;
import com.unclesam.client.schematica.SchematicaBridge;
import com.unclesam.client.setting.ModeSetting;
import com.unclesam.client.setting.NumberSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class MissingBlockEspModule extends Module {
   private final NumberSetting range = (NumberSetting)this.addSetting(new NumberSetting("Range", 24.0D, 4.0D, 64.0D, 1.0D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 1.5D, 1.0D, 4.0D, 0.5D));
   private final ModeSetting color = (ModeSetting)this.addSetting(new ModeSetting("Color", "Red", new String[]{"Red", "Orange", "Pink", "White"}));

   public MissingBlockEspModule() {
      super("MissingBlockESP", "Outlines schematic blocks you still need to place", ModuleCategory.FACTIONS, 0);
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(this.isEnabled() && this.mc.thePlayer != null) {
         double camX = this.mc.getRenderManager().viewerPosX;
         double camY = this.mc.getRenderManager().viewerPosY;
         double camZ = this.mc.getRenderManager().viewerPosZ;
         this.setupLineState();
         GL11.glLineWidth((float)this.lineWidth.get());
         float[] rgb = this.rgb();
         GlStateManager.color(rgb[0], rgb[1], rgb[2], 0.85F);
         GL11.glBegin(1);
         if(SchematicaBridge.isAvailable() && SchematicaBridge.hasSchematic()) {
            SchematicaBridge.forEachMissing(this.range.get(), (wx, wy, wz) -> {
               this.emitBoxEdges(wx - camX, wy - camY, wz - camZ);
            });
         } else {
            SchematicModule schem = SchematicModule.getInstance();
            if(schem != null && schem.hasSchematic()) {
               schem.forEachMissing(this.mc.thePlayer, this.range.get(), (wx, wy, wz) -> {
                  this.emitBoxEdges(wx - camX, wy - camY, wz - camZ);
               });
            }
         }

         GL11.glEnd();
         this.teardownLineState();
      }
   }

   private void emitBoxEdges(double x, double y, double z) {
      double x1 = x + 1.0D;
      double y1 = y + 1.0D;
      double z1 = z + 1.0D;
      this.line(x, y, z, x1, y, z);
      this.line(x1, y, z, x1, y, z1);
      this.line(x1, y, z1, x, y, z1);
      this.line(x, y, z1, x, y, z);
      this.line(x, y1, z, x1, y1, z);
      this.line(x1, y1, z, x1, y1, z1);
      this.line(x1, y1, z1, x, y1, z1);
      this.line(x, y1, z1, x, y1, z);
      this.line(x, y, z, x, y1, z);
      this.line(x1, y, z, x1, y1, z);
      this.line(x1, y, z1, x1, y1, z1);
      this.line(x, y, z1, x, y1, z1);
   }

   private void line(double ax, double ay, double az, double bx, double by, double bz) {
      GL11.glVertex3d(ax, ay, az);
      GL11.glVertex3d(bx, by, bz);
   }

   private float[] rgb() {
      return this.color.is("Orange")?new float[]{1.0F, 0.6F, 0.1F}:(this.color.is("Pink")?new float[]{1.0F, 0.3F, 0.7F}:(this.color.is("White")?new float[]{1.0F, 1.0F, 1.0F}:new float[]{1.0F, 0.2F, 0.2F}));
   }

   private void setupLineState() {
      GlStateManager.disableTexture2D();
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GL11.glEnable(2848);
   }

   private void teardownLineState() {
      GL11.glDisable(2848);
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
