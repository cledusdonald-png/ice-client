package com.iceclient.module.modules.schematic;

import com.google.common.collect.UnmodifiableIterator;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.schematica.SchematicaBridge;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.NumberSetting;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

/**
 * Front-end for Schematica's printer.
 *
 * <p>This drives {@code SchematicPrinter} rather than running a placement loop
 * of its own. There used to be a "V2" mode with our own engine, and running two
 * loops over the same schematic was worse than either alone -- both queued the
 * same holes, both sent placements, and the results fought each other. The
 * engine is gone; there is one printer and this configures it.
 *
 * <p>That also matches how every mature factions printer is built: Schematica's
 * engine, patched. Improvements belong in that engine, not in a second one
 * racing it.
 */
public class Printer extends Module {

   private final NumberSetting placeDistance = this.addNumber("Place Distance", 5.0D, 1.0D, 9.0D, 1.0D);
   private final BooleanSetting placeInstantly = this.addBool("Place Instantly", true);
   private final NumberSetting placeDelay = this.addNumber("Place Delay", 0.0D, 0.0D, 20.0D, 1.0D);
   private final NumberSetting timeout = this.addNumber("Timeout", 2.0D, 0.0D, 20.0D, 1.0D);
   private final BooleanSetting placeAdjacent = this.addBool("Place Adjacent", true);
   private final BooleanSetting breakWrong = this.addBool("Break Wrong Blocks", true);
   private final BooleanSetting removeExtra = this.addBool("Remove Extra Blocks", false);
   private final BooleanSetting breakInstantly = this.addBool("Break Instantly", false);
   private final NumberSetting breakDelay = this.addNumber("Break Delay", 4.0D, 0.0D, 20.0D, 1.0D);
   private final BooleanSetting autoTick = this.addBool("Auto Tick", true);
   private final BooleanSetting aTickTrapdoors = this.addBool("Tick Trapdoors", true);
   private final NumberSetting autoTickTimeout = this.addNumber("Auto Tick Timeout", 2.0D, 0.0D, 20.0D, 1.0D);
   private final KeybindSetting autoTickKey = this.addKeybind("Auto Tick Key", 0);
   // Nine toggles is most of the panel for something almost nobody changes, so
   // they live behind one switch. Off = use the whole hotbar.
   private final BooleanSetting limitSlots = this.addBool("Limit Hotbar Slots", false);
   private final BooleanSetting slot1 = this.addBool("Slot 1", true);
   private final BooleanSetting slot2 = this.addBool("Slot 2", true);
   private final BooleanSetting slot3 = this.addBool("Slot 3", true);
   private final BooleanSetting slot4 = this.addBool("Slot 4", true);
   private final BooleanSetting slot5 = this.addBool("Slot 5", true);
   private final BooleanSetting slot6 = this.addBool("Slot 6", true);
   private final BooleanSetting slot7 = this.addBool("Slot 7", true);
   private final BooleanSetting slot8 = this.addBool("Slot 8", true);
   private final BooleanSetting slot9 = this.addBool("Slot 9", true);

   private int autoTickCooldown;

   private final com.iceclient.schematica.PrintEngine engineImpl = new com.iceclient.schematica.PrintEngine();

   /** Snapshots the settings the engine needs into its plain config object. */
   private com.iceclient.schematica.PrintEngine.Config buildEngineConfig() {
      com.iceclient.schematica.PrintEngine.Config c = new com.iceclient.schematica.PrintEngine.Config();
      c.placeDistance = this.placeDistance.get();
      c.placeInstantly = this.placeInstantly.get();
      // Even "instantly" needs a ceiling, or one tick tries to place the whole
      // queue and the server drops you for packet spam.
      c.packetLimit = 64;
      c.delay = this.placeDelay.getInt();
      c.placeAdjacent = this.placeAdjacent.get();
      c.replaceWrong = this.breakWrong.get();
      c.breakInstantly = this.breakInstantly.get();
      c.keepSlot = false;
      c.removeExtra = this.removeExtra.get();
      c.breakDelay = this.breakDelay.getInt();
      c.useInventory = true;
      c.orientBlocks = true;
      c.slots = this.slots();
      return c;
   }

