package com.unclesam.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.unclesam.client.module.HudModule;
import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleManager;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.KeybindSetting;
import com.unclesam.client.setting.ModeSetting;
import com.unclesam.client.setting.NumberSetting;
import com.unclesam.client.setting.Setting;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;

public final class ConfigManager {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();

   private ConfigManager() {
   }

   private static File configFile() {
      File dir = new File(Minecraft.getMinecraft().mcDataDir, "config");
      if(!dir.exists()) {
         dir.mkdirs();
      }

      return new File(dir, "unclesamclient.json");
   }

   public static void save() {
      try {
         JsonObject modules = new JsonObject();

         for(Module m : ModuleManager.getModules()) {
            JsonObject mo = new JsonObject();
            mo.addProperty("enabled", Boolean.valueOf(m.isEnabled()));
            mo.addProperty("key", Integer.valueOf(m.getKeyCode()));
            if(m instanceof HudModule) {
               HudModule h = (HudModule)m;
               if(h.hasPos()) {
                  mo.addProperty("hudX", Integer.valueOf(h.getPosX()));
                  mo.addProperty("hudY", Integer.valueOf(h.getPosY()));
               }

               mo.addProperty("hudScale", Float.valueOf(h.getScale()));
               mo.addProperty("hudColor", Integer.valueOf(h.getColor()));
            }

            JsonObject settings = new JsonObject();

            for(Setting s : m.getSettings()) {
               if(s instanceof BooleanSetting) {
                  settings.addProperty(s.getName(), Boolean.valueOf(((BooleanSetting)s).get()));
               } else if(s instanceof NumberSetting) {
                  settings.addProperty(s.getName(), Double.valueOf(((NumberSetting)s).get()));
               } else if(s instanceof ModeSetting) {
                  settings.addProperty(s.getName(), ((ModeSetting)s).get());
               } else if(s instanceof KeybindSetting) {
                  settings.addProperty(s.getName(), Integer.valueOf(((KeybindSetting)s).get()));
               } else {
                  JsonElement custom = s.save();
                  if(custom != null) {
                     settings.add(s.getName(), custom);
                  }
               }
            }

            mo.add("settings", settings);
            modules.add(m.getName(), mo);
         }

         JsonObject root = new JsonObject();
         root.add("modules", modules);
         Writer w = new OutputStreamWriter(new FileOutputStream(configFile()), StandardCharsets.UTF_8);
         Throwable var21 = null;

         try {
            GSON.toJson(root, w);
         } catch (Throwable var16) {
            var21 = var16;
            throw var16;
         } finally {
            if(w != null) {
               if(var21 != null) {
                  try {
                     w.close();
                  } catch (Throwable var15) {
                     var21.addSuppressed(var15);
                  }
               } else {
                  w.close();
               }
            }

         }
      } catch (Throwable var18) {
         ;
      }

   }

   public static void load() {
      File file = configFile();
      if(file.exists()) {
         try {
            Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            Throwable var2 = null;

            try {
               JsonElement parsed = (new JsonParser()).parse(r);
               if(parsed.isJsonObject()) {
                  JsonObject root = parsed.getAsJsonObject();
                  if(root.has("modules") && root.get("modules").isJsonObject()) {
                     JsonObject modules = root.getAsJsonObject("modules");

                     for(Module m : ModuleManager.getModules()) {
                        if(modules.has(m.getName()) && modules.get(m.getName()).isJsonObject()) {
                           JsonObject mo = modules.getAsJsonObject(m.getName());
                           if(mo.has("key")) {
                              try {
                                 m.setKeyCode(mo.get("key").getAsInt());
                              } catch (Exception var31) {
                                 ;
                              }
                           }

                           if(m instanceof HudModule) {
                              HudModule h = (HudModule)m;

                              try {
                                 if(mo.has("hudX") && mo.has("hudY")) {
                                    h.setPos(mo.get("hudX").getAsInt(), mo.get("hudY").getAsInt());
                                 }

                                 if(mo.has("hudScale")) {
                                    h.setScale(mo.get("hudScale").getAsFloat());
                                 }

                                 if(mo.has("hudColor")) {
                                    h.setColor(mo.get("hudColor").getAsInt());
                                 }
                              } catch (Exception var30) {
                                 ;
                              }
                           }

                           if(mo.has("settings") && mo.get("settings").isJsonObject()) {
                              JsonObject settings = mo.getAsJsonObject("settings");

                              for(Setting s : m.getSettings()) {
                                 if(settings.has(s.getName())) {
                                    JsonElement v = settings.get(s.getName());

                                    try {
                                       if(s instanceof BooleanSetting) {
                                          ((BooleanSetting)s).set(v.getAsBoolean());
                                       } else if(s instanceof NumberSetting) {
                                          ((NumberSetting)s).set(v.getAsDouble());
                                       } else if(s instanceof ModeSetting) {
                                          ((ModeSetting)s).setByName(v.getAsString());
                                       } else if(s instanceof KeybindSetting) {
                                          ((KeybindSetting)s).set(v.getAsInt());
                                       } else {
                                          s.load(v);
                                       }
                                    } catch (Exception var29) {
                                       ;
                                    }
                                 }
                              }
                           }

                           if(mo.has("enabled")) {
                              try {
                                 m.setEnabled(mo.get("enabled").getAsBoolean());
                              } catch (Throwable var28) {
                                 ;
                              }
                           }
                        }
                     }

                     return;
                  }

                  return;
               }
            } catch (Throwable var32) {
               var2 = var32;
               throw var32;
            } finally {
               if(r != null) {
                  if(var2 != null) {
                     try {
                        r.close();
                     } catch (Throwable var27) {
                        var2.addSuppressed(var27);
                     }
                  } else {
                     r.close();
                  }
               }

            }

         } catch (Throwable var34) {
            ;
         }
      }
   }
}
