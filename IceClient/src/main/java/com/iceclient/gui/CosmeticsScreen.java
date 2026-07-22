package com.iceclient.gui;

import com.iceclient.cosmetic.Cosmetic;
import com.iceclient.cosmetic.CosmeticManager;
import com.iceclient.cosmetic.CosmeticRegistry;
import com.iceclient.cosmetic.CosmeticType;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;

/**
 * Wardrobe and shop.
 *
 * <p>One screen rather than two, because "what I own" and "what I could own" are
 * the same list with a different state per row -- splitting them would mean
 * showing every cape twice and making people tab back and forth to compare.
 * Owned items sort first so the wardrobe reads as yours, with the shop below it.
 */
public class CosmeticsScreen extends GuiScreen {

   private static final int PANEL_BG = 0xC807090F;
   private static final int CARD_BG = 0xD00A1B2C;
   private static final int CARD_HOVER = 0xF0123449;
   private static final int ACCENT = 0xFF5CC6FF;
   private static final int TEXT = 0xFFEAF4FF;
   private static final int MUTED = 0xFF7D8DA0;
   private static final int GOOD = 0xFF57E0A0;
   private static final int BAD = 0xFFFF6B6B;

   private final CosmeticType[] tabs = CosmeticType.values();
   private int tab;
   private Cosmetic selected;
   private String flash = "";
   private long flashUntil;

   private int panelX, panelY, panelW, panelH;
   private int listX, listY, listW, listH;
   private int sideX, sideW;
   private int scroll;

   @Override
   public void initGui() {
      this.panelW = Math.min(this.width - 60, 440);
      this.panelH = Math.min(this.height - 50, 260);
      this.panelX = (this.width - this.panelW) / 2;
      this.panelY = (this.height - this.panelH) / 2;

      this.sideW = 118;
      this.listX = this.panelX + 12;
      this.listY = this.panelY + 44;
      this.listW = this.panelW - this.sideW - 32;
      this.listH = this.panelH - 56;
      this.sideX = this.panelX + this.panelW - this.sideW - 12;

      if(this.selected == null) {
         List<Cosmetic> items = current();
         if(!items.isEmpty()) {
            this.selected = items.get(0);
         }
      }
   }

   private List<Cosmetic> current() {
      return CosmeticRegistry.ofType(this.tabs[this.tab]);
   }

   @Override
   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      drawRect(0, 0, this.width, this.height, 0xB0000000);
      drawRect(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, PANEL_BG);
      drawRect(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 1, ACCENT);

      this.fontRendererObj.drawStringWithShadow("COSMETICS", this.panelX + 12, this.panelY + 12, ACCENT);

      String bal = CosmeticManager.getBalance() + " frost";
      this.fontRendererObj.drawStringWithShadow(bal,
            this.panelX + this.panelW - this.fontRendererObj.getStringWidth(bal) - 12,
            this.panelY + 12, 0xFFFFC947);

      drawTabs(mouseX, mouseY);
      drawList(mouseX, mouseY);
      drawSide(mouseX, mouseY);

      if(!this.flash.isEmpty() && System.currentTimeMillis() < this.flashUntil) {
         this.fontRendererObj.drawStringWithShadow(this.flash,
               this.panelX + 12, this.panelY + this.panelH - 12,
               this.flash.startsWith("!") ? BAD : GOOD);
      }