   public Printer() {
      super("Printer", "Auto-places the loaded schematic", ModuleCategory.FACTIONS);
      this.placeDistance.inSection("GENERAL");
      this.placeInstantly.inSection("GENERAL");
      this.placeDelay.inSection("GENERAL");
      this.timeout.inSection("GENERAL");
      this.placeAdjacent.inSection("GENERAL");
      this.breakWrong.inSection("CLEAR");
      this.removeExtra.inSection("CLEAR");
      this.breakInstantly.inSection("CLEAR");
      this.breakDelay.inSection("CLEAR");
      this.autoTick.inSection("AUTO TICK");
      this.aTickTrapdoors.inSection("AUTO TICK");
      this.autoTickTimeout.inSection("AUTO TICK");
      this.autoTickKey.inSection("AUTO TICK");
      this.limitSlots.inSection("HOTBAR");
      this.slot1.inSection("HOTBAR");
      this.slot2.inSection("HOTBAR");
      this.slot3.inSection("HOTBAR");
      this.slot4.inSection("HOTBAR");
      this.slot5.inSection("HOTBAR");
      this.slot6.inSection("HOTBAR");
      this.slot7.inSection("HOTBAR");
      this.slot8.inSection("HOTBAR");
      this.slot9.inSection("HOTBAR");

      this.removeExtra.visibleWhen(this.breakWrong::get);
      this.breakInstantly.visibleWhen(this.breakWrong::get);
      this.breakDelay.visibleWhen(this.breakWrong::get);
      this.aTickTrapdoors.visibleWhen(this.autoTick::get);
      this.autoTickTimeout.visibleWhen(this.autoTick::get);

      this.slot1.visibleWhen(this.limitSlots::get);
      this.slot2.visibleWhen(this.limitSlots::get);
      this.slot3.visibleWhen(this.limitSlots::get);
      this.slot4.visibleWhen(this.limitSlots::get);
      this.slot5.visibleWhen(this.limitSlots::get);
      this.slot6.visibleWhen(this.limitSlots::get);
      this.slot7.visibleWhen(this.limitSlots::get);
      this.slot8.visibleWhen(this.limitSlots::get);
      this.slot9.visibleWhen(this.limitSlots::get);
   }

   /** Enabled hotbar slots, or all nine when the limit is off. */
   private boolean[] slots() {
      if(!this.limitSlots.get()) {
         return new boolean[]{true, true, true, true, true, true, true, true, true};
      }

      return new boolean[]{this.slot1.get(), this.slot2.get(), this.slot3.get(), this.slot4.get(),
            this.slot5.get(), this.slot6.get(), this.slot7.get(), this.slot8.get(), this.slot9.get()};
   }

   protected void onEnable() {
      // Schematica's own printer is never switched on. Turned off here as well
      // as in the tick, so a flag left over from a previous session cannot get
      // one free pass placing blocks before the tick notices it.
      if(SchematicaBridge.isAvailable()) {
         SchematicaBridge.setPrinting(false);
      }

      this.engineImpl.reset();
   }

