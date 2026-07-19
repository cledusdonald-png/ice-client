package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ColorSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

/**
 * Active potion effects as icon + name, with the remaining time beneath.
 *
 * <p>Icons come from vanilla's inventory sheet: an 8-wide grid of 18x18 sprites
 * starting at (0, 198), indexed by {@link Potion#getStatusIconIndex()}.
 */
public class PotionStatusModule extends HudModule {

   private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
   private static final int ICON = 18;
   private static final int ROW_H = 20;
   private static final int PAD = 2;

   private final BooleanSetting showIcons = (BooleanSetting)this.addSetting(new BooleanSetting("Show icons", true));
   private final BooleanSetting showTimer = (BooleanSetting)this.addSetting(new BooleanSetting("Show timer", true));
   private final BooleanSetting showAmplifier = (BooleanSetting)this.addSetting(new BooleanSetting("Show level", true));
   private final BooleanSetting hideAmbient = (BooleanSetting)this.addSetting(new BooleanSetting("Hide beacon effects", false));
   private final ColorSetting nameColor = (ColorSetting)this.addSetting(new ColorSetting("Name color", -1));
   private final ColorSetting timerColor = (ColorSetting)this.addSetting(new ColorSetting("Timer color", -11141291));

   /** Reaches vanilla's protected textured-rect helper from inside a Gui subclass. */
   private static final class Sprites extends Gui {
      void draw(int x, int y, int u, int v, int w, int h) {
         this.drawTexturedModalRect(x, y, u, v, w, h);
      }
   }

   private static final Sprites SPRITES = new Sprites();

   public PotionStatusModule() {
      super("Potion Status", "Lists active potion effects and their timers",
            ModuleCategory.GENERAL, 0, HudModule.Anchor.TOP_RIGHT, 24);
   }

   private List<PotionEffect> effects() {
      List<PotionEffect> out = new ArrayList();
      if(this.mc.thePlayer == null) {
         return out;
      }

      for(PotionEffect e : this.mc.thePlayer.getActivePotionEffects()) {
         if(this.hideAmbient.get() && e.getIsAmbient()) {
            continue;
         }

         out.add(e);
      }

      return out;
   }

   public int getWidth() {
      List<PotionEffect> list = this.effects();
      if(list.isEmpty()) {
         return 0;
      }

      int w = 0;
      for(PotionEffect e : list) {
         w = Math.max(w, this.mc.fontRendererObj.getStringWidth(this.nameOf(e)));
         if(this.showTimer.get()) {
            w = Math.max(w, this.mc.fontRendererObj.getStringWidth(this.timeOf(e)));
         }
      }

      return w + (this.showIcons.get() ? ICON + 4 : 0) + PAD * 2;
   }

   public int getHeight() {
      List<PotionEffect> list = this.effects();
      return list.isEmpty() ? 0 : list.size() * ROW_H + PAD * 2;
   }

   public void render(int x, int y) {
      List<PotionEffect> list = this.effects();
      if(list.isEmpty()) {
         return;
      }

      if(this.hasBackground()) {
         Gui.drawRect(x, y, x + this.getWidth(), y + this.getHeight(), this.backgroundColor());
      }

      int textX = x + PAD + (this.showIcons.get() ? ICON + 4 : 0);
      int rowY = y + PAD;

      for(PotionEffect e : list) {
         Potion potion = Potion.potionTypes[e.getPotionID()];

         if(this.showIcons.get() && potion != null && potion.hasStatusIcon()) {
            int idx = potion.getStatusIconIndex();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            this.mc.getTextureManager().bindTexture(INVENTORY);
            // 8 sprites per row, sheet origin (0, 198).
            SPRITES.draw(x + PAD, rowY, idx % 8 * ICON, 198 + idx / 8 * ICON, ICON, ICON);
            GlStateManager.disableBlend();
         }

         boolean timed = this.showTimer.get();
         int nameY = timed ? rowY + 1 : rowY + (ROW_H - 8) / 2;
         this.drawLine(this.nameOf(e), textX, nameY, this.styledColorOr(this.nameColor.getRGB()));

         if(timed) {
            this.drawLine(this.timeOf(e), textX, rowY + 11, this.timerColor.getRGB());
         }

         rowY += ROW_H;
      }

   }

   private void drawLine(String s, int x, int y, int col) {
      if(this.textShadow.get()) {
         this.mc.fontRendererObj.drawStringWithShadow(s, (float)x, (float)y, col);
      } else {
         this.mc.fontRendererObj.drawString(s, x, y, col);
      }

   }

   /** Chroma, when enabled, overrides the per-element colour. */
   private int styledColorOr(int fallback) {
      return this.textChroma.get() ? this.styledColor() : fallback;
   }

   private String nameOf(PotionEffect e) {
      Potion potion = Potion.potionTypes[e.getPotionID()];
      String name = potion == null ? "Effect" : I18n.format(potion.getName(), new Object[0]);
      if(this.showAmplifier.get()) {
         name = name + " " + roman(e.getAmplifier() + 1);
      }

      return name;
   }

   private String timeOf(PotionEffect e) {
      int secs = e.getDuration() / 20;
      return String.format("%d:%02d", Integer.valueOf(secs / 60), Integer.valueOf(secs % 60));
   }

   private static String roman(int n) {
      switch(n) {
      case 1:
         return "I";
      case 2:
         return "II";
      case 3:
         return "III";
      case 4:
         return "IV";
      case 5:
         return "V";
      default:
         return String.valueOf(n);
      }
   }
}
