package com.iceclient.module;

import com.iceclient.gui.HudEditorScreen;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.ColorUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public abstract class HudModule extends Module {
   protected static final int MARGIN = 4;
   private static final int UNSET = Integer.MIN_VALUE;
   public static final float MIN_SCALE = 0.5F;
   public static final float MAX_SCALE = 2.5F;
   private final HudModule.Anchor anchor;
   private final int anchorOffsetY;
   private int posX;
   private int posY;
   private float scale;
   private int color;

   /**
    * Shared styling every HUD element gets for free. Declared on the base class
    * rather than per-module so chroma / text shadow / background behave
    * identically across the whole HUD instead of each module reinventing them.
    */
   protected final BooleanSetting textShadow;
   protected final BooleanSetting textChroma;
   protected final BooleanSetting background;
   protected final NumberSetting backgroundAlpha;

   protected HudModule(String name, String description, ModuleCategory category, int defaultKey, HudModule.Anchor anchor) {
      this(name, description, category, defaultKey, anchor, 0);
   }

   protected HudModule(String name, String description, ModuleCategory category, int defaultKey, HudModule.Anchor anchor, int anchorOffsetY) {
      super(name, description, category, defaultKey);
      this.posX = Integer.MIN_VALUE;
      this.posY = Integer.MIN_VALUE;
      this.scale = 1.0F;
      this.color = -1;
      this.anchor = anchor;
      this.anchorOffsetY = anchorOffsetY;
      this.textShadow = (BooleanSetting)this.addSetting(new BooleanSetting("Text shadow", true));
      this.textChroma = (BooleanSetting)this.addSetting(new BooleanSetting("Chroma", false));
      this.background = (BooleanSetting)this.addSetting(new BooleanSetting("Background", true));
      this.backgroundAlpha = (NumberSetting)this.addSetting(new NumberSetting("Background alpha", 150.0D, 0.0D, 255.0D, 5.0D));
   }

   /** This element's colour, or a cycling chroma when that's switched on. */
   public int styledColor() {
      return this.textChroma.get() ? ColorUtil.chroma(0) : this.getColor();
   }

   public boolean hasBackground() {
      return this.background.get();
   }

   /** Black at the configured alpha -- the plate drawn behind an element. */
   public int backgroundColor() {
      return ColorUtil.withAlpha(0, (int)this.backgroundAlpha.get());
   }

   /** Draws text honouring this element's shadow + chroma settings. */
   protected void drawStyled(String text, int x, int y) {
      if(this.textShadow.get()) {
         this.mc.fontRendererObj.drawStringWithShadow(text, (float)x, (float)y, this.styledColor());
      } else {
         this.mc.fontRendererObj.drawString(text, x, y, this.styledColor());
      }
   }

   public abstract int getWidth();

   public abstract int getHeight();

   public abstract void render(int var1, int var2);

   public void renderScaled(int x, int y) {
      if(this.scale == 1.0F) {
         this.render(x, y);
      } else {
         GlStateManager.pushMatrix();
         GlStateManager.translate((float)x, (float)y, 0.0F);
         GlStateManager.scale(this.scale, this.scale, 1.0F);
         this.render(0, 0);
         GlStateManager.popMatrix();
      }
   }

   public float getScale() {
      return this.scale;
   }

   public void setScale(float s) {
      this.scale = Math.max(0.5F, Math.min(2.5F, s));
   }

   public int getColor() {
      return this.color;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public int getScaledWidth() {
      return Math.max(1, Math.round((float)this.getWidth() * this.scale));
   }

   public int getScaledHeight() {
      return Math.max(1, Math.round((float)this.getHeight() * this.scale));
   }

   public boolean isEmpty() {
      return this.getWidth() <= 0 || this.getHeight() <= 0;
   }

   public int getPosX() {
      this.ensurePlaced();
      return this.posX;
   }

   public int getPosY() {
      this.ensurePlaced();
      return this.posY;
   }

   public void setPos(int x, int y) {
      this.posX = x;
      this.posY = y;
   }

   public boolean hasPos() {
      return this.posX != Integer.MIN_VALUE;
   }

   private void ensurePlaced() {
      if(this.posX == Integer.MIN_VALUE) {
         ScaledResolution res = new ScaledResolution(this.mc);
         int w = Math.max(1, this.getWidth());
         int h = Math.max(1, this.getHeight());
         switch(this.anchor) {
         case TOP_RIGHT:
            this.posX = res.getScaledWidth() - 4 - w;
            this.posY = 4 + this.anchorOffsetY;
            break;
         case BOTTOM_LEFT:
            this.posX = 4;
            this.posY = res.getScaledHeight() - 4 - h;
            break;
         case BOTTOM_RIGHT:
            this.posX = res.getScaledWidth() - 4 - w;
            this.posY = res.getScaledHeight() - 4 - h;
            break;
         case TOP_LEFT:
         default:
            this.posX = 4;
            this.posY = 4 + this.anchorOffsetY;
         }

      }
   }

   public void clampToScreen() {
      ScaledResolution res = new ScaledResolution(this.mc);
      int maxX = Math.max(0, res.getScaledWidth() - this.getScaledWidth());
      int maxY = Math.max(0, res.getScaledHeight() - this.getScaledHeight());
      if(this.getPosX() > maxX) {
         this.posX = maxX;
      }

      if(this.getPosY() > maxY) {
         this.posY = maxY;
      }

      if(this.posX < 0) {
         this.posX = 0;
      }

      if(this.posY < 0) {
         this.posY = 0;
      }

   }

   @SubscribeEvent
   public void onRenderOverlay(Post event) {
      if(this.isEnabled()) {
         if(event.type == ElementType.ALL) {
            if(!(this.mc.currentScreen instanceof HudEditorScreen)) {
               if(!this.isEmpty()) {
                  this.clampToScreen();
                  this.renderScaled(this.getPosX(), this.getPosY());
               }
            }
         }
      }
   }

   public static enum Anchor {
      TOP_LEFT,
      TOP_RIGHT,
      BOTTOM_LEFT,
      BOTTOM_RIGHT;

      private Anchor() {
      }
   }
}
