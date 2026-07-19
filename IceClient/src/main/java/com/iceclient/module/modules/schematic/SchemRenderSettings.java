package com.iceclient.module.modules.schematic;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.schematic.SchematicRenderer;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.NumberSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class SchemRenderSettings extends Module {
   private final BooleanSetting renderInWater = this.addBool("Render In Water", true);
   private final BooleanSetting doBadBlock = this.addBool("Bad Block", true);
   private final BooleanSetting doAirBlock = this.addBool("Air Block", false);
   private final BooleanSetting doMissingBlock = this.addBool("Missing Block", true);
   private final BooleanSetting doFacingBlock = this.addBool("Facing Block", true);
   private final ColorSetting badBlockColor = this.addColor("Bad Block Color", -43691);
   private final ColorSetting airColor = this.addColor("Air Color", 1442840575);
   private final ColorSetting missingBlockColor = this.addColor("Missing Block Color", -11141291);
   private final ColorSetting invalidStateColor = this.addColor("Invalid State Color", -22016);
   private final NumberSetting renderDist = this.addNumber("Render Distance", 64.0D, 16.0D, 128.0D, 4.0D);

   public SchemRenderSettings() {
      super("Schem Render", "Schematic ghost overlay appearance", ModuleCategory.FACTIONS);
      this.renderInWater.inSection("RENDER");
      this.doBadBlock.inSection("BLOCKS");
      this.doAirBlock.inSection("BLOCKS");
      this.doMissingBlock.inSection("BLOCKS");
      this.doFacingBlock.inSection("BLOCKS");
      this.badBlockColor.inSection("COLORS");
      this.airColor.inSection("COLORS");
      this.missingBlockColor.inSection("COLORS");
      this.invalidStateColor.inSection("COLORS");
      this.renderDist.inSection("RENDER");
   }

   protected void onEnable() {
      this.apply();
   }

   protected void onDisable() {
      SchematicRenderer.resetColors();
   }

   public void apply() {
      SchematicRenderer.setOrbitSettings(this.renderInWater.get(), this.doBadBlock.get(), this.doAirBlock.get(), this.doMissingBlock.get(), this.doFacingBlock.get(), this.badBlockColor.getRGB(), this.airColor.getRGB(), this.missingBlockColor.getRGB(), this.invalidStateColor.getRGB(), this.renderDist.getInt());
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if(this.isEnabled() && event.phase == Phase.END) {
         this.apply();
      }
   }
}
