package com.unclesam.client.module.modules.factions;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.schematica.SchematicaBridge;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.NumberSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class AutoTickModule extends Module {
   private final NumberSetting range = (NumberSetting)this.addSetting(new NumberSetting("Range", 5.0D, 3.0D, 9.0D, 0.5D));
   private final NumberSetting perTick = (NumberSetting)this.addSetting(new NumberSetting("Clicks/tick", 4.0D, 1.0D, 32.0D, 1.0D));
   private final BooleanSetting repeaters = (BooleanSetting)this.addSetting(new BooleanSetting("Repeaters", true));
   private final BooleanSetting trapdoors = (BooleanSetting)this.addSetting(new BooleanSetting("Trapdoors", true));
   private final BooleanSetting rotate = (BooleanSetting)this.addSetting(new BooleanSetting("Rotate to face", false));

   public AutoTickModule() {
      super("AutoTick", "Ticks repeaters/trapdoors to match the schematic (arms cannons)", ModuleCategory.FACTIONS, 0);
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.START) {
         if(this.mc.thePlayer != null && this.mc.theWorld != null && this.mc.playerController != null) {
            if(SchematicaBridge.isAvailable() && SchematicaBridge.hasSchematic()) {
               List<BlockPos> toClick = new ArrayList();
               SchematicaBridge.forEachSchematicBlock(this.range.get(), (worldPos, schemState) -> {
                  IBlockState worldState = this.mc.theWorld.getBlockState(worldPos);
                  if(this.needsClick(schemState, worldState)) {
                     toClick.add(worldPos);
                  }

               });
               if(!((List)toClick).isEmpty()) {
                  int budget = this.perTick.getInt();

                  for(BlockPos pos : toClick) {
                     if(budget <= 0) {
                        break;
                     }

                     this.clickBlock(pos);
                     --budget;
                  }

               }
            }
         }
      }
   }

   private boolean needsClick(IBlockState schemState, IBlockState worldState) {
      if(this.repeaters.get() && schemState.getBlock() instanceof BlockRedstoneRepeater && worldState.getBlock() instanceof BlockRedstoneRepeater) {
         int target = ((Integer)schemState.getValue(BlockRedstoneRepeater.DELAY)).intValue();
         int current = ((Integer)worldState.getValue(BlockRedstoneRepeater.DELAY)).intValue();
         return target != current;
      } else if(this.trapdoors.get() && schemState.getBlock() instanceof BlockTrapDoor && worldState.getBlock() instanceof BlockTrapDoor) {
         boolean target = ((Boolean)schemState.getValue(BlockTrapDoor.OPEN)).booleanValue();
         boolean current = ((Boolean)worldState.getValue(BlockTrapDoor.OPEN)).booleanValue();
         return target != current;
      } else {
         return false;
      }
   }

   private void clickBlock(BlockPos pos) {
      if(this.rotate.get()) {
         this.faceBlock(pos);
      }

      Vec3 hitVec = new Vec3((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
      this.mc.playerController.onPlayerRightClick(this.mc.thePlayer, this.mc.theWorld, this.mc.thePlayer.inventory.getCurrentItem(), pos, EnumFacing.UP, hitVec);
      this.mc.thePlayer.swingItem();
   }

   private void faceBlock(BlockPos pos) {
      EntityPlayer player = this.mc.thePlayer;
      double dx = (double)pos.getX() + 0.5D - player.posX;
      double dy = (double)pos.getY() + 0.5D - (player.posY + (double)player.getEyeHeight());
      double dz = (double)pos.getZ() + 0.5D - player.posZ;
      double horizontalDist = Math.sqrt(dx * dx + dz * dz);
      player.rotationYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
      player.rotationPitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontalDist)));
   }
}
