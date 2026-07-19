package com.iceclient.module.modules.misc;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.util.ColorUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.List;

/**
 * Highlights inventory slots whose item matches a search string. Works in any
 * container -- chests, the player inventory, shop menus.
 *
 * <p>Typing is gated behind a focus key rather than always-on, because a
 * container GUI needs its own keys: number keys swap hotbar slots and E closes
 * the screen. Press the focus key, type, press it again (or Enter/Escape) to
 * hand the keyboard back.
 */
public class ItemSearch extends Module {

   private final KeybindSetting focusKey = (KeybindSetting)this.addSetting(new KeybindSetting("Focus search key", Keyboard.KEY_F));
   private final BooleanSetting searchLore = (BooleanSetting)this.addSetting(new BooleanSetting("Search lores and tool tips", true));
   /**
    * Registry name, e.g. {@code minecraft:diamond_sword}. Lets you find every
    * item of a type regardless of what a plugin renamed it to -- searching
    * "obsidian" still finds a stack called "§5Cursed Block".
    */
   private final BooleanSetting searchItemId = (BooleanSetting)this.addSetting(new BooleanSetting("Search item ID", true));
   /**
    * Raw NBT. This is the catch-all: custom plugin tags, enchantments the item
    * hides with HideFlags, owner data on heads, ability keys on relics -- none
    * of which appear in the rendered tooltip.
    */
   private final BooleanSetting searchNbt = (BooleanSetting)this.addSetting(new BooleanSetting("Search raw NBT", false));
   /**
    * Server lore is full of colour codes, and they land mid-word: "§cRed §fSword"
    * does not contain "red sword". Stripping them before matching is what makes
    * searching decorated item names work at all.
    */
   private final BooleanSetting ignoreColors = (BooleanSetting)this.addSetting(new BooleanSetting("Ignore colour codes", true));
   /**
    * Splits the query on spaces and requires every term to match somewhere on
    * the item, so "prot 4 boots" finds Protection IV boots without depending on
    * the order those words appear in the tooltip.
    */
   private final BooleanSetting allTerms = (BooleanSetting)this.addSetting(new BooleanSetting("Match all words", true));
   private final BooleanSetting dimNonMatching = (BooleanSetting)this.addSetting(new BooleanSetting("Dim non-matching", false));
   private final BooleanSetting chroma = (BooleanSetting)this.addSetting(new BooleanSetting("Chroma", false));
   private final ColorSetting slotColor = (ColorSetting)this.addSetting(new ColorSetting("Slot color", -1));

   private static String query = "";
   private static boolean focused;

   /** Search box bounds from the last draw, used to hit-test clicks. */
   private int boxX;
   private int boxY;
   private int boxW;
   private int boxH;

   public ItemSearch() {
      super("Item Search", "Highlights items by name, lore, enchantments, ID or NBT", ModuleCategory.MECHANIC);
   }

   public static void setQuery(String q) {
      query = q == null ? "" : q.trim().toLowerCase();
   }

   public static String getQuery() {
      return query;
   }

   /**
    * Consumes typing while the search box is focused, so it doesn't leak into
    * vanilla's container handling (hotbar swaps, E to close).
    */
   @SubscribeEvent
   public void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
      if(!this.isEnabled() || !(event.gui instanceof GuiContainer) || !Keyboard.getEventKeyState()) {
         return;
      }

      int k = Keyboard.getEventKey();

      if(k == this.focusKey.getKeyCode() && this.focusKey.getKeyCode() != 0) {
         focused = !focused;
         event.setCanceled(true);
         return;
      }

      if(!focused) {
         return;
      }

      if(k == Keyboard.KEY_ESCAPE) {
         focused = false;
         query = "";
      } else if(k == Keyboard.KEY_RETURN || k == Keyboard.KEY_NUMPADENTER) {
         focused = false;
      } else if(k == Keyboard.KEY_BACK) {
         if(!query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
         }
      } else {
         char c = Keyboard.getEventCharacter();
         if(c >= 32 && c != 127) {
            query = query + Character.toLowerCase(c);
         }
      }

