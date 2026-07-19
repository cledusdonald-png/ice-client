package com.unclesam.client.module.modules.factions;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ModeSetting;
import com.unclesam.client.setting.NumberSetting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.opengl.GL11;

public class BreadcrumbsModule extends Module {
   private static final int MAX_POINTS = 400;
   private final NumberSetting keepSeconds = (NumberSetting)this.addSetting(new NumberSetting("Keep (s)", 3.0D, 0.5D, 15.0D, 0.5D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final ModeSetting color = (ModeSetting)this.addSetting(new ModeSetting("Color", "White", new String[]{"White", "By type"}));
   private final BooleanSetting headMarker = (BooleanSetting)this.addSetting(new BooleanSetting("Head box", true));
   private final Map<Integer, BreadcrumbsModule.Trail> trails = new LinkedHashMap();

   public BreadcrumbsModule() {
      super("Breadcrumbs", "Trajectory trails for incoming TNT and falling blocks", ModuleCategory.FACTIONS, 0);
   }

   protected void onDisable() {
      this.trails.clear();
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.END) {
         if(this.mc.theWorld != null && this.mc.thePlayer != null) {
            long now = System.currentTimeMillis();
            Set<Integer> seenThisTick = new HashSet();

            for(Entity entity : this.mc.theWorld.loadedEntityList) {
               BreadcrumbsModule.Kind kind = this.kindOf(entity);
               if(kind != null) {
                  seenThisTick.add(Integer.valueOf(entity.getEntityId()));
                  BreadcrumbsModule.Trail trail = (BreadcrumbsModule.Trail)this.trails.get(Integer.valueOf(entity.getEntityId()));
                  if(trail == null) {
                     trail = new BreadcrumbsModule.Trail(kind);
                     this.trails.put(Integer.valueOf(entity.getEntityId()), trail);
                  }

                  trail.alive = true;
                  trail.points.add(new double[]{entity.posX, entity.posY, entity.posZ});
                  if(trail.points.size() > 400) {
                     trail.points.remove(0);
                  }
               }
            }

            long keepMs = (long)(this.keepSeconds.get() * 1000.0D);
            Iterator<Entry<Integer, BreadcrumbsModule.Trail>> it = this.trails.entrySet().iterator();

            while(it.hasNext()) {
               Entry<Integer, BreadcrumbsModule.Trail> entry = (Entry)it.next();
               if(!seenThisTick.contains(entry.getKey())) {
                  BreadcrumbsModule.Trail trail = (BreadcrumbsModule.Trail)entry.getValue();
                  if(trail.alive) {
                     trail.alive = false;
                     trail.despawnAt = now;
                  }

                  if(now - trail.despawnAt > keepMs) {
                     it.remove();
                  }
               }
            }

         }
      }
   }

   private BreadcrumbsModule.Kind kindOf(Entity entity) {
      return entity instanceof EntityTNTPrimed?BreadcrumbsModule.Kind.TNT:(entity instanceof EntityFallingBlock?BreadcrumbsModule.Kind.FALLING:null);
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(this.isEnabled() && !this.trails.isEmpty()) {
         double camX = this.mc.getRenderManager().viewerPosX;
         double camY = this.mc.getRenderManager().viewerPosY;
         double camZ = this.mc.getRenderManager().viewerPosZ;
         long now = System.currentTimeMillis();
         long keepMs = (long)(this.keepSeconds.get() * 1000.0D);
         GL11.glPushMatrix();
         this.setupLineState();
         GL11.glLineWidth((float)this.lineWidth.get());
         Iterator var12 = this.trails.values().iterator();

         while(true) {
            BreadcrumbsModule.Trail trail;
            float alpha;
            while(true) {
               if(!var12.hasNext()) {
                  this.teardownLineState();
                  GL11.glPopMatrix();
                  return;
               }

               trail = (BreadcrumbsModule.Trail)var12.next();
               if(trail.points.size() >= 2) {
                  alpha = 0.9F;
                  if(trail.alive) {
                     break;
                  }

                  float remaining = 1.0F - (float)(now - trail.despawnAt) / (float)keepMs;
                  if(remaining > 0.0F) {
                     alpha = 0.9F * remaining;
                     break;
                  }
               }
            }

            this.applyColor(trail.kind, alpha);
            GL11.glBegin(3);

            for(double[] p : trail.points) {
               GL11.glVertex3d(p[0] - camX, p[1] - camY, p[2] - camZ);
            }

            GL11.glEnd();
            if(this.headMarker.get() && trail.alive) {
               double[] head = (double[])trail.points.get(trail.points.size() - 1);
               this.drawHeadBox(head[0] - camX, head[1] - camY, head[2] - camZ, 0.9F);
            }
         }
      }
   }

   private void applyColor(BreadcrumbsModule.Kind kind, float alpha) {
      if(this.color.is("White")) {
         GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
      } else if(kind == BreadcrumbsModule.Kind.TNT) {
         GlStateManager.color(1.0F, 0.25F, 0.1F, alpha);
      } else {
         GlStateManager.color(0.9F, 0.8F, 0.4F, alpha);
      }

   }

   private void drawHeadBox(double x, double y, double z, float alpha) {
      double half = 0.5D;
      GlStateManager.color(1.0F, 0.95F, 0.2F, alpha);
      GL11.glPushMatrix();
      GL11.glTranslated(x, y, z);
      double min = -half;
      GL11.glBegin(2);
      GL11.glVertex3d(min, min, min);
      GL11.glVertex3d(half, min, min);
      GL11.glVertex3d(half, min, half);
      GL11.glVertex3d(min, min, half);
      GL11.glEnd();
      GL11.glBegin(2);
      GL11.glVertex3d(min, half, min);
      GL11.glVertex3d(half, half, min);
      GL11.glVertex3d(half, half, half);
      GL11.glVertex3d(min, half, half);
      GL11.glEnd();
      GL11.glBegin(1);
      GL11.glVertex3d(min, min, min);
      GL11.glVertex3d(min, half, min);
      GL11.glVertex3d(half, min, min);
      GL11.glVertex3d(half, half, min);
      GL11.glVertex3d(half, min, half);
      GL11.glVertex3d(half, half, half);
      GL11.glVertex3d(min, min, half);
      GL11.glVertex3d(min, half, half);
      GL11.glEnd();
      GL11.glPopMatrix();
   }

   private void setupLineState() {
      GlStateManager.disableTexture2D();
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GL11.glEnable(2848);
   }

   private void teardownLineState() {
      GL11.glDisable(2848);
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static enum Kind {
      TNT,
      FALLING;

      private Kind() {
      }
   }

   private static class Trail {
      final BreadcrumbsModule.Kind kind;
      final List<double[]> points = new ArrayList();
      boolean alive = true;
      long despawnAt = 0L;

      Trail(BreadcrumbsModule.Kind kind) {
         this.kind = kind;
      }
   }
}
