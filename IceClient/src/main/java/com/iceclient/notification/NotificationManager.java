package com.iceclient.notification;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleManager;
import com.iceclient.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Global toast queue, drawn top-right and stacked downward.
 *
 * <p>Static so any module can post without holding a reference -- raid alerts,
 * the printer finishing, a schematic loading. The {@code Notifications} module
 * only gates rendering and owns the settings; posting still works while it is
 * off, the toasts simply are not drawn, so nothing has to null-check the module.
 */
public class NotificationManager {

   private static final List<Notification> ACTIVE = new ArrayList<Notification>();

   /** Beyond this the stack covers the screen; oldest gets dropped. */
   private static final int MAX_VISIBLE = 5;

   private static final int WIDTH = 160;
   private static final int HEIGHT = 30;
   private static final int GAP = 4;
   private static final int MARGIN = 6;
   private static final int ACCENT_W = 3;

   public static void init() {
      MinecraftForge.EVENT_BUS.register(new NotificationManager());
   }

   public static void post(String title, String message) {
      post(title, message, Notification.Type.INFO, 3000L);
   }

   public static void post(String title, String message, Notification.Type type) {
      post(title, message, type, 3000L);
   }

   public static void post(String title, String message, Notification.Type type, long lifetimeMs) {
      synchronized(ACTIVE) {
         ACTIVE.add(new Notification(title, message, type, lifetimeMs));
         while(ACTIVE.size() > MAX_VISIBLE) {
            ACTIVE.remove(0);
         }
      }
   }

   public static void clear() {
      synchronized(ACTIVE) {
         ACTIVE.clear();
      }
   }

   @SubscribeEvent
   public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
      if(event.type != RenderGameOverlayEvent.ElementType.ALL) {
         return;
      }

      Module gate = ModuleManager.getByName("Notifications");
      if(gate == null || !gate.isEnabled()) {
         return;
      }

      // Reap expired entries before measuring, otherwise a toast that died this
      // frame still reserves a slot and the stack visibly jumps when it goes.
      synchronized(ACTIVE) {
         Iterator<Notification> it = ACTIVE.iterator();
         while(it.hasNext()) {
            if(it.next().isExpired()) {
               it.remove();
            }
         }

         if(ACTIVE.isEmpty()) {
            return;
         }

         Minecraft mc = Minecraft.getMinecraft();
         ScaledResolution res = new ScaledResolution(mc);
         FontRenderer font = mc.fontRendererObj;

         GlStateManager.pushMatrix();
         GlStateManager.enableBlend();

         int y = MARGIN;
         for(Notification n : ACTIVE) {
            float vis = n.visibility();

            // Slide in from off the right edge rather than fading alone -- a
            // pure fade reads as a glitch at high frame rates.
            int x = res.getScaledWidth() - MARGIN - Math.round((float)WIDTH * vis);
            int alpha = Math.max(0, Math.min(255, Math.round(255.0F * vis)));

            Gui.drawRect(x, y, x + WIDTH, y + HEIGHT, ColorUtil.withAlpha(0x101014, alpha * 220 / 255));
            Gui.drawRect(x, y, x + ACCENT_W, y + HEIGHT, ColorUtil.withAlpha(n.accent(), alpha));

            int textX = x + ACCENT_W + 5;
            font.drawStringWithShadow(n.getTitle(), (float)textX, (float)(y + 7),
                  ColorUtil.withAlpha(0xFFFFFF, alpha));
            font.drawStringWithShadow(trim(font, n.getMessage(), WIDTH - ACCENT_W - 10),
                  (float)textX, (float)(y + 18), ColorUtil.withAlpha(0xAAAAAA, alpha));

            y += HEIGHT + GAP;
         }

         GlStateManager.disableBlend();
         GlStateManager.popMatrix();
      }
   }

   /** Ellipsises to fit rather than letting a long message run off the plate. */
   private static String trim(FontRenderer font, String s, int maxWidth) {
      if(font.getStringWidth(s) <= maxWidth) {
         return s;
      }

      String out = font.trimStringToWidth(s, maxWidth - font.getStringWidth("..."));
      return out + "...";
   }
}
