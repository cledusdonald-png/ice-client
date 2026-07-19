package com.iceclient.module;

import com.iceclient.util.BindUtil;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.ModeSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public abstract class Module {
   protected final Minecraft mc;
   private final String name;
   private final String description;
   private ModuleCategory category;
   private String groupName;
   private int keyCode;
   private boolean enabled;
   private final List<Setting> settings;

   protected Module(String name, String description, ModuleCategory category, int defaultKey) {
      this.mc = Minecraft.getMinecraft();
      this.settings = new ArrayList();
      this.name = name;
      this.description = description;
      this.category = category;
      this.keyCode = defaultKey;
      this.enabled = false;
   }

   protected Module(String name, String description, ModuleCategory category) {
      this(name, description, category, 0);
   }

   protected BooleanSetting addBool(String name, boolean def) {
      return (BooleanSetting)this.addSetting(new BooleanSetting(name, def));
   }

   protected NumberSetting addNumber(String name, double def, double min, double max, double step) {
      return (NumberSetting)this.addSetting(new NumberSetting(name, def, min, max, step));
   }

   protected ModeSetting addMode(String name, String def, String... modes) {
      return (ModeSetting)this.addSetting(new ModeSetting(name, def, modes));
   }

   protected ColorSetting addColor(String name, int def) {
      return (ColorSetting)this.addSetting(new ColorSetting(name, def));
   }

   protected KeybindSetting addKeybind(String name, int def) {
      return (KeybindSetting)this.addSetting(new KeybindSetting(name, def));
   }

   public final void toggle() {
      this.setEnabled(!this.enabled);
   }

   public final void setEnabled(boolean state) {
      if(state != this.enabled) {
         this.enabled = state;
         if(this.enabled) {
            this.onEnable();
         } else {
            this.onDisable();
         }

      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public ModuleCategory getCategory() {
      return this.category;
   }

   public void setCategory(ModuleCategory category) {
      this.category = category;
   }

   public String getGroupName() {
      return this.groupName;
   }

   public void setGroupName(String groupName) {
      this.groupName = groupName;
   }

   public int getKeyCode() {
      return this.keyCode;
   }

   public void setKeyCode(int keyCode) {
      this.keyCode = keyCode;
   }

   public String getKeyName() {
      // Must go through BindUtil: module binds can now hold negative mouse
      // codes, and Keyboard.getKeyName() indexes an array with them, throwing
      // ArrayIndexOutOfBoundsException and taking the whole GUI render down.
      return BindUtil.getName(this.keyCode);
   }

   protected <T extends Setting> T addSetting(T setting) {
      this.settings.add(setting);
      return (T)setting;
   }

   public List<Setting> getSettings() {
      return Collections.unmodifiableList(this.settings);
   }

   public boolean hasSettings() {
      return !this.settings.isEmpty();
   }

   protected void onEnable() {
   }

   protected void onDisable() {
   }
}
