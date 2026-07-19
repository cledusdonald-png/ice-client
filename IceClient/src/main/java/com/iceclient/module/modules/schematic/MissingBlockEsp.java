package com.iceclient.module.modules.schematic;

import com.iceclient.util.BindUtil;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.schematic.Schematic;
import com.iceclient.schematic.SchematicManager;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.WorldRenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.input.Keyboard;

public class MissingBlockEsp extends Module {
   public final BooleanSetting drawEspBox = this.addBool("Draw ESP Box", true);
   public final BooleanSetting drawTracer = this.addBool("Draw Tracer", false);
   public final BooleanSetting showTrays = this.addBool("Show Trays", true);
   public final NumberSetting maxBlocks = this.addNumber("Max Blocks", 200.0D, 10.0D, 2000.0D, 10.0D);
   public final ColorSetting espColor = this.addColor("ESP Color", -11141291);
   public final KeybindSetting keybindToggle = this.addKeybind("Toggle Key", 0);
   private final List<BlockPos> missing = new ArrayList();
   private int ticks;
   private boolean armed = true;
   private int cooldown;

   public MissingBlockEsp() {
      super("Missing Block ESP", "ESP for missing schematic blocks", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.END) {
         if(this.mc.thePlayer != null && this.mc.theWorld != null && SchematicManager.isLoaded()) {
            if(this.keybindToggle.getKeyCode() != 0 && BindUtil.isDown(this.keybindToggle.getKeyCode())) {
               this.armed = !this.armed;
               this.cooldown = 8;
            }

            if(this.cooldown > 0) {
               --this.cooldown;
            } else if(this.armed) {
               if(++this.ticks >= 20) {
                  this.ticks = 0;
                  this.rescan();
               }
            }
         } else {
            this.missing.clear();
         }
      }
   }

   private void rescan() {
      this.missing.clear();
      Schematic s = SchematicManager.getLoaded();
      int limit = this.maxBlocks.getInt();

      for(int y = 0; y < s.getHeight(); ++y) {
         for(int z = 0; z < s.getLength(); ++z) {
            for(int x = 0; x < s.getWidth(); ++x) {
               if(!s.isAir(x, y, z)) {
                  BlockPos world = s.toWorld(x, y, z);
                  if(this.mc.theWorld.isBlockLoaded(world, false)) {
                     IBlockState want = s.getBlockState(x, y, z);
                     IBlockState have = this.mc.theWorld.getBlockState(world);
                     if(have.getBlock() != want.getBlock() && have.getBlock().getMaterial() == Material.air) {
                        this.missing.add(world);
                        if(this.missing.size() >= limit) {
                           return;
                        }
                     }
                  }
               }
            }
         }
      }

   }

   @SubscribeEvent
   public void onRender(RenderWorldLastEvent event) {
      if(this.isEnabled() && this.armed && !this.missing.isEmpty()) {
         int col = this.espColor.getRGB();
         GlStateManager.disableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.disableDepth();
         double camX = WorldRenderUtil.camX();
         double camY = WorldRenderUtil.camY();
         double camZ = WorldRenderUtil.camZ();

         for(BlockPos p : this.missing) {
            AxisAlignedBB box = new AxisAlignedBB(p, p.add(1, 1, 1));
            if(this.drawEspBox.get()) {
               WorldRenderUtil.outlineBox(box, col, 1.5F);
            }

            if(this.drawTracer.get() && this.mc.thePlayer != null) {
               double px = this.mc.thePlayer.posX;
               double py = this.mc.thePlayer.posY + (double)this.mc.thePlayer.getEyeHeight();
               double pz = this.mc.thePlayer.posZ;
               GlStateManager.color((float)(col >> 16 & 255) / 255.0F, (float)(col >> 8 & 255) / 255.0F, (float)(col & 255) / 255.0F, 0.8F);
               Tessellator tess = Tessellator.getInstance();
               WorldRenderer wr = tess.getWorldRenderer();
               wr.begin(3, DefaultVertexFormats.POSITION);
               wr.pos(px - camX, py - camY, pz - camZ).endVertex();
               wr.pos((double)p.getX() + 0.5D - camX, (double)p.getY() + 0.5D - camY, (double)p.getZ() + 0.5D - camZ).endVertex();
               tess.draw();
            }

            if(this.showTrays.get()) {
               AxisAlignedBB tray = new AxisAlignedBB((double)p.getX(), (double)p.getY(), (double)p.getZ(), (double)(p.getX() + 1), (double)p.getY() + 0.05D, (double)(p.getZ() + 1));
               WorldRenderUtil.outlineBox(tray, col, 2.0F);
            }
         }

         GlStateManager.enableDepth();
         GlStateManager.enableTexture2D();
      }
   }
}
