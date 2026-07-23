package com.iceclient.schematica.orbit;
import com.github.lunatrius.core.util.MBlockPos;
import com.github.lunatrius.schematica.block.state.BlockStateHelper;
import com.github.lunatrius.schematica.client.printer.nbtsync.NBTSync;
import com.github.lunatrius.schematica.client.printer.registry.PlacementData;
import com.github.lunatrius.schematica.client.printer.registry.PlacementRegistry;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import com.github.lunatrius.schematica.client.renderer.RenderSchematic;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.google.common.collect.UnmodifiableIterator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import com.github.lunatrius.core.util.BlockPosHelper;
import com.github.lunatrius.schematica.client.printer.nbtsync.SyncRegistry;
import com.github.lunatrius.schematica.client.util.BlockStateToItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.event.ForgeEventFactory;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class OrbitPrinter {
  public static final OrbitPrinter INSTANCE = new OrbitPrinter();
  public final Minecraft minecraft = Minecraft.getMinecraft();
  public final HashMap<BlockPos, Integer> syncBlacklist = new HashMap<>();
  long repeaterTimeout = 0L;
  Packet packet;
  public boolean isEnabled = true;
  public boolean isPrinting = true;
  public SchematicWorld schematic = null;
  public byte[][][] timeout = (byte[][][])null;
  
  public boolean isEnabled() {
    return this.isEnabled;
  }
  
  public void setEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }
  
  public boolean togglePrinting() {
    this.isPrinting = (!this.isPrinting && this.schematic != null);
    if (PrinterSettings.get().isEnabled() != this.isPrinting) {
      PrinterSettings.get().setEnabled(this.isPrinting);
    }
    clearQueues();
    return this.isPrinting;
  }
  
  public boolean isPrinting() {
    return this.isPrinting;
  }
  
  public void setPrinting(boolean isPrinting) {
    this.isPrinting = isPrinting;
    if (PrinterSettings.get().isEnabled() != this.isPrinting) {
      PrinterSettings.get().setEnabled(this.isPrinting);
    }
    clearQueues();
  }
  
  public SchematicWorld getSchematic() {
    return this.schematic;
  }
  
  public void setSchematic(SchematicWorld schematic) {
    this.isPrinting = false;
    this.schematic = schematic;
    refresh();
  }
  
  public void refresh() {
    if (this.schematic != null) {
      this.timeout = new byte[this.schematic.getWidth()][this.schematic.getHeight()][this.schematic.getLength()];
    } else {
      this.timeout = (byte[][][])null;
    } 
    this.syncBlacklist.clear();
    this.reconciling = false;
    this.retryQueue.clear();
    this.autoTickLastValue.clear();
    this.autoTickWaits.clear();
  }
  
  private final List<Pair<Pair<Pair<BlockPos, BlockPos>, Pair<BlockPos, BlockPos>>, List<BlockPos>>> queue = new LinkedList<>();
  private final HashMap<BlockPos, AtomicInteger> cacheAir = new HashMap();




  
  private final LinkedHashSet<BlockPos> retryQueue = new LinkedHashSet<>();




  
  private static final int RETRY_QUEUE_MAX = 8192;




  
  private static final int FAST_360_MIN_PACKETS = 256;



  
  private boolean reconciling = false;



  
  private final HashMap<BlockPos, Object> autoTickLastValue = new HashMap<>();
  private final HashMap<BlockPos, Integer> autoTickWaits = new HashMap<>();



  
  private static final int AUTOTICK_MAX_WAIT = 10;



  
  private float printTickYaw = Float.NaN;
  private boolean lookSentThisTick = false;
  private float pendingPinYaw = Float.NaN;
  
  private static BlockPos asKey(BlockPos pos) {
    return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
  }
  
  private void markPending(BlockPos pos) {
    if (this.reconciling || pos == null) {
      return;
    }
    if (this.retryQueue.size() < 8192) {
      this.retryQueue.add(asKey(pos));
    }
  }
  
  private void markDone(BlockPos pos) {
    if (this.reconciling || pos == null || this.retryQueue.isEmpty()) {
      return;
    }
    this.retryQueue.remove(asKey(pos));
  }





  
  private void markOverlayDirty(BlockPos realPos) {
    try {
      RenderSchematic renderer = RenderSchematic.INSTANCE;
      if (renderer != null) {
        renderer.markBlockForUpdate(realPos);
      }
    } catch (Throwable throwable) {}
  }

  
  public final void clearQueues() {
    this.reconciling = false;
    this.retryQueue.clear();
    this.autoTickLastValue.clear();
    this.autoTickWaits.clear();
    if (this.queue.isEmpty()) {
      return;
    }
    if (this.schematic != null) {
      for (Pair<Pair<Pair<BlockPos, BlockPos>, Pair<BlockPos, BlockPos>>, List<BlockPos>> pairListPair : this.queue) {
        for (BlockPos blockPos : pairListPair.getSecond()) {
          this.schematic.setBlockState(blockPos, Blocks.air.getDefaultState());
        }
        ((List)pairListPair.getSecond()).clear();
      } 
    }
    this.cacheAir.clear();
    this.queue.clear();
  }
  
  public int packetCount = 0;
  
  private static class PlayerState { private PlayerState() {}
    
    enum SneakType { RESET,
      SHIFT_DOWN,
      SHIFT_UP; }

    
    enum Slot {
      ONE(1),
      TWO(2),
      THREE(3),
      FOUR(4),
      FIVE(5),
      SIX(6),
      SEVEN(7),
      EIGHT(8),
      NINE(9),
      RESET;
      final int num;
      
      Slot() {
        this.num = -1;
      }
      
      Slot(int num) {
        this.num = num - 1;
      }
    }
    
    SneakType sneaking = SneakType.RESET;
    Slot slot = Slot.RESET; }
  enum SneakType {
    RESET, SHIFT_DOWN, SHIFT_UP;
  }
  
  public void resetState() { state.slot = PlayerState.Slot.RESET;
    state.sneaking = PlayerState.SneakType.RESET; }
  enum Slot {
    ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9), RESET;
    final int num; Slot() { this.num = -1; } Slot(int num) { this.num = num - 1; } } private static final PlayerState state = new PlayerState();
  
  public boolean print(WorldClient world, EntityPlayerSP player) {
    this.packetCount = 0;
    state.slot = PlayerState.Slot.RESET;
    state.sneaking = PlayerState.SneakType.RESET;

    
    this.printTickYaw = player.rotationYaw;
    this.lookSentThisTick = false;
    double dX = ClientProxy.playerPosition.x - this.schematic.position.getX();
    double dY = ClientProxy.playerPosition.y - this.schematic.position.getY();
    double dZ = ClientProxy.playerPosition.z - this.schematic.position.getZ();
    int x = (int)Math.floor(dX);
    int y = (int)Math.floor(dY);
    int z = (int)Math.floor(dZ);
    int range = PrinterSettings.get().placeDistance;
    int minX = Math.max(0, x - range);
    int maxX = Math.min(this.schematic.getWidth() - 1, x + range);
    int minY = Math.max(0, y - range);
    int maxY = Math.min(this.schematic.getHeight() - 1, y + range);
    int minZ = Math.max(0, z - range);
    int maxZ = Math.min(this.schematic.getLength() - 1, z + range);
    if (minX > maxX || minY > maxY || minZ > maxZ) {
      return false;
    }
    int slot = player.inventory.currentItem;
    boolean isSneaking = player.isSneaking();
    boolean isRenderingLayer = this.schematic.isRenderingLayer;
    int renderingLayer = this.schematic.renderingLayer;
    if (isRenderingLayer) {
      if (renderingLayer > maxY || renderingLayer < minY) {
        return false;
      }
      minY = maxY = renderingLayer;
    } 
    syncSneaking(player, true);
    this.packetCount++;
    double blockReachDistance = this.minecraft.playerController.getBlockReachDistance() - 0.1D;
    double blockReachDistanceSq = blockReachDistance * blockReachDistance;
    if (!this.queue.isEmpty()) {
      for (Pair<Pair<Pair<BlockPos, BlockPos>, Pair<BlockPos, BlockPos>>, List<BlockPos>> queueList : (Iterable<Pair<Pair<Pair<BlockPos, BlockPos>, Pair<BlockPos, BlockPos>>, List<BlockPos>>>)new LinkedList(this.queue)) {
        List<BlockPos> webs = (List<BlockPos>)queueList.getSecond();
        BlockPos firstReal = (BlockPos)((Pair)((Pair)queueList.getFirst()).getFirst()).getFirst(), secondReal = (BlockPos)((Pair)((Pair)queueList.getFirst()).getSecond()).getFirst();
        if (this.minecraft.theWorld.getBlockState(firstReal).getBlock() != Blocks.air && this.minecraft.theWorld.getBlockState(secondReal).getBlock() != Blocks.air) {
          for (BlockPos web : webs) {
            this.schematic.setBlockState(web, Blocks.air.getDefaultState());
          }
          this.queue.remove(queueList);
        } 
      } 
    }
    try {
      for (Iterator<BlockPos> objectIterator = this.cacheAir.keySet().iterator(); objectIterator.hasNext(); ) { BlockPos pos = objectIterator.next();
        AtomicInteger i = (AtomicInteger)this.cacheAir.get(pos);
        if (i.get() <= 0) {
          this.schematic.setBlockState(pos, Blocks.air.getDefaultState());
          this.cacheAir.remove(pos.getImmutable());
        }  }
    
    } catch (Throwable throwable) {}

    
    PrinterSettings settings = PrinterSettings.get();




    
    int packetLimit = (settings.print360 && settings.fast360) ? Math.max(settings.packetLimit, 256) : settings.packetLimit;
    boolean doPacketLimit = settings.doLimitPackets;
    for (MBlockPos pos : BlockPosHelper.getAllInBoxXZY(minX, minY, minZ, maxX, maxY, maxZ)) {
      if (doPacketLimit && this.packetCount >= packetLimit) {
        break;
      }
      if (pos.distanceSqToCenter(dX, dY, dZ) > blockReachDistanceSq) {
        continue;
      }
      try {
        int wy = this.schematic.position.getY() + pos.getY();
        if (wy >= 0 && wy <= 255 && 
          placeBlock(world, player, (BlockPos)pos)) {
          this.packetCount++;
          return syncSlotAndSneaking(player, slot, isSneaking, true);
        } 
      } catch (Throwable e) {
        Reference.logger.error("Could not place block!", e);
        syncSlotAndSneaking(player, slot, isSneaking, false);
        this.packetCount++;
        return syncSlotAndSneaking(player, slot, isSneaking, false);
      } 
    } 





    
    if (!this.retryQueue.isEmpty()) {
      double forgetDistance = blockReachDistance + 6.0D;
      double forgetDistanceSq = forgetDistance * forgetDistance;
      this.reconciling = true;
      try {
        Iterator<BlockPos> it = this.retryQueue.iterator();
        while (it.hasNext()) {
          BlockPos sp = it.next();
          int sx = sp.getX();
          int sy = sp.getY();
          int sz = sp.getZ();
          if (sx < 0 || sy < 0 || sz < 0 || sx >= this.schematic.getWidth() || sy >= this.schematic.getHeight() || sz >= this.schematic.getLength()) {
            it.remove();
            continue;
          } 
          int rwy = this.schematic.position.getY() + sy;
          if (rwy < 0 || rwy > 255) {
            it.remove();
            continue;
          } 
          BlockPos real = new BlockPos(this.schematic.position.getX() + sx, rwy, this.schematic.position.getZ() + sz);
          double dSq = player.getDistanceSq(real);
          if (dSq > forgetDistanceSq) {
            it.remove();
            continue;
          } 
          if (dSq > blockReachDistanceSq) {
            continue;
          }
          if (BlockStateHelper.areBlockStatesEqual(this.schematic.getBlockState(sp), world.getBlockState(real))) {
            it.remove();
            continue;
          } 
          if (doPacketLimit && this.packetCount >= packetLimit) {
            break;
          }
          try {
            if (placeBlock(world, player, sp)) {
              this.packetCount++;
              return syncSlotAndSneaking(player, slot, isSneaking, true);
            } 
          } catch (Throwable throwable) {}
        } 
      } finally {
        
        this.reconciling = false;
      } 
    } 
    this.packetCount++;
    return syncSlotAndSneaking(player, slot, isSneaking, true);
  }
  
  public boolean syncSlotAndSneaking(EntityPlayerSP player, int slot, boolean isSneaking, boolean success) {
    if (!(PrinterSettings.get()).setHeldItem) {
      player.inventory.currentItem = slot;
    }
    syncSneaking(player, isSneaking);
    return success;
  }
  
  public boolean placeBlock(WorldClient world, EntityPlayerSP player, BlockPos pos) {
    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();
    if (this.timeout[x][y][z] > 0) {
      this.timeout[x][y][z] = (byte)(this.timeout[x][y][z] - 1);
      return false;
    } 
    int wx = this.schematic.position.getX() + x;
    int wy = this.schematic.position.getY() + y;
    int wz = this.schematic.position.getZ() + z;
    BlockPos realPos = new BlockPos(wx, wy, wz);



    
    IBlockState overlayBefore = world.getBlockState(realPos);
    try {
      return placeBlock0(world, player, pos, x, y, z, realPos);
    } finally {
      if (world.getBlockState(realPos) != overlayBefore) {
        markOverlayDirty(realPos);
      }
    } 
  }
  
  private boolean placeBlock0(WorldClient world, EntityPlayerSP player, BlockPos pos, int x, int y, int z, BlockPos realPos) {
    IBlockState blockState = this.schematic.getBlockState(pos);
    IBlockState realBlockState = world.getBlockState(realPos);
    Block realBlock = realBlockState.getBlock();

    
    if (BlockStateHelper.areBlockStatesEqual(blockState, realBlockState)) {
      markDone(pos);
      
      this.autoTickLastValue.remove(realPos);
      this.autoTickWaits.remove(realPos);
      
      NBTSync handler = SyncRegistry.INSTANCE.getHandler(realBlock);
      if (handler != null) {
        this.timeout[x][y][z] = (byte)PrinterSettings.get().timeout;
        Integer tries = this.syncBlacklist.get(realPos);
        if (tries == null) {
          tries = Integer.valueOf(0);
        } else if (tries.intValue() >= 20) {
          return true;
        } 
        Reference.logger.trace("Trying to sync block at {} {}", new Object[] { realPos, tries });
        boolean success = handler.execute((EntityPlayer)player, (World)this.schematic, pos, (World)world, realPos);
        if (success) {
          this.syncBlacklist.put(realPos, Integer.valueOf(tries.intValue() + 1));
        } else {
          this.packetCount++;
        } 
        return success;
      } 
      return false;
    } 
    this.syncBlacklist.put(realPos, Integer.valueOf(0));
    markPending(pos);

    
    IBlockState schemBlockState = blockState;
    Block schemBlock = schemBlockState.getBlock();
    int schemID = Block.getIdFromBlock(schemBlock);
    
    int realID = Block.getIdFromBlock(realBlock);
    PrinterSettings settings = PrinterSettings.get();



    
    if (PrinterSettings.isPistonCell(schemBlock)) {
      return false;
    }




    
    if (!(realBlock instanceof net.minecraft.block.BlockAir) && !(realBlock instanceof net.minecraft.block.BlockLiquid))
    {
      
      if (schemID != realID && !PrinterSettings.SAME_IDS.contains(Pair.EitherPair.of(Integer.valueOf(schemID), Integer.valueOf(realID)))) {
        if (realBlock instanceof net.minecraft.block.BlockAir || realBlock instanceof net.minecraft.block.BlockLiquid || PrinterSettings.isPistonCell(realBlock)) {
          return false;
        }
        if ((settings.destroyBlocks || settings.destroyair || settings.breakBadBlocksModule) && !this.minecraft.theWorld.isAirBlock(realPos) && this.minecraft.playerController.isInCreativeMode()) {
          if (realBlock instanceof BlockDispenser || schemBlock instanceof BlockDispenser || realBlock instanceof BlockPistonBase || schemBlock instanceof BlockPistonBase || realBlock instanceof BlockSlab || schemBlock instanceof BlockSlab || realBlock instanceof net.minecraft.block.BlockRedstoneWire || schemBlock instanceof net.minecraft.block.BlockRedstoneWire || realBlock instanceof BlockRedstoneRepeater || schemBlock instanceof BlockRedstoneRepeater || realBlock instanceof BlockRedstoneComparator || schemBlock instanceof BlockRedstoneComparator || realID == 152 || schemID == 152 || realID == 69 || schemID == 69 || realID == 89 || schemID == 89 || realBlock instanceof net.minecraft.block.BlockCarpet || schemBlock instanceof net.minecraft.block.BlockCarpet || realBlock instanceof BlockTrapDoor || schemBlock instanceof BlockTrapDoor || realBlock instanceof net.minecraft.block.BlockLadder || schemBlock instanceof net.minecraft.block.BlockLadder) {



            
            this.minecraft.playerController.clickBlock(realPos, EnumFacing.DOWN);
            this.timeout[x][y][z] = (byte)PrinterSettings.get().timeout;
            this.packetCount++;
            return !settings.destroyInstantly;
          } 
          if ((settings.destroyBlocks && schemID != 0) || (schemID == 0 && settings.removeAir)) {
            this.minecraft.playerController.clickBlock(realPos, EnumFacing.DOWN);
            this.timeout[x][y][z] = (byte)settings.timeout;
            this.packetCount++;
            return !settings.destroyInstantly;
          } 
        } 
      } else if (schemID == realID || PrinterSettings.SAME_IDS.contains(Pair.EitherPair.of(Integer.valueOf(schemID), Integer.valueOf(realID)))) {
        syncSneaking(player, false);
        this.packetCount++;




        
        boolean powerMismatch = false;
        if (settings.delayPowered) {
          for (UnmodifiableIterator<IProperty> unmodifiableIterator1 = schemBlockState.getProperties().keySet().iterator(); unmodifiableIterator1.hasNext(); ) { IProperty<?> powerProp = unmodifiableIterator1.next();
            if (PrinterSettings.isPowerStateProperty(powerProp) && realBlockState
              .getProperties().containsKey(powerProp) && 
              !Objects.equals(schemBlockState.getValue(powerProp), realBlockState.getValue(powerProp))) {
              powerMismatch = true;
              break;
            }  }
        
        }
        if ((settings.breakBadBlocksModule || settings.destroyBlocks) && schemBlockState.getBlock() instanceof BlockSlab && realBlockState instanceof BlockSlab && (
          (BlockSlab)schemBlock).isDouble() != ((BlockSlab)realBlock).isDouble() && (
          settings.breakBadBlocksModule || settings.destroyBlocks) && this.minecraft.playerController.clickBlock(realPos, EnumFacing.DOWN)) {
          this.timeout[x][y][z] = (byte)settings.timeout;
          this.packetCount++;
          return !settings.destroyInstantly;
        } 

        
        for (UnmodifiableIterator<IProperty> unmodifiableIterator = schemBlockState.getProperties().keySet().iterator(); unmodifiableIterator.hasNext(); ) { IProperty<?> iProperty = unmodifiableIterator.next();
          if (Objects.equals(schemBlockState.getValue(iProperty), realBlockState.getValue(iProperty))) {
            continue;
          }
          if (iProperty == BlockPistonBase.FACING || iProperty == BlockTrapDoor.HALF || iProperty == BlockDirectional.FACING || iProperty == BlockDispenser.FACING || iProperty == BlockLever.FACING || iProperty == BlockBanner.FACING || iProperty == BlockBanner.ROTATION || iProperty == BlockStandingSign.ROTATION || iProperty == BlockDoor.FACING || iProperty == BlockDoor.HALF || iProperty == BlockDoor.HINGE || iProperty == BlockFurnace.FACING || iProperty == BlockTorch.FACING || iProperty == BlockSlab.HALF) {




            
            if (powerMismatch) {
              continue;
            }
            if ((settings.breakBadBlocksModule || settings.destroyBlocks) && this.minecraft.playerController.clickBlock(realPos, EnumFacing.DOWN)) {
              this.timeout[x][y][z] = (byte)settings.timeout;
              this.packetCount++;
              return !settings.destroyInstantly;
            }  continue;
          } 
          if (iProperty == BlockRedstoneRepeater.DELAY || iProperty == BlockFenceGate.OPEN || iProperty == BlockTrapDoor.OPEN || iProperty == BlockRedstoneComparator.MODE || iProperty == BlockDoor.OPEN) {



            
            if (realBlock instanceof BlockTrapDoor && !settings.aTickTrapdoors) {
              continue;
            }
            boolean openState = (iProperty == BlockFenceGate.OPEN || iProperty == BlockTrapDoor.OPEN || iProperty == BlockDoor.OPEN);
            if (openState && powerMismatch) {
              continue;
            }
            if (settings.autoTick) {
              Object targetVal = schemBlockState.getValue(iProperty);
              Object currentVal = realBlockState.getValue(iProperty);
              Object tickedFrom = this.autoTickLastValue.get(realPos);
              if (tickedFrom != null && Objects.equals(tickedFrom, currentVal)) {


                
                int waited = ((Integer)this.autoTickWaits.getOrDefault(realPos, Integer.valueOf(0))).intValue() + 1;
                if (waited < 10) {
                  this.autoTickWaits.put(realPos, Integer.valueOf(waited));
                  this.timeout[x][y][z] = 1;
                  return false;
                } 
                
                this.autoTickWaits.remove(realPos);
              } 




              
              Object startVal = currentVal;
              int clicksLeft = Math.max(1, iProperty.getAllowedValues().size() - 1);
              boolean clicked = false;
              while (!Objects.equals(currentVal, targetVal) && clicksLeft-- > 0 && 
                this.minecraft.playerController.onPlayerRightClick(player, world, player.getCurrentEquippedItem(), realPos, EnumFacing.UP, new Vec3(0.0D, 0.0D, 0.0D))) {

                
                this.packetCount++;
                clicked = true;
                IBlockState predicted = world.getBlockState(realPos);
                if (!predicted.getProperties().containsKey(iProperty)) {
                  break;
                }
                Object newVal = predicted.getValue(iProperty);
                if (Objects.equals(newVal, currentVal)) {
                  break;
                }
                currentVal = newVal;
              } 
              if (clicked) {
                this.autoTickLastValue.put(realPos, startVal);
                this.autoTickWaits.remove(realPos);
                this.timeout[x][y][z] = (byte)Math.max(1, settings.autoTickTimeout);
                player.swingItem();
                this.packetCount++;
                return !settings.placeInsantly;
              } 
            } 
          }  }
        
        return false;
      } 
    }
    if (BlockStateHelper.areBlockStatesEqual(schemBlockState, world.getBlockState(realPos))) {
      return false;
    }
    if (schemBlock instanceof BlockBanner && !settings.placeBanners) {
      this.timeout[x][y][z] = (byte)(settings.timeout + 3);
      return false;
    } 
    
    if (this.schematic.isAirBlock(pos)) {
      return false;
    }
    if (!realBlock.isReplaceable((World)world, realPos)) {
      return false;
    }
    ItemStack itemStack = BlockStateToItemStack.getItemStack(blockState, new MovingObjectPosition((Entity)player), this.schematic, pos);
    if (itemStack == null || itemStack.getItem() == null) {
      Reference.logger.debug("{} is missing a mapping!", new Object[] { blockState });
      return false;
    } 
    if (placeBlock(world, player, realPos, blockState, itemStack)) {
      this.packetCount++;
      this.timeout[x][y][z] = (byte)PrinterSettings.get().timeout;
      if (schemBlock instanceof BlockSlab) {
        this.timeout[x][y][z] = (byte)(this.timeout[x][y][z] + 1);
      }
      if (settings.placeInsantly && settings.uberFast && (schemID == 87 || schemID == 80 || schemID == 49 || schemID == 4 || schemID == 1 || schemID == 165 || schemID == 24)) {

        
        BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ()); int i;
        for (i = 1; i < settings.placeDistance / 3; i++) {
          BlockPos newPos = mbp.add(0, -i, 0);
          if (newPos.getY() < 0)
            break;  IBlockState state = this.schematic.getBlockState(newPos);
          if (state == null || state.getBlock() == null || !BlockStateHelper.areBlockStatesEqual(state, blockState)) {
            break;
          }
          Block real = blockAt(realPos.offset(EnumFacing.DOWN, i));
          if (real instanceof net.minecraft.block.BlockAir || real instanceof net.minecraft.block.BlockLiquid) {
            if (!placeBlock(world, player, realPos.offset(EnumFacing.DOWN, i), blockState, itemStack)) {
              break;
            }
            this.timeout[mbp.getX()][mbp.getY()][mbp.getZ()] = (byte)PrinterSettings.get().timeout;
          } 
        } 
        for (i = 1; i < settings.placeDistance / 3; i++) {
          BlockPos newPos = mbp.add(-i, 0, 0);
          if (newPos.getX() < 0)
            break;  IBlockState state = this.schematic.getBlockState(newPos);
          if (state == null || state.getBlock() == null || !BlockStateHelper.areBlockStatesEqual(state, blockState)) {
            break;
          }
          Block real = blockAt(realPos.offset(EnumFacing.WEST, i));
          if (real instanceof net.minecraft.block.BlockAir || real instanceof net.minecraft.block.BlockLiquid) {
            if (!placeBlock(world, player, realPos.offset(EnumFacing.WEST, i), blockState, itemStack)) {
              break;
            }
            this.timeout[mbp.getX()][mbp.getY()][mbp.getZ()] = (byte)PrinterSettings.get().timeout;
          } 
        } 
        for (i = 1; i < settings.placeDistance / 3; i++) {
          BlockPos newPos = mbp.add(0, 0, -i);
          if (newPos.getZ() < 0)
            break;  IBlockState state = this.schematic.getBlockState(newPos);
          if (state == null || state.getBlock() == null || !BlockStateHelper.areBlockStatesEqual(state, blockState)) {
            break;
          }
          Block real = blockAt(realPos.offset(EnumFacing.NORTH, i));
          if (real instanceof net.minecraft.block.BlockAir || real instanceof net.minecraft.block.BlockLiquid) {
            if (!placeBlock(world, player, realPos.offset(EnumFacing.NORTH, i), blockState, itemStack)) {
              break;
            }
            this.timeout[mbp.getX()][mbp.getY()][mbp.getZ()] = (byte)PrinterSettings.get().timeout;
          } 
        } 
      } 
      
      return !(PrinterSettings.get()).placeInsantly;
    } 
    return false;
  }
  
  private boolean isSolid(World world, BlockPos pos, EnumFacing side) {
    BlockPos offset = pos.offset(side);
    IBlockState blockState = world.getBlockState(offset);
    Block block = blockState.getBlock();
    if (block == null) {
      return false;
    }
    if (block.isAir((IBlockAccess)world, offset)) {
      return false;
    }
    if (block instanceof net.minecraftforge.fluids.BlockFluidBase) {
      return false;
    }
    return !block.isReplaceable(world, offset);
  }
  
  private List<EnumFacing> getSolidSides(World world, BlockPos pos) {
    if (!(PrinterSettings.get()).placeAdjacent) {
      return Arrays.asList(EnumFacing.VALUES);
    }
    List<EnumFacing> list = new ArrayList<>();
    for (EnumFacing side : EnumFacing.VALUES) {
      if (isSolid(world, pos, side)) {
        list.add(side);
      }
    } 
    return list;
  }








  
  private boolean isValidUnderYaw(PlacementData data, IBlockState blockState, EntityPlayerSP player, BlockPos pos, World world, float yaw) {
    float oldYaw = player.rotationYaw;
    player.rotationYaw = yaw;
    try {
      return data.isValidPlayerFacing(blockState, (EntityPlayer)player, pos, world);
    } finally {
      player.rotationYaw = oldYaw;
    } 
  }
  
  private float findPlacementYaw(IBlockState blockState, EntityPlayerSP player, BlockPos pos, World world, PlacementData data, float originalYaw) {
    float result = Float.NaN;
    for (int i = 0; i < 16; i++) {
      player.rotationYaw = -180.0F + i * 22.5F;
      if (data.isValidPlayerFacing(blockState, (EntityPlayer)player, pos, world)) {
        result = player.rotationYaw;
        break;
      } 
    } 
    player.rotationYaw = originalYaw;
    return result;
  }
  
  private boolean placeBlock(WorldClient world, EntityPlayerSP player, BlockPos pos, IBlockState blockState, ItemStack itemStack) {
    if (itemStack.getItem() instanceof net.minecraft.item.ItemBucket);

    
    double blockReachDistance = this.minecraft.playerController.getBlockReachDistance() - 0.1D;
    double blockReachDistanceSq = blockReachDistance * blockReachDistance;
    if (player.getDistanceSq(pos) > blockReachDistanceSq) {
      return false;
    }
    
    PlacementData data = PlacementRegistry.INSTANCE.getPlacementData(blockState, itemStack);
    float oldYaw = player.rotationYaw;
    boolean rotated = false;

    
    this.packet = null; try {
      EnumFacing direction;
      float offsetX, offsetY, offsetZ;
      int extraClicks;
      if (data != null) {
        boolean validNow = data.isValidPlayerFacing(blockState, (EntityPlayer)player, pos, (World)world);
        if ((PrinterSettings.get()).print360 && (PrinterSettings.get()).fast360) {










          
          float targetYaw = findPlacementYaw(blockState, player, pos, (World)world, data, oldYaw);
          if (Float.isNaN(targetYaw)) {
            if (!validNow)
            {
              
              return false;
            }
          }
          else {
            
            player.rotationYaw = targetYaw;
            this.pendingPinYaw = targetYaw;
            this.packet = (Packet)new C03PacketPlayer.C05PacketPlayerLook(targetYaw, player.rotationPitch, player.onGround);
            rotated = true;
          } 
        } else if ((PrinterSettings.get()).print360) {
          if (isValidUnderYaw(data, blockState, player, pos, (World)world, this.printTickYaw)) {



            
            player.rotationYaw = this.printTickYaw;
            rotated = true;
          } else {
            float targetYaw = findPlacementYaw(blockState, player, pos, (World)world, data, oldYaw);
            if (Float.isNaN(targetYaw)) {
              if (!validNow)
              {
                
                return false; } 
            } else {
              if (this.lookSentThisTick)
              {

                
                return false;
              }
              player.rotationYaw = targetYaw;


              
              this.pendingPinYaw = targetYaw;
              this.packet = (Packet)new C03PacketPlayer.C05PacketPlayerLook(targetYaw, player.rotationPitch, player.onGround);
              rotated = true;
            } 
          } 
        } else if (!validNow) {
          
          return false;
        } 
      } 
      List<EnumFacing> solidSides = getSolidSides((World)world, pos);
      if (solidSides.size() == 0) {
        return false;
      }




      
      if (data != null) {
        List<EnumFacing> validDirections = data.getValidBlockFacings(solidSides, blockState);
        if (validDirections.size() == 0) {
          return false;
        }
        direction = validDirections.get(0);
        offsetX = data.getOffsetX(blockState);
        offsetY = data.getOffsetY(blockState);
        offsetZ = data.getOffsetZ(blockState);
        extraClicks = data.getExtraClicks(blockState);
      } else {
        direction = solidSides.get(0);
        offsetX = 0.5F;
        offsetY = 0.5F;
        offsetZ = 0.5F;
        extraClicks = 0;
      } 
      if (!swapToItem(player.inventory, itemStack)) {
        return false;
      }



      
      return placeBlock(world, player, pos, direction, offsetX, offsetY, offsetZ, extraClicks);
    } finally {
      if (rotated) {
        player.rotationYaw = oldYaw;
      }
    } 
  }
  
  private boolean placeBlock(WorldClient world, EntityPlayerSP player, BlockPos pos, EnumFacing direction, float offsetX, float offsetY, float offsetZ, int extraClicks) {
    ItemStack itemStack = player.getCurrentEquippedItem();
    boolean success = false;
    if (!this.minecraft.playerController.isInCreativeMode() && itemStack != null && itemStack.stackSize <= extraClicks) {
      return false;
    }
    BlockPos offset = pos.offset(direction);
    EnumFacing side = direction.getOpposite();
    Vec3 hitVec = new Vec3((offset.getX() + offsetX), (offset.getY() + offsetY), (offset.getZ() + offsetZ));



    
    Block clickedBlock = world.getBlockState(offset).getBlock();
    if (clickedBlock instanceof net.minecraft.block.BlockContainer || !clickedBlock.isFullCube()) {
      syncSneaking(player, true);
    }
    success = placeBlock(world, player, itemStack, offset, side, hitVec);
    for (int i = 0; success && i < extraClicks; i++) {
      success = placeBlock(world, player, itemStack, offset, side, hitVec);
    }
    if (itemStack != null && itemStack.stackSize == 0 && success) {
      player.inventory.mainInventory[player.inventory.currentItem] = null;
    }
    return success;
  }
  
  private boolean placeBlock(WorldClient world, EntityPlayerSP player, ItemStack itemStack, BlockPos pos, EnumFacing side, Vec3 hitVec) {
    boolean success = !ForgeEventFactory.onPlayerInteract((EntityPlayer)player, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, (World)world, pos, side).isCanceled();
    if (success) {
      
      if (this.packet != null) {
        player.sendQueue.addToSendQueue(this.packet);
        this.packet = null;



        
        this.lookSentThisTick = true;
        this.printTickYaw = this.pendingPinYaw;
      } 
      success = this.minecraft.playerController.onPlayerRightClick(player, world, itemStack, pos, side, hitVec);
      if (success) {
        player.swingItem();
        this.schematic.markBlockForUpdate(pos);


        
        markOverlayDirty(pos.offset(side));
      } 
    } 

    
    return success;
  }





  

   /**
    * Sneaks or unsneaks, and tells the server.
    *
    * <p>Hand-written from the bytecode: this is the one method JD-Core could not
    * decompile. The printer sneaks before clicking containers so a chest or
    * dispenser opens its GUI instead of swallowing the placement, and the state
    * is tracked so it does not send a packet every tick.
    */
   public void syncSneaking(EntityPlayerSP player, boolean isSneaking) {
      if(state.sneaking == PlayerState.SneakType.RESET || isSneaking) {
         if(state.sneaking == PlayerState.SneakType.SHIFT_DOWN) {
            return;
         }
      } else if(state.sneaking == PlayerState.SneakType.SHIFT_UP) {
         return;
      }

      state.sneaking = isSneaking
            ? PlayerState.SneakType.SHIFT_DOWN
            : PlayerState.SneakType.SHIFT_UP;

      player.setSneaking(isSneaking);
      player.sendQueue.addToSendQueue(new net.minecraft.network.play.client.C0BPacketEntityAction(
            player, isSneaking
                  ? net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SNEAKING
                  : net.minecraft.network.play.client.C0BPacketEntityAction.Action.STOP_SNEAKING));
   }

   /** World lookup, replacing Orbit's ChunkUtil. */
   private static Block blockAt(BlockPos pos) {
      net.minecraft.client.multiplayer.WorldClient w = Minecraft.getMinecraft().theWorld;
      return w == null ? Blocks.air : w.getBlockState(pos).getBlock();
   }




  
  public boolean swapToItem(InventoryPlayer inventory, ItemStack itemStack) {
    return swapToItem(inventory, itemStack, true);
  }
  
  private boolean swapToItem(InventoryPlayer inventory, ItemStack itemStack, boolean swapSlots) {
    int slot = 0;
    
    if (itemStack == null) {
      slot = inventory.getFirstEmptyStack();
      if (slot == -1) {
        return false;
      }
    } else {
      int slot2 = getInventorySlotWithItem(inventory, itemStack);





      
      slot = slot2;
    } 
    
    if (this.minecraft.playerController.isInCreativeMode() && (slot < 0 || slot >= 9) && ConfigurationHandler.swapSlotsQueue.size() > 0) {
      inventory.currentItem = getNextSlot();
      if (itemStack == null) {
        inventory.setInventorySlotContents(inventory.currentItem, null);
      } else {
        inventory.setInventorySlotContents(inventory.currentItem, itemStack.copy());
      } 
      this.minecraft.playerController.sendSlotPacket(inventory.getStackInSlot(inventory.currentItem), 36 + inventory.currentItem);
      return true;
    } 
    if (slot >= 0 && slot < 9) {
      inventory.currentItem = slot;
      return true;
    }  if (swapSlots && slot >= 9 && slot < 36 && 
      swapSlots(slot)) {
      return swapToItem(inventory, itemStack, false);
    }
    
    return false;
  }
  
  private int getInventorySlotWithItem(InventoryPlayer inventory, ItemStack itemStack) {
    for (int i = 0; i < inventory.mainInventory.length; i++) {
      if (inventory.mainInventory[i] != null && inventory.mainInventory[i].isItemEqual(itemStack)) {
        if ((PrinterSettings.get()).disableGens && 
          !inventory.mainInventory[i].hasTagCompound() && !inventory.mainInventory[i].isItemEnchanted()) {
          return i;
        }
        return i;
      } 
    } 
    return -1;
  }
  
  private boolean swapSlots(int from) {
    if (ConfigurationHandler.swapSlotsQueue.size() > 0) {
      int slot = getNextSlot();
      swapSlots(from, slot);
      return true;
    } 
    return false;
  }
  
  private int getNextSlot() {
    int slot = ((Integer)ConfigurationHandler.swapSlotsQueue.poll()).intValue() % 9;
    ConfigurationHandler.swapSlotsQueue.offer(Integer.valueOf(slot));
    return slot;
  }
  
  private boolean swapSlots(int from, int to) {
    return (this.minecraft.playerController.windowClick(this.minecraft.thePlayer.inventoryContainer.windowId, from, to, 2, (EntityPlayer)this.minecraft.thePlayer) == null);
  }
}


/* Location:              C:\Users\Evan\OneDrive\Desktop\orbitunobsdsa.jar!\com\github\lunatrius\schematica\client\printer\OrbitPrinterSettings.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */