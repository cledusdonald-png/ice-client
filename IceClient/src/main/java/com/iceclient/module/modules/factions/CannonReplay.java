package com.iceclient.module.modules.factions;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.notification.Notification;
import com.iceclient.notification.NotificationManager;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.BoxBatch;
import com.iceclient.util.BindUtil;
import com.iceclient.setting.KeybindSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Records a cannon shot and plays it back in the world.
 *
 * <p>Cannons resolve over a handful of ticks and the interesting part -- where
 * the sand stack desynced, which TNT went off early -- is over before you can
 * look at it. This samples every primed TNT and falling block each tick into a
 * frame list, then redraws those frames on demand at whatever speed you like.
 *
 * <p>Recording is deliberately cheap: positions only, as primitive doubles, no
 * entity references. Holding entity references would pin dead entities in
 * memory and read stale positions once they were removed from the world.
 */
public class CannonReplay extends Module {

   /** ~30s at 20tps. Cannon shots are seconds; this is a generous ceiling. */
   private static final int MAX_FRAMES = 600;

   private final NumberSetting radius = this.addNumber("Record radius", 64.0D, 16.0D, 128.0D, 8.0D);
   private final NumberSetting speed = this.addNumber("Playback speed", 0.5D, 0.1D, 2.0D, 0.1D);
   private final BooleanSetting autoRecord = this.addBool("Auto-record on TNT", true);
   private final BooleanSetting loop = this.addBool("Loop playback", true);
   private final BooleanSetting showTrails = this.addBool("Show trails", true);
   private final KeybindSetting recordKey = this.addKeybind("Record toggle", Keyboard.KEY_R);
   private final KeybindSetting playKey = this.addKeybind("Play toggle", Keyboard.KEY_P);

   private final List<Frame> frames = new ArrayList<Frame>();

   private boolean recording;
   private boolean playing;

   /** Fractional so playback speed below 1x advances slower than a tick. */
   private double playhead;

   /** Edge-detect state for the two keybinds. */
   private boolean recordWasDown;
   private boolean playWasDown;

   /** Ticks with no cannon entities seen, used to auto-stop a recording. */
   private int idleTicks;

   public CannonReplay() {
      super("CannonReplay", "Records and replays cannon shots", ModuleCategory.FACTIONS);
   }

   protected void onDisable() {
      this.recording = false;
      this.playing = false;
      this.frames.clear();
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled()
            || this.mc.thePlayer == null || this.mc.theWorld == null) {
         return;
      }

      this.pollKeys();

      if(this.recording) {
         this.captureFrame();
      } else if(this.autoRecord.get() && !this.playing && this.cannonEntityNearby()) {
         this.startRecording();
      }

