package com.iceclient.ping;

import com.iceclient.util.ColorUtil;
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The shared list of placed pings, and the one place they are drawn.
 *
 * <p>Static and centralised so PingBlock, ChunkPing and PingAdjust all act on
 * the same markers -- three modules each keeping their own list would mean
 * "adjust the last ping" could not see a ping the other module placed.
 */
public final class PingManager {

   /** Beyond this the world is unreadable; oldest is dropped. */
   private static final int MAX = 12;

   private static final List<Ping> PINGS = new ArrayList<Ping>();

   private PingManager() {
   }

   public static void init() {
      MinecraftForge.EVENT_BUS.register(new Handler());
   }

   public static void add(Ping ping) {
      synchronized(PINGS) {
         PINGS.add(ping);
         while(PINGS.size() > MAX) {
            PINGS.remove(0);
         }
      }
   }

   /** The most recently placed ping, which is what PingAdjust operates on. */
   public static Ping newest() {
      synchronized(PINGS) {
         return PINGS.isEmpty() ? null : PINGS.get(PINGS.size() - 1);
      }
   }

   public static boolean removeNewest() {
      synchronized(PINGS) {
         if(PINGS.isEmpty()) {
            return false;
         }

         PINGS.remove(PINGS.size() - 1);
         return true;
      }
   }

   public static void clear() {
      synchronized(PINGS) {
         PINGS.clear();
      }
   }

   public static int count() {
      synchronized(PINGS) {
         return PINGS.size();
      }
   }

   public static List<Ping> snapshot() {
      synchronized(PINGS) {
         return new ArrayList<Ping>(PINGS);
      }
   }

   /**
    * Render hook lives on its own object rather than on the modules, so pings
    * stay visible regardless of which module placed them and whether that
    * module is still enabled.
    */
   public static class Handler {

      @SubscribeEvent
      public void onRenderWorld(RenderWorldLastEvent event) {
         Minecraft mc = Minecraft.getMinecraft();
         if(mc.theWorld == null || mc.thePlayer == null) {
            return;
         }

         synchronized(PINGS) {
            Iterator<Ping> it = PINGS.iterator();
            while(it.hasNext()) {
               if(it.next().isExpired()) {
                  it.remove();
               }
            }

            for(Ping p : PINGS) {
               draw(p);
            }
         }

      }

      private static void draw(Ping p) {
         BlockPos pos = p.getPos();
         int color = ColorUtil.withAlpha(p.getColor(), Math.round(255.0F * p.alpha()));

         if(p.getType() == Ping.Type.CHUNK) {
            // Whole chunk column: snap to the chunk the position falls in and
            // draw floor to build limit, which is what makes it readable from
            // outside the base.
            int cx = pos.getX() >> 4 << 4;
            int cz = pos.getZ() >> 4 << 4;
            WorldRenderUtil.outlineBox(new AxisAlignedBB(
                  (double)cx, 0.0D, (double)cz,
                  (double)(cx + 16), 256.0D, (double)(cz + 16)), color, 2.0F, true);
         } else {
            WorldRenderUtil.outlineBox(new AxisAlignedBB(
                  (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(),
                  (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1)),
                  color, 2.5F, true);

            // A beam makes a single block findable from across the map, which
            // a one-block outline never is.
            WorldRenderUtil.beam((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D,
                  40.0D, 0.25D, color, true);
         }

      }
   }
}
