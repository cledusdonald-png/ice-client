package com.iceclient.module.modules.hud;

import com.iceclient.module.HudModule;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.TextHudModule;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;

/**
 * Server tick rate, measured from the interval between time-update packets.
 *
 * <p>The server sends {@link S03PacketTimeUpdate} once per second under normal
 * load, so 20 ticks over that interval is the baseline. We can't read the
 * server's real TPS from a vanilla client, so this is an estimate -- it's
 * smoothed over several samples to stop it jittering on packet jitter alone.
 */
public class TpsHud extends TextHudModule {

   private static final int SAMPLES = 10;
   private final double[] samples = new double[SAMPLES];
   private int sampleCount;
   private int sampleIndex;
   private long lastPacket;

   public TpsHud() {
      super("TPS", "Estimates server tick rate", ModuleCategory.HUD, HudModule.Anchor.TOP_LEFT, 100);
   }

   protected void onDisable() {
      this.sampleCount = 0;
      this.sampleIndex = 0;
      this.lastPacket = 0L;
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || this.mc.theWorld == null) {
         this.lastPacket = 0L;
      }
   }

   /** Called from the packet hook when a time update lands. */
   public void onTimeUpdate() {
      long now = System.currentTimeMillis();
      if(this.lastPacket != 0L) {
         double elapsed = (double)(now - this.lastPacket) / 1000.0D;
         if(elapsed > 0.05D && elapsed < 10.0D) {
            this.samples[this.sampleIndex] = Math.min(20.0D, 20.0D / elapsed);
            this.sampleIndex = (this.sampleIndex + 1) % SAMPLES;
            if(this.sampleCount < SAMPLES) {
               ++this.sampleCount;
            }
         }
      }

      this.lastPacket = now;
   }

   private double tps() {
      if(this.sampleCount == 0) {
         return -1.0D;
      }

      double total = 0.0D;
      for(int i = 0; i < this.sampleCount; ++i) {
         total += this.samples[i];
      }

      return total / (double)this.sampleCount;
   }

   protected List<String> lines() {
      double t = this.tps();
      if(t < 0.0D) {
         return Collections.emptyList();
      }

      return Collections.singletonList("TPS: " + String.format("%.1f", Double.valueOf(t)));
   }
}
