package com.iceclient.module.modules.factions;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.ColorUtil;
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.block.BlockDispenser;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Boxes nearby dispensers, colour-coded by whether they're loaded.
 *
 * <p>Contents are only known for dispensers the server has actually sent us a
 * tile entity for -- an unopened dispenser reads as empty, so "empty" here means
 * "not known to be loaded" rather than a guarantee.
 */
public class DispenserCheck extends Module {

   private final NumberSetting range = (NumberSetting)this.addSetting(new NumberSetting("Range", 32.0D, 8.0D, 128.0D, 8.0D));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final BooleanSetting showLoaded = (BooleanSetting)this.addSetting(new BooleanSetting("Show loaded", true));
   private final BooleanSetting showEmpty = (BooleanSetting)this.addSetting(new BooleanSetting("Show empty", true));
   private final BooleanSetting showCount = (BooleanSetting)this.addSetting(new BooleanSetting("Show count", true));
   private final ColorSetting loadedColor = (ColorSetting)this.addSetting(new ColorSetting("Loaded color", -16711936));
   private final ColorSetting emptyColor = (ColorSetting)this.addSetting(new ColorSetting("Empty color", -65536));

   public DispenserCheck() {
      super("Dispenser Check", "Boxes dispensers and shows whether they're loaded", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || this.mc.theWorld == null || this.mc.thePlayer == null) {
         return;
      }

      double max = this.range.get();
      float w = (float)this.lineWidth.get();

      for(TileEntity te : new ArrayList<TileEntity>(this.mc.theWorld.loadedTileEntityList)) {
         if(!(te instanceof TileEntityDispenser)) {
            continue;
         }

         BlockPos p = te.getPos();
         if(this.mc.thePlayer.getDistance((double)p.getX() + 0.5D, (double)p.getY(), (double)p.getZ() + 0.5D) > max) {
            continue;
         }

         int count = this.countItems((TileEntityDispenser)te);
         boolean loaded = count > 0;
         if(loaded && !this.showLoaded.get() || !loaded && !this.showEmpty.get()) {
            continue;
         }

         int col = ColorUtil.withAlpha(loaded ? this.loadedColor.getRGB() : this.emptyColor.getRGB(), 200);
         WorldRenderUtil.outlineBox(new AxisAlignedBB(p, p.add(1, 1, 1)), col, w);
         if(this.showCount.get()) {
            WorldRenderUtil.text3d(String.valueOf(count),
                  (double)p.getX() + 0.5D, (double)p.getY() + 1.2D, (double)p.getZ() + 0.5D, col, 0.02F);
         }
      }

   }

   private int countItems(TileEntityDispenser d) {
      int total = 0;
      for(int i = 0; i < d.getSizeInventory(); ++i) {
         net.minecraft.item.ItemStack s = d.getStackInSlot(i);
         if(s != null) {
            total += s.stackSize;
         }
      }

      return total;
   }
}
