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

   /** Slot tabs, plus one past the end for user-supplied capes. */
   private final CosmeticType[] tabs = CosmeticType.values();
   private final int customTab = this.tabs.length;
   private int tab;
   private Cosmetic selected;
   private String flash = "";
   private long flashUntil;

   private int panelX, panelY, panelW, panelH;
   private int listX, listY, listW, listH;
   private int sideX, sideW;
   private int scroll;

   /** Preview rotation. Starts at 180 so you open on the back, where capes are. */
   private float previewYaw = 180.0F;
   private float previewPitch;
   private boolean dragging;
   private boolean hasDragged;
   private int dragFromX, dragFromY;
   private float dragYaw, dragPitch;

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

      // Pull the authoritative balance whenever the wardrobe opens, so a code
      // redeemed on another machine shows up without a restart.
      com.iceclient.cosmetic.CosmeticApi.sync(null);
   }

   private List<Cosmetic> current() {
      if(this.tab == this.customTab) {
         return java.util.Collections.emptyList();
      }

      return CosmeticRegistry.ofType(this.tabs[this.tab]);
   }

   private String tabLabel(int i) {
      return i == this.customTab ? "Custom" : this.tabs[i].getLabel();
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

      if(this.tab == this.customTab) {
         drawCustomList(mouseX, mouseY);
      } else {
         drawList(mouseX, mouseY);
      }

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

      for(int i = 0; i <= this.customTab; ++i) {
         String label = tabLabel(i);
         int w = this.fontRendererObj.getStringWidth(label) + 14;
         boolean on = i == this.tab;
         boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 12;

         drawRect(x, y, x + w, y + 12, on ? ACCENT : (hov ? CARD_HOVER : CARD_BG));
         this.fontRendererObj.drawString(label, x + 7, y + 2, on ? 0xFF04202E : (hov ? TEXT : MUTED));
         x += w + 4;
      }
   }

   /**
    * The Custom tab: capes loaded from the user's own folder.
    *
    * <p>Drawn separately from the catalogue list because these have no price,
    * rarity or ownership -- they are files, and the only actions that make sense
    * are wear, take off, and open the folder.
    */
   private void drawCustomList(int mouseX, int mouseY) {
      List<String> capes = com.iceclient.cosmetic.CustomCapes.names();
      int rowH = 16;
      int y = this.listY;

      String hint = "Drop 64x32 PNGs in the folder, then Refresh.";
      this.fontRendererObj.drawString(hint, this.listX, y, MUTED);
      y += 14;

      if(capes.isEmpty()) {
         this.fontRendererObj.drawString("No capes found.", this.listX, y, MUTED);

         String err = com.iceclient.cosmetic.CustomCapes.getLastError();
         if(!err.isEmpty()) {
            List<String> wrapped = this.fontRendererObj.listFormattedStringToWidth(err, this.listW);
            int ey = y + 14;
            for(int i = 0; i < wrapped.size() && i < 3; ++i) {
               this.fontRendererObj.drawString(wrapped.get(i), this.listX, ey, BAD);
               ey += 10;
            }
         }
      } else {
         String active = com.iceclient.cosmetic.CosmeticManager.getCustomCape();

         for(String name : capes) {
            if(y > this.listY + this.listH - rowH) {
               break;
            }

            boolean on = name.equals(active);
            boolean hov = mouseX >= this.listX && mouseX <= this.listX + this.listW
                  && mouseY >= y && mouseY <= y + rowH - 2;

            drawRect(this.listX, y, this.listX + this.listW, y + rowH - 2,
                  hov ? CARD_HOVER : CARD_BG);
            if(on) {
               drawRect(this.listX, y, this.listX + 2, y + rowH - 2, GOOD);
            }

            this.fontRendererObj.drawString(name, this.listX + 8, y + 3, on ? TEXT : MUTED);
            if(on) {
               this.fontRendererObj.drawString("WORN",
                     this.listX + this.listW - this.fontRendererObj.getStringWidth("WORN") - 8,
                     y + 3, GOOD);
            }

            y += rowH;
         }
      }

      // Two buttons at the foot of the list column.
      int bw = (this.listW - 6) / 2;
      int by = this.listY + this.listH - 18;
      drawButton(this.listX, by, bw, "Refresh", mouseX, mouseY);
      drawButton(this.listX + bw + 6, by, bw, "Open Folder", mouseX, mouseY);
   }

   private void drawButton(int x, int y, int w, String label, int mouseX, int mouseY) {
      boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 16;
      drawRect(x, y, x + w, y + 16, hov ? CARD_HOVER : CARD_BG);
      this.fontRendererObj.drawString(label,
            x + w / 2 - this.fontRendererObj.getStringWidth(label) / 2, y + 4,
            hov ? TEXT : MUTED);
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
         drawRotatablePlayer(this.sideX + this.sideW / 2, y + 84, 36);

         if(!this.dragging && !this.hasDragged) {
            String hint = "drag to turn";
            this.fontRendererObj.drawString(hint,
                  this.sideX + this.sideW / 2 - this.fontRendererObj.getStringWidth(hint) / 2,
                  y + 88, 0xFF3A4654);
         }
      }

      int ty = y + 96;

      if(this.selected == null) {
         this.fontRendererObj.drawString("Nothing here yet.", this.sideX + 8, ty, MUTED);
         return;
      }

      this.fontRendererObj.drawStringWithShadow(this.selected.getName(), this.sideX + 8, ty, TEXT);
      this.fontRendererObj.drawString(this.selected.getRarity().getLabel(), this.sideX + 8, ty + 10,
            this.selected.getRarity().getColor());

      // Description is wrapped AND clipped to the space above the button --
      // it used to run straight underneath it when a blurb was long.
      int buttonTop = this.listY + this.listH - 26;
      int dy = ty + 23;
      List<String> wrapped = this.fontRendererObj.listFormattedStringToWidth(
            this.selected.getDescription(), this.sideW - 16);

      for(String line : wrapped) {
         if(dy + 9 > buttonTop - 3) {
            break;
         }

         this.fontRendererObj.drawString(line, this.sideX + 8, dy, MUTED);
         dy += 9;
      }

      drawActionButton(mouseX, mouseY);
   }

   /**
    * The preview model, turned by dragging rather than following the cursor.
    *
    * <p>Vanilla's {@code drawEntityOnScreen} aims the model at the mouse, which
    * is useless here: the thing you want to look at is the back, and the mouse
    * has to be over the list to click anything. Holding a fixed yaw and letting
    * the user spin it is the only way to actually see a cape.
    */
   private void drawRotatablePlayer(int x, int y, int scale) {
      net.minecraft.entity.player.EntityPlayer p = this.mc.thePlayer;

      GlStateManager.enableColorMaterial();
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)x, (float)y, 50.0F);
      GlStateManager.scale((float)(-scale), (float)scale, (float)scale);
      GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);

      float prevYawOffset = p.renderYawOffset;
      float prevYaw = p.rotationYaw;
      float prevPitch = p.rotationPitch;
      float prevHeadPrev = p.prevRotationYawHead;
      float prevHead = p.rotationYawHead;

      GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
      net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
      GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(-this.previewPitch, 1.0F, 0.0F, 0.0F);

      // Body and head share the yaw so the model turns as one piece.
      p.renderYawOffset = this.previewYaw;
      p.rotationYaw = this.previewYaw;
      p.rotationPitch = 0.0F;
      p.rotationYawHead = this.previewYaw;
      p.prevRotationYawHead = this.previewYaw;

      GlStateManager.translate(0.0F, 0.0F, 0.0F);
      net.minecraft.client.renderer.entity.RenderManager rm = this.mc.getRenderManager();
      rm.setPlayerViewY(180.0F);
      rm.setRenderShadow(false);
      rm.renderEntityWithPosYaw(p, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
      rm.setRenderShadow(true);

      p.renderYawOffset = prevYawOffset;
      p.rotationYaw = prevYaw;
      p.rotationPitch = prevPitch;
      p.rotationYawHead = prevHead;
      p.prevRotationYawHead = prevHeadPrev;

      GlStateManager.popMatrix();
      net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
      GlStateManager.disableRescaleNormal();
      GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit);
      GlStateManager.disableTexture2D();
      GlStateManager.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);
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

      // Dragging on the preview turns the model; it must be checked before the
      // side panel's button so a drag that ends over the button does not buy.
      if(mouseX >= this.sideX && mouseX <= this.sideX + this.sideW
            && mouseY >= this.listY && mouseY <= this.listY + 96) {
         this.dragging = true;
         this.dragFromX = mouseX;
         this.dragFromY = mouseY;
         this.dragYaw = this.previewYaw;
         this.dragPitch = this.previewPitch;
         return;
      }

      // tabs
      int x = this.panelX + 12;
      int ty = this.panelY + 28;
      for(int i = 0; i <= this.customTab; ++i) {
         int w = this.fontRendererObj.getStringWidth(tabLabel(i)) + 14;
         if(mouseX >= x && mouseX <= x + w && mouseY >= ty && mouseY <= ty + 12) {
            this.tab = i;
            this.scroll = 0;
            if(i == this.customTab) {
               com.iceclient.cosmetic.CustomCapes.reload();
               this.selected = null;
            } else {
               List<Cosmetic> items = current();
               this.selected = items.isEmpty() ? null : items.get(0);
            }
            return;
         }
         x += w + 4;
      }

      if(this.tab == this.customTab) {
         customClick(mouseX, mouseY);
         return;
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

   /** Clicks on the Custom tab: pick a cape, or the two buttons under the list. */
   private void customClick(int mouseX, int mouseY) {
      int bw = (this.listW - 6) / 2;
      int by = this.listY + this.listH - 18;

      if(mouseY >= by && mouseY <= by + 16) {
         if(mouseX >= this.listX && mouseX <= this.listX + bw) {
            com.iceclient.cosmetic.CustomCapes.reload();
            say("Reloaded — " + com.iceclient.cosmetic.CustomCapes.names().size() + " found.");
            return;
         }

         if(mouseX >= this.listX + bw + 6 && mouseX <= this.listX + this.listW) {
            try {
               java.awt.Desktop.getDesktop().open(com.iceclient.cosmetic.CustomCapes.folder());
            } catch (Exception e) {
               say("!Couldn't open the folder — it's at .minecraft/config/iceclient/capes");
            }
            return;
         }
      }

      List<String> capes = com.iceclient.cosmetic.CustomCapes.names();
      int y = this.listY + 14;

      for(String name : capes) {
         if(mouseY >= y && mouseY <= y + 14
               && mouseX >= this.listX && mouseX <= this.listX + this.listW) {
            if(name.equals(CosmeticManager.getCustomCape())) {
               CosmeticManager.setCustomCape(null);
               say("Took off " + name + ".");
            } else {
               CosmeticManager.setCustomCape(name);
               say("Wearing " + name + ".");
            }
            return;
         }

         y += 16;
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

      // Purchases go through the server, which owns the balance and the price --
      // buying locally would just mean editing a number on your own disk.
      final String id = c.getId();
      final String label = c.getName();
      say("Buying " + label + "…");

      com.iceclient.cosmetic.CosmeticApi.purchase(id, new com.iceclient.cosmetic.CosmeticApi.Callback() {
         public void done(boolean ok, String msg) {
            if(ok) {
               CosmeticManager.equip(id);
               say("Bought and equipped " + label + ".");
            } else {
               say("!" + msg);
            }
         }
      });
   }

   /** Shows a message, and pushes the new loadout so others see it right away. */
   private void say(String msg) {
      com.iceclient.IceClient.COSMETIC_SYNC.pokeNow();
      this.flash = msg;
      this.flashUntil = System.currentTimeMillis() + 4000L;
   }

   @Override
   protected void mouseClickMove(int mouseX, int mouseY, int button, long held) {
      if(this.dragging) {
         this.hasDragged = true;
         this.previewYaw = this.dragYaw + (mouseX - this.dragFromX) * 1.6F;
         // Pitch is clamped so the model cannot be flipped upside down, which
         // makes it impossible to tell what you are looking at.
         this.previewPitch = Math.max(-35.0F,
               Math.min(35.0F, this.dragPitch + (mouseY - this.dragFromY) * 0.9F));
      }
   }

   @Override
   protected void mouseReleased(int mouseX, int mouseY, int state) {
      this.dragging = false;
      super.mouseReleased(mouseX, mouseY, state);
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
