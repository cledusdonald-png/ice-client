package com.iceclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iceclient.cosmetic.CosmeticManager;
import com.iceclient.cosmetic.CosmeticType;
import com.iceclient.module.HudModule;
import com.iceclient.module.Module;
import com.iceclient.module.ModuleManager;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.ModeSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.setting.Setting;
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

   private static File configDir() {
      File dir = new File(Minecraft.getMinecraft().mcDataDir, "config");
      if(!dir.exists()) {
         dir.mkdirs();
      }

      return dir;
   }

   private static File configFile() {
      return new File(configDir(), "iceclient.json");
   }

   /**
    * The pre-rename config file.
    *
    * <p>Kept only so {@link #load()} can migrate it once. Without this the
    * package rename would silently reset every module toggle, keybind and HUD
    * position -- the file would still be sitting there, just under a name
    * nothing reads any more.
    */
   private static File legacyConfigFile() {
      return new File(configDir(), "unclesamclient.json");
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
         root.add("cosmetics", saveCosmetics());
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

   /**
    * Owned cosmetics, what is worn, and the balance.
    *
    * <p>Stored beside the modules rather than in their own file so there is one
    * config to back up or delete. Unknown ids are dropped on load, so removing a
    * cosmetic from the registry cannot leave a config that fails to parse.
    */
   private static JsonObject saveCosmetics() {
      JsonObject c = new JsonObject();
      c.addProperty("balance", Integer.valueOf(CosmeticManager.getBalance()));
      c.addProperty("hideVanillaCape", Boolean.valueOf(CosmeticManager.isVanillaCapeHidden()));
      c.addProperty("petOnShoulder", Boolean.valueOf(CosmeticManager.isPetOnShoulder()));

      com.google.gson.JsonArray owned = new com.google.gson.JsonArray();
      for(String id : CosmeticManager.getOwned()) {
         owned.add(new com.google.gson.JsonPrimitive(id));
      }
      c.add("owned", owned);

      JsonObject worn = new JsonObject();
      for(CosmeticType t : CosmeticType.values()) {
         String id = CosmeticManager.getEquipped(t);
         if(id != null) {
            worn.addProperty(t.name(), id);
         }
      }
      c.add("equipped", worn);

      return c;
   }

   private static void loadCosmetics(JsonObject root) {
      if(root == null || !root.has("cosmetics")) {
         return;
      }

      try {
         JsonObject c = root.getAsJsonObject("cosmetics");

         if(c.has("balance")) {
            CosmeticManager.setBalance(c.get("balance").getAsInt());
         }

         if(c.has("hideVanillaCape")) {
            CosmeticManager.setVanillaCapeHidden(c.get("hideVanillaCape").getAsBoolean());
         }

         if(c.has("petOnShoulder")) {
            CosmeticManager.setPetOnShoulder(c.get("petOnShoulder").getAsBoolean());
         }

         if(c.has("owned")) {
            java.util.Set<String> ids = new java.util.HashSet<String>();
            for(com.google.gson.JsonElement e : c.getAsJsonArray("owned")) {
               ids.add(e.getAsString());
            }
            CosmeticManager.setOwned(ids);
         }

         if(c.has("equipped")) {
            JsonObject worn = c.getAsJsonObject("equipped");
            for(CosmeticType t : CosmeticType.values()) {
               if(worn.has(t.name())) {
                  CosmeticManager.setEquipped(t, worn.get(t.name()).getAsString());
               }
            }
         }
      } catch (Exception e) {
         // A malformed cosmetics block must not cost someone their module config.
      }
   }

   public static void load() {
      File file = configFile();

      // One-time migration off the pre-rename filename. Read-only: the old file
      // is left in place rather than deleted, so downgrading to a build from
      // before the rename still finds its settings.
      if(!file.exists() && legacyConfigFile().exists()) {
         file = legacyConfigFile();
      }

      if(file.exists()) {
         try {
            Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            Throwable var2 = null;

            try {
               JsonElement parsed = (new JsonParser()).parse(r);
               if(parsed.isJsonObject()) {
                  JsonObject root = parsed.getAsJsonObject();
                  loadCosmetics(root);
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
