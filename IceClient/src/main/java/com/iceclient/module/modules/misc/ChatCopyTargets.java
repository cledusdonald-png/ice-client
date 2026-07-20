package com.iceclient.module.modules.misc;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-line copy hit boxes, shared between the drawing and the click handling.
 *
 * <p>The geometry has to come from {@code MixinGuiNewChat}, which is the only
 * place with access to the private drawn-line list and the chat scale. The click
 * arrives separately in {@code MixinGuiChat}. Rather than have the click
 * re-derive line positions -- and risk the icon and the hit box drifting apart
 * -- the chat renderer records both here each frame and the click just looks
 * them up.
 *
 * <p>Coordinates are GUI-scaled pixels, the space {@code GuiScreen.mouseClicked}
 * already works in, so no conversion is needed at click time.
 */
public final class ChatCopyTargets {

   /** One drawn chat line: its full row, its icon box, and its text. */
   public static final class Target {
      public final int rowTop;
      public final int rowBottom;
      public final int iconLeft;
      public final int iconRight;
      public final String text;

      Target(int rowTop, int rowBottom, int iconLeft, int iconRight, String text) {
         this.rowTop = rowTop;
         this.rowBottom = rowBottom;
         this.iconLeft = iconLeft;
         this.iconRight = iconRight;
         this.text = text;
      }
   }

   private static final List<Target> TARGETS = new ArrayList<Target>();

   private ChatCopyTargets() {
   }

   public static void clear() {
      TARGETS.clear();
   }

   public static void add(int rowTop, int rowBottom, int iconLeft, int iconRight, String text) {
      TARGETS.add(new Target(rowTop, rowBottom, iconLeft, iconRight, text));
   }

   public static List<Target> all() {
      return TARGETS;
   }

   /** Text of the line whose copy icon contains the point, or null. */
   public static String iconAt(int x, int y) {
      for(Target t : TARGETS) {
         if(y >= t.rowTop && y <= t.rowBottom && x >= t.iconLeft && x <= t.iconRight) {
            return t.text;
         }
      }

      return null;
   }

   /** Text of the line the point falls on anywhere, or null -- for shift-click. */
   public static String rowAt(int x, int y) {
      for(Target t : TARGETS) {
         if(y >= t.rowTop && y <= t.rowBottom) {
            return t.text;
         }
      }

      return null;
   }
}
