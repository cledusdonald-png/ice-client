package com.unclesam.client.module.modules.hud;

import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.ModeSetting;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class CpsModule extends HudModule {
   private static final int KEY_BOX = 20;
   private static final int BOX_W = 60;
   private static final int BOX_H = 20;
   private static final int BELOW_KEYSTROKES = 66;
   private final ModeSetting mode = (ModeSetting)this.addSetting(new ModeSetting("Display", "Left + Right", new String[]{"Left + Right", "Left", "Right", "Total"}));
   private final Deque<Long> leftClicks = new ArrayDeque();
   private final Deque<Long> rightClicks = new ArrayDeque();
   private boolean prevLeftDown = false;
   private boolean prevRightDown = false;

   public CpsModule() {
      super("CPS Display", "Shows live left/right clicks-per-second", ModuleCategory.GENERAL, 0, HudModule.Anchor.TOP_LEFT, 66);
      this.setEnabled(true);
   }

   public int getWidth() {
      return 60;
   }

   public int getHeight() {
      return 20;
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.START) {
         long now = System.currentTimeMillis();
         boolean leftDown = Mouse.isButtonDown(0);
         if(leftDown && !this.prevLeftDown) {
            this.leftClicks.add(Long.valueOf(now));
         }

         this.prevLeftDown = leftDown;
         boolean rightDown = Mouse.isButtonDown(1);
         if(rightDown && !this.prevRightDown) {
            this.rightClicks.add(Long.valueOf(now));
         }

         this.prevRightDown = rightDown;
         this.prune(this.leftClicks, now);
         this.prune(this.rightClicks, now);
      }
   }

   private void prune(Deque<Long> deque, long now) {
      while(!deque.isEmpty() && now - ((Long)deque.peekFirst()).longValue() > 1000L) {
         deque.pollFirst();
      }

   }

   public void render(int x, int y) {
      // Background and border are gated together: a border floating with no
      // plate behind it reads as a rendering bug rather than a style.
      if(this.hasBackground()) {
         this.drawBoxBackground(x, y, x + 60, y + 20, this.backgroundColor());
         this.drawBoxBorder(x, y, x + 60, y + 20, -1);
      }

      String label = this.buildLabel();
      int textX = x + 30 - this.mc.fontRendererObj.getStringWidth(label) / 2;
      int textY = y + 10 - 4;
      this.drawStyled(label, textX, textY);
   }

   private String buildLabel() {
      int left = this.leftClicks.size();
      int right = this.rightClicks.size();
      return this.mode.is("Left")?"CPS: " + left:(this.mode.is("Right")?"CPS: " + right:(this.mode.is("Total")?"CPS: " + (left + right):"CPS: " + left + " / " + right));
   }

   private void drawBoxBackground(int left, int top, int right, int bottom, int color) {
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

   private void drawBoxBorder(int left, int top, int right, int bottom, int color) {
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      GlStateManager.color(r, g, b, a);
      GL11.glLineWidth(1.0F);
      GL11.glBegin(2);
      GL11.glVertex2d((double)left, (double)top);
      GL11.glVertex2d((double)right, (double)top);
      GL11.glVertex2d((double)right, (double)bottom);
      GL11.glVertex2d((double)left, (double)bottom);
      GL11.glEnd();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
