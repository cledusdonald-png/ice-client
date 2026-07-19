package com.iceclient.module.modules.visual;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.BoxBatch;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TntTimerModule extends Module {
   private final NumberSetting range = (NumberSetting)this.addSetting(new NumberSetting("Range", 64.0D, 8.0D, 128.0D, 4.0D));
   private final BooleanSetting ticks = (BooleanSetting)this.addSetting(new BooleanSetting("Show ticks", false));
   private final BooleanSetting box = (BooleanSetting)this.addSetting(new BooleanSetting("Box TNT", true));
   private final BooleanSetting throughWalls = (BooleanSetting)this.addSetting(new BooleanSetting("Through walls", true));
   private final BoxBatch batch = new BoxBatch();

   public TntTimerModule() {
      super("TNT Timer", "Fuse countdown + batched box on primed TNT", ModuleCategory.FACTIONS, 0);
      this.box.inSection("FPS");
      this.throughWalls.inSection("FPS");
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(this.isEnabled() && this.mc.thePlayer != null && this.mc.theWorld != null) {
         double camX = this.mc.getRenderManager().viewerPosX;
         double camY = this.mc.getRenderManager().viewerPosY;
         double camZ = this.mc.getRenderManager().viewerPosZ;
         double maxSq = this.range.get() * this.range.get();

         for(Entity entity : this.mc.theWorld.loadedEntityList) {
            if(entity instanceof EntityTNTPrimed) {
               EntityTNTPrimed tnt = (EntityTNTPrimed)entity;
               if(this.mc.thePlayer.getDistanceSqToEntity(tnt) <= maxSq) {
                  int fuse = tnt.fuse;
                  if(this.box.get()) {
                     float[] rgb = this.rgbFor(fuse);
                     AxisAlignedBB bb = tnt.getEntityBoundingBox();
                     this.batch.add(bb.minX - camX, bb.minY - camY, bb.minZ - camZ, bb.maxX - camX, bb.maxY - camY, bb.maxZ - camZ, rgb[0], rgb[1], rgb[2], 0.22F, 0.9F);
                  }

                  String text = this.ticks.get()?fuse + "t":String.format("%.1f", new Object[]{Float.valueOf((float)fuse / 20.0F)}) + "s";
                  this.drawLabel(tnt.posX - camX, tnt.posY + 0.9D - camY, tnt.posZ - camZ, text, this.colorFor(fuse));
               }
            }
         }

         this.batch.flush(1.5F, this.throughWalls.get());
      }
   }

   private float[] rgbFor(int fuse) {
      return fuse <= 20?new float[]{1.0F, 0.25F, 0.25F}:(fuse <= 40?new float[]{1.0F, 0.75F, 0.25F}:new float[]{0.93F, 0.93F, 0.93F});
   }

   private int colorFor(int fuse) {
      return fuse <= 20?-49088:(fuse <= 40?-16320:-1250068);
   }

   private void drawLabel(double x, double y, double z, String text, int color) {
      GlStateManager.pushMatrix();
      GlStateManager.translate(x, y, z);
      GlStateManager.rotate(-this.mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(this.mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
      float scale = 0.025F;
      GlStateManager.scale(-scale, -scale, scale);
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.enableTexture2D();
      int w = this.mc.fontRendererObj.getStringWidth(text);
      Gui.drawRect(-w / 2 - 2, -2, w / 2 + 2, 9, Integer.MIN_VALUE);
      this.mc.fontRendererObj.drawStringWithShadow(text, (float)(-w) / 2.0F, 0.0F, color);
      GlStateManager.disableBlend();
      GlStateManager.enableDepth();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }
}
