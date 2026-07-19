package com.iceclient.module.modules.groups;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.BindUtil;
import com.iceclient.util.ColorUtil;
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Marks a spot you're looking at with a temporary beacon.
 *
 * <p>Local-only: without a server-side plugin there's no channel to broadcast a
 * ping over, so this marks the spot for you rather than your whole team. The
 * Lunar/Apollo waypoint path in {@code LunarClientApi} is the shared version.
 */
public class PingLocation extends Module {

   private final KeybindSetting pingKey = (KeybindSetting)this.addSetting(new KeybindSetting("Ping key", Keyboard.KEY_R));
   private final NumberSetting duration = (NumberSetting)this.addSetting(new NumberSetting("Duration (s)", 15.0D, 1.0D, 120.0D, 5.0D));
   private final NumberSetting maxPings = (NumberSetting)this.addSetting(new NumberSetting("Max pings", 5.0D, 1.0D, 20.0D, 1.0D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final BooleanSetting showDistance = (BooleanSetting)this.addSetting(new BooleanSetting("Show distance", true));
   private final BooleanSetting beam = (BooleanSetting)this.addSetting(new BooleanSetting("Beam", true));
   private final ColorSetting color = (ColorSetting)this.addSetting(new ColorSetting("Color", -16711681));

   private final List<Ping> pings = new ArrayList();
   private boolean wasDown;

   private static class Ping {
      final BlockPos pos;
      final long expiresAt;

      Ping(BlockPos pos, long expiresAt) {
         this.pos = pos;
         this.expiresAt = expiresAt;
      }
   }

   public PingLocation() {
      super("Ping Location", "Marks a looked-at spot with a temporary beacon", ModuleCategory.FACTIONS);
   }

   protected void onDisable() {
      this.pings.clear();
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled()) {
         return;
      }

      long now = System.currentTimeMillis();
      Iterator<Ping> it = this.pings.iterator();
      while(it.hasNext()) {
         if(now > it.next().expiresAt) {
            it.remove();
         }
      }

      if(this.mc.thePlayer == null || this.mc.currentScreen != null) {
         this.wasDown = false;
         return;
      }

      boolean down = BindUtil.isDown(this.pingKey.getKeyCode());
      if(down && !this.wasDown) {
         this.addPing();
      }

      this.wasDown = down;
   }

   private void addPing() {
      MovingObjectPosition hit = this.mc.objectMouseOver;
      BlockPos p = hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            ? hit.getBlockPos()
            : new BlockPos(this.mc.thePlayer);

      this.pings.add(new Ping(p, System.currentTimeMillis() + (long)(this.duration.get() * 1000.0D)));
      while(this.pings.size() > (int)this.maxPings.get()) {
         this.pings.remove(0);
      }

   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.pings.isEmpty() || this.mc.thePlayer == null) {
         return;
      }

      int col = ColorUtil.withAlpha(this.color.getRGB(), 220);
      float w = (float)this.lineWidth.get();

      for(Ping ping : new ArrayList<Ping>(this.pings)) {
         BlockPos p = ping.pos;
         WorldRenderUtil.outlineBox(new AxisAlignedBB(p, p.add(1, 1, 1)), col, w);

         if(this.beam.get()) {
            WorldRenderUtil.drawLine((double)p.getX() + 0.5D, (double)p.getY(), (double)p.getZ() + 0.5D,
                  (double)p.getX() + 0.5D, 256.0D, (double)p.getZ() + 0.5D, ColorUtil.withAlpha(this.color.getRGB(), 90), w);
         }

         if(this.showDistance.get()) {
            int d = (int)this.mc.thePlayer.getDistance((double)p.getX() + 0.5D, (double)p.getY(), (double)p.getZ() + 0.5D);
            WorldRenderUtil.text3d(d + "m", (double)p.getX() + 0.5D, (double)p.getY() + 1.3D, (double)p.getZ() + 0.5D, col, 0.02F);
         }
      }

   }
}
