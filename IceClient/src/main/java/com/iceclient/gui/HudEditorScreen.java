package com.iceclient.gui;

import com.iceclient.config.ConfigManager;
import com.iceclient.module.HudModule;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public class HudEditorScreen extends GuiScreen {
   private static final int PLACEHOLDER_W = 60;
   private static final int PLACEHOLDER_H = 14;
   private static final int ACCENT = -10696961;
   private static final int OUTLINE_IDLE = 1079822079;
   private static final int PANEL_BG = -267446493;
   private static final int PANEL_DIV = -14799814;
   private static final int TRACK = -14141369;
   private static final int TEXT_PRIMARY = -1379073;
   private static final int TEXT_DIM = -8615271;
   private static final int[] SWATCHES = new int[]{-1, -10696961, -43691, -22016, -2731, -11141291, -11141121, -5614081, -43521};
   private HudModule dragging;
   private int dragOffsetX;
   private int dragOffsetY;
   private HudModule selected;
   private boolean draggingScale;
   private int px;
   private int py;
   private int pw;
   private int ph;
   private int scaleTrackL;
   private int scaleTrackR;
   private int scaleTrackY;

   public HudEditorScreen() {
   }

   private List<HudModule> hudModules() {
      List<HudModule> list = new ArrayList();

      for(Module m : ModuleManager.getModules()) {
         if(m instanceof HudModule && m.isEnabled()) {
            list.add((HudModule)m);
         }
      }

      return list;
   }

   private int boxW(HudModule h) {
      return h.isEmpty()?Math.round(60.0F * h.getScale()):h.getScaledWidth();
   }

   private int boxH(HudModule h) {
      return h.isEmpty()?Math.round(14.0F * h.getScale()):h.getScaledHeight();
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      ScaledResolution res = new ScaledResolution(this.mc);
      String title = "HUD EDITOR";
      String hint = "Drag to move  -  Scroll to resize  -  Ctrl+Scroll resizes everything  -  R resets  -  Esc to save";
      this.fontRendererObj.drawStringWithShadow(title, (float)(res.getScaledWidth() / 2 - this.fontRendererObj.getStringWidth(title) / 2), 8.0F, -1379073);
      this.fontRendererObj.drawStringWithShadow(hint, (float)(res.getScaledWidth() / 2 - this.fontRendererObj.getStringWidth(hint) / 2), 20.0F, -8615271);

      for(HudModule h : this.hudModules()) {
         int x = h.getPosX();
         int y = h.getPosY();
         int w = this.boxW(h);
         int hh = this.boxH(h);
         if(h.isEmpty()) {
            drawRect(x, y, x + w, y + hh, 1610612736);
            String label = this.fontRendererObj.trimStringToWidth(h.getName(), w - 4);
            this.fontRendererObj.drawStringWithShadow(label, (float)(x + 2), (float)(y + 3), -8615271);
         } else {
            h.renderScaled(x, y);
         }

         boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + hh;
         int col = this.selected == h?-10696961:(!hovered && this.dragging != h?1079822079:-10696961);
         this.drawOutline(x, y, x + w, y + hh, col);
      }

      if(this.selected != null) {
         this.drawPanel(mouseX, mouseY, res);
      }

      super.drawScreen(mouseX, mouseY, partialTicks);
   }

   private void drawPanel(int mouseX, int mouseY, ScaledResolution res) {
      this.pw = 250;
      this.ph = 54;
      this.px = res.getScaledWidth() / 2 - this.pw / 2;
      this.py = 32;
      drawRect(this.px, this.py, this.px + this.pw, this.py + this.ph, -267446493);
      drawRect(this.px, this.py, this.px + this.pw, this.py + 1, -14799814);
      drawRect(this.px, this.py + this.ph - 1, this.px + this.pw, this.py + this.ph, -14799814);
      this.fontRendererObj.drawStringWithShadow("Selected: " + this.selected.getName(), (float)(this.px + 10), (float)(this.py + 6), -1379073);
      this.fontRendererObj.drawString("Scale", this.px + 10, this.py + 22, -8615271);
      this.scaleTrackL = this.px + 50;
      this.scaleTrackR = this.px + this.pw - 60;
      this.scaleTrackY = this.py + 25;
      drawRect(this.scaleTrackL, this.scaleTrackY - 1, this.scaleTrackR, this.scaleTrackY + 1, -14141369);
      float frac = (this.selected.getScale() - 0.5F) / 2.0F;
      int fill = this.scaleTrackL + (int)((float)(this.scaleTrackR - this.scaleTrackL) * frac);
      drawRect(this.scaleTrackL, this.scaleTrackY - 1, fill, this.scaleTrackY + 1, -10696961);
      drawRect(fill - 1, this.scaleTrackY - 3, fill + 1, this.scaleTrackY + 3, -1379073);
      this.fontRendererObj.drawString(String.format("%.2fx", new Object[]{Float.valueOf(this.selected.getScale())}), this.px + this.pw - 52, this.py + 22, -1379073);
      int sx = this.px + 10;
      int sy = this.py + 38;
      int sw = 14;
      int sh = 10;
      int gap = 3;

      for(int i = 0; i < SWATCHES.length; ++i) {
         int cx = sx + i * (sw + gap);
         drawRect(cx, sy, cx + sw, sy + sh, SWATCHES[i]);
         boolean sel = (this.selected.getColor() & 16777215) == (SWATCHES[i] & 16777215);
         if(sel) {
            this.drawOutline(cx - 1, sy - 1, cx + sw + 1, sy + sh + 1, -1379073);
         }
      }

   }

   private void drawOutline(int left, int top, int right, int bottom, int color) {
      drawRect(left, top, right, top + 1, color);
      drawRect(left, bottom - 1, right, bottom, color);
      drawRect(left, top, left + 1, bottom, color);
      drawRect(right - 1, top, right, bottom, color);
   }

   public void handleMouseInput() throws IOException {
      super.handleMouseInput();
      int wheel = Mouse.getEventDWheel();
      if(wheel != 0) {
         float step = wheel > 0 ? 0.05F : -0.05F;

         // Ctrl+scroll resizes every element at once, anywhere on screen.
         // Sizing twenty elements one at a time to match is the tedious part of
         // laying out a HUD, and it does not need the cursor to be over
         // anything in particular.
         if(isCtrlKeyDown()) {
            for(HudModule h : this.hudModules()) {
               h.setScale(h.getScale() + step);
            }

            return;
         }

         ScaledResolution res = new ScaledResolution(this.mc);
         int mouseX = Mouse.getEventX() * res.getScaledWidth() / this.mc.displayWidth;
         int mouseY = res.getScaledHeight() - Mouse.getEventY() * res.getScaledHeight() / this.mc.displayHeight - 1;
         List<HudModule> list = this.hudModules();

         for(int i = list.size() - 1; i >= 0; --i) {
            HudModule h = (HudModule)list.get(i);
            int x = h.getPosX();
            int y = h.getPosY();
            if(mouseX >= x && mouseX <= x + this.boxW(h) && mouseY >= y && mouseY <= y + this.boxH(h)) {
               h.setScale(h.getScale() + step);
               this.selected = h;
               return;
            }
         }

      }
   }

   protected void keyTyped(char typedChar, int keyCode) throws IOException {
      // R resets: the selected element alone, or every element with Ctrl held.
      // Scaling is easy to overshoot and there is otherwise no way back to 1x
      // short of dragging the slider by eye.
      if(keyCode == 19) {
         if(isCtrlKeyDown()) {
            for(HudModule h : this.hudModules()) {
               h.setScale(1.0F);
            }
         } else if(this.selected != null) {
            this.selected.setScale(1.0F);
         }

         return;
      }

      super.keyTyped(typedChar, keyCode);
   }

   protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if(mouseButton == 0) {
         if(this.selected != null && mouseX >= this.px && mouseX <= this.px + this.pw && mouseY >= this.py && mouseY <= this.py + this.ph) {
            if(mouseY >= this.scaleTrackY - 4 && mouseY <= this.scaleTrackY + 4 && mouseX >= this.scaleTrackL - 3 && mouseX <= this.scaleTrackR + 3) {
               this.draggingScale = true;
               this.updateScaleFromMouse(mouseX);
            } else {
               int sx = this.px + 10;
               int sy = this.py + 38;
               int sw = 14;
               int sh = 10;
               int gap = 3;

               for(int i = 0; i < SWATCHES.length; ++i) {
                  int cx = sx + i * (sw + gap);
                  if(mouseX >= cx && mouseX <= cx + sw && mouseY >= sy && mouseY <= sy + sh) {
                     this.selected.setColor(SWATCHES[i]);
                     return;
                  }
               }

            }
         } else {
            List<HudModule> list = this.hudModules();

            for(int i = list.size() - 1; i >= 0; --i) {
               HudModule h = (HudModule)list.get(i);
               int x = h.getPosX();
               int y = h.getPosY();
               if(mouseX >= x && mouseX <= x + this.boxW(h) && mouseY >= y && mouseY <= y + this.boxH(h)) {
                  this.selected = h;
                  this.dragging = h;
                  this.dragOffsetX = mouseX - x;
                  this.dragOffsetY = mouseY - y;
                  return;
               }
            }

            this.selected = null;
         }
      }
   }

   protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
      if(this.draggingScale) {
         this.updateScaleFromMouse(mouseX);
      } else {
         if(this.dragging != null) {
            this.dragging.setPos(mouseX - this.dragOffsetX, mouseY - this.dragOffsetY);
            this.dragging.clampToScreen();
         }

      }
   }

   private void updateScaleFromMouse(int mouseX) {
      float frac = (float)(mouseX - this.scaleTrackL) / (float)(this.scaleTrackR - this.scaleTrackL);
      frac = Math.max(0.0F, Math.min(1.0F, frac));
      this.selected.setScale(0.5F + frac * 2.0F);
   }

   protected void mouseReleased(int mouseX, int mouseY, int state) {
      super.mouseReleased(mouseX, mouseY, state);
      this.dragging = null;
      this.draggingScale = false;
   }

   public void onGuiClosed() {
      ConfigManager.save();
   }

   public boolean doesGuiPauseGame() {
      return false;
   }
}
