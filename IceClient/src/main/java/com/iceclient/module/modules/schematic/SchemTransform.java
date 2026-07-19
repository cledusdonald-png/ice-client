package com.iceclient.module.modules.schematic;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import com.iceclient.schematica.SchematicaBridge;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.util.BindUtil;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * Rotate, flip and share the loaded schematic from keybinds.
 *
 * <p>Rotation and flipping delegate to Schematica's own helpers via
 * {@link SchematicaBridge} -- they rebuild the block array and remap directional
 * states, so stairs and chests still face correctly afterwards.
 *
 * <p>"Share" here means sharing the <em>placement</em>, not the file: a compact
 * {@code name@x,y,z} descriptor on the clipboard that a teammate who already has
 * the same .schematic can paste to line theirs up identically. Actually
 * transferring the file would need a transport this client does not have -- there
 * is no group socket, and servers will not relay arbitrary binary in chat.
 */
public class SchemTransform extends Module {

   private static final String PREFIX = "schem:";

   private final KeybindSetting rotateKey = this.addKeybind("Rotate clockwise", Keyboard.KEY_LBRACKET);
   private final KeybindSetting rotateCcwKey = this.addKeybind("Rotate counter-clockwise", Keyboard.KEY_RBRACKET);
   private final KeybindSetting flipXKey = this.addKeybind("Flip X", 0);
   private final KeybindSetting flipZKey = this.addKeybind("Flip Z", 0);
   private final KeybindSetting shareKey = this.addKeybind("Copy placement", 0);
   private final KeybindSetting applyKey = this.addKeybind("Apply placement", 0);
   private final BooleanSetting notify = this.addBool("Notify on change", true);

   private boolean rotWasDown;
   private boolean rotCcwWasDown;
   private boolean flipXWasDown;
   private boolean flipZWasDown;
   private boolean shareWasDown;
   private boolean applyWasDown;

   public SchemTransform() {
      super("SchemTransform", "Rotate, flip and share the loaded schematic", ModuleCategory.PRINTER);
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled()) {
         return;
      }

      if(this.mc.thePlayer == null || this.mc.currentScreen != null) {
         this.clearEdges();
         return;
      }

      if(this.pressed(this.rotateKey, this.rotWasDown)) {
         this.doRotate(true);
      }

      this.rotWasDown = this.down(this.rotateKey);

      if(this.pressed(this.rotateCcwKey, this.rotCcwWasDown)) {
         this.doRotate(false);
      }

      this.rotCcwWasDown = this.down(this.rotateCcwKey);

      if(this.pressed(this.flipXKey, this.flipXWasDown)) {
         this.doFlip(EnumFacing.EAST);
      }

      this.flipXWasDown = this.down(this.flipXKey);

      if(this.pressed(this.flipZKey, this.flipZWasDown)) {
         this.doFlip(EnumFacing.SOUTH);
      }

      this.flipZWasDown = this.down(this.flipZKey);

      if(this.pressed(this.shareKey, this.shareWasDown)) {
         this.copyPlacement();
      }

      this.shareWasDown = this.down(this.shareKey);

      if(this.pressed(this.applyKey, this.applyWasDown)) {
         this.applyPlacement();
      }

      this.applyWasDown = this.down(this.applyKey);
   }

   private boolean down(KeybindSetting k) {
      return k.getKeyCode() != 0 && BindUtil.isDown(k.getKeyCode());
   }

   private boolean pressed(KeybindSetting k, boolean wasDown) {
      return this.down(k) && !wasDown;
   }

   private void clearEdges() {
      this.rotWasDown = false;
      this.rotCcwWasDown = false;
      this.flipXWasDown = false;
      this.flipZWasDown = false;
      this.shareWasDown = false;
      this.applyWasDown = false;
   }

   private boolean requireSchematic() {
      if(!SchematicaBridge.isAvailable() || !SchematicaBridge.hasSchematic()) {
         this.say("No schematic loaded", Notification.Type.WARNING);
         return false;
      }

      return true;
   }

   private void doRotate(boolean clockwise) {
      if(!this.requireSchematic()) {
         return;
      }

      if(SchematicaBridge.rotate(clockwise)) {
         this.say("Rotated " + (clockwise ? "CW" : "CCW"), Notification.Type.SUCCESS);
      } else {
         // Schematica refuses rotation on schematics containing blocks it
         // cannot remap; surfacing that beats a silent no-op.
         this.say("Rotation failed", Notification.Type.ERROR);
      }

   }

   private void doFlip(EnumFacing axis) {
      if(!this.requireSchematic()) {
         return;
      }

      if(SchematicaBridge.flip(axis)) {
         this.say("Flipped " + axis.getName(), Notification.Type.SUCCESS);
      } else {
         this.say("Flip failed", Notification.Type.ERROR);
      }

   }

   private void copyPlacement() {
      if(!this.requireSchematic()) {
         return;
      }

      int[] p = SchematicaBridge.position();
      if(p == null) {
         return;
      }

      String descriptor = PREFIX + SchematicaBridge.name() + "@" + p[0] + "," + p[1] + "," + p[2];

      try {
         Toolkit.getDefaultToolkit().getSystemClipboard()
               .setContents(new StringSelection(descriptor), null);
         this.say("Placement copied", Notification.Type.SUCCESS);
      } catch (Throwable t) {
         this.say("Clipboard unavailable", Notification.Type.ERROR);
      }

   }

   /**
    * Moves the loaded schematic to the coordinates in a copied descriptor.
    *
    * <p>Only the position is applied. The name is carried for the human reading
    * it -- silently loading a different file because a pasted string named one
    * would be a surprising thing to do on a keypress.
    */
   private void applyPlacement() {
      if(!this.requireSchematic()) {
         return;
      }

      String text = this.clipboard();
      if(text == null || !text.startsWith(PREFIX)) {
         this.say("No placement on clipboard", Notification.Type.WARNING);
         return;
      }

      int at = text.lastIndexOf(64);
      if(at < 0) {
         this.say("Malformed placement", Notification.Type.ERROR);
         return;
      }

      String[] parts = text.substring(at + 1).split(",");
      if(parts.length != 3) {
         this.say("Malformed placement", Notification.Type.ERROR);
         return;
      }

      try {
         int tx = Integer.parseInt(parts[0].trim());
         int ty = Integer.parseInt(parts[1].trim());
         int tz = Integer.parseInt(parts[2].trim());

         int[] cur = SchematicaBridge.position();
         if(cur == null) {
            return;
         }

         // nudge() is relative, so the move is expressed as a delta from where
         // the schematic currently sits.
         SchematicaBridge.nudge(tx - cur[0], ty - cur[1], tz - cur[2]);
         this.say("Moved to " + tx + ", " + ty + ", " + tz, Notification.Type.SUCCESS);
      } catch (NumberFormatException e) {
         this.say("Malformed placement", Notification.Type.ERROR);
      }

   }

   private String clipboard() {
      try {
         Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
         if(t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return (String)t.getTransferData(DataFlavor.stringFlavor);
         }
      } catch (Throwable ignored) {
         // Clipboard held by another process -- treat as empty.
      }

      return null;
   }

   private void say(String msg, Notification.Type type) {
      if(this.notify.get()) {
         NotificationManager.post("Schematic", msg, type);
      }

   }
}
