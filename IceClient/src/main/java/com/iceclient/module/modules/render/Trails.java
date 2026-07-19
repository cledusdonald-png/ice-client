package com.iceclient.module.modules.render;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.ColorUtil;
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Draws the path each player has walked or flown over the last stretch of
 * distance -- useful for reading where someone came from, or which way a
 * runner went.
 *
 * <p>Trails are length-bounded rather than time-bounded so a sprinting player
 * and a hovering one leave comparable marks. Points are only recorded once the
 * player has moved a minimum step, so standing still doesn't grow the buffer.
 */
public class Trails extends Module {

   private final NumberSetting length = (NumberSetting)this.addSetting(new NumberSetting("Length (blocks)", 10.0D, 2.0D, 64.0D, 1.0D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final NumberSetting timeout = (NumberSetting)this.addSetting(new NumberSetting("Timeout (s)", 10.0D, 1.0D, 60.0D, 1.0D));
   private final BooleanSetting selfTrail = (BooleanSetting)this.addSetting(new BooleanSetting("Own trail", false));
   private final BooleanSetting throughWalls = (BooleanSetting)this.addSetting(new BooleanSetting("Through walls", true));
   private final BooleanSetting fade = (BooleanSetting)this.addSetting(new BooleanSetting("Fade out", true));
   private final BooleanSetting chroma = (BooleanSetting)this.addSetting(new BooleanSetting("Chroma", false));
   private final ColorSetting color = (ColorSetting)this.addSetting(new ColorSetting("Color", -16711681));

   /** Minimum movement before a new point is recorded, in blocks. */
   private static final double MIN_STEP = 0.25D;

   private final Map<UUID, Trail> trails = new HashMap();

   /**
    * Points carry their own timestamp so a trail decays on its own.
    *
    * <p>The first version only expired a whole trail once its player went away,
    * which meant someone standing still or hovering kept a full-length trail
    * behind them forever. Age now lives on the points, not the player.
    */
   private static class Trail {
      final List<double[]> points = new ArrayList();
      double tracked;
      long lastSeen = System.currentTimeMillis();
   }

   public Trails() {
      super("Trails", "Draws the path players have walked or flown", ModuleCategory.MECHANIC);
   }

   protected void onDisable() {
      this.trails.clear();
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled() || this.mc.theWorld == null) {
         return;
      }

      long now = System.currentTimeMillis();
      double maxLen = this.length.get();

      for(EntityPlayer p : this.mc.theWorld.playerEntities) {
         if(p == this.mc.thePlayer && !this.selfTrail.get()) {
            continue;
         }

         Trail t = this.trails.get(p.getUniqueID());
         if(t == null) {
            t = new Trail();
            this.trails.put(p.getUniqueID(), t);
         }

         t.lastSeen = now;
         double[] last = t.points.isEmpty() ? null : t.points.get(t.points.size() - 1);
         if(last == null) {
            t.points.add(new double[]{p.posX, p.posY, p.posZ, (double)now});
            continue;
         }

         double step = Math.sqrt(sq(p.posX - last[0]) + sq(p.posY - last[1]) + sq(p.posZ - last[2]));
         if(step < MIN_STEP) {
            continue;
         }

         t.points.add(new double[]{p.posX, p.posY, p.posZ, (double)now});
         t.tracked += step;

         // Drop points off the tail until the trail is within the length budget.
         while(t.tracked > maxLen && t.points.size() > 2) {
            double[] a = t.points.get(0);
            double[] b = t.points.get(1);
            t.tracked -= Math.sqrt(sq(b[0] - a[0]) + sq(b[1] - a[1]) + sq(b[2] - a[2]));
            t.points.remove(0);
         }
      }

      long keep = (long)(this.timeout.get() * 1000.0D);

      // Age out individual points. This is what makes a trail behind someone who
      // stopped moving shrink away instead of hanging there indefinitely.
      Iterator<Map.Entry<UUID, Trail>> it = this.trails.entrySet().iterator();
      while(it.hasNext()) {
         Trail t = it.next().getValue();

         while(!t.points.isEmpty() && now - (long)t.points.get(0)[3] > keep) {
            if(t.points.size() > 1) {
               double[] a = t.points.get(0);
               double[] b = t.points.get(1);
               t.tracked -= Math.sqrt(sq(b[0] - a[0]) + sq(b[1] - a[1]) + sq(b[2] - a[2]));
               if(t.tracked < 0.0D) {
                  t.tracked = 0.0D;
               }
            }

            t.points.remove(0);
         }

         // Nothing left to draw and the player is gone: drop the trail entirely.
         if(t.points.isEmpty() && now - t.lastSeen > keep) {
            it.remove();
         }
      }

   }

   private static double sq(double v) {
      return v * v;
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.trails.isEmpty()) {
         return;
      }

      int base = this.chroma.get() ? ColorUtil.chroma(0) : this.color.getRGB();
      float r = (float)(base >> 16 & 255) / 255.0F;
      float g = (float)(base >> 8 & 255) / 255.0F;
      float b = (float)(base & 255) / 255.0F;

      GlStateManager.pushMatrix();
      GlStateManager.translate(-WorldRenderUtil.camX(), -WorldRenderUtil.camY(), -WorldRenderUtil.camZ());
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_LIGHTING);
      if(this.throughWalls.get()) {
         GL11.glDisable(GL11.GL_DEPTH_TEST);
      }

      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glLineWidth((float)this.lineWidth.get());

      for(Trail t : new ArrayList<Trail>(this.trails.values())) {
         if(t.points.size() < 2) {
            continue;
         }

         GL11.glBegin(GL11.GL_LINE_STRIP);
         int n = t.points.size();
         for(int i = 0; i < n; ++i) {
            double[] p = t.points.get(i);
            // Oldest end of the trail fades out, so direction of travel reads.
            float alpha = this.fade.get() ? Math.max(0.1F, (float)(i + 1) / (float)n) : 1.0F;
            GL11.glColor4f(r, g, b, alpha);
            GL11.glVertex3d(p[0], p[1] + 0.1D, p[2]);
         }

         GL11.glEnd();
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glDisable(GL11.GL_BLEND);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GlStateManager.popMatrix();
   }
}