      super.drawScreen(mouseX, mouseY, partialTicks);
   }

   private void drawTabs(int mouseX, int mouseY) {
      int x = this.panelX + 12;
      int y = this.panelY + 28;

      for(int i = 0; i < this.tabs.length; ++i) {
         String label = this.tabs[i].getLabel();
         int w = this.fontRendererObj.getStringWidth(label) + 14;
         boolean on = i == this.tab;
         boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 12;

         drawRect(x, y, x + w, y + 12, on ? ACCENT : (hov ? CARD_HOVER : CARD_BG));
         this.fontRendererObj.drawString(label, x + 7, y + 2, on ? 0xFF04202E : (hov ? TEXT : MUTED));
         x += w + 4;
      }
   }

   private void drawList(int mouseX, int mouseY) {
      List<Cosmetic> items = current();
      int rowH = 26;

      // Clip to the list area so scrolled rows do not bleed into the tabs.
      GL11.glEnable(GL11.GL_SCISSOR_TEST);
      net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(this.mc);
      int f = sr.getScaleFactor();
      GL11.glScissor(this.listX * f, (sr.getScaledHeight() - (this.listY + this.listH)) * f,
            this.listW * f, this.listH * f);

      int y = this.listY - this.scroll;

      for(Cosmetic c : items) {
         if(y + rowH >= this.listY && y <= this.listY + this.listH) {
            boolean owned = CosmeticManager.owns(c.getId());
            boolean equipped = c.getId().equals(CosmeticManager.getEquipped(c.getType()));
            boolean hov = mouseX >= this.listX && mouseX <= this.listX + this.listW
                  && mouseY >= y && mouseY <= y + rowH
                  && mouseY >= this.listY && mouseY <= this.listY + this.listH;
            boolean sel = c == this.selected;

            drawRect(this.listX, y, this.listX + this.listW, y + rowH - 2,
                  sel ? CARD_HOVER : (hov ? CARD_HOVER : CARD_BG));

            if(sel || equipped) {
               drawRect(this.listX, y, this.listX + 2, y + rowH - 2, equipped ? GOOD : ACCENT);
            }

            this.fontRendererObj.drawStringWithShadow(c.getName(), this.listX + 8, y + 5,
                  owned ? TEXT : MUTED);
            this.fontRendererObj.drawString(c.getRarity().getLabel(), this.listX + 8, y + 15,
                  c.getRarity().getColor());

            // Right-hand state: equipped > owned > price.
            String right;
            int col;
            if(equipped) {
               right = "EQUIPPED";
               col = GOOD;
            } else if(owned) {
               right = "Owned";
               col = MUTED;
            } else if(c.getPrice() > 0) {
               right = String.valueOf(c.getPrice());
               col = CosmeticManager.getBalance() >= c.getPrice() ? 0xFFFFC947 : MUTED;
            } else {
               right = "Locked";
               col = MUTED;
            }

            this.fontRendererObj.drawString(right,
                  this.listX + this.listW - this.fontRendererObj.getStringWidth(right) - 8,
                  y + 10, col);
         }

         y += rowH;
      }

      GL11.glDisable(GL11.GL_SCISSOR_TEST);
   }

   private void drawSide(int mouseX, int mouseY) {
      int y = this.listY;
      drawRect(this.sideX, y, this.sideX + this.sideW, this.listY + this.listH, CARD_BG);

      // Live preview of your own character, so equipping shows immediately.
      if(this.mc.thePlayer != null) {
         GuiInventory.drawEntityOnScreen(this.sideX + this.sideW / 2, y + 86, 38,
               (float)(this.sideX + this.sideW / 2) - 0.0F, (float)(y + 40), this.mc.thePlayer);
      }

      int ty = y + 100;

      if(this.selected == null) {
         this.fontRendererObj.drawString("Nothing here yet.", this.sideX + 8, ty, MUTED);
         return;
      }

      this.fontRendererObj.drawStringWithShadow(this.selected.getName(), this.sideX + 8, ty, TEXT);
      this.fontRendererObj.drawString(this.selected.getRarity().getLabel(), this.sideX + 8, ty + 11,
            this.selected.getRarity().getColor());

      // Description wrapped to the panel, so long blurbs do not run off the edge.
      List<String> wrapped = this.fontRendererObj.listFormattedStringToWidth(
            this.selected.getDescription(), this.sideW - 16);
      int dy = ty + 25;
      for(int i = 0; i < wrapped.size() && i < 4; ++i) {
         this.fontRendererObj.drawString(wrapped.get(i), this.sideX + 8, dy, MUTED);
         dy += 10;
      }

      drawActionButton(mouseX, mouseY);
   }

   /** The one button whose label depends on what you can actually do next. */
   private void drawActionButton(int mouseX, int mouseY) {
      int bx = this.sideX + 8;
      int bw = this.sideW - 16;
      int by = this.listY + this.listH - 26;
      int bh = 18;

      boolean hov = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
      String label;
      int bg;
      int fg;

      if(this.selected.getType() == CosmeticType.EMOTE && !CosmeticManager.canPlayEmote()) {
         label = "Coming soon";
         bg = CARD_BG;
         fg = MUTED;
      } else if(this.selected.getId().equals(CosmeticManager.getEquipped(this.selected.getType()))) {
         label = "Unequip";
         bg = hov ? CARD_HOVER : CARD_BG;
         fg = TEXT;
      } else if(CosmeticManager.owns(this.selected.getId())) {
         label = "Equip";
         bg = hov ? 0xFF7FD8FF : ACCENT;
         fg = 0xFF04202E;
      } else if(this.selected.getPrice() > 0) {
         label = "Buy — " + this.selected.getPrice();
         boolean afford = CosmeticManager.getBalance() >= this.selected.getPrice();
         bg = afford ? (hov ? 0xFFFFD666 : 0xFFFFC947) : CARD_BG;
         fg = afford ? 0xFF2A1F00 : MUTED;
      } else {
         label = "Not for sale";
         bg = CARD_BG;
         fg = MUTED;
      }

      drawRect(bx, by, bx + bw, by + bh, bg);
      this.fontRendererObj.drawString(label,
            bx + bw / 2 - this.fontRendererObj.getStringWidth(label) / 2, by + 5, fg);
   }

   @Override
   protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
      super.mouseClicked(mouseX, mouseY, button);
      if(button != 0) {
         return;
      }

      // tabs
      int x = this.panelX + 12;
      int ty = this.panelY + 28;
      for(int i = 0; i < this.tabs.length; ++i) {
         int w = this.fontRendererObj.getStringWidth(this.tabs[i].getLabel()) + 14;
         if(mouseX >= x && mouseX <= x + w && mouseY >= ty && mouseY <= ty + 12) {
            this.tab = i;
            this.scroll = 0;
            List<Cosmetic> items = current();
            this.selected = items.isEmpty() ? null : items.get(0);
            return;
         }
         x += w + 4;
      }

      // list
      if(mouseX >= this.listX && mouseX <= this.listX + this.listW
            && mouseY >= this.listY && mouseY <= this.listY + this.listH) {
         int idx = (mouseY - this.listY + this.scroll) / 26;
         List<Cosmetic> items = current();
         if(idx >= 0 && idx < items.size()) {
            this.selected = items.get(idx);
         }
         return;
      }

      // action button
      int bx = this.sideX + 8;
      int bw = this.sideW - 16;
      int by = this.listY + this.listH - 26;
      if(this.selected != null && mouseX >= bx && mouseX <= bx + bw
            && mouseY >= by && mouseY <= by + 18) {
         doAction();
      }
   }

   private void doAction() {
      Cosmetic c = this.selected;

      if(c.getType() == CosmeticType.EMOTE && !CosmeticManager.canPlayEmote()) {
         say("!Emotes aren't animated yet.");
         return;
      }

      if(c.getId().equals(CosmeticManager.getEquipped(c.getType()))) {
         CosmeticManager.unequip(c.getType());
         say("Unequipped " + c.getName() + ".");
         return;
      }

      if(CosmeticManager.owns(c.getId())) {
         CosmeticManager.equip(c.getId());
         say("Equipped " + c.getName() + ".");
         return;
      }

      String err = CosmeticManager.purchase(c.getId());
      if(err != null) {
         say("!" + err);
      } else {
         CosmeticManager.equip(c.getId());
         say("Bought and equipped " + c.getName() + ".");
      }
   }

   private void say(String msg) {
      this.flash = msg;
      this.flashUntil = System.currentTimeMillis() + 4000L;
   }

   @Override
   public void handleMouseInput() throws IOException {
      super.handleMouseInput();
      int wheel = org.lwjgl.input.Mouse.getEventDWheel();
      if(wheel != 0) {
         int rows = current().size() * 26;
         int max = Math.max(0, rows - this.listH);
         this.scroll = Math.max(0, Math.min(max, this.scroll - Integer.signum(wheel) * 18));
      }
   }

   @Override
   public boolean doesGuiPauseGame() {
      return false;
   }
}
