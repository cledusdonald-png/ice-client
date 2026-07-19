package com.iceclient.module.modules.schematic;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import com.iceclient.schematic.Selection;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.util.BindUtil;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Keybinds and styling for the Point A / Point B region used to save a
 * schematic.
 *
 * <p>Points are taken from the block you are looking at rather than the block
 * you stand in -- you generally want to mark a corner of a wall you can see, and
 * standing precisely on a corner mid-raid is not realistic.
 *
 * <p>The region lives in {@link Selection} and is drawn by
 * {@code SelectionRenderer}, not here -- see the note above the accessors for
 * why the drawing is not gated on this module being enabled.
 */
public class SelectionTool extends Module {

   private final KeybindSetting pointAKey = this.addKeybind("Set point A", Keyboard.KEY_COMMA);
   private final KeybindSetting pointBKey = this.addKeybind("Set point B", Keyboard.KEY_PERIOD);
   private final KeybindSetting clearKey = this.addKeybind("Clear selection", 0);
   private final BooleanSetting showBox = this.addBool("Draw region box", true);
   private final BooleanSetting showCorners = this.addBool("Draw corner markers", true);
   private final BooleanSetting fillCorners = this.addBool("Solid corner blocks", true);
   private final ColorSetting colorA = this.addColor("Point A colour", 0xFFFF3333);
   private final ColorSetting colorB = this.addColor("Point B colour", 0xFF3388FF);
   private final ColorSetting boxColor = this.addColor("Region colour", 0xFF8C5AFF);
   private final BooleanSetting throughWalls = this.addBool("Through walls", true);

   private boolean aWasDown;
   private boolean bWasDown;
   private boolean clearWasDown;

   public SelectionTool() {
      super("SelectionTool", "Marks a Point A/B region to save as a schematic", ModuleCategory.PRINTER);
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled()) {
         return;
      }

      if(this.mc.thePlayer == null || this.mc.currentScreen != null) {
         this.aWasDown = false;
         this.bWasDown = false;
         this.clearWasDown = false;
         return;
      }

      boolean a = this.down(this.pointAKey);
      if(a && !this.aWasDown) {
         this.setPoint(true);
      }

      this.aWasDown = a;

      boolean b = this.down(this.pointBKey);
      if(b && !this.bWasDown) {
         this.setPoint(false);
      }

      this.bWasDown = b;

      boolean c = this.down(this.clearKey);
      if(c && !this.clearWasDown) {
         Selection.clear();
         NotificationManager.post("Selection", "Cleared", Notification.Type.INFO);
      }

      this.clearWasDown = c;
   }

   private boolean down(KeybindSetting k) {
      return k.getKeyCode() != 0 && BindUtil.isDown(k.getKeyCode());
   }

   private void setPoint(boolean isA) {
      BlockPos pos = this.lookedAtBlock();
      if(pos == null) {
         NotificationManager.post("Selection", "Look at a block first", Notification.Type.WARNING);
         return;
      }

      if(isA) {
         Selection.setA(pos);
      } else {
         Selection.setB(pos);
      }

      String label = isA ? "A" : "B";
      String extra = Selection.isComplete()
            ? "  (" + Selection.volume() + " blocks)"
            : "";

      NotificationManager.post("Point " + label,
            pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + extra,
            Notification.Type.SUCCESS);
   }

   private BlockPos lookedAtBlock() {
      MovingObjectPosition hit = this.mc.objectMouseOver;
      return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            ? hit.getBlockPos()
            : null;
   }

   // --- Style, read by SelectionRenderer ---
   //
   // The drawing deliberately does NOT live here. Corners can be set from the
   // workspace GUI's Points tab, which does not know or care whether this
   // module is switched on -- so gating the render on isEnabled() meant setting
   // two points and seeing nothing, with nothing on screen explaining why.
   // SelectionRenderer draws whenever a selection exists; this module owns the
   // keybinds and the look.

   public boolean showRegionBox() {
      return this.showBox.get();
   }

   public boolean showCornerMarkers() {
      return this.showCorners.get();
   }

   public boolean fillCornerBlocks() {
      return this.fillCorners.get();
   }

   public boolean drawThroughWalls() {
      return this.throughWalls.get();
   }

   public int pointAColor() {
      return this.colorA.getRGB();
   }

   public int pointBColor() {
      return this.colorB.getRGB();
   }

   public int regionColor() {
      return this.boxColor.getRGB();
   }
}
