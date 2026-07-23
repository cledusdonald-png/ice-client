package com.iceclient.gui;

import com.iceclient.cosmetic.Cosmetic;
import com.iceclient.cosmetic.CosmeticManager;
import com.iceclient.cosmetic.CosmeticRegistry;
import com.iceclient.cosmetic.CosmeticType;
import com.iceclient.cosmetic.EmoteManager;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Radial emote picker, open only while its key is held.
 *
 * <p>Hold-to-open rather than click-to-open because an emote is a half-second
 * decision in the middle of doing something else: a menu you have to open and
 * then close is slower than the thing it is for. Releasing the key plays
 * whatever the cursor is nearest, so the whole interaction is one gesture.
 *
 * <p>Selection is by <em>angle</em>, not by hit box. Anywhere in a wedge counts,
 * so a flick in roughly the right direction picks correctly without aiming --
 * which is the point of a wheel over a list.
 */
public class EmoteWheelScreen extends GuiScreen {

   private static final int RING = 78;
   private static final int DEAD_ZONE = 22;

   private static final int BG = 0xC0060A12;
   private static final int SLICE = 0xB00E2033;
   private static final int SLICE_ON = 0xE01D4E70;
   private static final int ACCENT = 0xFF5CC6FF;
   private static final int TEXT = 0xFFEAF4FF;
   private static final int MUTED = 0xFF7D8DA0;
   private static final int LOCKED = 0xFF4A5462;

   private final int holdKey;
   private final List<Cosmetic> emotes = new ArrayList<Cosmetic>();
   private int hovered = -1;

   public EmoteWheelScreen(int holdKey) {
      this.holdKey = holdKey;

      // Owned first, so the wheel is mostly things you can actually play.
      for(Cosmetic c : CosmeticRegistry.ofType(CosmeticType.EMOTE)) {
         if(CosmeticManager.owns(c.getId())) {
            this.emotes.add(c);
         }
      }

      for(Cosmetic c : CosmeticRegistry.ofType(CosmeticType.EMOTE)) {
         if(!CosmeticManager.owns(c.getId())) {
            this.emotes.add(c);
         }
      }
   }

   @Override
   public void updateScreen() {
      // The key going up is the confirm. Checked here rather than in keyTyped
      // because Minecraft delivers key-release to a screen inconsistently.
      if(!Keyboard.isKeyDown(this.holdKey)) {
         play();
         this.mc.displayGuiScreen(null);
      }
   }

   private void play() {
      if(this.hovered < 0 || this.hovered >= this.emotes.size()) {
         return;
      }

      Cosmetic c = this.emotes.get(this.hovered);
      if(!CosmeticManager.owns(c.getId())) {
         return;
      }

      CosmeticManager.equip(c.getId());
      EmoteManager.start(this.mc.thePlayer, c.getId());
      com.iceclient.IceClient.COSMETIC_SYNC.pokeNow();
   }

   @Override
   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      int cx = this.width / 2;
      int cy = this.height / 2;
      int n = this.emotes.size();

      if(n == 0) {
         drawCenteredString(this.fontRendererObj, "No emotes yet — buy one in the wardrobe.",
               cx, cy - 4, MUTED);
         return;
      }

      // Which wedge the cursor is in. Straight up is the first entry, so the
      // list reads clockwise from twelve o'clock.
      double dx = mouseX - cx;
      double dy = mouseY - cy;
      double dist = Math.sqrt(dx * dx + dy * dy);

      if(dist < DEAD_ZONE) {
         this.hovered = -1;
      } else {
         double ang = Math.toDegrees(Math.atan2(dx, -dy));
         if(ang < 0.0D) {
            ang += 360.0D;
         }

         double step = 360.0D / n;
         this.hovered = (int)Math.floor((ang + step / 2.0D) % 360.0D / step);
      }

      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);

      ring(cx, cy, DEAD_ZONE, RING + 18, BG);

      double step = 360.0D / n;
      for(int i = 0; i < n; ++i) {
         boolean on = i == this.hovered;
         double from = i * step - step / 2.0D - 90.0D;
         wedge(cx, cy, DEAD_ZONE + 2, RING, from + 1.5D, from + step - 1.5D,
               on ? SLICE_ON : SLICE);
      }

      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();

      for(int i = 0; i < n; ++i) {
         Cosmetic c = this.emotes.get(i);
         boolean owned = CosmeticManager.owns(c.getId());
         boolean on = i == this.hovered;

         double a = Math.toRadians(i * step - 90.0D);
         int lx = cx + (int)(Math.cos(a) * (DEAD_ZONE + RING) / 2.0D);
         int ly = cy + (int)(Math.sin(a) * (DEAD_ZONE + RING) / 2.0D);

         drawCenteredString(this.fontRendererObj, c.getName(), lx, ly - 8,
               owned ? (on ? TEXT : MUTED) : LOCKED);

         if(!owned) {
            drawCenteredString(this.fontRendererObj, c.getPrice() + " frost", lx, ly + 2, LOCKED);
         }
      }

      // Centre: what releasing now would do.
      String label = this.hovered < 0
            ? "release to cancel"
            : this.emotes.get(this.hovered).getName();
      int col = this.hovered < 0
            ? MUTED
            : (CosmeticManager.owns(this.emotes.get(this.hovered).getId()) ? ACCENT : LOCKED);
      drawCenteredString(this.fontRendererObj, label, cx, cy - 4, col);
   }

   /** Filled annulus, used for the backdrop. */
   private static void ring(int cx, int cy, int inner, int outer, int color) {
      wedge(cx, cy, inner, outer, 0.0D, 360.0D, color);
   }

   /** A slice of an annulus between two angles, in degrees clockwise from east. */
   private static void wedge(int cx, int cy, int inner, int outer,
                             double from, double to, int color) {
      float a = (color >>> 24 & 255) / 255.0F;
      float r = (color >> 16 & 255) / 255.0F;
      float g = (color >> 8 & 255) / 255.0F;
      float b = (color & 255) / 255.0F;
      GlStateManager.color(r, g, b, a);

      GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
      double stepDeg = 3.0D;

      for(double d = from; d <= to; d += stepDeg) {
         double rad = Math.toRadians(d);
         double c = Math.cos(rad);
         double s = Math.sin(rad);
         GL11.glVertex2d(cx + c * inner, cy + s * inner);
         GL11.glVertex2d(cx + c * outer, cy + s * outer);
      }

      // Close the final sliver exactly, or wedges end a degree or two short.
      double rad = Math.toRadians(to);
      GL11.glVertex2d(cx + Math.cos(rad) * inner, cy + Math.sin(rad) * inner);
      GL11.glVertex2d(cx + Math.cos(rad) * outer, cy + Math.sin(rad) * outer);
      GL11.glEnd();
   }

   @Override
   public boolean doesGuiPauseGame() {
      return false;
   }
}
