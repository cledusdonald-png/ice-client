package com.iceclient.module.modules.schematic;

import com.google.common.collect.UnmodifiableIterator;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.schematica.PrintEngine;
import com.iceclient.schematica.SchematicaBridge;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.ModeSetting;
import com.iceclient.setting.NumberSetting;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class Printer extends Module {
   /**
    * V1 hands everything to Schematica's own printer -- proven, but a closed
    * box: it picks its own ordering, sends one placement a tick and cannot be
    * bounded. V2 runs {@link PrintEngine}, which is what every setting below
    * the packet limit actually needs.
    */
   private final ModeSetting version = this.addMode("Version", "V2", "V1", "V2");
   private final BooleanSetting limitPackets = this.addBool("Limit Packets", false);
   private final NumberSetting packetLimit = this.addNumber("Limit Packet Count", 64.0D, 1.0D, 512.0D, 1.0D);
   private final NumberSetting placeDistance = this.addNumber("Place Distance", 5.0D, 1.0D, 9.0D, 1.0D);
   private final BooleanSetting placeInstantly = this.addBool("Place Instantly", true);
   private final NumberSetting placeDelay = this.addNumber("Place Delay", 0.0D, 0.0D, 20.0D, 1.0D);
   private final NumberSetting timeout = this.addNumber("Timeout", 2.0D, 0.0D, 20.0D, 1.0D);
   private final BooleanSetting placeAdjacent = this.addBool("Place Adjacent", true);
   private final BooleanSetting replaceWrong = this.addBool("Replace Wrong Blocks", false);
   private final BooleanSetting breakInstantly = this.addBool("Break Blocks Instantly", false);
   private final BooleanSetting disableGens = this.addBool("Disable Gens", false);
   private final BooleanSetting keepSlot = this.addBool("Keep Selected Slot", false);
   /**
    * On a creative build server, pull each needed block straight from the
    * creative inventory instead of requiring it in the hotbar. No-ops in
    * survival, so it is safe to leave on.
    */
   private final BooleanSetting creativeGrab = this.addBool("Creative Grab", true);
   /**
    * Rotate to place directional blocks (stairs, pistons, repeaters) in the
    * right orientation. Off means everything places in its default facing --
    * fine for a plain obsidian wall, wrong for a redstone cannon.
    */
   private final BooleanSetting faceBlocks = this.addBool("Orient Directional Blocks", true);
   /**
    * Break and re-place a same-type block that is facing the wrong way -- the
    * dispenser case. Without it a wrongly-oriented block reads as "already
    * built" and is left pointing the wrong direction.
    */
   private final BooleanSetting fixOrientation = this.addBool("Fix Wrong Orientation", true);
   /**
    * Attempt to place blocks with no solid neighbour to click. Only works where
    * the server allows floating placement; harmless otherwise.
    */
   private final BooleanSetting midAir = this.addBool("Place In Mid Air", false);
   private final BooleanSetting clearExtra = this.addBool("Clear Extra Blocks", false);
   private final BooleanSetting clearInstantly = this.addBool("Clear Instantly", false);
   private final BooleanSetting autoTick = this.addBool("Auto Tick", true);
   private final BooleanSetting aTickTrapdoors = this.addBool("Tick Trapdoors", true);
   private final NumberSetting autoTickTimeout = this.addNumber("Auto Tick Timeout", 2.0D, 0.0D, 20.0D, 1.0D);
   private final KeybindSetting autoTickKey = this.addKeybind("Auto Tick Key", 0);
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
   private final PrintEngine engine = new PrintEngine();

   /** Snapshots the settings the engine needs into its plain config object. */
   private PrintEngine.Config buildConfig() {
      PrintEngine.Config c = new PrintEngine.Config();
      c.placeDistance = this.placeDistance.get();
      c.placeInstantly = this.placeInstantly.get();
      // With the limit off, "instantly" still needs a ceiling or one tick can
      // try to place the entire queue and disconnect you for packet spam.
      c.packetLimit = this.limitPackets.get() ? (int)this.packetLimit.get() : 64;
      c.delay = this.placeDelay.getInt();
      c.placeAdjacent = this.placeAdjacent.get();
      c.replaceWrong = this.replaceWrong.get();
      c.breakInstantly = this.breakInstantly.get();
      c.disableGens = this.disableGens.get();
      c.keepSlot = this.keepSlot.get();
      c.creativeGrab = this.creativeGrab.get();
      c.faceBlocks = this.faceBlocks.get();
      c.fixOrientation = this.fixOrientation.get();
      c.midAir = this.midAir.get();
      c.slots = new boolean[]{this.slot1.get(), this.slot2.get(), this.slot3.get(),
            this.slot4.get(), this.slot5.get(), this.slot6.get(),
            this.slot7.get(), this.slot8.get(), this.slot9.get()};
      return c;
   }

   public Printer() {
      super("Printer", "Auto-places the loaded schematic", ModuleCategory.FACTIONS);
      this.version.inSection("GENERAL");
      this.limitPackets.inSection("GENERAL");
      this.packetLimit.inSection("GENERAL");
      this.replaceWrong.inSection("CLEAR");
      this.breakInstantly.inSection("CLEAR");
      this.disableGens.inSection("GENERAL");
      this.keepSlot.inSection("GENERAL");
      this.creativeGrab.inSection("GENERAL");
      this.faceBlocks.inSection("GENERAL");
      this.fixOrientation.inSection("CLEAR");
      this.midAir.inSection("GENERAL");
      this.placeDistance.inSection("GENERAL");
      this.placeInstantly.inSection("GENERAL");
      this.placeDelay.inSection("GENERAL");
      this.timeout.inSection("GENERAL");
      this.placeAdjacent.inSection("GENERAL");
      this.clearExtra.inSection("CLEAR");
      this.clearInstantly.inSection("CLEAR");
      this.autoTick.inSection("AUTO TICK");
      this.aTickTrapdoors.inSection("AUTO TICK");
      this.autoTickTimeout.inSection("AUTO TICK");
      this.autoTickKey.inSection("AUTO TICK");
      this.slot1.inSection("HOTBAR");
      this.slot2.inSection("HOTBAR");
      this.slot3.inSection("HOTBAR");
      this.slot4.inSection("HOTBAR");
      this.slot5.inSection("HOTBAR");
      this.slot6.inSection("HOTBAR");
      this.slot7.inSection("HOTBAR");
      this.slot8.inSection("HOTBAR");
      this.slot9.inSection("HOTBAR");
      BooleanSetting var10000 = this.clearInstantly;
      BooleanSetting var10001 = this.clearExtra;
      this.clearExtra.getClass();
      var10000.visibleWhen(var10001::get);
      var10000 = this.aTickTrapdoors;
      var10001 = this.autoTick;
      this.autoTick.getClass();
      var10000.visibleWhen(var10001::get);
      NumberSetting var2 = this.autoTickTimeout;
      var10001 = this.autoTick;
      this.autoTick.getClass();
      var2.visibleWhen(var10001::get);

      // The packet count only means anything with the limit on, and the V2-only
      // options are hidden under V1 rather than shown doing nothing.
      this.packetLimit.visibleWhen(this.limitPackets::get);
      this.breakInstantly.visibleWhen(this.replaceWrong::get);
      this.replaceWrong.visibleWhen(this::isV2);
      this.disableGens.visibleWhen(this::isV2);
      this.keepSlot.visibleWhen(this::isV2);
      this.creativeGrab.visibleWhen(this::isV2);
      this.faceBlocks.visibleWhen(this::isV2);
      this.fixOrientation.visibleWhen(this::isV2);
      this.midAir.visibleWhen(this::isV2);
      this.limitPackets.visibleWhen(this::isV2);
   }

   private boolean isV2() {
      return this.version.is("V2");
   }

   protected void onEnable() {
      if(SchematicaBridge.isAvailable()) {
         SchematicaBridge.setPrinterEnabled(true);
         this.pushFullConfig();
         SchematicaBridge.setPrinting(true);
      }
   }

   protected void onDisable() {
      if(SchematicaBridge.isAvailable()) {
         SchematicaBridge.setPrinting(false);
      }

      // Drop the queue: it is positions relative to a schematic that may be
      // unloaded or moved before this is switched back on.
      this.engine.reset();
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.END) {
         if(this.mc.thePlayer != null && this.mc.theWorld != null && this.mc.playerController != null) {
            if(SchematicaBridge.isAvailable() && SchematicaBridge.hasSchematic()) {
               if(this.isV2()) {
                  // Schematica's printer must be off, or both loops place into
                  // the same holes and every block gets a duplicate packet.
                  if(SchematicaBridge.isPrinting()) {
                     SchematicaBridge.setPrinting(false);
                  }

                  if(this.mc.currentScreen == null) {
                     this.engine.tick(this.buildConfig());
                  }
               } else {
                  SchematicaBridge.applyPrinterScalars(this.placeDistance.getInt(), this.placeInstantly.get(), this.placeDelay.getInt(), this.timeout.getInt(), this.placeAdjacent.get(), this.clearExtra.get(), this.clearInstantly.get());
                  SchematicaBridge.setPrinterEnabled(true);
                  if(this.mc.currentScreen == null && !SchematicaBridge.isPrinting()) {
                     SchematicaBridge.setPrinting(true);
                  }
               }

               if(this.autoTick.get() && this.mc.currentScreen == null) {
                  this.runAutoTick();
               }

            }
         }
      }
   }

   private void pushFullConfig() {
      SchematicaBridge.applyPrinterConfig(this.placeDistance.getInt(), this.placeInstantly.get(), this.placeDelay.getInt(), this.timeout.getInt(), this.placeAdjacent.get(), this.clearExtra.get(), this.clearInstantly.get(), new boolean[]{this.slot1.get(), this.slot2.get(), this.slot3.get(), this.slot4.get(), this.slot5.get(), this.slot6.get(), this.slot7.get(), this.slot8.get(), this.slot9.get()});
   }

   private void runAutoTick() {
      if(this.autoTickCooldown-- <= 0) {
         BlockPos[] target = new BlockPos[]{null};
         SchematicaBridge.forEachSchematicBlock(this.placeDistance.get(), (world, want) -> {
            if(target[0] == null) {
               if(this.mc.theWorld.isBlockLoaded(world, false)) {
                  IBlockState have = this.mc.theWorld.getBlockState(world);
                  if(this.needsTick(have, want)) {
                     target[0] = world;
                  }

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

   private void clickBlock(BlockPos p) {
      this.mc.playerController.onPlayerRightClick(this.mc.thePlayer, this.mc.theWorld, this.mc.thePlayer.getHeldItem(), p, EnumFacing.UP, new Vec3(0.5D, 0.5D, 0.5D));
   }
}
