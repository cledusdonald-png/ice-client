package com.unclesam.client.module.modules.factions;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

final class PrinterModule$Candidate {
   final BlockPos pos;
   final IBlockState state;

   PrinterModule$Candidate(BlockPos pos, IBlockState state) {
      this.pos = pos;
      this.state = state;
   }
}
