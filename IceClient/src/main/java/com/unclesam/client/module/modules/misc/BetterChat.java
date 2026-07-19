package com.unclesam.client.module.modules.misc;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ModeSetting;
import com.unclesam.client.setting.NumberSetting;
import net.minecraft.util.EnumChatFormatting;

/**
 * Settings for the chat improvements applied in {@code MixinGuiNewChat}.
 *
 * <p>Sizes are stored in pixels rather than as vanilla's 0-1 slider fractions,
 * since the point of the feature is to go past the range those fractions map to.
 */
public class BetterChat extends Module {

   private final NumberSetting lines =
         this.addNumber("Scrollback lines", 500.0D, 100.0D, 2000.0D, 50.0D);
   private final BooleanSetting stamps =
         this.addBool("Timestamps", true);
   private final ModeSetting stampFormat =
         this.addMode("Timestamp format", "HH:mm", "HH:mm", "HH:mm:ss", "hh:mm a");
   private final ModeSetting stampColor =
         this.addMode("Timestamp colour", "Gray", "Gray", "Dark gray", "Aqua", "Yellow");
   private final BooleanSetting resize =
         this.addBool("Custom chat size", false);
   private final NumberSetting width =
         this.addNumber("Chat width", 320.0D, 100.0D, 640.0D, 10.0D);
   private final NumberSetting heightFocused =
         this.addNumber("Height (open)", 180.0D, 60.0D, 400.0D, 10.0D);
   private final NumberSetting heightUnfocused =
         this.addNumber("Height (closed)", 90.0D, 20.0D, 300.0D, 10.0D);

   public BetterChat() {
      super("BetterChat", "Longer scrollback, timestamps, resizable chat box", ModuleCategory.GENERAL);
   }

   public int scrollback() {
      return (int)this.lines.get();
   }

   public boolean timestamps() {
      return this.stamps.get();
   }

   public String timestampFormat() {
      return this.stampFormat.get();
   }

   public String timestampColor() {
      String mode = this.stampColor.get();
      if("Dark gray".equals(mode)) {
         return EnumChatFormatting.DARK_GRAY.toString();
      } else if("Aqua".equals(mode)) {
         return EnumChatFormatting.AQUA.toString();
      } else if("Yellow".equals(mode)) {
         return EnumChatFormatting.YELLOW.toString();
      } else {
         return EnumChatFormatting.GRAY.toString();
      }
   }

   public boolean customSize() {
      return this.resize.get();
   }

   public int chatWidth() {
      return (int)this.width.get();
   }

   public int chatHeightFocused() {
      return (int)this.heightFocused.get();
   }

   public int chatHeightUnfocused() {
      return (int)this.heightUnfocused.get();
   }
}
