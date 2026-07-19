package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;

public class CoordinatesModule extends HudModule {
   private static final int PAD = 4;
   private static final int LINE_H = 10;
   private static final float FONT_SCALE = 1.4F;
   private final BooleanSetting showFacing = (BooleanSetting)this.addSetting(new BooleanSetting("Show facing", true));

   public CoordinatesModule() {
      super("Coordinates", "Shows your XYZ position and facing", ModuleCategory.GENERAL, 0, HudModule.Anchor.BOTTOM_LEFT);
   }

   public boolean isEmpty() {
      return this.mc.thePlayer == null;
   }

   private String[] lines() {
      int x = (int)Math.floor(this.mc.thePlayer.posX);
      int y = (int)Math.floor(this.mc.thePlayer.posY);
      int z = (int)Math.floor(this.mc.thePlayer.posZ);
      return this.showFacing.get()?new String[]{"X: " + x, "Y: " + y, "Z: " + z, "Facing: " + this.facing()}:new String[]{"X: " + x, "Y: " + y, "Z: " + z};
   }

   private int baseWidth() {
      int w = 0;

      for(String line : this.lines()) {
         w = Math.max(w, this.mc.fontRendererObj.getStringWidth(line));
      }

      return w + 8;
   }

   private int baseHeight() {
      return this.lines().length * 10 + 8;
   }

   public int getWidth() {
      return this.mc.thePlayer == null?0:Math.round((float)this.baseWidth() * 1.4F);
   }

   public int getHeight() {
      return this.mc.thePlayer == null?0:Math.round((float)this.baseHeight() * 1.4F);
   }

   public void render(int x, int y) {
      if(this.mc.thePlayer != null) {
         String[] lines = this.lines();
         GlStateManager.pushMatrix();
         GlStateManager.translate((float)x, (float)y, 0.0F);
         GlStateManager.scale(1.4F, 1.4F, 1.0F);
         if(this.hasBackground()) {
            RenderUtil.panel(0, 0, this.baseWidth(), this.baseHeight(), this.backgroundColor(), -14013902);
         }

         for(int i = 0; i < lines.length; ++i) {
            // The facing line keeps its own muted colour; the coordinate lines
            // go through drawStyled so chroma and text shadow finally apply.
            if(this.showFacing.get() && i == lines.length - 1) {
               RenderUtil.text(this.mc.fontRendererObj, lines[i], 4, 4 + i * 10, -7697773);
            } else {
               this.drawStyled(lines[i], 4, 4 + i * 10);
            }
         }

         GlStateManager.popMatrix();
      }
   }

   private String facing() {
      float yaw = this.mc.thePlayer.rotationYaw % 360.0F;
      if(yaw < 0.0F) {
         yaw += 360.0F;
      }

      String[] dirs = new String[]{"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
      return dirs[Math.round(yaw / 45.0F) & 7];
   }
}
