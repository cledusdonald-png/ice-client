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
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Sets and draws the Point A / Point B region used to save a schematic.
 *
 * <p>Points are taken from the block you are looking at rather than the block
 * you stand in -- you generally want to mark a corner of a wall you can see, and
 * standing precisely on a corner mid-raid is not realistic.
 *
 * <p>The region itself lives in {@link Selection} so the workspace GUI can read
 * the same corners this draws.
 */
public class SelectionTool extends Module {

   private final KeybindSetting pointAKey = this.addKeybind("Set point A", Keyboard.KEY_COMMA);
   private final KeybindSetting pointBKey = this.addKeybind("Set point B", Keyboard.KEY_PERIOD);
   private final KeybindSetting clearKey = this.addKeybind("Clear selection", 0);
   private final BooleanSetting showBox = this.addBool("Draw region box", true);
   private final BooleanSetting showCorners = this.addBool("Draw corner markers", true);
   private final ColorSetting boxColor = this.addColor("Box colour", 0xFF8C5AFF);
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

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled()) {
         return;
      }

      int color = this.boxColor.getRGB();
      boolean through = this.throughWalls.get();

      if(this.showCorners.get()) {
         this.drawCorner(Selection.getA(), color, through);
         this.drawCorner(Selection.getB(), color, through);
      }

      if(this.showBox.get() && Selection.isComplete()) {
         BlockPos lo = Selection.min();
         BlockPos hi = Selection.max();

         // +1 on the max corner because the region is inclusive: a selection
         // from (0,0,0) to (0,0,0) is one block, not zero-sized.
         WorldRenderUtil.outlineBox(new AxisAlignedBB(
               (double)lo.getX(), (double)lo.getY(), (double)lo.getZ(),
               (double)(hi.getX() + 1), (double)(hi.getY() + 1), (double)(hi.getZ() + 1)),
               color, 2.0F, through);
      }

   }

   private void drawCorner(BlockPos pos, int color, boolean through) {
      if(pos == null) {
         return;
      }

      WorldRenderUtil.outlineBox(new AxisAlignedBB(
            (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(),
            (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1)),
            color, 2.5F, through);
   }
}
