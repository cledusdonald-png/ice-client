package com.unclesam.client.module.modules.schematic;

import com.unclesam.client.gui.ClickGuiScreen;
import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Suppresses Schematica's own screens (the "Red point / Blue point / Save the
 * selection as a schematic" dialog and friends) so only Ice's schematic UI is
 * reachable.
 *
 * <p>Intercepted at {@link GuiOpenEvent} rather than by unbinding Schematica's
 * keys: the screens can also be opened from Schematica's own controls and
 * commands, and cancelling the open catches every route in one place. Schematica
 * itself is left running -- it still does the rendering and printing that Ice
 * drives through {@code SchematicaBridge}.
 */
public class HideSchematicaGui extends Module {

   private static final String SCHEMATICA_GUI_PACKAGE = "com.github.lunatrius.schematica.client.gui";

   private final BooleanSetting openIceInstead = (BooleanSetting)this.addSetting(new BooleanSetting("Open Ice menu instead", true));

   public HideSchematicaGui() {
      super("Hide Schematica GUI", "Blocks Schematica's own screens in favour of Ice's", ModuleCategory.PRINTER);
   }

   @SubscribeEvent
   public void onGuiOpen(GuiOpenEvent event) {
      if(!this.isEnabled() || event.gui == null) {
         return;
      }

      String name = event.gui.getClass().getName();
      if(!name.startsWith(SCHEMATICA_GUI_PACKAGE)) {
         return;
      }

      if(this.openIceInstead.get()) {
         // Swap in Ice's menu rather than swallowing the keypress entirely --
         // otherwise the key just does nothing and feels broken.
         event.gui = new ClickGuiScreen();
      } else {
         event.setCanceled(true);
      }

   }
}
