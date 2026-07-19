package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.util.RenderUtil;

public class CompassModule extends HudModule {
   private static final int HEIGHT = 22;
   private static final int TICK_STEP = 15;
   private static final int LABEL_STEP = 15;
   private final NumberSetting width = (NumberSetting)this.addSetting(new NumberSetting("Width", 200.0D, 100.0D, 400.0D, 10.0D));
   private final NumberSetting span = (NumberSetting)this.addSetting(new NumberSetting("Degrees shown", 100.0D, 45.0D, 180.0D, 5.0D));
   private final BooleanSetting showHeading = (BooleanSetting)this.addSetting(new BooleanSetting("Show heading", true));
   private final BooleanSetting background = (BooleanSetting)this.addSetting(new BooleanSetting("Background", true));

   public CompassModule() {
      super("Compass", "Direction strip with cardinals and degrees", ModuleCategory.GENERAL, 0, HudModule.Anchor.TOP_LEFT);
   }

   public boolean isEmpty() {
      return this.mc.thePlayer == null;
   }

   public int getWidth() {
      return (int)this.width.get();
   }

   public int getHeight() {
      return 22;
   }

   private float heading() {
      float yaw = this.mc.thePlayer.rotationYaw % 360.0F;
      if(yaw < 0.0F) {
         yaw += 360.0F;
      }

      return yaw;
   }

   private String cardinal(int deg) {
      switch((deg % 360 + 360) % 360) {
      case 0:
         return "S";
      case 45:
         return "SW";
      case 90:
         return "W";
      case 135:
         return "NW";
      case 180:
         return "N";
      case 225:
         return "NE";
      case 270:
         return "E";
      case 315:
         return "SE";
      default:
         return null;
      }
   }

   public void render(int x, int y) {
      if(this.mc.thePlayer != null) {
         int w = this.getWidth();
         float centerHeading = this.heading();
         float degreesShown = (float)this.span.get();
         float pxPerDeg = (float)w / degreesShown;
         int centerX = x + w / 2;
         if(this.background.get()) {
            // Was passing a hard-coded plate colour, so the Background alpha
            // slider next to the toggle did nothing.
            RenderUtil.panel(x, y, x + w, y + 22, this.backgroundColor(), -14013902);
         }

         int first = (int)Math.floor((double)((centerHeading - degreesShown / 2.0F) / 15.0F)) * 15;
         int last = (int)Math.ceil((double)((centerHeading + degreesShown / 2.0F) / 15.0F)) * 15;

         for(int deg = first; deg <= last; deg += 15) {
            float delta = (float)deg - centerHeading;
            int tx = Math.round((float)centerX + delta * pxPerDeg);
            if(tx >= x + 2 && tx <= x + w - 2) {
               String label = this.cardinal(deg);
               boolean isCardinal = label != null;
               if(label == null && deg % 15 == 0) {
                  int shown = (deg % 360 + 360) % 360;
                  label = String.valueOf(shown);
               }

               int tickTop = y + 3;
               int tickBot = isCardinal?y + 9:y + 7;
               RenderUtil.rect(tx, tickTop, tx + 1, tickBot, isCardinal?-1250068:-7697773);
               if(label != null) {
                  int lw = this.mc.fontRendererObj.getStringWidth(label);
                  RenderUtil.text(this.mc.fontRendererObj, label, tx - lw / 2, y + 11, isCardinal?-1250068:-7697773);
               }
            }
         }

         RenderUtil.rect(centerX, y + 1, centerX + 1, y + 5, -4048054);
         if(this.showHeading.get()) {
            String h = String.valueOf(Math.round(centerHeading));
            int hw = this.mc.fontRendererObj.getStringWidth(h);
            this.drawStyled(h, centerX - hw / 2, y - 10);
         }

      }
   }
}
