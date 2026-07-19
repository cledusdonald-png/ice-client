package com.iceclient.module.modules.schematic;

import com.iceclient.util.BindUtil;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.input.Keyboard;

public class AutoTick extends Module {
   public final BooleanSetting mcc = this.addBool("MCC", false);
   public final NumberSetting timeout = this.addNumber("Timeout", 2.0D, 0.0D, 20.0D, 1.0D);
   public final KeybindSetting keybindToggle = this.addKeybind("Toggle Key", 0);
   private int cooldown;
   private boolean armed = true;
   private int keyCooldown;

   public AutoTick() {
      super("Auto Tick", "Auto-clicks redstone/levers near you", ModuleCategory.FACTIONS);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.END) {
         if(this.mc.thePlayer != null && this.mc.theWorld != null) {
            if(this.keybindToggle.getKeyCode() != 0 && BindUtil.isDown(this.keybindToggle.getKeyCode())) {
               if(this.keyCooldown-- <= 0) {
                  this.armed = !this.armed;
                  this.keyCooldown = 10;
               }
            } else {
               this.keyCooldown = 0;
            }

            if(this.armed && this.cooldown-- <= 0) {
               BlockPos me = this.mc.thePlayer.getPosition();
               int r = this.mcc.get()?6:4;

               for(int x = -r; x <= r; ++x) {
                  for(int y = -r; y <= r; ++y) {
                     for(int z = -r; z <= r; ++z) {
                        BlockPos p = me.add(x, y, z);
                        if(this.isTickBlock(p)) {
                           this.mc.playerController.onPlayerRightClick(this.mc.thePlayer, this.mc.theWorld, this.mc.thePlayer.getHeldItem(), p, EnumFacing.UP, new Vec3(0.5D, 0.5D, 0.5D));
                           this.cooldown = this.timeout.getInt();
                           return;
                        }
                     }
                  }
               }

            }
         }
      }
   }

   private boolean isTickBlock(BlockPos p) {
      Block b = this.mc.theWorld.getBlockState(p).getBlock();
      return b == Blocks.lever || b == Blocks.stone_button || b == Blocks.wooden_button || b == Blocks.redstone_block || b == Blocks.trapdoor;
   }
}
