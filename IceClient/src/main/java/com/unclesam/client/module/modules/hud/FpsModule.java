package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.util.RenderUtil;
import net.minecraft.client.Minecraft;

public class FpsModule extends HudModule {
   private static final int PAD = 4;
   private static final int LINE_H = 12;
   private final BooleanSetting showSuffix = (BooleanSetting)this.addSetting(new BooleanSetting("Show \'FPS\' label", true));

   public FpsModule() {
      super("FPS", "Shows current frames per second", ModuleCategory.GENERAL, 0, HudModule.Anchor.TOP_RIGHT);
   }

   private String label() {
      return Minecraft.getDebugFPS() + (this.showSuffix.get()?" FPS":"");
   }

   public int getWidth() {
      return this.mc.fontRendererObj.getStringWidth(this.label()) + 8;
   }

   public int getHeight() {
      return 16;
   }

   public void render(int x, int y) {
      // Was drawing a hard-coded plate and colour, which meant the inherited
      // Background / Chroma / Text shadow settings showed in the GUI but did
      // nothing. Going through the HudModule helpers makes them apply.
      if(this.hasBackground()) {
         RenderUtil.panel(x, y, x + this.getWidth(), y + this.getHeight(), this.backgroundColor(), -14013902);
      }

      this.drawStyled(this.label(), x + 4, y + 4);
   }
}
