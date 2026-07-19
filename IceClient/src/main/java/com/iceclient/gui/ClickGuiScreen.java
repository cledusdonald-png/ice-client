package com.iceclient.gui;

import com.iceclient.admin.AdminAccess;
import com.iceclient.util.BindUtil;
import com.iceclient.config.ConfigManager;
import com.iceclient.gui.HudEditorScreen;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.ModuleManager;
import com.iceclient.schematic.Selection;
import com.iceclient.schematica.SchematicaBridge;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.ModeSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.setting.Setting;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ClickGuiScreen extends GuiScreen {
   private static final int PANEL_BG = -939063025;
   private static final int SIDEBAR_BG = 402653184;
   private static final int CARD_BG = 218103807;
   private static final int CARD_HOVER = 486539263;
   private static final int CARD_ACTIVE = 576505599;
   private static final int OVERLAY_BG = -435548650;
   private static final int SCRIM = 1711276032;
   private static final int TEXT_PRIMARY = -1379073;
   private static final int TEXT_SECONDARY = -8088413;
   private static final int TEXT_MUTED = -11642264;
   private static final int ACCENT = -10696961;
   private static final int ACCENT_DIM = -14069917;
   private static final int DIVIDER = -15327445;
   private static final int ICON_BG = 352321535;
   private static final int TRACK = -14141369;
   private static final int PILL_BG = 184549375;
   private static final int SETTING_H = 14;
   private static final int SIDEBAR_W = 104;
   private static final int PAD = 18;
   private static final float SCALE = 0.8F;
   private ClickGuiScreen.View view = ClickGuiScreen.View.MODULES;
   private ModuleCategory selectedCategory;
   private String search = "";
   private boolean searchFocused;
   private int panelX;
   private int panelY;
   private int panelW;
   private int panelH;
   private int contentX;
   private int contentY;
   private int contentW;
   private int contentH;
   private int gridX;
   private int gridY;
   private int gridW;
   private int gridH;
   private int searchX;
   private int searchY;
   private int searchW;
   private int searchH;
   private int tabsY;
   private final List<ClickGuiScreen.NavItem> nav = new ArrayList();
   private final List<ClickGuiScreen.TabItem> tabs = new ArrayList();
   private final List<ClickGuiScreen.Card> cards = new ArrayList();
   private int scrollY;
   private int contentTotalH;
   private ClickGuiScreen.Card openCard;
   private int drawerX;
   private int drawerY;
   private int drawerW;
   private int drawerH;
   private int drawerScroll;
   private final List<ClickGuiScreen.DrawerEntry> drawerEntries = new ArrayList();
   private int drawerContentH;
   private Module bindingModule;
   private KeybindSetting bindingSetting;
   /** Number field being typed into, and the raw text typed so far. */
   private NumberSetting editingNumber;
   private String editBuffer = "";
   private NumberSetting draggingSlider;
   private int dragTrackLeft;
   private int dragTrackRight;
   private final List<String> schemFiles = new ArrayList();
   private final List<ClickGuiScreen.SButton> sButtons = new ArrayList();
   private String loadedFile;
   private String schemStatus = "";
   private int schemScroll = 0;
   private int schemListX;
   private int schemListY;
   private int schemListW;
   private int schemListH;

   public ClickGuiScreen() {
   }

   public void initGui() {
      this.panelW = Math.min(Math.round((float)(this.width - 40) / 0.8F), 640);
      this.panelH = Math.min(Math.round((float)(this.height - 30) / 0.8F), 400);
      int onW = Math.round((float)this.panelW * 0.8F);
      int onH = Math.round((float)this.panelH * 0.8F);
      this.panelX = (this.width - onW) / 2;
      this.panelY = (this.height - onH) / 2;
      this.contentX = 122;
      this.contentY = 56;
      this.contentW = this.panelW - 18 - this.contentX;
      this.contentH = this.panelH - this.contentY - 18;
      this.searchW = Math.min(170, this.contentW / 2);
      this.searchH = 18;
      this.searchX = this.panelW - 18 - this.searchW;
      this.searchY = 16;
      this.tabsY = this.contentY;
      this.gridX = this.contentX;
      this.gridY = this.contentY + 26;
      this.gridW = this.contentW;
      this.gridH = this.panelH - 18 - this.gridY;
      this.buildNav();
      this.buildTabs();
      this.buildCards();
   }

   private void buildNav() {
      this.nav.clear();
      int y = 52;
      this.nav.add(this.navItem("Modules", ClickGuiScreen.View.MODULES, y));
      y = y + 24;
      this.nav.add(this.navItem("Macros", ClickGuiScreen.View.MACROS, y));
      y = y + 24;
      this.nav.add(this.navItem("FPS", ClickGuiScreen.View.FPS, y));
      y = y + 24;
      this.nav.add(this.navItem("Schematic", ClickGuiScreen.View.SCHEMATIC, y));
      y = y + 24;
      this.nav.add(this.navItem("Admin", ClickGuiScreen.View.ADMIN, y));
      ClickGuiScreen.NavItem hud = new ClickGuiScreen.NavItem();
      hud.label = "Edit HUD";
      hud.action = true;
      hud.run = () -> {
         this.mc.displayGuiScreen(new HudEditorScreen());
      };
      hud.x = 12;
      hud.y = this.panelH - 30;
      hud.w = 84;
      hud.h = 20;
      this.nav.add(hud);
   }

   private ClickGuiScreen.NavItem navItem(String label, ClickGuiScreen.View v, int y) {
      ClickGuiScreen.NavItem n = new ClickGuiScreen.NavItem();
      n.label = label;
      n.view = v;
      n.x = 12;
      n.y = y;
      n.w = 84;
      n.h = 22;
      return n;
   }

   private void buildTabs() {
      this.tabs.clear();
      int x = this.contentX;
      ClickGuiScreen.TabItem all = new ClickGuiScreen.TabItem();
      all.category = null;
      all.label = "All";
      this.sizeTab(all, x);
      this.tabs.add(all);
      x = all.x + all.w + 6;

      for(ModuleCategory c : ModuleCategory.TABS) {
         ClickGuiScreen.TabItem t = new ClickGuiScreen.TabItem();
         t.category = c;
         t.label = c.label;
         this.sizeTab(t, x);
         this.tabs.add(t);
         x = t.x + t.w + 6;
      }

   }

   private void sizeTab(ClickGuiScreen.TabItem t, int x) {
      int w = this.fontRendererObj.getStringWidth(t.label) + 16;
      t.x = x;
      t.y = this.tabsY;
      t.w = w;
      t.h = 18;
   }

   /** Typed passphrase, masked on screen, and the last attempt's result. */
   private String adminInput = "";
   private boolean adminFailed;

   /**
    * The unlock prompt.
    *
    * <p>Masked purely so the passphrase is not readable over a shoulder or in a
    * screenshot -- the same and only thing this whole gate achieves. It is not
    * a secret from anyone holding the jar.
    */
   private void drawAdminPrompt(int mouseX, int mouseY) {
      int x = this.contentX;
      int y = this.contentY + 40;

      this.fontRendererObj.drawStringWithShadow("Enter passphrase to unlock:", (float)x, (float)y, -8088413);

      int boxW = Math.min(220, this.contentW);
      this.roundRect(x, y + 14, x + boxW, y + 32, 3.0F, -14141369);

      StringBuilder masked = new StringBuilder();
      for(int i = 0; i < this.adminInput.length(); ++i) {
         masked.append('*');
      }

      this.fontRendererObj.drawStringWithShadow(masked.toString() + "_", (float)(x + 6), (float)(y + 19), -1379073);

      if(this.adminFailed) {
         this.fontRendererObj.drawStringWithShadow("Incorrect.", (float)x, (float)(y + 38), -43691);
      }

      this.fontRendererObj.drawStringWithShadow("Type, then press Enter. Re-locks on restart.",
            (float)x, (float)(y + 54), -10461088);
   }

   private void buildCards() {
      this.cards.clear();
      Map<String, ClickGuiScreen.Card> groups = new LinkedHashMap();
      List<ClickGuiScreen.Card> singles = new ArrayList();
      String q = this.search.trim().toLowerCase();

      for(Module m : ModuleManager.getModules()) {
         // Admin modules appear only on their own page. Without this they would
         // show on the "All" tab and in search results, which defeats the point
         // of hiding them.
         if(m.getCategory() == ModuleCategory.ADMIN
               && this.selectedCategory != ModuleCategory.ADMIN) {
            continue;
         }

         if(this.selectedCategory == null || m.getCategory() == this.selectedCategory) {
            if(m.getGroupName() != null) {
               ClickGuiScreen.Card gc = (ClickGuiScreen.Card)groups.get(m.getGroupName());
               if(gc == null) {
                  gc = new ClickGuiScreen.Card();
                  gc.name = m.getGroupName();
                  gc.members = new ArrayList();
                  groups.put(m.getGroupName(), gc);
               }

               gc.members.add(m);
            } else {
               ClickGuiScreen.Card c = new ClickGuiScreen.Card();
               c.name = m.getName();
               c.module = m;
               singles.add(c);
            }
         }
      }

      List<ClickGuiScreen.Card> all = new ArrayList(groups.values());
      all.addAll(singles);
      all.sort((a, b) -> {
         return a.name.compareToIgnoreCase(b.name);
      });

      for(ClickGuiScreen.Card c : all) {
         if(q.isEmpty() || this.cardMatches(c, q)) {
            this.cards.add(c);
         }
      }

      this.layoutGrid();
   }

   private boolean cardMatches(ClickGuiScreen.Card c, String q) {
      if(c.name.toLowerCase().contains(q)) {
         return true;
      } else {
         if(c.isGroup()) {
            for(Module m : c.members) {
               if(m.getName().toLowerCase().contains(q)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private void layoutGrid() {
      int gap = 8;
      int cols = Math.max(3, Math.min(4, Math.round((float)this.gridW / 160.0F)));
      int cardW = (this.gridW - (cols - 1) * gap) / cols;
      int cardH = 46;
      int x = this.gridX;
      int y = 0;
      int col = 0;

      for(ClickGuiScreen.Card c : this.cards) {
         c.x = this.gridX + col * (cardW + gap);
         c.y = y;
         c.w = cardW;
         c.h = cardH;
         ++col;
         if(col >= cols) {
            col = 0;
            y += cardH + gap;
         }
      }

      this.contentTotalH = col == 0?y:y + cardH + gap;
      this.clampScroll();
   }

   private void clampScroll() {
      int max = Math.max(0, this.contentTotalH - this.gridH);
      if(this.scrollY > max) {
         this.scrollY = max;
      }

      if(this.scrollY < 0) {
         this.scrollY = 0;
      }

   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      drawRect(0, 0, this.width, this.height, 1711276032);
      int mx = Math.round((float)(mouseX - this.panelX) / 0.8F);
      int my = Math.round((float)(mouseY - this.panelY) / 0.8F);
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)this.panelX, (float)this.panelY, 0.0F);
      GlStateManager.scale(0.8F, 0.8F, 1.0F);
      this.roundRect(0, 0, this.panelW, this.panelH, 6.0F, -939063025);
      this.drawSidebar(mx, my);
      if(this.view == ClickGuiScreen.View.MODULES) {
         this.drawHeaderModules();
         this.drawTabs(mx, my);
         this.enableScissor(this.gridX, this.gridY, this.gridW, this.gridH);
         this.drawGrid(mx, my);
         this.disableScissor();
      } else if(this.view == ClickGuiScreen.View.MACROS) {
         this.drawPageHeader("Macros", "Bind keybinds to chat commands and messages");
         this.enableScissor(this.contentX, this.contentY, this.contentW, this.contentH);
         this.drawMacrosPage(mx, my);
         this.disableScissor();
      } else if(this.view == ClickGuiScreen.View.FPS) {
         this.drawPageHeader("FPS", "Performance and rendering toggles");
         this.enableScissor(this.contentX, this.contentY, this.contentW, this.contentH);
         this.drawFpsPage(mx, my);
         this.disableScissor();
      } else if(this.view == ClickGuiScreen.View.ADMIN) {
         if(AdminAccess.isUnlocked()) {
            this.drawPageHeader("Admin", "Hidden modules -- unlocked for this session");
            this.enableScissor(this.gridX, this.gridY, this.gridW, this.gridH);
            this.drawGrid(mx, my);
            this.disableScissor();
         } else {
            this.drawPageHeader("Admin", "Locked");
            this.drawAdminPrompt(mx, my);
         }
      } else {
         this.drawSchematicHeader();
         this.layoutSchematic();
         this.enableScissor(this.contentX, this.contentY, this.contentW, this.contentH);
         this.drawSchematicPanel(mx, my);
         this.disableScissor();
      }

      if(this.openCard != null) {
         this.drawDrawer(mx, my);
      }

      GlStateManager.popMatrix();
      super.drawScreen(mouseX, mouseY, partialTicks);
   }

   private void drawSidebar(int mouseX, int mouseY) {
      drawRect(0, 0, 104, this.panelH, 402653184);
      drawRect(104, 0, 105, this.panelH, -15327445);
      this.drawScaled("ICE", 14.0F, 16.0F, 1.6F, -10696961, true);
      int iceW = (int)((float)this.fontRendererObj.getStringWidth("ICE") * 1.6F);
      this.drawScaled("CLIENT", (float)(16 + iceW), 20.0F, 1.0F, -1379073, true);

      for(ClickGuiScreen.NavItem n : this.nav) {
         boolean active = !n.action && this.view == n.view;
         boolean hov = mouseX >= n.x && mouseX <= n.x + n.w && mouseY >= n.y && mouseY <= n.y + n.h;
         if(active) {
            this.roundRect(n.x, n.y, n.x + n.w, n.y + n.h, 5.0F, 576505599);
         } else if(hov) {
            this.roundRect(n.x, n.y, n.x + n.w, n.y + n.h, 5.0F, 318767103);
         }

         int col = active?-10696961:(hov?-1379073:-8088413);
         this.fontRendererObj.drawStringWithShadow(n.label, (float)(n.x + 10), (float)(n.y + n.h / 2 - 4), col);
      }

      this.fontRendererObj.drawString("v0.1", 14, this.panelH - 46, -11642264);
   }

   private void drawHeaderModules() {
      this.drawScaled("Modules", (float)this.contentX, 15.0F, 1.5F, -1379073, true);
      this.fontRendererObj.drawString("Manage built-in client mods", this.contentX, 36, -8088413);
      int total = ModuleManager.getModules().size();
      int on = 0;

      for(Module m : ModuleManager.getModules()) {
         if(m.isEnabled()) {
            ++on;
         }
      }

      this.fontRendererObj.drawString(on + " enabled / " + total + " modules", this.contentX, 46, -11642264);
      this.roundRect(this.searchX, this.searchY, this.searchX + this.searchW, this.searchY + this.searchH, 5.0F, this.searchFocused?419430399:218103807);
      String shown = this.search.isEmpty() && !this.searchFocused?"Search modules":this.search;
      int scol = this.search.isEmpty() && !this.searchFocused?-11642264:-1379073;
      this.fontRendererObj.drawString(shown, this.searchX + 8, this.searchY + this.searchH / 2 - 4, scol);
      if(this.searchFocused) {
         int cx = this.searchX + 8 + this.fontRendererObj.getStringWidth(this.search);
         drawRect(cx, this.searchY + 4, cx + 1, this.searchY + this.searchH - 4, -10696961);
      }

   }

   private void drawSchematicHeader() {
      this.drawScaled("Schematic", (float)this.contentX, 15.0F, 1.5F, -1379073, true);
      this.fontRendererObj.drawString("Load, position and print a schematic", this.contentX, 36, -8088413);
   }

   private void drawTabs(int mouseX, int mouseY) {
      for(ClickGuiScreen.TabItem t : this.tabs) {
         boolean active = t.category == this.selectedCategory;
         boolean hov = mouseX >= t.x && mouseX <= t.x + t.w && mouseY >= t.y && mouseY <= t.y + t.h;
         if(active) {
            this.roundRect(t.x, t.y, t.x + t.w, t.y + t.h, 4.0F, 576505599);
         }

         int col = active?-10696961:(hov?-1379073:-8088413);
         int tw = this.fontRendererObj.getStringWidth(t.label);
         this.fontRendererObj.drawString(t.label, t.x + t.w / 2 - tw / 2, t.y + t.h / 2 - 4, col);
      }

   }

   private void drawGrid(int mouseX, int mouseY) {
      for(ClickGuiScreen.Card c : this.cards) {
         int dy = this.gridY + c.y - this.scrollY;
         if(dy + c.h >= this.gridY && dy <= this.gridY + this.gridH) {
            this.drawCard(c, dy, mouseX, mouseY);
         }
      }

      this.drawScrollbar();
   }

   private void drawCard(ClickGuiScreen.Card c, int dy, int mouseX, int mouseY) {
      boolean hov = mouseX >= c.x && mouseX <= c.x + c.w && mouseY >= dy && mouseY <= dy + c.h && mouseY >= this.gridY && mouseY <= this.gridY + this.gridH;
      boolean active = c.active();
      this.roundRect(c.x, dy, c.x + c.w, dy + c.h, 4.0F, hov?486539263:218103807);
      if(active) {
         this.roundRect(c.x, dy, c.x + c.w, dy + c.h, 4.0F, 576505599);
         drawRect(c.x + 2, dy + 5, c.x + 4, dy + c.h - 5, -10696961);
      }

      int ix = c.x + 12;
      int iy = dy + c.h / 2 - 11;
      this.roundRect(ix, iy, ix + 22, iy + 22, 4.0F, active?576505599:352321535);
      String letter = c.name.substring(0, 1).toUpperCase();
      this.fontRendererObj.drawStringWithShadow(letter, (float)(ix + 11 - this.fontRendererObj.getStringWidth(letter) / 2), (float)(iy + 7), active?-10696961:-8088413);
      int tx = ix + 34;
      if(c.isGroup()) {
         this.fontRendererObj.drawStringWithShadow(this.trim(c.name, c.w - 52), (float)tx, (float)(dy + c.h / 2 - 9), -1379073);
         this.fontRendererObj.drawString(c.members.size() + " features", tx, dy + c.h / 2 + 2, -11642264);
      } else {
         this.fontRendererObj.drawStringWithShadow(this.trim(c.name, c.w - 52), (float)tx, (float)(dy + c.h / 2 - 4), active?-1379073:-8088413);
      }

      boolean gearHov = mouseX >= c.x + c.w - 20 && mouseX <= c.x + c.w - 4 && mouseY >= dy + c.h - 20 && mouseY <= dy + c.h - 4;
      this.drawGear(c.x + c.w - 12, dy + c.h - 12, gearHov?-10696961:-11642264);
   }

   private void drawGear(int cx, int cy, int color) {
      drawRect(cx - 3, cy - 3, cx + 3, cy + 3, color);
      drawRect(cx - 1, cy - 5, cx + 1, cy + 5, color);
      drawRect(cx - 5, cy - 1, cx + 5, cy + 1, color);
      drawRect(cx - 1, cy - 1, cx + 1, cy + 1, 218103807);
   }

   private void roundRect(int l, int t, int r, int b, float rad, int color) {
      float a = (float)(color >>> 24 & 255) / 255.0F;
      float cr = (float)(color >> 16 & 255) / 255.0F;
      float cg = (float)(color >> 8 & 255) / 255.0F;
      float cb = (float)(color & 255) / 255.0F;
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.color(cr, cg, cb, a);
      GL11.glBegin(7);
      this.quad((float)l + rad, (float)t, (float)r - rad, (float)b);
      this.quad((float)l, (float)t + rad, (float)l + rad, (float)b - rad);
      this.quad((float)r - rad, (float)t + rad, (float)r, (float)b - rad);
      GL11.glEnd();
      this.arc((float)l + rad, (float)t + rad, rad, 180, 270);
      this.arc((float)r - rad, (float)t + rad, rad, 270, 360);
      this.arc((float)r - rad, (float)b - rad, rad, 0, 90);
      this.arc((float)l + rad, (float)b - rad, rad, 90, 180);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void quad(float x1, float y1, float x2, float y2) {
      GL11.glVertex2f(x1, y2);
      GL11.glVertex2f(x2, y2);
      GL11.glVertex2f(x2, y1);
      GL11.glVertex2f(x1, y1);
   }

   private void arc(float cx, float cy, float rad, int a0, int a1) {
      GL11.glBegin(6);
      GL11.glVertex2f(cx, cy);

      for(int a = a0; a <= a1; a += 10) {
         double ang = Math.toRadians((double)a);
         GL11.glVertex2f(cx + (float)Math.cos(ang) * rad, cy + (float)Math.sin(ang) * rad);
      }

      GL11.glEnd();
   }

   private void drawSwitch(int x, int y, boolean on) {
      int w = 22;
      int h = 10;
      drawRect(x, y, x + w, y + h, on?-14069917:-14141369);
      int knob = on?x + w - 9:x + 1;
      drawRect(knob, y + 1, knob + 8, y + h - 1, on?-10696961:-9734265);
   }

   private void drawScrollbar() {
      int max = Math.max(0, this.contentTotalH - this.gridH);
      if(max > 0) {
         int trackX = this.gridX + this.gridW - 3;
         int thumbH = Math.max(20, this.gridH * this.gridH / this.contentTotalH);
         int travel = this.gridH - thumbH;
         int thumbY = this.gridY + travel * this.scrollY / max;
         drawRect(trackX, thumbY, trackX + 3, thumbY + thumbH, -14069917);
      }
   }

   private void buildDrawer() {
      this.drawerEntries.clear();
      int y = 0;
      if(this.openCard != null) {
         if(this.openCard.module != null) {
            y = this.addKeybind(this.openCard.module, y);
            y = this.addSettings(this.openCard.module, y);
         } else {
            for(Module m : this.openCard.members) {
               ClickGuiScreen.DrawerEntry mh = new ClickGuiScreen.DrawerEntry();
               mh.kind = ClickGuiScreen.DKind.MEMBER;
               mh.module = m;
               mh.label = m.getName();
               mh.y = y;
               mh.h = 18;
               this.drawerEntries.add(mh);
               y = y + 18;
               y = this.addSettings(m, y);
               y = y + 4;
            }
         }

         this.drawerContentH = y;
      }
   }

   private int addKeybind(Module m, int y) {
      ClickGuiScreen.DrawerEntry e = new ClickGuiScreen.DrawerEntry();
      e.kind = ClickGuiScreen.DKind.KEYBIND;
      e.module = m;
      e.label = "Keybind";
      e.y = y;
      e.h = 14;
      this.drawerEntries.add(e);
      return y + 14;
   }

   private int addSettings(Module m, int y) {
      String section = "\u0000";
      Iterator var4 = m.getSettings().iterator();

      while(true) {
         ClickGuiScreen.DrawerEntry e;
         while(true) {
            if(!var4.hasNext()) {
               return y;
            }

            Setting s = (Setting)var4.next();
            if(s.isVisible()) {
               if(!Objects.equals(s.getSection(), section)) {
                  section = s.getSection();
                  if(section != null) {
                     e = new ClickGuiScreen.DrawerEntry();
                     e.kind = ClickGuiScreen.DKind.SECTION;
                     e.label = section;
                     e.y = y;
                     e.h = 12;
                     this.drawerEntries.add(e);
                     y += 12;
                  }
               }

               e = new ClickGuiScreen.DrawerEntry();
               e.setting = s;
               e.module = m;
               e.y = y;
               e.h = 14;
               if(s instanceof BooleanSetting) {
                  e.kind = ClickGuiScreen.DKind.BOOL;
                  break;
               }

               if(s instanceof NumberSetting) {
                  e.kind = ClickGuiScreen.DKind.NUMBER;
                  break;
               }

               if(s instanceof ModeSetting) {
                  e.kind = ClickGuiScreen.DKind.MODE;
                  break;
               }

               if(s instanceof KeybindSetting) {
                  e.kind = ClickGuiScreen.DKind.KEYSETTING;
                  break;
               }

               if(s instanceof ColorSetting) {
                  e.kind = ClickGuiScreen.DKind.COLOR;
                  break;
               }
            }
         }

         this.drawerEntries.add(e);
         y += 14;
      }
   }

   private void drawDrawer(int mouseX, int mouseY) {
      drawRect(0, 0, this.panelW, this.panelH, 1711276032);
      this.drawerW = Math.min(280, this.panelW - 60);
      this.drawerH = Math.min(Math.max(90, this.drawerContentH + 40), this.panelH - 60);
      this.drawerX = this.panelW / 2 - this.drawerW / 2;
      this.drawerY = this.panelH / 2 - this.drawerH / 2;
      this.roundRect(this.drawerX, this.drawerY, this.drawerX + this.drawerW, this.drawerY + this.drawerH, 6.0F, -435548650);
      this.fontRendererObj.drawStringWithShadow(this.openCard.name, (float)(this.drawerX + 12), (float)(this.drawerY + 10), -1379073);
      String x = "x";
      this.fontRendererObj.drawStringWithShadow(x, (float)(this.drawerX + this.drawerW - 14), (float)(this.drawerY + 9), -8088413);
      drawRect(this.drawerX, this.drawerY + 26, this.drawerX + this.drawerW, this.drawerY + 27, -15327445);
      int bodyTop = this.drawerY + 30;
      int bodyH = this.drawerY + this.drawerH - 8 - bodyTop;
      int maxScroll = Math.max(0, this.drawerContentH - bodyH);
      if(this.drawerScroll > maxScroll) {
         this.drawerScroll = maxScroll;
      }

      if(this.drawerScroll < 0) {
         this.drawerScroll = 0;
      }

      this.enableScissor(this.drawerX, bodyTop, this.drawerW, bodyH);
      int rowX = this.drawerX + 12;
      int rowW = this.drawerW - 24;

      for(ClickGuiScreen.DrawerEntry e : this.drawerEntries) {
         int ey = bodyTop + e.y - this.drawerScroll;
         if(ey + e.h >= bodyTop && ey <= bodyTop + bodyH) {
            this.drawDrawerEntry(e, rowX, ey, rowW, mouseX);
         }
      }

      this.disableScissor();
   }

   private void drawDrawerEntry(ClickGuiScreen.DrawerEntry e, int x, int y, int w, int mouseX) {
      switch(e.kind) {
      case SECTION:
         this.fontRendererObj.drawString(e.label.toUpperCase(), x, y + 3, -11642264);
         break;
      case MEMBER:
         this.fontRendererObj.drawStringWithShadow(e.label, (float)x, (float)(y + 5), -1379073);
         this.drawSwitch(x + w - 22, y + 3, e.module.isEnabled());
         break;
      case KEYBIND:
         this.fontRendererObj.drawString("Keybind", x + 4, y + 3, -8088413);
         String key = this.bindingModule == e.module?"...":e.module.getKeyName();
         this.fontRendererObj.drawString(key, x + w - this.fontRendererObj.getStringWidth(key), y + 3, this.bindingModule == e.module?-10696961:-1379073);
         break;
      case BOOL:
         this.fontRendererObj.drawString(e.setting.getName(), x + 4, y + 3, -8088413);
         boolean on = ((BooleanSetting)e.setting).get();
         this.drawSwitch(x + w - 22, y + 1, on);
         break;
      case NUMBER:
         NumberSetting n = (NumberSetting)e.setting;
         this.fontRendererObj.drawString(e.setting.getName(), x + 4, y + 3, -8088413);
         int tl = x + w / 2;
         int tr = x + w - 34;
         int ty = y + 7;
         drawRect(tl, ty - 1, tr, ty + 1, -14141369);
         int fill = tl + (int)((double)(tr - tl) * n.getFraction());
         drawRect(tl, ty - 1, fill, ty + 1, -10696961);
         drawRect(fill - 1, ty - 3, fill + 1, ty + 3, -1379073);
         // Click the value on the right to type an exact number instead of
         // fighting the slider for it.
         boolean editingThis = this.editingNumber == n;
         String val = editingThis ? this.editBuffer + "_" : this.formatNumber(n);
         this.fontRendererObj.drawString(val, x + w - this.fontRendererObj.getStringWidth(val), y + 3,
               editingThis ? -10696961 : -1379073);
         break;
      case MODE:
         this.fontRendererObj.drawString(e.setting.getName(), x + 4, y + 3, -8088413);
         // Renamed from 'val'/'key': switch cases share one scope in Java, and
         // the decompiler emitted the same name in sibling cases.
         String modeVal = ((ModeSetting)e.setting).get();
         this.fontRendererObj.drawString(modeVal, x + w - this.fontRendererObj.getStringWidth(modeVal), y + 3, -10696961);
         break;
      case KEYSETTING:
         this.fontRendererObj.drawString(e.setting.getName(), x + 4, y + 3, -8088413);
         KeybindSetting k = (KeybindSetting)e.setting;
         String bindKey = this.bindingSetting == k?"...":k.getKeyName();
         this.fontRendererObj.drawString(bindKey, x + w - this.fontRendererObj.getStringWidth(bindKey), y + 3, this.bindingSetting == k?-10696961:-1379073);
         break;
      case COLOR:
         this.fontRendererObj.drawString(e.setting.getName(), x + 4, y + 3, -8088413);
         int rgb = ((ColorSetting)e.setting).getRGB();
         drawRect(x + w - 16, y + 1, x + w, y + 14 - 1, -16777216 | rgb);
      }

   }

   private void drawScaled(String s, float x, float y, float scale, int color, boolean shadow) {
      GlStateManager.pushMatrix();
      GlStateManager.translate(x, y, 0.0F);
      GlStateManager.scale(scale, scale, 1.0F);
      if(shadow) {
         this.fontRendererObj.drawStringWithShadow(s, 0.0F, 0.0F, color);
      } else {
         this.fontRendererObj.drawString(s, 0, 0, color);
      }

      GlStateManager.popMatrix();
   }

   private String trim(String s, int maxWidth) {
      return this.fontRendererObj.trimStringToWidth(s, Math.max(10, maxWidth));
   }

   private String formatNumber(NumberSetting n) {
      double v = n.get();
      return v == Math.floor(v)?String.valueOf((int)v):String.format("%.1f", new Object[]{Double.valueOf(v)});
   }

   private void drawBorder(int left, int top, int right, int bottom, int color) {
      drawRect(left, top, right, top + 1, color);
      drawRect(left, bottom - 1, right, bottom, color);
      drawRect(left, top, left + 1, bottom, color);
      drawRect(right - 1, top, right, bottom, color);
   }

   private void enableScissor(int x, int y, int w, int h) {
      ScaledResolution res = new ScaledResolution(this.mc);
      int scale = res.getScaleFactor();
      int ax = this.panelX + Math.round((float)x * 0.8F);
      int ay = this.panelY + Math.round((float)y * 0.8F);
      int sw = Math.round((float)w * 0.8F);
      int sh = Math.round((float)h * 0.8F);
      int sy = this.mc.displayHeight - (ay + sh) * scale;
      GL11.glEnable(3089);
      GL11.glScissor(ax * scale, sy, sw * scale, sh * scale);
   }

   private void disableScissor() {
      GL11.glDisable(3089);
   }

   public void handleMouseInput() throws IOException {
      super.handleMouseInput();
      int wheel = Mouse.getEventDWheel();
      if(wheel != 0) {
         if(this.openCard != null) {
            this.drawerScroll -= wheel / 4;
         } else if(this.view == ClickGuiScreen.View.SCHEMATIC) {
            int max = Math.max(0, this.schemFiles.size() * 14 - this.schemListH);
            this.schemScroll -= wheel / 8;
            if(this.schemScroll < 0) {
               this.schemScroll = 0;
            }

            if(this.schemScroll > max) {
               this.schemScroll = max;
            }
         } else {
            this.scrollY -= wheel / 4;
            this.clampScroll();
         }

      }
   }

   /** Applies whatever was typed into the number field, clamped by the setting. */
   private void commitEdit() {
      if(this.editingNumber == null) {
         return;
      }

      try {
         if(!this.editBuffer.isEmpty()) {
            this.editingNumber.set(Double.parseDouble(this.editBuffer));
         }
      } catch (NumberFormatException var2) {
         // Leave the old value alone rather than zeroing it on a typo.
      }

      this.editingNumber = null;
      this.editBuffer = "";
   }

   protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      // While waiting for a bind, a click IS the bind -- capture it as a mouse
      // code rather than letting it click whatever is under the cursor.
      //
      // Left-click cancels instead of binding. Binding left-click is almost
      // never what someone means, and it's destructive: the module would then
      // toggle on every attack and every inventory click. Clicking away to
      // cancel is also what the button is for everywhere else in this GUI.
      if(this.bindingSetting != null || this.bindingModule != null) {
         if(mouseButton != 0) {
            int code = BindUtil.fromMouseButton(mouseButton);
            if(this.bindingSetting != null) {
               this.bindingSetting.set(code);
            } else {
               this.bindingModule.setKeyCode(code);
            }
         }

         this.bindingSetting = null;
         this.bindingModule = null;
         return;
      }

      super.mouseClicked(mouseX, mouseY, mouseButton);
      mouseX = Math.round((float)(mouseX - this.panelX) / 0.8F);
      mouseY = Math.round((float)(mouseY - this.panelY) / 0.8F);
      if(this.openCard != null) {
         this.handleDrawerClick(mouseX, mouseY);
      } else {
         for(ClickGuiScreen.NavItem n : this.nav) {
            if(mouseX >= n.x && mouseX <= n.x + n.w && mouseY >= n.y && mouseY <= n.y + n.h) {
               if(n.action) {
                  if(n.run != null) {
                     n.run.run();
                  }
               } else {
                  this.view = n.view;
                  this.scrollY = 0;

                  // The grid is category-driven, so entering and leaving Admin
                  // has to move the selection with it -- otherwise Admin would
                  // show the previous tab's modules, or the Modules page would
                  // come back still pinned to Admin.
                  if(n.view == ClickGuiScreen.View.ADMIN) {
                     this.selectedCategory = ModuleCategory.ADMIN;
                     this.adminInput = "";
                     this.adminFailed = false;
                  } else if(this.selectedCategory == ModuleCategory.ADMIN) {
                     this.selectedCategory = null;
                  }

                  this.buildCards();
               }

               return;
            }
         }

         if(this.view == ClickGuiScreen.View.MACROS) {
            this.handleMacrosClick(mouseX, mouseY, mouseButton);
         } else if(this.view == ClickGuiScreen.View.FPS) {
            this.handleFpsClick(mouseX, mouseY, mouseButton);
         } else if(this.view == ClickGuiScreen.View.ADMIN && !AdminAccess.isUnlocked()) {
            // Locked: the prompt is keyboard-only, so swallow clicks rather
            // than letting them fall through to the schematic handler.
            return;
         } else if(this.view != ClickGuiScreen.View.MODULES
               && this.view != ClickGuiScreen.View.ADMIN) {
            this.handleSchematicClick(mouseX, mouseY, mouseButton);
         } else {
            // The Admin page reuses the module grid but has no search box or
            // category tabs of its own -- it is always one fixed category.
            boolean modules = this.view == ClickGuiScreen.View.MODULES;

            this.searchFocused = modules
                  && mouseX >= this.searchX && mouseX <= this.searchX + this.searchW
                  && mouseY >= this.searchY && mouseY <= this.searchY + this.searchH;
            if(!this.searchFocused) {
               for(ClickGuiScreen.TabItem t : modules ? this.tabs : new ArrayList<ClickGuiScreen.TabItem>()) {
                  if(mouseX >= t.x && mouseX <= t.x + t.w && mouseY >= t.y && mouseY <= t.y + t.h) {
                     this.selectedCategory = t.category;
                     this.scrollY = 0;
                     this.buildCards();
                     return;
                  }
               }

               if(mouseX >= this.gridX && mouseX <= this.gridX + this.gridW && mouseY >= this.gridY && mouseY <= this.gridY + this.gridH) {
                  Iterator var11 = this.cards.iterator();

                  int dy;
                  ClickGuiScreen.Card c;
                  while(true) {
                     if(!var11.hasNext()) {
                        return;
                     }

                     c = (ClickGuiScreen.Card)var11.next();
                     dy = this.gridY + c.y - this.scrollY;
                     if(mouseX >= c.x && mouseX <= c.x + c.w && mouseY >= dy && mouseY <= dy + c.h) {
                        break;
                     }
                  }

                  boolean gear = mouseX >= c.x + c.w - 20 && mouseX <= c.x + c.w - 4 && mouseY >= dy + c.h - 20 && mouseY <= dy + c.h - 4;
                  if(mouseButton != 1 && (mouseButton != 0 || !gear)) {
                     if(mouseButton == 0) {
                        this.toggleCard(c);
                     }
                  } else {
                     this.openCard = c;
                     this.drawerScroll = 0;
                     this.buildDrawer();
                  }

               }
            }
         }
      }
   }

   private void toggleCard(ClickGuiScreen.Card c) {
      if(c.module != null) {
         c.module.toggle();
      } else {
         boolean anyOn = c.active();

         for(Module m : c.members) {
            m.setEnabled(!anyOn);
         }

      }
   }

   private void handleDrawerClick(int mouseX, int mouseY) {
      if(mouseX >= this.drawerX && mouseX <= this.drawerX + this.drawerW && mouseY >= this.drawerY && mouseY <= this.drawerY + this.drawerH) {
         if(mouseX >= this.drawerX + this.drawerW - 18 && mouseY <= this.drawerY + 24) {
            this.openCard = null;
         } else {
            int bodyTop = this.drawerY + 30;
            int rowX = this.drawerX + 12;
            int rowW = this.drawerW - 24;

            for(ClickGuiScreen.DrawerEntry e : this.drawerEntries) {
               int ey = bodyTop + e.y - this.drawerScroll;
               if(mouseY >= ey && mouseY <= ey + e.h) {
                  switch(e.kind) {
                  case MEMBER:
                     e.module.toggle();
                     return;
                  case KEYBIND:
                     this.bindingModule = e.module;
                     return;
                  case BOOL:
                     ((BooleanSetting)e.setting).toggle();
                     this.buildDrawer();
                     return;
                  case NUMBER:
                     // Right-hand value zone types; the track to its left drags.
                     if(mouseX >= rowX + rowW - 34) {
                        this.editingNumber = (NumberSetting)e.setting;
                        this.editBuffer = "";
                        return;
                     }

                     this.commitEdit();
                     this.draggingSlider = (NumberSetting)e.setting;
                     this.dragTrackLeft = rowX + rowW / 2;
                     this.dragTrackRight = rowX + rowW - 34;
                     ((NumberSetting)e.setting).setFromFraction((double)(mouseX - this.dragTrackLeft) / (double)(this.dragTrackRight - this.dragTrackLeft));
                     return;
                  case MODE:
                     ((ModeSetting)e.setting).cycle();
                     return;
                  case KEYSETTING:
                     this.bindingSetting = (KeybindSetting)e.setting;
                     return;
                  default:
                     return;
                  }
               }
            }

         }
      } else {
         this.openCard = null;
      }
   }

   protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
      mouseX = Math.round((float)(mouseX - this.panelX) / 0.8F);
      mouseY = Math.round((float)(mouseY - this.panelY) / 0.8F);
      if(this.draggingSlider != null) {
         this.draggingSlider.setFromFraction((double)(mouseX - this.dragTrackLeft) / (double)(this.dragTrackRight - this.dragTrackLeft));
      }

   }

   protected void mouseReleased(int mouseX, int mouseY, int state) {
      super.mouseReleased(mouseX, mouseY, state);
      this.draggingSlider = null;
   }

   protected void keyTyped(char typedChar, int keyCode) throws IOException {
      // Binding a macro: the next key pressed is the bind.
      if(this.bindingMacro != null) {
         boolean clear = keyCode == 1 || keyCode == 211 || keyCode == 14;
         this.bindingMacro.setKeyCode(clear ? 0 : keyCode);
         this.bindingMacro = null;
         return;
      }

      // Typing the Admin passphrase. First, so no other text route can eat the
      // keystrokes while the prompt is up.
      if(this.view == ClickGuiScreen.View.ADMIN && !AdminAccess.isUnlocked()) {
         if(keyCode == 28 || keyCode == 156) {
            this.adminFailed = !AdminAccess.tryUnlock(this.adminInput);
            this.adminInput = "";
            if(AdminAccess.isUnlocked()) {
               this.selectedCategory = ModuleCategory.ADMIN;
               this.buildCards();
            }
         } else if(keyCode == 1) {
            this.view = ClickGuiScreen.View.MODULES;
            this.selectedCategory = null;
            this.adminInput = "";
            this.buildCards();
         } else if(keyCode == 14) {
            if(!this.adminInput.isEmpty()) {
               this.adminInput = this.adminInput.substring(0, this.adminInput.length() - 1);
            }
         } else if(typedChar >= 32 && typedChar != 127 && this.adminInput.length() < 64) {
            this.adminInput = this.adminInput + typedChar;
         }

         return;
      }

      // Typing a schematic position coordinate.
      if(this.editingAxis >= 0) {
         if(keyCode == 28 || keyCode == 156) {
            this.commitAxis();
         } else if(keyCode == 1) {
            this.editingAxis = -1;
            this.axisInput = "";
         } else if(keyCode == 14) {
            if(!this.axisInput.isEmpty()) {
               this.axisInput = this.axisInput.substring(0, this.axisInput.length() - 1);
            }
         } else if(this.axisInput.length() < 8
               && (Character.isDigit(typedChar) || (typedChar == '-' && this.axisInput.isEmpty()))) {
            this.axisInput = this.axisInput + typedChar;
         }

         return;
      }

      // Typing a macro cooldown. Before the macro-command route, since both are
      // live on the same page and this one is the narrower target.
      if(this.editingCooldown != null) {
         if(keyCode == 28 || keyCode == 156) {
            double parsed = parseDuration(this.cooldownInput);
            if(this.cooldownInput.trim().isEmpty()) {
               // Empty means "no cooldown", which is a normal thing to want and
               // should not read as a parse failure.
               this.editingCooldown.setCooldownSeconds(0.0D);
            } else if(parsed >= 0.0D) {
               this.editingCooldown.setCooldownSeconds(parsed);
            }

            this.editingCooldown = null;
            this.cooldownInput = "";
         } else if(keyCode == 1) {
            this.editingCooldown = null;
            this.cooldownInput = "";
         } else if(keyCode == 14) {
            if(!this.cooldownInput.isEmpty()) {
               this.cooldownInput = this.cooldownInput.substring(0, this.cooldownInput.length() - 1);
            }
         } else if(this.cooldownInput.length() < 10
               && (Character.isDigit(typedChar) || typedChar == '.'
                     || typedChar == 'm' || typedChar == 's')) {
            this.cooldownInput = this.cooldownInput + typedChar;
         }

         return;
      }

      // Naming a selection on the Points tab. Checked before the macro routes
      // so a schematic name containing letters cannot fall through to them.
      if(this.editingSaveName) {
         if(keyCode == 28 || keyCode == 156 || keyCode == 1) {
            this.editingSaveName = false;
            if(this.saveName.trim().isEmpty()) {
               this.saveName = "selection";
            }
         } else if(keyCode == 14) {
            if(!this.saveName.isEmpty()) {
               this.saveName = this.saveName.substring(0, this.saveName.length() - 1);
            }
         } else if(typedChar >= 32 && typedChar != 127 && this.saveName.length() < 40) {
            // Strip characters the filesystem will not take, rather than
            // failing at write time with a name the user already typed.
            if("\\/:*?\"<>|".indexOf(typedChar) < 0) {
               this.saveName = this.saveName + typedChar;
            }
         }

         return;
      }

      // Typing a macro command.
      if(this.macroInputFocused) {
         if(keyCode == 28 || keyCode == 156) {
            this.addMacroFromInput();
         } else if(keyCode == 1) {
            this.macroInputFocused = false;
            this.macroInput = "";
         } else if(keyCode == 14) {
            if(!this.macroInput.isEmpty()) {
               this.macroInput = this.macroInput.substring(0, this.macroInput.length() - 1);
            }
         } else if(typedChar >= 32 && typedChar != 127) {
            this.macroInput = this.macroInput + typedChar;
         }

         return;
      }

      // Typing into a number field takes precedence over every other key route.
      if(this.editingNumber != null) {
         if(keyCode == 28 || keyCode == 156) {          // enter / numpad enter
            this.commitEdit();
         } else if(keyCode == 1) {                       // escape -- discard
            this.editingNumber = null;
            this.editBuffer = "";
         } else if(keyCode == 14) {                      // backspace
            if(!this.editBuffer.isEmpty()) {
               this.editBuffer = this.editBuffer.substring(0, this.editBuffer.length() - 1);
            }
         } else if(Character.isDigit(typedChar) || typedChar == '.' || typedChar == '-') {
            this.editBuffer = this.editBuffer + typedChar;
         }

         return;
      }

      if(this.bindingModule != null) {
         boolean clear = keyCode == 1 || keyCode == 211 || keyCode == 14;
         this.bindingModule.setKeyCode(clear?0:keyCode);
         this.bindingModule = null;
      } else if(this.bindingSetting == null) {
         if(this.searchFocused) {
            if(keyCode == 1) {
               this.searchFocused = false;
            } else if(keyCode == 14) {
               if(!this.search.isEmpty()) {
                  this.search = this.search.substring(0, this.search.length() - 1);
               }

               this.buildCards();
            } else if(typedChar >= 32 && typedChar != 127) {
               this.search = this.search + typedChar;
               this.buildCards();
            }
         } else if(this.openCard != null && keyCode == 1) {
            this.openCard = null;
         } else {
            super.keyTyped(typedChar, keyCode);
         }
      } else {
         boolean clear = keyCode == 1 || keyCode == 211 || keyCode == 14;
         this.bindingSetting.set(clear?0:keyCode);
         this.bindingSetting = null;
      }
   }

   /**
    * Workspace sub-tab: 0 Browser, 1 Transform, 2 Points.
    *
    * <p>These are tabs inside the Schematic page rather than four more sidebar
    * entries, because they all act on the one loaded schematic -- separating
    * them at nav level would imply they were independent screens.
    */
   private int schemTab = 0;

   private static final String[] SCHEM_TABS = new String[]{"Browser", "Transform", "Points"};

   /** Hit box of each sub-tab, filled during layout. */
   private final int[] schemTabX = new int[SCHEM_TABS.length];
   private final int[] schemTabW = new int[SCHEM_TABS.length];
   private int schemTabY;

   /** Name the Points page will save under; edited in place. */
   private String saveName = "selection";
   private boolean editingSaveName;

   /** Which position axis is being typed on the Browser tab: -1, or 0/1/2. */
   private int editingAxis = -1;
   private String axisInput = "";

   private void layoutSchematicTabs() {
      this.schemTabY = this.contentY + 18;
      int x = this.contentX;

      for(int i = 0; i < SCHEM_TABS.length; ++i) {
         int w = this.fontRendererObj.getStringWidth(SCHEM_TABS[i]) + 16;
         this.schemTabX[i] = x;
         this.schemTabW[i] = w;
         x += w + 4;
      }

   }

   private void drawSchematicTabs(int mouseX, int mouseY) {
      for(int i = 0; i < SCHEM_TABS.length; ++i) {
         boolean on = this.schemTab == i;
         boolean hov = mouseX >= this.schemTabX[i] && mouseX <= this.schemTabX[i] + this.schemTabW[i]
               && mouseY >= this.schemTabY && mouseY <= this.schemTabY + 16;

         drawRect(this.schemTabX[i], this.schemTabY, this.schemTabX[i] + this.schemTabW[i],
               this.schemTabY + 16, on ? -14069917 : (hov ? 486539263 : 218103807));

         int tw = this.fontRendererObj.getStringWidth(SCHEM_TABS[i]);
         this.fontRendererObj.drawStringWithShadow(SCHEM_TABS[i],
               (float)(this.schemTabX[i] + this.schemTabW[i] / 2 - tw / 2),
               (float)(this.schemTabY + 4), on ? -1379073 : -8088413);
      }

   }

   /** @return true when the click landed on a tab and switched pages */
   private boolean handleSchemTabClick(int mouseX, int mouseY) {
      if(mouseY < this.schemTabY || mouseY > this.schemTabY + 16) {
         return false;
      }

      for(int i = 0; i < SCHEM_TABS.length; ++i) {
         if(mouseX >= this.schemTabX[i] && mouseX <= this.schemTabX[i] + this.schemTabW[i]) {
            if(this.schemTab != i) {
               this.schemTab = i;
               this.editingSaveName = false;
               // Each tab owns a different button set, so the old page's hit
               // boxes must not survive the switch.
               this.layoutSchematic();
            }

            return true;
         }
      }

      return false;
   }

   private void layoutSchematic() {
      this.sButtons.clear();
      this.layoutSchematicTabs();

      if(this.schemTab == 1) {
         this.layoutTransformPage();
         return;
      }

      if(this.schemTab == 2) {
         this.layoutPointsPage();
         return;
      }

      this.refreshSchemFiles();
      // Below the sub-tab row, which sits at contentY+18 and is 16 tall.
      int top = this.contentY + 40;
      int bottomRow = 22;
      this.schemListX = this.contentX;
      this.schemListY = top;
      this.schemListW = this.contentW * 45 / 100;
      this.schemListH = this.contentH - 40 - bottomRow - 6;
      int colX = this.schemListX + this.schemListW + 8;
      int colW = this.contentX + this.contentW - colX;
      if(SchematicaBridge.isAvailable()) {
         int rowGap = 20;
         int by = top + 12;
         int minusX = colX + colW - 40;
         int plusX = colX + colW - 18;
         this.sButtons.add(new ClickGuiScreen.SButton(0, minusX, by, 18, 14, "-"));
         this.sButtons.add(new ClickGuiScreen.SButton(1, plusX, by, 18, 14, "+"));
         this.sButtons.add(new ClickGuiScreen.SButton(2, minusX, by + rowGap, 18, 14, "-"));
         this.sButtons.add(new ClickGuiScreen.SButton(3, plusX, by + rowGap, 18, 14, "+"));
         this.sButtons.add(new ClickGuiScreen.SButton(4, minusX, by + 2 * rowGap, 18, 14, "-"));
         this.sButtons.add(new ClickGuiScreen.SButton(5, plusX, by + 2 * rowGap, 18, 14, "+"));
         int cy = by + 3 * rowGap + 4;
         this.sButtons.add(new ClickGuiScreen.SButton(6, colX, cy, colW, 16, "Move Here"));
         this.sButtons.add(new ClickGuiScreen.SButton(7, colX, cy + 20, colW, 16, "Render"));
         this.sButtons.add(new ClickGuiScreen.SButton(8, colX, cy + 40, colW, 16, "Printer"));
      }

      int by2 = this.contentY + this.contentH - bottomRow;
      int gap = 5;
      int bw = (this.contentW - 2 * gap) / 3;
      this.sButtons.add(new ClickGuiScreen.SButton(9, this.contentX, by2, bw, bottomRow - 2, "New Test Box"));
      this.sButtons.add(new ClickGuiScreen.SButton(10, this.contentX + bw + gap, by2, bw, bottomRow - 2, "Open Folder"));
      this.sButtons.add(new ClickGuiScreen.SButton(11, this.contentX + 2 * (bw + gap), by2, bw, bottomRow - 2, "Unload"));
   }

   /**
    * Transform page: rotate, flip and nudge the loaded schematic.
    *
    * <p>Button ids continue from the Browser page's 0-11 rather than restarting,
    * so {@link #schematicAction} stays one flat switch and no id can mean two
    * different things depending on which tab is open.
    */
   private void layoutTransformPage() {
      int top = this.contentY + 44;
      int gap = 6;
      int bw = (this.contentW - gap) / 2;
      int bh = 18;

      this.sButtons.add(new ClickGuiScreen.SButton(12, this.contentX, top, bw, bh, "Rotate CW"));
      this.sButtons.add(new ClickGuiScreen.SButton(13, this.contentX + bw + gap, top, bw, bh, "Rotate CCW"));

      int row2 = top + bh + gap;
      this.sButtons.add(new ClickGuiScreen.SButton(14, this.contentX, row2, bw, bh, "Flip X"));
      this.sButtons.add(new ClickGuiScreen.SButton(15, this.contentX + bw + gap, row2, bw, bh, "Flip Z"));

      int row3 = row2 + bh + gap * 3;
      int tw = (this.contentW - gap * 2) / 3;
      this.sButtons.add(new ClickGuiScreen.SButton(16, this.contentX, row3, tw, bh, "Move Here"));
      this.sButtons.add(new ClickGuiScreen.SButton(17, this.contentX + tw + gap, row3, tw, bh, "Copy Placement"));
      this.sButtons.add(new ClickGuiScreen.SButton(18, this.contentX + 2 * (tw + gap), row3, tw, bh, "Apply Placement"));

      int row4 = row3 + bh + gap * 3;
      int nw = (this.contentW - gap * 5) / 6;
      String[] labels = new String[]{"-X", "+X", "-Y", "+Y", "-Z", "+Z"};
      for(int i = 0; i < 6; ++i) {
         this.sButtons.add(new ClickGuiScreen.SButton(19 + i,
               this.contentX + i * (nw + gap), row4, nw, bh, labels[i]));
      }

   }

   private void drawTransformPage(int mouseX, int mouseY) {
      if(!SchematicaBridge.isAvailable()) {
         this.fontRendererObj.drawStringWithShadow("Schematica mod isn\'t installed.",
               (float)this.contentX, (float)(this.contentY + 44), -8088413);
         return;
      }

      boolean has = SchematicaBridge.hasSchematic();
      String header = has
            ? "Transforming: " + (this.loadedFile != null ? this.loadedFile : SchematicaBridge.name())
            : "Load a schematic from the Browser tab first";
      this.fontRendererObj.drawStringWithShadow(this.trim(header, this.contentW),
            (float)this.contentX, (float)(this.contentY + 6), has ? -10696961 : -8088413);

      this.drawButtons(mouseX, mouseY);

      int[] pos = SchematicaBridge.position();
      String posText = pos == null ? "Position: -" : "Position: " + pos[0] + ", " + pos[1] + ", " + pos[2];
      this.fontRendererObj.drawStringWithShadow(posText, (float)this.contentX,
            (float)(this.contentY + this.contentH - 30), -8088413);

      this.fontRendererObj.drawStringWithShadow("Nudge step: hold Shift for 5",
            (float)this.contentX, (float)(this.contentY + this.contentH - 18), -10461088);

      if(!this.schemStatus.isEmpty()) {
         this.fontRendererObj.drawStringWithShadow(this.trim(this.schemStatus, this.contentW),
               (float)this.contentX, (float)(this.contentY + this.contentH - 6), -8088413);
      }

   }

   /** Points page: the Point A/B region and saving it out as a schematic. */
   private void layoutPointsPage() {
      int gap = 6;
      int bh = 18;
      int bw = (this.contentW - gap * 2) / 3;
      int row = this.contentY + 96;

      this.sButtons.add(new ClickGuiScreen.SButton(25, this.contentX, row, bw, bh, "Set A to me"));
      this.sButtons.add(new ClickGuiScreen.SButton(26, this.contentX + bw + gap, row, bw, bh, "Set B to me"));
      this.sButtons.add(new ClickGuiScreen.SButton(27, this.contentX + 2 * (bw + gap), row, bw, bh, "Clear"));

      int row2 = row + bh + gap * 2;
      int half = (this.contentW - gap) / 2;
      this.sButtons.add(new ClickGuiScreen.SButton(28, this.contentX, row2, half, bh, "Rename"));
      this.sButtons.add(new ClickGuiScreen.SButton(29, this.contentX + half + gap, row2, half, bh, "Save Selection"));
   }

   private void drawPointsPage(int mouseX, int mouseY) {
      int y = this.contentY + 44;

      BlockPos a = Selection.getA();
      BlockPos b = Selection.getB();

      this.fontRendererObj.drawStringWithShadow("REGION", (float)this.contentX, (float)(this.contentY + 6), -8088413);

      this.fontRendererObj.drawStringWithShadow(
            "Point A: " + (a == null ? "unset" : a.getX() + ", " + a.getY() + ", " + a.getZ()),
            (float)this.contentX, (float)y, a == null ? -8088413 : -1379073);

      this.fontRendererObj.drawStringWithShadow(
            "Point B: " + (b == null ? "unset" : b.getX() + ", " + b.getY() + ", " + b.getZ()),
            (float)this.contentX, (float)(y + 12), b == null ? -8088413 : -1379073);

      int[] size = Selection.size();
      String sizeText = size == null
            ? "Size: -    (set both points, or bind SelectionTool's keys)"
            : "Size: " + size[0] + " x " + size[1] + " x " + size[2] + "    (" + Selection.volume() + " blocks)";
      this.fontRendererObj.drawStringWithShadow(sizeText, (float)this.contentX, (float)(y + 28),
            size == null ? -8088413 : -10696961);

      // The cap mirrors the bridge's, so the limit is visible before you click
      // Save rather than as a failure message afterwards.
      if(Selection.volume() > 4000000L) {
         this.fontRendererObj.drawStringWithShadow("Too large to save (max 4,000,000 blocks)",
               (float)this.contentX, (float)(y + 40), -43691);
      }

      String nameLabel = "Save as: " + this.saveName + (this.editingSaveName ? "_" : ".schematic");
      this.fontRendererObj.drawStringWithShadow(nameLabel, (float)this.contentX,
            (float)(this.contentY + 84), this.editingSaveName ? -10696961 : -8088413);

      this.drawButtons(mouseX, mouseY);

      if(!this.schemStatus.isEmpty()) {
         this.fontRendererObj.drawStringWithShadow(this.trim(this.schemStatus, this.contentW),
               (float)this.contentX, (float)(this.contentY + this.contentH - 8), -8088413);
      }

   }

   private void refreshSchemFiles() {
      this.schemFiles.clear();
      if(SchematicaBridge.isAvailable()) {
         this.schemFiles.addAll(SchematicaBridge.listFiles());
      }

      int max = Math.max(0, this.schemFiles.size() * 14 - this.schemListH);
      if(this.schemScroll > max) {
         this.schemScroll = max;
      }

      if(this.schemScroll < 0) {
         this.schemScroll = 0;
      }

   }

   private void drawSchematicPanel(int mouseX, int mouseY) {
      this.drawSchematicTabs(mouseX, mouseY);

      if(this.schemTab == 1) {
         this.drawTransformPage(mouseX, mouseY);
         return;
      }

      if(this.schemTab == 2) {
         this.drawPointsPage(mouseX, mouseY);
         return;
      }

      if(!SchematicaBridge.isAvailable()) {
         this.fontRendererObj.drawStringWithShadow("Schematica mod isn\'t installed.", (float)this.contentX, (float)(this.contentY + 22), -8088413);
         this.drawButtons(mouseX, mouseY);
      } else {
         boolean has = SchematicaBridge.hasSchematic();
         String header = has?"Active: " + (this.loadedFile != null?this.loadedFile:SchematicaBridge.name()):"No schematic loaded";
         this.fontRendererObj.drawStringWithShadow(this.trim(header, this.contentW), (float)this.contentX, (float)(this.contentY + 6), has?-10696961:-8088413);
         drawRect(this.schemListX, this.schemListY, this.schemListX + this.schemListW, this.schemListY + this.schemListH, 570425344);
         if(this.schemFiles.isEmpty()) {
            this.fontRendererObj.drawStringWithShadow("No files.", (float)(this.schemListX + 4), (float)(this.schemListY + 4), -8088413);
         } else {
            for(int i = 0; i < this.schemFiles.size(); ++i) {
               int rowY = this.schemListY + i * 14 - this.schemScroll;
               if(rowY + 14 >= this.schemListY && rowY <= this.schemListY + this.schemListH) {
                  String name = (String)this.schemFiles.get(i);
                  boolean isActive = name.equals(this.loadedFile);
                  boolean hov = mouseX >= this.schemListX && mouseX <= this.schemListX + this.schemListW && mouseY >= rowY && mouseY <= rowY + 14 && mouseY >= this.schemListY && mouseY <= this.schemListY + this.schemListH;
                  if(isActive) {
                     drawRect(this.schemListX, rowY, this.schemListX + this.schemListW, rowY + 14, -14069917);
                  } else if(hov) {
                     drawRect(this.schemListX, rowY, this.schemListX + this.schemListW, rowY + 14, 486539263);
                  }

                  this.fontRendererObj.drawStringWithShadow(this.trim(name, this.schemListW - 8), (float)(this.schemListX + 4), (float)(rowY + 3), isActive?-1379073:-8088413);
               }
            }
         }

         int colX = this.schemListX + this.schemListW + 8;
         int by = this.schemListY + 12;
         int rowGap = 20;
         this.fontRendererObj.drawStringWithShadow("POSITION", (float)colX, (float)this.schemListY, -8088413);
         int[] pos = SchematicaBridge.position();
         String[] axes = new String[]{"X", "Y", "Z"};

         for(int i = 0; i < 3; ++i) {
            boolean editing = this.editingAxis == i;
            String v = editing
                  ? this.axisInput + "_"
                  : (pos == null ? "-" : String.valueOf(pos[i]));

            if(editing) {
               this.roundRect(colX - 2, by + i * rowGap, colX + 62, by + i * rowGap + 14, 3.0F, -14141369);
            }

            // Click the value to type a coordinate outright -- nudging from
            // 4837 to a number across the map with +/- is not realistic.
            this.fontRendererObj.drawStringWithShadow(axes[i] + " " + v, (float)colX,
                  (float)(by + i * rowGap + 3), editing ? -10696961 : (has ? -1379073 : -8088413));
         }

         this.drawButtons(mouseX, mouseY);
         if(!this.schemStatus.isEmpty()) {
            this.fontRendererObj.drawStringWithShadow(this.trim(this.schemStatus, this.contentW), (float)this.contentX, (float)(this.schemListY + this.schemListH + 2), -8088413);
         }

      }
   }

   private void drawButtons(int mouseX, int mouseY) {
      Module printer = ModuleManager.getByName("Printer");

      for(ClickGuiScreen.SButton b : this.sButtons) {
         boolean hov = b.hit(mouseX, mouseY);
         drawRect(b.x, b.y, b.x + b.w, b.y + b.h, hov?486539263:218103807);
         String label = b.label;
         int color = -1379073;
         if(b.id == 7) {
            boolean on = SchematicaBridge.isAvailable() && SchematicaBridge.isRendering();
            label = "Render: " + (on?"ON":"OFF");
            color = on?-10696961:-8088413;
         } else if(b.id == 8) {
            boolean on = printer != null && printer.isEnabled();
            label = "Printer: " + (on?"ON":"OFF");
            color = on?-10696961:-8088413;
         }

         int tw = this.fontRendererObj.getStringWidth(label);
         this.fontRendererObj.drawStringWithShadow(label, (float)(b.x + b.w / 2 - tw / 2), (float)(b.y + b.h / 2 - 4), color);
      }

   }

   private void handleSchematicClick(int mouseX, int mouseY, int mouseButton) {
      if(mouseButton == 0) {
         // Tabs first: they overlay the same region the pages draw into, and a
         // page hit box must never swallow a tab click.
         if(this.handleSchemTabClick(mouseX, mouseY)) {
            return;
         }

         for(ClickGuiScreen.SButton b : this.sButtons) {
            if(b.hit(mouseX, mouseY)) {
               this.schematicAction(b.id);
               return;
            }
         }

         // Position values on the Browser tab are click-to-type.
         if(this.schemTab == 0 && SchematicaBridge.isAvailable() && SchematicaBridge.hasSchematic()) {
            int colX = this.schemListX + this.schemListW + 8;
            int by = this.schemListY + 12;

            if(mouseX >= colX - 2 && mouseX <= colX + 62) {
               for(int i = 0; i < 3; ++i) {
                  int rowTop = by + i * 20;
                  if(mouseY >= rowTop && mouseY <= rowTop + 14) {
                     int[] p = SchematicaBridge.position();
                     this.editingAxis = i;
                     this.axisInput = p == null ? "" : String.valueOf(p[i]);
                     return;
                  }
               }
            }
         }

         // The file list only exists on the Browser tab.
         if(this.schemTab == 0 && SchematicaBridge.isAvailable() && mouseX >= this.schemListX && mouseX <= this.schemListX + this.schemListW && mouseY >= this.schemListY && mouseY <= this.schemListY + this.schemListH) {
            for(int i = 0; i < this.schemFiles.size(); ++i) {
               int rowY = this.schemListY + i * 14 - this.schemScroll;
               if(mouseY >= rowY && mouseY <= rowY + 14 && rowY >= this.schemListY && rowY + 14 <= this.schemListY + this.schemListH + 14) {
                  String name = (String)this.schemFiles.get(i);
                  boolean ok = SchematicaBridge.load(name);
                  this.loadedFile = ok?name:this.loadedFile;
                  this.schemStatus = ok?"Loaded " + name + " -- now Move Here.":"Failed to load " + name;
                  return;
               }
            }
         }

      }
   }

   private void schematicAction(int id) {
      int step = isShiftKeyDown()?5:1;
      switch(id) {
      case 0:
         SchematicaBridge.nudge(-step, 0, 0);
         break;
      case 1:
         SchematicaBridge.nudge(step, 0, 0);
         break;
      case 2:
         SchematicaBridge.nudge(0, -step, 0);
         break;
      case 3:
         SchematicaBridge.nudge(0, step, 0);
         break;
      case 4:
         SchematicaBridge.nudge(0, 0, -step);
         break;
      case 5:
         SchematicaBridge.nudge(0, 0, step);
         break;
      case 6:
         SchematicaBridge.moveHere();
         this.schemStatus = SchematicaBridge.hasSchematic()?"Moved to you.":"Load a schematic first.";
         break;
      case 7:
         SchematicaBridge.setRendering(!SchematicaBridge.isRendering());
         break;
      case 8:
         Module printer = ModuleManager.getByName("Printer");
         if(printer != null) {
            printer.toggle();
         }
         break;
      case 9:
         String written = SchematicaBridge.writeTestSchematic();
         this.refreshSchemFiles();
         this.schemStatus = written == null?"Couldn\'t write test box.":"Wrote " + written + " -- click it to load.";
         break;
      case 10:
         this.openFolder();
         break;
      case 11:
         SchematicaBridge.unload();
         this.loadedFile = null;
         this.schemStatus = "Unloaded.";
         break;

      // --- Transform tab ---
      case 12:
         this.schemStatus = SchematicaBridge.rotate(true)
               ? "Rotated clockwise." : "Rotate failed -- load a schematic first.";
         break;
      case 13:
         this.schemStatus = SchematicaBridge.rotate(false)
               ? "Rotated counter-clockwise." : "Rotate failed -- load a schematic first.";
         break;
      case 14:
         this.schemStatus = SchematicaBridge.flip(EnumFacing.EAST)
               ? "Flipped on X." : "Flip failed -- load a schematic first.";
         break;
      case 15:
         this.schemStatus = SchematicaBridge.flip(EnumFacing.SOUTH)
               ? "Flipped on Z." : "Flip failed -- load a schematic first.";
         break;
      case 16:
         SchematicaBridge.moveHere();
         this.schemStatus = SchematicaBridge.hasSchematic() ? "Moved to you." : "Load a schematic first.";
         break;
      case 17:
         this.copyPlacement();
         break;
      case 18:
         this.applyPlacement();
         break;
      case 19:
         SchematicaBridge.nudge(-step, 0, 0);
         break;
      case 20:
         SchematicaBridge.nudge(step, 0, 0);
         break;
      case 21:
         SchematicaBridge.nudge(0, -step, 0);
         break;
      case 22:
         SchematicaBridge.nudge(0, step, 0);
         break;
      case 23:
         SchematicaBridge.nudge(0, 0, -step);
         break;
      case 24:
         SchematicaBridge.nudge(0, 0, step);
         break;

      // --- Points tab ---
      case 25:
         this.setPointToPlayer(true);
         break;
      case 26:
         this.setPointToPlayer(false);
         break;
      case 27:
         Selection.clear();
         this.schemStatus = "Selection cleared.";
         break;
      case 28:
         // Typing is handled in keyTyped while this flag is set, rather than
         // with a text-field widget this GUI does not have.
         this.editingSaveName = !this.editingSaveName;
         this.schemStatus = this.editingSaveName ? "Type a name, Enter to finish." : "";
         break;
      case 29:
         this.saveSelection();
      }

   }

   /**
    * Applies the typed coordinate to the loaded schematic.
    *
    * <p>Expressed as a delta because {@code nudge} is relative -- there is no
    * absolute setter on the bridge, and re-deriving the delta here keeps that
    * detail out of the input handling.
    */
   private void commitAxis() {
      int axis = this.editingAxis;
      String text = this.axisInput;
      this.editingAxis = -1;
      this.axisInput = "";

      int[] cur = SchematicaBridge.position();
      if(cur == null || axis < 0 || text.isEmpty() || "-".equals(text)) {
         return;
      }

      try {
         int target = Integer.parseInt(text);
         int dx = axis == 0 ? target - cur[0] : 0;
         int dy = axis == 1 ? target - cur[1] : 0;
         int dz = axis == 2 ? target - cur[2] : 0;
         SchematicaBridge.nudge(dx, dy, dz);
         this.schemStatus = "Moved to " + (axis == 0 ? target : cur[0]) + ", "
               + (axis == 1 ? target : cur[1]) + ", " + (axis == 2 ? target : cur[2]);
      } catch (NumberFormatException e) {
         this.schemStatus = "Not a number.";
      }

   }

   private void setPointToPlayer(boolean isA) {
      if(this.mc.thePlayer == null) {
         return;
      }

      BlockPos pos = new BlockPos(
            MathHelper.floor_double(this.mc.thePlayer.posX),
            MathHelper.floor_double(this.mc.thePlayer.posY),
            MathHelper.floor_double(this.mc.thePlayer.posZ));

      if(isA) {
         Selection.setA(pos);
      } else {
         Selection.setB(pos);
      }

      this.schemStatus = "Point " + (isA ? "A" : "B") + " set to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
   }

   private void saveSelection() {
      if(!Selection.isComplete()) {
         this.schemStatus = "Set both points first.";
         return;
      }

      String written = SchematicaBridge.saveRegion(Selection.getA(), Selection.getB(), this.saveName);
      if(written == null) {
         this.schemStatus = "Save failed -- region may be too large.";
         return;
      }

      this.refreshSchemFiles();
      this.schemStatus = "Saved " + written + " -- find it on the Browser tab.";
   }

   private void copyPlacement() {
      int[] p = SchematicaBridge.position();
      if(p == null) {
         this.schemStatus = "Load a schematic first.";
         return;
      }

      String descriptor = "schem:" + SchematicaBridge.name() + "@" + p[0] + "," + p[1] + "," + p[2];

      try {
         Toolkit.getDefaultToolkit().getSystemClipboard()
               .setContents(new StringSelection(descriptor), null);
         this.schemStatus = "Placement copied to clipboard.";
      } catch (Throwable t) {
         this.schemStatus = "Clipboard unavailable.";
      }

   }

   private void applyPlacement() {
      int[] cur = SchematicaBridge.position();
      if(cur == null) {
         this.schemStatus = "Load a schematic first.";
         return;
      }

      String text = null;
      try {
         Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
         if(t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            text = (String)t.getTransferData(DataFlavor.stringFlavor);
         }
      } catch (Throwable ignored) {
         // Clipboard owned by another process -- treated as empty below.
      }

      if(text == null || !text.startsWith("schem:")) {
         this.schemStatus = "No placement on the clipboard.";
         return;
      }

      int at = text.lastIndexOf(64);
      String[] parts = at < 0 ? null : text.substring(at + 1).split(",");
      if(parts == null || parts.length != 3) {
         this.schemStatus = "Malformed placement.";
         return;
      }

      try {
         int tx = Integer.parseInt(parts[0].trim());
         int ty = Integer.parseInt(parts[1].trim());
         int tz = Integer.parseInt(parts[2].trim());

         // nudge() is relative, so express the move as a delta from where the
         // schematic currently sits.
         SchematicaBridge.nudge(tx - cur[0], ty - cur[1], tz - cur[2]);
         this.schemStatus = "Moved to " + tx + ", " + ty + ", " + tz + ".";
      } catch (NumberFormatException e) {
         this.schemStatus = "Malformed placement.";
      }

   }

   private void openFolder() {
      File dir = SchematicaBridge.isAvailable()?SchematicaBridge.directory():null;
      if(dir != null) {
         this.schemStatus = "Folder: " + dir.getAbsolutePath();

         try {
            Desktop.getDesktop().open(dir);
         } catch (Throwable var3) {
            ;
         }

      }
   }

   public void onGuiClosed() {
      ConfigManager.save();
   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   private static class Card {
      String name;
      Module module;
      List<Module> members;
      int x;
      int y;
      int w;
      int h;

      private Card() {
      }

      boolean isGroup() {
         return this.members != null;
      }

      boolean active() {
         if(this.module != null) {
            return this.module.isEnabled();
         } else {
            for(Module m : this.members) {
               if(m.isEnabled()) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private static enum DKind {
      SECTION,
      MEMBER,
      KEYBIND,
      BOOL,
      NUMBER,
      MODE,
      KEYSETTING,
      COLOR;

      private DKind() {
      }
   }

   private static class DrawerEntry {
      ClickGuiScreen.DKind kind;
      String label;
      Module module;
      Setting setting;
      int y;
      int h;

      private DrawerEntry() {
      }
   }

   private static class NavItem {
      String label;
      ClickGuiScreen.View view;
      boolean action;
      Runnable run;
      int x;
      int y;
      int w;
      int h;

      private NavItem() {
      }
   }

   private static class SButton {
      int id;
      int x;
      int y;
      int w;
      int h;
      String label;

      SButton(int id, int x, int y, int w, int h, String label) {
         this.id = id;
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.label = label;
      }

      boolean hit(int mx, int my) {
         return mx >= this.x && mx <= this.x + this.w && my >= this.y && my <= this.y + this.h;
      }
   }

   private static class TabItem {
      ModuleCategory category;
      String label;
      int x;
      int y;
      int w;
      int h;

      private TabItem() {
      }
   }

   // ---------------------------------------------------------------------
   // Macros / FPS pages
   // ---------------------------------------------------------------------

   /** Row height for both list pages. */
   private static final int PAGE_ROW_H = 18;

   private String macroInput = "";
   private boolean macroInputFocused;
   private com.iceclient.macro.Macro bindingMacro;

   /** The macro whose cooldown is being typed, and the raw text so far. */
   private com.iceclient.macro.Macro editingCooldown;
   private String cooldownInput = "";

   /**
    * Parses a duration with an optional unit suffix into seconds.
    *
    * <p>Accepts {@code 250ms}, {@code 10s}, {@code 2m} and a bare number, which
    * is read as seconds. Milliseconds matter here because macro cooldowns are
    * used to stay under server rate limits, and those are often tighter than a
    * whole second -- the old right-click cycle could only reach 5/10/20/30s.
    *
    * @return seconds, or -1 when the text does not parse
    */
   private static double parseDuration(String text) {
      if(text == null) {
         return -1.0D;
      }

      String s = text.trim().toLowerCase();
      if(s.isEmpty()) {
         return -1.0D;
      }

      double mult = 1.0D;
      if(s.endsWith("ms")) {
         mult = 0.001D;
         s = s.substring(0, s.length() - 2);
      } else if(s.endsWith("s")) {
         s = s.substring(0, s.length() - 1);
      } else if(s.endsWith("m")) {
         mult = 60.0D;
         s = s.substring(0, s.length() - 1);
      }

      try {
         double v = Double.parseDouble(s.trim());
         return v < 0.0D ? -1.0D : v * mult;
      } catch (NumberFormatException e) {
         return -1.0D;
      }
   }

   /** Formats seconds back into the shortest sensible unit for display. */
   private static String formatDuration(double seconds) {
      if(seconds <= 0.0D) {
         return "-";
      }

      if(seconds < 1.0D) {
         return Math.round(seconds * 1000.0D) + "ms";
      }

      if(seconds >= 60.0D && seconds % 60.0D == 0.0D) {
         return (int)(seconds / 60.0D) + "m";
      }

      return seconds % 1.0D == 0.0D
            ? (int)seconds + "s"
            : String.format("%.2fs", Double.valueOf(seconds));
   }
   private int pageScroll;

   /** Toggle pill matching the rest of the GUI: 20x10, knob slides on state. */
   private void pill(int x, int y, boolean on) {
      this.roundRect(x, y, x + 20, y + 10, 5.0F, on ? -10696961 : -14141369);
      int knob = on ? x + 11 : x + 1;
      this.roundRect(knob, y + 1, knob + 8, y + 9, 4.0F, -1379073);
   }

   /** Shared title block for the simple pages. */
   private void drawPageHeader(String title, String subtitle) {
      this.fontRendererObj.drawString(title, this.contentX, 20, -1379073);
      this.fontRendererObj.drawString(subtitle, this.contentX, 34, -8088413);
   }

   private void drawMacrosPage(int mx, int my) {
      java.util.List<com.iceclient.macro.Macro> macros =
            com.iceclient.module.modules.misc.Macros.getMacros();

      int x = this.contentX;
      int w = this.contentW;
      int y = this.contentY - this.pageScroll;

      // Input row: type the command, then click Add.
      int inputW = w - 60;
      this.roundRect(x, y, x + inputW, y + 16, 3.0F, this.macroInputFocused ? -14069917 : 218103807);
      String shown = this.macroInput.isEmpty() && !this.macroInputFocused
            ? "Message or command..."
            : this.macroInput + (this.macroInputFocused ? "_" : "");
      this.fontRendererObj.drawString(shown, x + 5, y + 4,
            this.macroInput.isEmpty() && !this.macroInputFocused ? -11642264 : -1379073);

      this.roundRect(x + inputW + 6, y, x + w, y + 16, 3.0F, 218103807);
      this.fontRendererObj.drawString("ADD", x + inputW + 18, y + 4, -10696961);

      y += 24;

      if(macros.isEmpty()) {
         this.fontRendererObj.drawString("No macros yet - type a command above and click ADD.", x, y + 4, -11642264);
         this.fontRendererObj.drawString("Then click the key box to bind it.", x, y + 16, -11642264);
         return;
      }

      for(com.iceclient.macro.Macro m : new java.util.ArrayList<com.iceclient.macro.Macro>(macros)) {
         boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + PAGE_ROW_H - 2;
         this.roundRect(x, y, x + w, y + PAGE_ROW_H - 2, 3.0F, hover ? 486539263 : 218103807);

         String text = m.getText();
         int maxText = w - 150;
         while(this.fontRendererObj.getStringWidth(text) > maxText && text.length() > 1) {
            text = text.substring(0, text.length() - 1);
         }

         this.fontRendererObj.drawString(text, x + 6, y + 4, m.isEnabled() ? -1379073 : -11642264);

         // Cooldown readout, so a macro that's waiting is visible at a glance.
         // Click it to type an exact value with a unit.
         boolean editingCd = this.editingCooldown == m;
         String cd = editingCd
               ? this.cooldownInput + "_"
               : formatDuration(m.getCooldownSeconds());

         if(editingCd) {
            this.roundRect(x + w - 132, y + 1, x + w - 100, y + PAGE_ROW_H - 4, 3.0F, -14141369);
         }

         this.fontRendererObj.drawString(cd, x + w - 128, y + 4, editingCd ? -10696961 : -8088413);

         // Key box
         boolean binding = this.bindingMacro == m;
         String key = binding ? "..." : m.getKeyName();
         int kw = Math.max(34, this.fontRendererObj.getStringWidth(key) + 10);
         this.roundRect(x + w - 96, y + 1, x + w - 96 + kw, y + PAGE_ROW_H - 4, 3.0F, -14141369);
         this.fontRendererObj.drawString(key, x + w - 91, y + 4, binding ? -10696961 : -1379073);

         // Enabled pill
         this.pill(x + w - 46, y + 3, m.isEnabled());

         // Remove
         this.fontRendererObj.drawString("x", x + w - 14, y + 4, -43691);

         y += PAGE_ROW_H;
      }

   }

   private void drawFpsPage(int mx, int my) {
      int x = this.contentX;
      int w = this.contentW;
      int y = this.contentY - this.pageScroll;

      for(Module m : this.fpsModules()) {
         boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + PAGE_ROW_H - 2;
         this.roundRect(x, y, x + w, y + PAGE_ROW_H - 2, 3.0F, hover ? 486539263 : 218103807);
         this.fontRendererObj.drawString(m.getName(), x + 6, y + 4, m.isEnabled() ? -1379073 : -8088413);

         String desc = m.getDescription();
         int maxDesc = w - 200;
         if(desc != null && maxDesc > 40) {
            while(this.fontRendererObj.getStringWidth(desc) > maxDesc && desc.length() > 1) {
               desc = desc.substring(0, desc.length() - 1);
            }

            this.fontRendererObj.drawString(desc, x + 130, y + 4, -11642264);
         }

         this.pill(x + w - 30, y + 3, m.isEnabled());
         y += PAGE_ROW_H;
      }

   }

   /**
    * Modules that affect performance or rendering load.
    *
    * <p>Selected by name rather than category: they live in different
    * categories for the module grid, but belong together here.
    */
   private java.util.List<Module> fpsModules() {
      String[] names = new String[]{"FPS", "FPS Particles", "Clear Water", "Fullbright",
            "No Fog", "No Weather", "No Fire", "Chunk Borders", "Hit Boxes", "Trails", "Nametags"};
      java.util.List<Module> out = new java.util.ArrayList();

      for(String n : names) {
         Module m = ModuleManager.getByName(n);
         if(m != null) {
            out.add(m);
         }
      }

      return out;
   }

   private void handleMacrosClick(int mouseX, int mouseY, int button) {
      java.util.List<com.iceclient.macro.Macro> macros =
            com.iceclient.module.modules.misc.Macros.getMacros();

      int x = this.contentX;
      int w = this.contentW;
      int y = this.contentY - this.pageScroll;
      int inputW = w - 60;

      if(mouseY >= y && mouseY <= y + 16) {
         if(mouseX >= x && mouseX <= x + inputW) {
            this.macroInputFocused = true;
            return;
         }

         if(mouseX >= x + inputW + 6 && mouseX <= x + w) {
            this.addMacroFromInput();
            return;
         }
      }

      this.macroInputFocused = false;
      y += 24;

      for(com.iceclient.macro.Macro m : new java.util.ArrayList<com.iceclient.macro.Macro>(macros)) {
         if(mouseY >= y && mouseY <= y + PAGE_ROW_H - 2) {
            if(mouseX >= x + w - 96 && mouseX <= x + w - 52) {
               this.bindingMacro = m;
            } else if(mouseX >= x + w - 46 && mouseX <= x + w - 22) {
               m.setEnabled(!m.isEnabled());
            } else if(mouseX >= x + w - 18) {
               com.iceclient.module.modules.misc.Macros.remove(m);
            } else if(mouseX >= x + w - 132 && mouseX <= x + w - 100) {
               // Click the cooldown to type an exact value; the old right-click
               // cycle could only reach 5/10/20/30s.
               this.editingCooldown = m;
               this.cooldownInput = m.getCooldownSeconds() > 0.0D
                     ? formatDuration(m.getCooldownSeconds())
                     : "";
               this.macroInputFocused = false;
            } else if(button == 1) {
               // Right-click still cycles, as the quick path.
               double cd = m.getCooldownSeconds();
               m.setCooldownSeconds(cd <= 0.0D ? 5.0D : (cd < 10.0D ? 10.0D : (cd < 20.0D ? 20.0D : (cd < 30.0D ? 30.0D : 0.0D))));
            }

            return;
         }

         y += PAGE_ROW_H;
      }

   }

   private void handleFpsClick(int mouseX, int mouseY, int button) {
      int x = this.contentX;
      int w = this.contentW;
      int y = this.contentY - this.pageScroll;

      for(Module m : this.fpsModules()) {
         if(mouseY >= y && mouseY <= y + PAGE_ROW_H - 2 && mouseX >= x && mouseX <= x + w) {
            m.toggle();
            return;
         }

         y += PAGE_ROW_H;
      }

   }

   private void addMacroFromInput() {
      String text = this.macroInput.trim();
      if(!text.isEmpty()) {
         com.iceclient.macro.Macro added =
               com.iceclient.module.modules.misc.Macros.add(text, 0);
         this.macroInput = "";
         this.macroInputFocused = false;

         // Modules start disabled; a macro in a switched-off module never fires.
         Module macros = ModuleManager.getByName("Macros");
         if(macros != null && !macros.isEnabled()) {
            macros.setEnabled(true);
         }

         // Jump straight into binding -- a macro with no key does nothing, and
         // that's the single most likely reason one "doesn't work".
         this.bindingMacro = added;
      }

   }

   private static enum View {
      MODULES,
      MACROS,
      FPS,
      SCHEMATIC,
      ADMIN;

      private View() {
      }
   }
}
