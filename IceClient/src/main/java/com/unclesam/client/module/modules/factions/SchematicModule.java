package com.unclesam.client.module.modules.factions;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.schematica.SchematicaBridge;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.KeybindSetting;
import com.unclesam.client.setting.NumberSetting;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class SchematicModule extends Module {
   private static SchematicModule INSTANCE;
   private final NumberSetting renderDistance = (NumberSetting)this.addSetting(new NumberSetting("Render dist", 48.0D, 8.0D, 128.0D, 1.0D));
   private final BooleanSetting showBlocks = (BooleanSetting)this.addSetting(new BooleanSetting("Show blocks", true));
   private final BooleanSetting showOutline = (BooleanSetting)this.addSetting(new BooleanSetting("Show outline", true));
   private final KeybindSetting cycleKey = (KeybindSetting)this.addSetting(new KeybindSetting("Cycle schem", 0));
   private final KeybindSetting switch1 = (KeybindSetting)this.addSetting(new KeybindSetting("Switch schem 1", 0));
   private final KeybindSetting switch2 = (KeybindSetting)this.addSetting(new KeybindSetting("Switch schem 2", 0));
   private final KeybindSetting switch3 = (KeybindSetting)this.addSetting(new KeybindSetting("Switch schem 3", 0));
   private final KeybindSetting switch4 = (KeybindSetting)this.addSetting(new KeybindSetting("Switch schem 4", 0));
   private final KeybindSetting switch5 = (KeybindSetting)this.addSetting(new KeybindSetting("Switch schem 5", 0));
   private final List<SchematicModule.Loaded> loaded = new ArrayList();
   private int active = -1;

   public SchematicModule() {
      super("Schematica", "Ghost overlay for loaded .schematics -- a build guide, not an auto-placer", ModuleCategory.FACTIONS, 0);
      INSTANCE = this;
      this.getSchematicsDir();
   }

   public static SchematicModule getInstance() {
      return INSTANCE;
   }

   private SchematicModule.Loaded active() {
      return this.active >= 0 && this.active < this.loaded.size()?(SchematicModule.Loaded)this.loaded.get(this.active):null;
   }

   private File getSchematicsDir() {
      File dir = new File(this.mc.mcDataDir, "schematics");
      if(!dir.exists()) {
         dir.mkdirs();
      }

      return dir;
   }

   public void openFolder() {
      File dir = this.getSchematicsDir();
      this.sendMessage("Schematics folder: " + dir.getAbsolutePath());

      try {
         Desktop.getDesktop().open(dir);
      } catch (Throwable var3) {
         this.sendMessage("(Couldn\'t auto-open it -- browse to the path above manually.)");
      }

   }

   protected void onEnable() {
      if(SchematicaBridge.isAvailable()) {
         SchematicaBridge.setRendering(true);
      }

   }

   protected void onDisable() {
      if(SchematicaBridge.isAvailable()) {
         SchematicaBridge.setRendering(false);
      }

   }

   public String getFolderPath() {
      return this.getSchematicsDir().getAbsolutePath();
   }

   public String getActiveName() {
      SchematicModule.Loaded l = this.active();
      return l == null?null:l.name;
   }

   public BlockPos getActiveOrigin() {
      SchematicModule.Loaded l = this.active();
      return l == null?null:l.origin;
   }

   public void nudgeOrigin(int dx, int dy, int dz) {
      SchematicModule.Loaded l = this.active();
      if(l == null) {
         this.sendMessage("Load a schematic first.");
      } else {
         if(l.origin == null) {
            if(this.mc.thePlayer == null) {
               return;
            }

            l.origin = new BlockPos(this.mc.thePlayer.posX, this.mc.thePlayer.posY, this.mc.thePlayer.posZ);
         }

         l.origin = l.origin.add(dx, dy, dz);
      }
   }

   public List<String> listFiles() {
      List<String> names = new ArrayList();
      File[] files = this.getSchematicsDir().listFiles((dir, n) -> {
         return n.toLowerCase().endsWith(".schematic");
      });
      if(files != null) {
         Arrays.sort(files, (a, b) -> {
            return a.getName().compareToIgnoreCase(b.getName());
         });

         for(File f : files) {
            names.add(f.getName());
         }
      }

      return names;
   }

   public void createTestSchematic() {
      try {
         short w = 5;
         short h = 4;
         short l = 5;
         byte[] blocks = new byte[w * h * l];

         for(int y = 0; y < h; ++y) {
            for(int z = 0; z < l; ++z) {
               for(int x = 0; x < w; ++x) {
                  boolean shell = x == 0 || x == w - 1 || z == 0 || z == l - 1 || y == 0;
                  blocks[(y * l + z) * w + x] = (byte)(shell?1:0);
               }
            }
         }

         NBTTagCompound nbt = new NBTTagCompound();
         nbt.setShort("Width", w);
         nbt.setShort("Height", h);
         nbt.setShort("Length", l);
         nbt.setString("Materials", "Alpha");
         nbt.setByteArray("Blocks", blocks);
         nbt.setByteArray("Data", new byte[w * h * l]);
         nbt.setTag("Entities", new NBTTagList());
         nbt.setTag("TileEntities", new NBTTagList());
         File out = new File(this.getSchematicsDir(), "test_box.schematic");
         CompressedStreamTools.writeCompressed(nbt, new FileOutputStream(out));
         this.sendMessage("Wrote " + out.getName() + " (" + w + "x" + h + "x" + l + " stone box). Click it in the list to load, then Set Origin Here.");
      } catch (Exception var9) {
         this.sendMessage("Failed to write test schematic: " + var9.getMessage());
      }

   }

   public void loadSchematic(String fileName) {
      try {
         String name = fileName.endsWith(".schematic")?fileName:fileName + ".schematic";
         File file = new File(this.getSchematicsDir(), name);
         if(!file.exists()) {
            this.sendMessage("Schematic not found: " + file.getName() + " -- drop it in " + this.getSchematicsDir().getAbsolutePath() + " (run \'.schem folder\' to open it).");
            return;
         }

         NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(file));
         SchematicModule.Loaded l = new SchematicModule.Loaded();
         l.width = nbt.getShort("Width");
         l.height = nbt.getShort("Height");
         l.length = nbt.getShort("Length");
         l.blocks = nbt.getByteArray("Blocks");
         l.data = nbt.getByteArray("Data");
         l.name = file.getName();
         l.file = file;
         int existing = this.indexOfName(l.name);
         if(existing >= 0) {
            l.origin = ((SchematicModule.Loaded)this.loaded.get(existing)).origin;
            this.loaded.set(existing, l);
            this.active = existing;
         } else {
            this.loaded.add(l);
            this.active = this.loaded.size() - 1;
         }

         this.sendMessage("Loaded " + l.name + " (" + l.width + "x" + l.height + "x" + l.length + ") [" + (this.active + 1) + "/" + this.loaded.size() + "]. Run .schem setpos to place it.");
      } catch (Exception var7) {
         this.sendMessage("Failed to load schematic: " + var7.getMessage());
      }

   }

   private int indexOfName(String name) {
      for(int i = 0; i < this.loaded.size(); ++i) {
         if(((SchematicModule.Loaded)this.loaded.get(i)).name.equalsIgnoreCase(name)) {
            return i;
         }
      }

      return -1;
   }

   public void cycle() {
      if(this.loaded.isEmpty()) {
         this.sendMessage("No schematics loaded.");
      } else {
         this.active = (this.active + 1) % this.loaded.size();
         this.announceActive();
      }
   }

   public void switchTo(int oneBasedIndex) {
      int idx = oneBasedIndex - 1;
      if(idx >= 0 && idx < this.loaded.size()) {
         this.active = idx;
         this.announceActive();
      } else {
         this.sendMessage("No schematic in slot " + oneBasedIndex + " (loaded: " + this.loaded.size() + ").");
      }
   }

   private void announceActive() {
      SchematicModule.Loaded l = this.active();
      if(l != null) {
         this.sendMessage("Active schematic: " + l.name + " [" + (this.active + 1) + "/" + this.loaded.size() + "]");
      }

   }

   public void listLoaded() {
      if(this.loaded.isEmpty()) {
         this.sendMessage("No schematics loaded.");
      } else {
         StringBuilder sb = new StringBuilder("Loaded schematics:");

         for(int i = 0; i < this.loaded.size(); ++i) {
            sb.append(i == this.active?" >":" ").append(i + 1).append(":").append(((SchematicModule.Loaded)this.loaded.get(i)).name);
         }

         this.sendMessage(sb.toString());
      }
   }

   public void setOriginToPlayer() {
      if(this.mc.thePlayer != null) {
         SchematicModule.Loaded l = this.active();
         if(l == null) {
            this.sendMessage("Load a schematic first with .schem load <filename>");
         } else {
            l.origin = new BlockPos(this.mc.thePlayer.posX, this.mc.thePlayer.posY, this.mc.thePlayer.posZ);
            this.sendMessage("Origin of " + l.name + " set to " + l.origin.getX() + ", " + l.origin.getY() + ", " + l.origin.getZ());
         }
      }
   }

   public void shareToClipboard() {
      SchematicModule.Loaded l = this.active();
      if(l != null && l.file != null) {
         try {
            byte[] fileBytes = Files.readAllBytes(l.file.toPath());
            String base64 = Base64.getEncoder().encodeToString(fileBytes);
            int ox = l.origin != null?l.origin.getX():0;
            int oy = l.origin != null?l.origin.getY():0;
            int oz = l.origin != null?l.origin.getZ():0;
            String payload = "USC-SCHEM:" + l.name + ":" + ox + "," + oy + "," + oz + ":" + base64;
            GuiScreen.setClipboardString(payload);
            this.sendMessage("Copied " + l.name + " + coords (" + ox + ", " + oy + ", " + oz + ") to your clipboard.");
         } catch (Exception var8) {
            this.sendMessage("Failed to copy to clipboard: " + var8.getMessage());
         }

      } else {
         this.sendMessage("No schematic loaded to share.");
      }
   }

   public void importFromClipboard() {
      try {
         String payload = GuiScreen.getClipboardString();
         if(payload == null || !payload.startsWith("USC-SCHEM:")) {
            this.sendMessage("Clipboard doesn\'t contain a shared schematic.");
            return;
         }

         String[] parts = payload.split(":", 4);
         String name = parts[1];
         String[] coords = parts[2].split(",");
         byte[] fileBytes = Base64.getDecoder().decode(parts[3]);
         File out = new File(this.getSchematicsDir(), name);
         Files.write(out.toPath(), fileBytes, new OpenOption[0]);
         this.loadSchematic(name);
         SchematicModule.Loaded l = this.active();
         if(l != null) {
            l.origin = new BlockPos(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
            this.sendMessage("Imported " + name + ", placed at " + l.origin.getX() + ", " + l.origin.getY() + ", " + l.origin.getZ());
         }
      } catch (Exception var8) {
         this.sendMessage("Failed to import from clipboard: " + var8.getMessage());
      }

   }

   private void sendMessage(String msg) {
      if(this.mc.thePlayer != null) {
         this.mc.thePlayer.addChatMessage(new ChatComponentText("[Ice] " + msg));
      }

   }

   @SubscribeEvent
   public void onKeyInput(KeyInputEvent event) {
      if(this.mc.thePlayer != null && this.mc.currentScreen == null) {
         if(Keyboard.getEventKeyState()) {
            int key = Keyboard.getEventKey();
            if(this.cycleKey.matches(key)) {
               this.cycle();
            } else if(this.switch1.matches(key)) {
               this.switchTo(1);
            } else if(this.switch2.matches(key)) {
               this.switchTo(2);
            } else if(this.switch3.matches(key)) {
               this.switchTo(3);
            } else if(this.switch4.matches(key)) {
               this.switchTo(4);
            } else if(this.switch5.matches(key)) {
               this.switchTo(5);
            }

         }
      }
   }

   public boolean hasSchematic() {
      SchematicModule.Loaded l = this.active();
      return l != null && l.blocks != null && l.origin != null;
   }

   public int getBlockIdAt(BlockPos worldPos) {
      SchematicModule.Loaded l = this.active();
      if(l != null && l.blocks != null && l.origin != null) {
         int index = this.localIndex(l, worldPos);
         return index >= 0 && index < l.blocks.length?l.blocks[index] & 255:0;
      } else {
         return 0;
      }
   }

   public int getBlockMetaAt(BlockPos worldPos) {
      SchematicModule.Loaded l = this.active();
      if(l != null && l.data != null && l.origin != null) {
         int index = this.localIndex(l, worldPos);
         return index >= 0 && index < l.data.length?l.data[index] & 255:0;
      } else {
         return 0;
      }
   }

   private int localIndex(SchematicModule.Loaded l, BlockPos worldPos) {
      int x = worldPos.getX() - l.origin.getX();
      int y = worldPos.getY() - l.origin.getY();
      int z = worldPos.getZ() - l.origin.getZ();
      return x >= 0 && y >= 0 && z >= 0 && x < l.width && y < l.height && z < l.length?(y * l.length + z) * l.width + x:-1;
   }

   public BlockPos findNextPlaceable(EntityPlayer player) {
      return this.findNextPlaceable(player, 4.5D, false, false);
   }

   public BlockPos findNextPlaceable(EntityPlayer player, double reach) {
      return this.findNextPlaceable(player, reach, false, false);
   }

   public BlockPos findNextPlaceable(EntityPlayer player, double reach, boolean allowLiquids, boolean nearest) {
      SchematicModule.Loaded l = this.active();
      if(l != null && l.blocks != null && l.origin != null) {
         double reachSq = reach * reach;
         BlockPos best = null;
         double bestDistSq = Double.MAX_VALUE;
         int[] b = this.reachBounds(l, player, reach);

         for(int y = b[1]; y <= b[4]; ++y) {
            for(int z = b[2]; z <= b[5]; ++z) {
               for(int x = b[0]; x <= b[3]; ++x) {
                  int index = (y * l.length + z) * l.width + x;
                  if(index >= 0 && index < l.blocks.length) {
                     int blockId = l.blocks[index] & 255;
                     if(blockId != 0) {
                        BlockPos worldPos = l.origin.add(x, y, z);
                        if(this.isFillable(worldPos, allowLiquids)) {
                           double distSq = this.eyeDistSq(player, worldPos);
                           if(distSq <= reachSq) {
                              if(!nearest) {
                                 return worldPos;
                              }

                              if(distSq < bestDistSq) {
                                 bestDistSq = distSq;
                                 best = worldPos;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private int[] reachBounds(SchematicModule.Loaded l, EntityPlayer player, double reach) {
      int r = (int)Math.ceil(reach) + 1;
      int px = MathHelper.floor_double(player.posX) - l.origin.getX();
      int py = MathHelper.floor_double(player.posY) - l.origin.getY();
      int pz = MathHelper.floor_double(player.posZ) - l.origin.getZ();
      return new int[]{Math.max(0, px - r), Math.max(0, py - r), Math.max(0, pz - r), Math.min(l.width - 1, px + r), Math.min(l.height - 1, py + r), Math.min(l.length - 1, pz + r)};
   }

   public BlockPos findNextClearable(EntityPlayer player, double reach) {
      SchematicModule.Loaded l = this.active();
      if(l != null && l.blocks != null && l.origin != null) {
         double reachSq = reach * reach;
         int[] b = this.reachBounds(l, player, reach);

         for(int y = b[1]; y <= b[4]; ++y) {
            for(int z = b[2]; z <= b[5]; ++z) {
               for(int x = b[0]; x <= b[3]; ++x) {
                  int index = (y * l.length + z) * l.width + x;
                  if(index >= 0 && index < l.blocks.length) {
                     int wantId = l.blocks[index] & 255;
                     if(wantId != 0) {
                        BlockPos worldPos = l.origin.add(x, y, z);
                        if(!this.mc.theWorld.isAirBlock(worldPos)) {
                           int haveId = Block.getIdFromBlock(this.mc.theWorld.getBlockState(worldPos).getBlock());
                           if(haveId != wantId && this.eyeDistSq(player, worldPos) <= reachSq) {
                              return worldPos;
                           }
                        }
                     }
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private double eyeDistSq(EntityPlayer player, BlockPos pos) {
      double dx = (double)pos.getX() + 0.5D - player.posX;
      double dy = (double)pos.getY() + 0.5D - (player.posY + (double)player.getEyeHeight());
      double dz = (double)pos.getZ() + 0.5D - player.posZ;
      return dx * dx + dy * dy + dz * dz;
   }

   private boolean isFillable(BlockPos pos, boolean allowLiquids) {
      return this.mc.theWorld.isAirBlock(pos)?true:(allowLiquids?this.mc.theWorld.getBlockState(pos).getBlock().getMaterial().isLiquid():false);
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(this.isEnabled() && this.mc.thePlayer != null) {
         if(!SchematicaBridge.isAvailable()) {
            SchematicModule.Loaded l = this.active();
            if(l != null && l.blocks != null && l.origin != null) {
               double camX = this.mc.getRenderManager().viewerPosX;
               double camY = this.mc.getRenderManager().viewerPosY;
               double camZ = this.mc.getRenderManager().viewerPosZ;
               if(this.showBlocks.get()) {
                  this.renderSolidGhost(l, camX, camY, camZ);
                  this.renderGhostGrid(l, camX, camY, camZ);
               }

               if(this.showOutline.get()) {
                  this.setupLineState();
                  GL11.glLineWidth(3.0F);
                  GlStateManager.color(1.0F, 0.85F, 0.1F, 0.9F);
                  this.drawBounds(l, camX, camY, camZ);
                  GL11.glLineWidth(1.5F);
                  this.teardownLineState();
               }

            }
         }
      }
   }

   private void renderSolidGhost(SchematicModule.Loaded l, double camX, double camY, double camZ) {
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.disableCull();
      GlStateManager.depthMask(false);
      GlStateManager.color(0.3F, 0.9F, 1.0F, 0.16F);
      double maxDist = this.renderDistance.get();
      double maxDistSq = maxDist * maxDist;
      double px = this.mc.thePlayer.posX;
      double py = this.mc.thePlayer.posY;
      double pz = this.mc.thePlayer.posZ;
      int[] b = this.reachBounds(l, this.mc.thePlayer, maxDist);
      GL11.glBegin(7);

      for(int y = b[1]; y <= b[4]; ++y) {
         for(int z = b[2]; z <= b[5]; ++z) {
            for(int x = b[0]; x <= b[3]; ++x) {
               if(this.schemSolid(l, x, y, z)) {
                  double wx = (double)(l.origin.getX() + x);
                  double wy = (double)(l.origin.getY() + y);
                  double wz = (double)(l.origin.getZ() + z);
                  double ddx = wx + 0.5D - px;
                  double ddy = wy + 0.5D - py;
                  double ddz = wz + 0.5D - pz;
                  if(ddx * ddx + ddy * ddy + ddz * ddz <= maxDistSq) {
                     double a0 = wx - camX;
                     double a1 = a0 + 1.0D;
                     double c0 = wz - camZ;
                     double c1 = c0 + 1.0D;
                     double d0 = wy - camY;
                     double d1 = d0 + 1.0D;
                     if(!this.schemSolid(l, x, y - 1, z)) {
                        GL11.glVertex3d(a0, d0, c0);
                        GL11.glVertex3d(a1, d0, c0);
                        GL11.glVertex3d(a1, d0, c1);
                        GL11.glVertex3d(a0, d0, c1);
                     }

                     if(!this.schemSolid(l, x, y + 1, z)) {
                        GL11.glVertex3d(a0, d1, c0);
                        GL11.glVertex3d(a0, d1, c1);
                        GL11.glVertex3d(a1, d1, c1);
                        GL11.glVertex3d(a1, d1, c0);
                     }

                     if(!this.schemSolid(l, x, y, z - 1)) {
                        GL11.glVertex3d(a0, d0, c0);
                        GL11.glVertex3d(a0, d1, c0);
                        GL11.glVertex3d(a1, d1, c0);
                        GL11.glVertex3d(a1, d0, c0);
                     }

                     if(!this.schemSolid(l, x, y, z + 1)) {
                        GL11.glVertex3d(a0, d0, c1);
                        GL11.glVertex3d(a1, d0, c1);
                        GL11.glVertex3d(a1, d1, c1);
                        GL11.glVertex3d(a0, d1, c1);
                     }

                     if(!this.schemSolid(l, x - 1, y, z)) {
                        GL11.glVertex3d(a0, d0, c0);
                        GL11.glVertex3d(a0, d0, c1);
                        GL11.glVertex3d(a0, d1, c1);
                        GL11.glVertex3d(a0, d1, c0);
                     }

                     if(!this.schemSolid(l, x + 1, y, z)) {
                        GL11.glVertex3d(a1, d0, c0);
                        GL11.glVertex3d(a1, d1, c0);
                        GL11.glVertex3d(a1, d1, c1);
                        GL11.glVertex3d(a1, d0, c1);
                     }
                  }
               }
            }
         }
      }

      GL11.glEnd();
      GlStateManager.depthMask(true);
      GlStateManager.enableCull();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void forEachMissing(EntityPlayer player, double maxDist, SchematicModule.MissingVisitor visitor) {
      SchematicModule.Loaded l = this.active();
      if(l != null && l.blocks != null && l.origin != null && this.mc.theWorld != null) {
         double maxSq = maxDist * maxDist;
         int[] b = this.reachBounds(l, player, maxDist);

         for(int y = b[1]; y <= b[4]; ++y) {
            for(int z = b[2]; z <= b[5]; ++z) {
               for(int x = b[0]; x <= b[3]; ++x) {
                  if(this.schemSolid(l, x, y, z)) {
                     BlockPos wp = l.origin.add(x, y, z);
                     if(this.mc.theWorld.isAirBlock(wp)) {
                        double wx = (double)wp.getX();
                        double wy = (double)wp.getY();
                        double wz = (double)wp.getZ();
                        double ddx = wx + 0.5D - player.posX;
                        double ddy = wy + 0.5D - player.posY;
                        double ddz = wz + 0.5D - player.posZ;
                        if(ddx * ddx + ddy * ddy + ddz * ddz <= maxSq) {
                           visitor.accept(wx, wy, wz);
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private void renderGhostGrid(SchematicModule.Loaded l, double camX, double camY, double camZ) {
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.depthMask(false);
      GL11.glLineWidth(1.0F);
      GlStateManager.color(0.75F, 0.95F, 1.0F, 0.45F);
      double maxDist = this.renderDistance.get();
      double maxDistSq = maxDist * maxDist;
      double px = this.mc.thePlayer.posX;
      double py = this.mc.thePlayer.posY;
      double pz = this.mc.thePlayer.posZ;
      int[] b = this.reachBounds(l, this.mc.thePlayer, maxDist);
      GL11.glBegin(1);

      for(int y = b[1]; y <= b[4]; ++y) {
         for(int z = b[2]; z <= b[5]; ++z) {
            for(int x = b[0]; x <= b[3]; ++x) {
               if(this.schemSolid(l, x, y, z)) {
                  double wx = (double)(l.origin.getX() + x);
                  double wy = (double)(l.origin.getY() + y);
                  double wz = (double)(l.origin.getZ() + z);
                  double ddx = wx + 0.5D - px;
                  double ddy = wy + 0.5D - py;
                  double ddz = wz + 0.5D - pz;
                  if(ddx * ddx + ddy * ddy + ddz * ddz <= maxDistSq) {
                     double a0 = wx - camX;
                     double a1 = a0 + 1.0D;
                     double c0 = wz - camZ;
                     double c1 = c0 + 1.0D;
                     double d0 = wy - camY;
                     double d1 = d0 + 1.0D;
                     if(!this.schemSolid(l, x, y - 1, z)) {
                        this.faceEdges(a0, d0, c0, a1, d0, c0, a1, d0, c1, a0, d0, c1);
                     }

                     if(!this.schemSolid(l, x, y + 1, z)) {
                        this.faceEdges(a0, d1, c0, a1, d1, c0, a1, d1, c1, a0, d1, c1);
                     }

                     if(!this.schemSolid(l, x, y, z - 1)) {
                        this.faceEdges(a0, d0, c0, a1, d0, c0, a1, d1, c0, a0, d1, c0);
                     }

                     if(!this.schemSolid(l, x, y, z + 1)) {
                        this.faceEdges(a0, d0, c1, a1, d0, c1, a1, d1, c1, a0, d1, c1);
                     }

                     if(!this.schemSolid(l, x - 1, y, z)) {
                        this.faceEdges(a0, d0, c0, a0, d0, c1, a0, d1, c1, a0, d1, c0);
                     }

                     if(!this.schemSolid(l, x + 1, y, z)) {
                        this.faceEdges(a1, d0, c0, a1, d0, c1, a1, d1, c1, a1, d1, c0);
                     }
                  }
               }
            }
         }
      }

      GL11.glEnd();
      GlStateManager.depthMask(true);
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void faceEdges(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4) {
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x2, y2, z2);
      GL11.glVertex3d(x2, y2, z2);
      GL11.glVertex3d(x3, y3, z3);
      GL11.glVertex3d(x3, y3, z3);
      GL11.glVertex3d(x4, y4, z4);
      GL11.glVertex3d(x4, y4, z4);
      GL11.glVertex3d(x1, y1, z1);
   }

   private boolean schemSolid(SchematicModule.Loaded l, int x, int y, int z) {
      if(x >= 0 && y >= 0 && z >= 0 && x < l.width && y < l.height && z < l.length) {
         int index = (y * l.length + z) * l.width + x;
         return index >= 0 && index < l.blocks.length?(l.blocks[index] & 255) != 0:false;
      } else {
         return false;
      }
   }

   private void drawBounds(SchematicModule.Loaded l, double camX, double camY, double camZ) {
      double x0 = (double)l.origin.getX() - camX;
      double x1 = x0 + (double)l.width;
      double y0 = (double)l.origin.getY() - camY;
      double y1 = y0 + (double)l.height;
      double z0 = (double)l.origin.getZ() - camZ;
      double z1 = z0 + (double)l.length;
      GL11.glBegin(2);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glEnd();
      GL11.glBegin(2);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glEnd();
      GL11.glBegin(1);
      GL11.glVertex3d(x0, y0, z0);
      GL11.glVertex3d(x0, y1, z0);
      GL11.glVertex3d(x1, y0, z0);
      GL11.glVertex3d(x1, y1, z0);
      GL11.glVertex3d(x1, y0, z1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x0, y0, z1);
      GL11.glVertex3d(x0, y1, z1);
      GL11.glEnd();
   }

   private void setupLineState() {
      GlStateManager.disableTexture2D();
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GL11.glLineWidth(1.5F);
   }

   private void teardownLineState() {
      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static class Loaded {
      short width;
      short height;
      short length;
      byte[] blocks;
      byte[] data;
      String name;
      File file;
      BlockPos origin;

      private Loaded() {
      }
   }

   public interface MissingVisitor {
      void accept(double var1, double var3, double var5);
   }
}