   protected void onDisable() {
      if(SchematicaBridge.isAvailable()) {
         SchematicaBridge.setPrinting(false);
      }

      // Drop the queue: it holds positions from a schematic that may be moved
      // or unloaded before this is switched back on.
      this.engineImpl.reset();
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.END) {
         if(this.mc.thePlayer != null && this.mc.theWorld != null && this.mc.playerController != null) {
            if(SchematicaBridge.isAvailable() && SchematicaBridge.hasSchematic()) {
               // Schematica's own printer stays off, always.
               //
               // There used to be a switch between it and ours, which was a
               // mistake: whichever was not selected still had to be actively
               // suppressed every tick, both wanted the same holes, and the
               // "Schematica" side placed blocks everywhere because its rule
               // for wrongness is "state differs" -- which is true of every
               // repeater and every extended piston in a cannon.
               if(SchematicaBridge.isPrinting()) {
                  SchematicaBridge.setPrinting(false);
               }

               if(this.mc.currentScreen == null) {
                  this.engineImpl.tick(this.buildEngineConfig());

                  // Auto-tick after placing: a repeater has to exist before its
                  // delay can be set. Breaking is the engine's job now, since it
                  // already knows which positions are wrong.
                  if(this.autoTick.get()) {
                     this.runAutoTick();
                  }
               }
            }
         }
      }
   }

   private void pushFullConfig() {
      SchematicaBridge.applyPrinterConfig(this.placeDistance.getInt(), this.placeInstantly.get(),
            this.placeDelay.getInt(), this.timeout.getInt(), this.placeAdjacent.get(),
            false, false, this.slots());
   }


   /**
    * Piston bases, heads and the moving-block entity.
    *
    * <p>An extended piston is a base with {@code extended=true} plus a separate
    * head block that no schematic contains, so every one of them reads as
    * "wrong" to a printer. Skipping the whole cell is simpler and safer than
    * trying to work out which half is legitimately misplaced.
    */
   private static boolean isPistonCell(Block b) {
      return b instanceof BlockPistonBase
            || b instanceof BlockPistonExtension
            || b instanceof BlockPistonMoving;
   }

   private void runAutoTick() {
      if(this.autoTickCooldown-- <= 0) {
         BlockPos[] target = new BlockPos[]{null};
         SchematicaBridge.forEachSchematicBlock(this.placeDistance.get(), (world, want) -> {
            if(target[0] == null && this.mc.theWorld.isBlockLoaded(world, false)) {
               IBlockState have = this.mc.theWorld.getBlockState(world);
               if(this.needsTick(have, want)) {
                  target[0] = world;
               }
            }
         });

         if(target[0] != null) {
            this.clickBlock(target[0]);
            this.autoTickCooldown = this.autoTickTimeout.getInt();
         }
      }
   }

   private boolean needsTick(IBlockState have, IBlockState want) {
      Block b = want.getBlock();
      return b instanceof BlockRedstoneRepeater && have.getBlock() instanceof BlockRedstoneRepeater?this.differs(have, want, "delay"):(b instanceof BlockRedstoneComparator && have.getBlock() instanceof BlockRedstoneComparator?this.differs(have, want, "mode"):(this.aTickTrapdoors.get() && b instanceof BlockTrapDoor && have.getBlock() instanceof BlockTrapDoor?this.differs(have, want, "open"):false));
   }

   private boolean differs(IBlockState have, IBlockState want, String propName) {
      Comparable<?> h = this.prop(have, propName);
      Comparable<?> w = this.prop(want, propName);
      return h != null && w != null && !h.equals(w);
   }

   private Comparable<?> prop(IBlockState state, String propName) {
      UnmodifiableIterator var3 = state.getProperties().entrySet().iterator();

      while(var3.hasNext()) {
         Entry<?, ?> e = (Entry)var3.next();
         if(((IProperty)e.getKey()).getName().equals(propName)) {
            return (Comparable)e.getValue();
         }
      }

      return null;
   }

   /**
    * Right-clicks a block to advance its state (repeater delay, comparator mode).
    *
    * <p>Critically this refuses to click while holding a placeable block. A
    * right-click with a block in hand does not tick anything -- it places that
    * block against the face we clicked, which is where the printer's reputation
    * for "randomly placing blocks everywhere" came from. Auto Tick defaults on,
    * so this fired constantly. If no empty or non-block slot is available we
    * skip the tick; a missed repeater delay is far cheaper than a stray block
    * inside a cannon.
    */
   private void clickBlock(BlockPos p) {
      int safe = this.emptyHandSlot();
      if(safe < 0) {
         return;
      }

      if(this.mc.thePlayer.inventory.currentItem != safe) {
         this.mc.thePlayer.inventory.currentItem = safe;
         this.mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(safe));
      }

      // Hit vector is world-space in 1.8.9; the centre of the block's top face.
      Vec3 hit = new Vec3((double)p.getX() + 0.5D, (double)p.getY() + 1.0D, (double)p.getZ() + 0.5D);
      this.mc.playerController.onPlayerRightClick(this.mc.thePlayer, this.mc.theWorld,
            this.mc.thePlayer.getHeldItem(), p, EnumFacing.UP, hit);
      this.mc.thePlayer.swingItem();
   }

   /** A hotbar slot that will not place anything when right-clicked, or -1. */
   private int emptyHandSlot() {
      int current = this.mc.thePlayer.inventory.currentItem;
      if(isTickSafe(this.mc.thePlayer.inventory.getStackInSlot(current))) {
         return current;
      }

      for(int i = 0; i < 9; ++i) {
         if(isTickSafe(this.mc.thePlayer.inventory.getStackInSlot(i))) {
            return i;
         }
      }

      return -1;
   }

   private static boolean isTickSafe(ItemStack s) {
      return s == null || !(s.getItem() instanceof ItemBlock);
   }
}
