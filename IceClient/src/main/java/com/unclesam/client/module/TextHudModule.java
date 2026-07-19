package com.unclesam.client.module;

import com.unclesam.client.gui.HudEditorScreen;
import net.minecraft.client.gui.Gui;

import java.util.List;

/**
 * Base for HUD elements that are just lines of text.
 *
 * <p>Ice's {@link HudModule} contract is {@code getWidth()/getHeight()/render(x,y)},
 * which means every text element would otherwise hand-roll its own measuring,
 * background plate and shadow handling. Subclasses here supply {@link #lines()}
 * and get sizing, the background plate and the shared style settings
 * (shadow / chroma / background / alpha) applied consistently.
 *
 * <p>Returning an empty list collapses the element to zero size, so it takes up
 * no room in the HUD editor when it has nothing to say.
 */
public abstract class TextHudModule extends HudModule {

   protected static final int PAD = 3;
   protected static final int LINE_H = 10;

   protected TextHudModule(String name, String description, ModuleCategory category,
                           HudModule.Anchor anchor, int anchorOffsetY) {
      super(name, description, category, 0, anchor, anchorOffsetY);
   }

   /** The lines to draw, top to bottom. Empty hides the element. */
   protected abstract List<String> lines();

   /**
    * Lines to draw, substituting a placeholder while the HUD editor is open.
    *
    * <p>Without this an element with nothing to say (no nearby players, no
    * cooldowns) reports zero size, and the editor -- which sizes its drag boxes
    * from getWidth()/getHeight() -- has nothing to grab. You could never
    * position it until the moment it happened to have content.
    */
   private List<String> displayLines() {
      List<String> ls = this.lines();
      if(!ls.isEmpty()) {
         return ls;
      }

      return this.mc.currentScreen instanceof HudEditorScreen
            ? java.util.Collections.singletonList(this.getName())
            : ls;
   }

   public int getWidth() {
      List<String> ls = this.displayLines();
      if(ls.isEmpty()) {
         return 0;
      }

      int w = 0;
      for(String s : ls) {
         w = Math.max(w, this.mc.fontRendererObj.getStringWidth(s));
      }

      return w + PAD * 2;
   }

   public int getHeight() {
      List<String> ls = this.displayLines();
      return ls.isEmpty() ? 0 : ls.size() * LINE_H + PAD * 2;
   }

   public void render(int x, int y) {
      List<String> ls = this.displayLines();
      if(ls.isEmpty()) {
         return;
      }

      if(this.hasBackground()) {
         Gui.drawRect(x, y, x + this.getWidth(), y + this.getHeight(), this.backgroundColor());
      }

      int ly = y + PAD;
      for(String s : ls) {
         this.drawStyled(s, x + PAD, ly);
         ly += LINE_H;
      }
   }
}