      if(this.playing) {
         this.advancePlayhead();
      }

   }

   private void pollKeys() {
      // Typing in chat must not toggle a recording.
      if(this.mc.currentScreen != null) {
         this.recordWasDown = false;
         this.playWasDown = false;
         return;
      }

      boolean rec = this.recordKey.get() != 0 && BindUtil.isDown(this.recordKey.get());
      if(rec && !this.recordWasDown) {
         if(this.recording) {
            this.stopRecording();
         } else {
            this.startRecording();
         }
      }

      this.recordWasDown = rec;

      boolean play = this.playKey.get() != 0 && BindUtil.isDown(this.playKey.get());
      if(play && !this.playWasDown) {
         this.togglePlayback();
      }

      this.playWasDown = play;
   }

   private void startRecording() {
      this.frames.clear();
      this.recording = true;
      this.playing = false;
      this.idleTicks = 0;
      NotificationManager.post("CannonReplay", "Recording", Notification.Type.INFO);
   }

   private void stopRecording() {
      this.recording = false;
      NotificationManager.post("CannonReplay", this.frames.size() + " frames captured",
            Notification.Type.SUCCESS);
   }

   private void togglePlayback() {
      if(this.frames.isEmpty()) {
         NotificationManager.post("CannonReplay", "Nothing recorded", Notification.Type.WARNING);
         return;
      }

      this.playing = !this.playing;
      this.playhead = 0.0D;
   }

   /**
    * Snapshots this tick's cannon entities.
    *
    * <p>Stops automatically after a second of seeing nothing: a shot that has
    * finished should not keep appending empty frames until the buffer fills,
    * because that would push the interesting frames out of a capped list.
    */
   private void captureFrame() {
      Frame f = new Frame();
      double r = this.radius.get();
      double rSq = r * r;

      for(Entity e : this.mc.theWorld.loadedEntityList) {
         boolean tnt = e instanceof EntityTNTPrimed;
         boolean sand = e instanceof EntityFallingBlock;
         if(!tnt && !sand) {
            continue;
         }

         double dx = e.posX - this.mc.thePlayer.posX;
         double dy = e.posY - this.mc.thePlayer.posY;
         double dz = e.posZ - this.mc.thePlayer.posZ;
         if(dx * dx + dy * dy + dz * dz > rSq) {
            continue;
         }

         f.add(e.posX, e.posY, e.posZ, tnt);
      }

      if(f.isEmpty()) {
         if(++this.idleTicks > 20) {
            this.stopRecording();
         }
      } else {
         this.idleTicks = 0;
      }

      this.frames.add(f);

      if(this.frames.size() > MAX_FRAMES) {
         this.frames.remove(0);
      }

   }

   private boolean cannonEntityNearby() {
      double r = this.radius.get();
      double rSq = r * r;

      for(Entity e : this.mc.theWorld.loadedEntityList) {
         if(!(e instanceof EntityTNTPrimed)) {
            continue;
         }

         double dx = e.posX - this.mc.thePlayer.posX;
         double dy = e.posY - this.mc.thePlayer.posY;
         double dz = e.posZ - this.mc.thePlayer.posZ;
         if(dx * dx + dy * dy + dz * dz <= rSq) {
            return true;
         }
      }

      return false;
   }

   private void advancePlayhead() {
      this.playhead += this.speed.get();

      if(this.playhead >= (double)this.frames.size()) {
         if(this.loop.get()) {
            this.playhead = 0.0D;
         } else {
            this.playing = false;
            this.playhead = (double)Math.max(0, this.frames.size() - 1);
         }
      }

   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || !this.playing || this.frames.isEmpty()) {
         return;
      }

      int index = (int)Math.floor(this.playhead);
      if(index < 0 || index >= this.frames.size()) {
         return;
      }

      BoxBatch batch = new BoxBatch();

      if(this.showTrails.get()) {
         // Everything up to the playhead, dimmed, so the path the stack took is
         // visible as well as where it is now.
         for(int i = 0; i < index; ++i) {
            this.frames.get(i).addTo(batch, 0.10F, 0.0F);
         }
      }

      this.frames.get(index).addTo(batch, 0.35F, 0.9F);

      if(!batch.isEmpty()) {
         batch.flush(1.5F, true);
      }

   }

   /**
    * One tick's worth of positions, stored flat.
    *
    * <p>Parallel primitive arrays rather than a list of position objects: a
    * 600-frame recording of a big stack is tens of thousands of points, and one
    * small object each would make this a noticeable allocation.
    */
   private static final class Frame {

      private double[] xs = new double[16];
      private double[] ys = new double[16];
      private double[] zs = new double[16];
      private boolean[] isTnt = new boolean[16];
      private int count;

      boolean isEmpty() {
         return this.count == 0;
      }

      void add(double x, double y, double z, boolean tnt) {
         if(this.count == this.xs.length) {
            this.grow();
         }

         this.xs[this.count] = x;
         this.ys[this.count] = y;
         this.zs[this.count] = z;
         this.isTnt[this.count] = tnt;
         ++this.count;
      }

      private void grow() {
         int n = this.xs.length * 2;
         double[] nx = new double[n];
         double[] ny = new double[n];
         double[] nz = new double[n];
         boolean[] nt = new boolean[n];

         System.arraycopy(this.xs, 0, nx, 0, this.count);
         System.arraycopy(this.ys, 0, ny, 0, this.count);
         System.arraycopy(this.zs, 0, nz, 0, this.count);
         System.arraycopy(this.isTnt, 0, nt, 0, this.count);

         this.xs = nx;
         this.ys = ny;
         this.zs = nz;
         this.isTnt = nt;
      }

      /** TNT draws red, falling blocks sand-yellow. */
      void addTo(BoxBatch batch, float fillAlpha, float lineAlpha) {
         for(int i = 0; i < this.count; ++i) {
            float r = this.isTnt[i] ? 1.0F : 0.85F;
            float g = this.isTnt[i] ? 0.25F : 0.78F;
            float b = this.isTnt[i] ? 0.25F : 0.45F;

            // Entity position is the centre of the base, so the box is built
            // outward in X/Z and upward in Y.
            batch.add(this.xs[i] - 0.49D, this.ys[i], this.zs[i] - 0.49D,
                  this.xs[i] + 0.49D, this.ys[i] + 0.98D, this.zs[i] + 0.49D,
                  r, g, b, fillAlpha, lineAlpha);
         }

      }
   }
}
