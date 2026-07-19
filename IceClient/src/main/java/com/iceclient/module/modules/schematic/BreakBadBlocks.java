package com.iceclient.module.modules.schematic;

import com.iceclient.util.BindUtil;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.schematic.Schematic;
import com.iceclient.schematic.SchematicManager;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.KeybindSetting;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.input.Keyboard;

public class BreakBadBlocks extends Module {
   public final BooleanSetting dispensers = this.addBool("Dispensers", true);
   public final BooleanSetting slabs = this.addBool("Slabs", true);
   public final BooleanSetting pistons = this.addBool("Pistons", true);
   public final KeybindSetting toggle = this.addKeybind("Toggle Key", 0);
   private boolean armed = true;
   private int keyCooldown;

   public BreakBadBlocks() {
      super("Break Bad Blocks", "Breaks wrong blocks blocking the schematic", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.END) {
         if(this.mc.thePlayer != null && this.mc.theWorld != null && SchematicManager.isLoaded()) {
            if(this.toggle.getKeyCode() != 0 && BindUtil.isDown(this.toggle.getKeyCode())) {
               if(this.keyCooldown-- <= 0) {
                  this.armed = !this.armed;
                  this.keyCooldown = 10;
               }
            } else {
               this.keyCooldown = 0;
            }

            if(this.armed) {
               Schematic s = SchematicManager.getLoaded();
               double maxReach = 4.5D;

               for(int y = 0; y < s.getHeight(); ++y) {
                  for(int z = 0; z < s.getLength(); ++z) {
                     for(int x = 0; x < s.getWidth(); ++x) {
                        if(!s.isAir(x, y, z)) {
                           BlockPos world = s.toWorld(x, y, z);
                           if(this.mc.theWorld.isBlockLoaded(world, false)) {
                              IBlockState want = s.getBlockState(x, y, z);
                              IBlockState have = this.mc.theWorld.getBlockState(world);
                              if(have.getBlock() != want.getBlock() && have.getBlock().getMaterial() != Material.air && this.shouldBreak(have.getBlock()) && this.mc.thePlayer.getDistance((double)world.getX() + 0.5D, (double)world.getY() + 0.5D, (double)world.getZ() + 0.5D) <= maxReach) {
                                 this.mc.playerController.clickBlock(world, EnumFacing.UP);
                                 this.mc.thePlayer.swingItem();
                                 return;
                              }
                           }
                        }
                     }
                  }
               }

            }
         }
      }
   }

   private boolean shouldBreak(Block b) {
      return b == Blocks.dispenser?this.dispensers.get():(b != Blocks.stone_slab && b != Blocks.wooden_slab?(b != Blocks.piston && b != Blocks.sticky_piston?true:this.pistons.get()):this.slabs.get());
   }
}