      event.setCanceled(true);
   }

   /** Focus is per-screen; drop it when the container closes. */
   @SubscribeEvent
   public void onTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
      if(this.mc.currentScreen == null || !(this.mc.currentScreen instanceof GuiContainer)) {
         focused = false;
      }

   }

   @SubscribeEvent
   public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
      if(!this.isEnabled() || !(event.gui instanceof GuiContainer)) {
         return;
      }

      GuiContainer gui = (GuiContainer)event.gui;
      this.drawSearchBox(gui);

      if(query.isEmpty()) {
         return;
      }

      int col = ColorUtil.withAlpha(this.chroma.get() ? ColorUtil.chroma(0) : this.slotColor.getRGB(), 120);

      // Slot coords are container-relative, so we need guiLeft/guiTop -- both
      // protected on GuiContainer. ReflectionHelper takes the SRG name too, so
      // this resolves in the obfuscated runtime as well as in dev.
      int left = this.intField(gui, "guiLeft", "field_147003_i");
      int top = this.intField(gui, "guiTop", "field_147009_r");

      int dim = ColorUtil.withAlpha(0, 140);
      for(Slot slot : gui.inventorySlots.inventorySlots) {
         ItemStack stack = slot.getStack();
         if(stack == null) {
            continue;
         }

         int x = left + slot.xDisplayPosition;
         int y = top + slot.yDisplayPosition;
         if(this.matches(stack)) {
            Gui.drawRect(x, y, x + 16, y + 16, col);
         } else if(this.dimNonMatching.get()) {
            Gui.drawRect(x, y, x + 16, y + 16, dim);
         }
      }

   }

   /** Small search field drawn above the container. */
   private void drawSearchBox(GuiContainer gui) {
      String label;
      if(focused) {
         label = "Search: " + query + "_";
      } else if(!query.isEmpty()) {
         label = "Search: " + query;
      } else {
         label = this.focusKey.getKeyCode() == 0
               ? "Click to search"
               : "Click to search [" + this.focusKey.getKeyName() + "]";
      }

      int w = Math.max(110, this.mc.fontRendererObj.getStringWidth(label) + 10);
      int x = (gui.width - w) / 2;
      int y = 4;

      // Remembered so mouse clicks can be hit-tested against the same rect.
      this.boxX = x;
      this.boxY = y;
      this.boxW = w;
      this.boxH = 14;

      Gui.drawRect(x, y, x + w, y + 14, ColorUtil.withAlpha(0, focused ? 200 : 170));
      if(focused) {
         // Thin accent underline so it's obvious the keyboard is captured.
         Gui.drawRect(x, y + 13, x + w, y + 14, -10696961);
      }

      this.mc.fontRendererObj.drawStringWithShadow(label, (float)(x + 5), (float)(y + 3),
            focused ? -1 : -5592406);
   }

   /**
    * Click the box to focus it, click anywhere else to release it.
    *
    * <p>Only left-press is consumed, and only when it lands inside the box --
    * every other click falls through to the container so item handling is
    * untouched.
    */
   @SubscribeEvent
   public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
      if(!this.isEnabled() || !(event.gui instanceof GuiContainer) || this.boxW <= 0) {
         return;
      }

      if(Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) {
         return;
      }

      GuiContainer gui = (GuiContainer)event.gui;
      // LWJGL reports from the bottom-left in real pixels; the GUI works in
      // scaled coordinates from the top-left.
      int mx = Mouse.getEventX() * gui.width / this.mc.displayWidth;
      int my = gui.height - Mouse.getEventY() * gui.height / this.mc.displayHeight - 1;

      boolean inside = mx >= this.boxX && mx <= this.boxX + this.boxW
            && my >= this.boxY && my <= this.boxY + this.boxH;

      if(inside) {
         focused = true;
         event.setCanceled(true);
      } else if(focused) {
         focused = false;
      }

   }

   private int intField(GuiContainer gui, String mcpName, String srgName) {
      try {
         return ((Integer)net.minecraftforge.fml.relauncher.ReflectionHelper
               .getPrivateValue(GuiContainer.class, gui, new String[]{mcpName, srgName})).intValue();
      } catch (Throwable var5) {
         return 0;
      }
   }

   private boolean matches(ItemStack stack) {
      try {
         if(query.isEmpty()) {
            return false;
         }

         // Built once per item, not once per term: with "match all words" on,
         // testing terms separately would re-run getTooltip (and the NBT dump)
         // for every word, on every slot, every frame.
         String haystack = this.searchableText(stack);

         if(!this.allTerms.get()) {
            return haystack.contains(query);
         }

         // Every word must match somewhere on the item, though not necessarily
         // in the same field -- "prot 4 boots" can take "boots" from the name
         // and "prot"/"4" from an enchantment line.
         for(String term : query.split("\\s+")) {
            if(!term.isEmpty() && !haystack.contains(term)) {
               return false;
            }
         }

         return true;
      } catch (Throwable var4) {
         // A malformed stack from a server plugin shouldn't break rendering.
         return false;
      }
   }

   /**
    * Everything about an item that can be searched, as one lowercase blob.
    *
    * <p>The raw NBT dump is the expensive part -- it serialises the whole tag
    * tree -- which is why it is off by default and appended last.
    */
   private String searchableText(ItemStack stack) {
      StringBuilder sb = new StringBuilder(128);
      sb.append(stack.getDisplayName()).append('\n');

      if(this.searchItemId.get() && stack.getItem() != null) {
         Object id = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
         if(id != null) {
            sb.append(id.toString()).append('\n');
         }
      }

      if(this.searchLore.get()) {
         List<String> tip = stack.getTooltip(this.mc.thePlayer, false);
         for(String line : tip) {
            if(line != null) {
               sb.append(line).append('\n');
            }
         }
      }

      if(this.searchNbt.get() && stack.hasTagCompound()) {
         sb.append(stack.getTagCompound().toString());
      }

      String text = sb.toString();
      if(this.ignoreColors.get()) {
         text = net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(text);
      }

      return text == null ? "" : text.toLowerCase();
   }
}
