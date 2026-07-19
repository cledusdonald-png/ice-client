package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class KeystrokesModule extends HudModule {
   private static final int BOX = 20;

   private final BooleanSetting showJump = (BooleanSetting)this.addSetting(new BooleanSetting("Show jump", true));
   private final BooleanSetting showSneak = (BooleanSetting)this.addSetting(new BooleanSetting("Show sneak", true));
   private final BooleanSetting showSprint = (BooleanSetting)this.addSetting(new BooleanSetting("Show sprint", false));
   private final BooleanSetting showMouse = (BooleanSetting)this.addSetting(new BooleanSetting("Show mouse buttons", false));
   private final BooleanSetting shortLabels = (BooleanSetting)this.addSetting(new BooleanSetting("Short labels", false));

   public KeystrokesModule() {
      super("Keystrokes", "Shows WASD and mouse clicks on screen", ModuleCategory.GENERAL, 0, HudModule.Anchor.TOP_LEFT);
      this.setEnabled(true);
   }

   /** Wide rows stacked under WASD, in draw order. Height follows this. */
   private int extraRows() {
      int rows = 0;
      if(this.showMouse.get()) {
         ++rows;
      }

      if(this.showJump.get()) {
         ++rows;
      }

      if(this.showSneak.get()) {
         ++rows;
      }

      if(this.showSprint.get()) {
         ++rows;
      }

      return rows;
   }

   public int getWidth() {
      return 60;
   }

   public int getHeight() {
      // Two rows of WASD plus however many wide rows are switched on, so
      // turning one off actually reclaims the space in the HUD editor.
      return 40 + this.extraRows() * BOX;
   }

   public void render(int x, int y) {
      FontRenderer font = this.mc.fontRendererObj;
      boolean w = this.mc.gameSettings.keyBindForward.isKeyDown();
      boolean a = this.mc.gameSettings.keyBindLeft.isKeyDown();
      boolean s = this.mc.gameSettings.keyBindBack.isKeyDown();
      boolean d = this.mc.gameSettings.keyBindRight.isKeyDown();
      this.drawKey(x + 20, y, "W", w, font);
      this.drawKey(x, y + 20, "A", a, font);
      this.drawKey(x + 20, y + 20, "S", s, font);
      this.drawKey(x + 40, y + 20, "D", d, font);

      int row = y + 40;

      if(this.showMouse.get()) {
         this.drawKey(x, row, "LMB", org.lwjgl.input.Mouse.isButtonDown(0), font);
         this.drawKeyWide(x + 20, row, 40, "RMB", org.lwjgl.input.Mouse.isButtonDown(1), font);
         row += BOX;
      }

      if(this.showJump.get()) {
         this.drawKeyWide(x, row, 60,
               this.shortLabels.get() ? "SPC" : "JUMP",
               this.mc.gameSettings.keyBindJump.isKeyDown(), font);
         row += BOX;
      }

      if(this.showSneak.get()) {
         this.drawKeyWide(x, row, 60,
               this.shortLabels.get() ? "SHF" : "SHIFT",
               this.mc.gameSettings.keyBindSneak.isKeyDown(), font);
         row += BOX;
      }

      if(this.showSprint.get()) {
         this.drawKeyWide(x, row, 60,
               this.shortLabels.get() ? "CTL" : "SPRINT",
               this.mc.gameSettings.keyBindSprint.isKeyDown(), font);
      }
   }

   private void drawKey(int x, int y, String label, boolean active, FontRenderer font) {
      this.drawKeyWide(x, y, 20, label, active, font);
   }

   /**
    * Draws one key box.
    *
    * <p>Pressed keys use {@link #styledColor()} so they pick up chroma; the
    * released grey stays fixed, since the contrast between the two states is
    * the entire point of the element.
    */
   private void drawKeyWide(int x, int y, int width, String label, boolean active, FontRenderer font) {
      int color = active?this.styledColor():5592405;

      if(this.hasBackground()) {
         // Pressed keys keep their brighter plate; released ones fall back to
         // the shared background alpha instead of a hard-coded one.
         this.drawRect(x, y, x + width - 1, y + 20 - 1, active?-2130706433:this.backgroundColor());
      }

      int tx = x + width / 2 - font.getStringWidth(label) / 2;

      // Not drawStyled(): that would substitute styledColor() and lose the
      // released-key grey. Shadow is honoured explicitly instead.
      if(this.textShadow.get()) {
         font.drawStringWithShadow(label, (float)tx, (float)(y + 10 - 4), color);
      } else {
         font.drawString(label, tx, y + 10 - 4, color);
      }
   }

   private void drawRect(int left, int top, int right, int bottom, int color) {
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      GlStateManager.color(r, g, b, a);
      GL11.glBegin(7);
      GL11.glVertex2d((double)left, (double)bottom);
      GL11.glVertex2d((double)right, (double)bottom);
      GL11.glVertex2d((double)right, (double)top);
      GL11.glVertex2d((double)left, (double)top);
      GL11.glEnd();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
