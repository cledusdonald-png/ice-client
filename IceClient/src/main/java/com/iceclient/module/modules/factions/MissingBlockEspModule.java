package com.iceclient.module.modules.factions;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.module.modules.factions.SchematicModule;
import com.iceclient.schematica.SchematicaBridge;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ModeSetting;
import com.iceclient.setting.NumberSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class MissingBlockEspModule extends Module {
   private final NumberSetting range = (NumberSetting)this.addSetting(new NumberSetting("Range", 24.0D, 4.0D, 64.0D, 1.0D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 1.5D, 1.0D, 4.0D, 0.5D));
   private final ModeSetting color = (ModeSetting)this.addSetting(new ModeSetting("Color", "Red", new String[]{"Red", "Orange", "Pink", "White"}));
   /**
    * Outline the surface of the missing region instead of a full cube per
    * block. A solid wall then shows one clean outline rather than a dense grid
    * of every internal edge -- far fewer lines to look at and to draw.
    */
   private final BooleanSetting surfaceOnly = (BooleanSetting)this.addSetting(new BooleanSetting("Surface only", true));

   /** Hard cap so a huge schematic cannot flood the frame with geometry. */
   private static final int MAX_BOXES = 4000;

   private final java.util.Set<Long> missing = new java.util.HashSet<Long>();

   public MissingBlockEspModule() {
      super("MissingBlockESP", "Outlines schematic blocks you still need to place", ModuleCategory.FACTIONS, 0);
   }

   private static long key(int x, int y, int z) {
      return ((long)(x & 0x3FFFFF)) | ((long)(y & 0xFFF) << 22) | ((long)(z & 0x3FFFFF) << 34);
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      // "Render: OFF" must silence this too -- it draws a box per missing
      // block, so an unbuilt schematic is a solid grid of wireframe cubes.
      if(!SchematicaBridge.shouldRenderOverlays()) {
         return;
      }

      if(!this.isEnabled() || this.mc.thePlayer == null) {
         return;
      }

      // Gather the missing positions first, so surface culling can ask whether
      // each neighbour is also missing.
      this.missing.clear();
      boolean bridge = SchematicaBridge.isAvailable() && SchematicaBridge.hasSchematic();
      SchematicModule schem = bridge ? null : SchematicModule.getInstance();

      if(bridge) {
         SchematicaBridge.forEachMissing(this.range.get(), (wx, wy, wz) -> {
            if(this.missing.size() < MAX_BOXES) {
               this.missing.add(key((int)Math.floor(wx), (int)Math.floor(wy), (int)Math.floor(wz)));
            }
         });
      } else if(schem != null && schem.hasSchematic()) {
         schem.forEachMissing(this.mc.thePlayer, this.range.get(), (wx, wy, wz) -> {
            if(this.missing.size() < MAX_BOXES) {
               this.missing.add(key((int)Math.floor(wx), (int)Math.floor(wy), (int)Math.floor(wz)));
            }
         });
      }

      if(this.missing.isEmpty()) {
         return;
      }

      double camX = this.mc.getRenderManager().viewerPosX;
      double camY = this.mc.getRenderManager().viewerPosY;
      double camZ = this.mc.getRenderManager().viewerPosZ;

      this.setupLineState();
      GL11.glLineWidth((float)this.lineWidth.get());
      float[] rgb = this.rgb();
      GlStateManager.color(rgb[0], rgb[1], rgb[2], 0.85F);
      GL11.glBegin(1);

      boolean surface = this.surfaceOnly.get();
      for(Long k : this.missing) {
         long v = k.longValue();
         int x = signed22((int)(v & 0x3FFFFF));
         int y = (int)((v >> 22) & 0xFFF);
         int z = signed22((int)((v >> 34) & 0x3FFFFF));

         if(surface) {
            // Skip a block whose six neighbours are all also missing: it is
            // buried inside the region and none of its edges are visible.
            if(this.missing.contains(key(x + 1, y, z)) && this.missing.contains(key(x - 1, y, z))
                  && this.missing.contains(key(x, y + 1, z)) && this.missing.contains(key(x, y - 1, z))
                  && this.missing.contains(key(x, y, z + 1)) && this.missing.contains(key(x, y, z - 1))) {
               continue;
            }
         }

         this.emitBoxEdges((double)x - camX, (double)y - camY, (double)z - camZ);
      }

      GL11.glEnd();
      this.teardownLineState();
   }

   /** Sign-extend a 22-bit coordinate back to a full int. */
   private static int signed22(int v) {
      return (v << 10) >> 10;
   }

   private void emitBoxEdges(double x, double y, double z) {
      double x1 = x + 1.0D;
      double y1 = y + 1.0D;
      double z1 = z + 1.0D;
      this.line(x, y, z, x1, y, z);
      this.line(x1, y, z, x1, y, z1);
      this.line(x1, y, z1, x, y, z1);
      this.line(x, y, z1, x, y, z);
      this.line(x, y1, z, x1, y1, z);
      this.line(x1, y1, z, x1, y1, z1);
      this.line(x1, y1, z1, x, y1, z1);
      this.line(x, y1, z1, x, y1, z);
      this.line(x, y, z, x, y1, z);
      this.line(x1, y, z, x1, y1, z);
      this.line(x1, y, z1, x1, y1, z1);
      this.line(x, y, z1, x, y1, z1);
   }

   private void line(double ax, double ay, double az, double bx, double by, double bz) {
      GL11.glVertex3d(ax, ay, az);
      GL11.glVertex3d(bx, by, bz);
   }

   private float[] rgb() {
      return this.color.is("Orange")?new float[]{1.0F, 0.6F, 0.1F}:(this.color.is("Pink")?new float[]{1.0F, 0.3F, 0.7F}:(this.color.is("White")?new float[]{1.0F, 1.0F, 1.0F}:new float[]{1.0F, 0.2F, 0.2F}));
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
}
