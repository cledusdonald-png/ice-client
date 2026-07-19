package com.iceclient.module.modules.render;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import com.iceclient.setting.ColorSetting;
import com.iceclient.setting.KeybindSetting;
import com.iceclient.setting.NumberSetting;
import com.iceclient.util.BindUtil;
import com.iceclient.util.ColorUtil;
import com.iceclient.util.WorldRenderUtil;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent world markers with beacon beams and distance labels.
 *
 * <p>Waypoints live for the session only -- saving them belongs in ConfigManager
 * alongside the rest of the client's persisted state, which is a separate piece
 * of work from the rendering here.
 */
public class Waypoints extends Module {

   private final KeybindSetting createKey = (KeybindSetting)this.addSetting(new KeybindSetting("Create waypoint", 0));
   private final BooleanSetting drawBeacon = (BooleanSetting)this.addSetting(new BooleanSetting("Draw beacon", true));
   // Beam shape. Previously this was a single 1px line, which read as a hairline
   // at range rather than a beacon.
   private final NumberSetting beamWidth = (NumberSetting)this.addSetting(new NumberSetting("Beam width", 0.35D, 0.05D, 3.0D, 0.05D));
   private final NumberSetting beamHeight = (NumberSetting)this.addSetting(new NumberSetting("Beam height", 256.0D, 8.0D, 512.0D, 8.0D));
   private final NumberSetting beamAlpha = (NumberSetting)this.addSetting(new NumberSetting("Beam alpha", 0.45D, 0.05D, 1.0D, 0.05D));
   private final NumberSetting beamOffsetY = (NumberSetting)this.addSetting(new NumberSetting("Beam start Y offset", 0.0D, -64.0D, 64.0D, 1.0D));
   private final BooleanSetting beamThroughWalls = (BooleanSetting)this.addSetting(new BooleanSetting("Beam through walls", true));
   private final BooleanSetting scopeServer = (BooleanSetting)this.addSetting(new BooleanSetting("Only this server", true));
   private final BooleanSetting scopeDimension = (BooleanSetting)this.addSetting(new BooleanSetting("Only this dimension", true));
   private final BooleanSetting showBox = (BooleanSetting)this.addSetting(new BooleanSetting("Box", true));
   private final BooleanSetting showDistance = (BooleanSetting)this.addSetting(new BooleanSetting("Show distance", true));
   private final NumberSetting lineWidth = (NumberSetting)this.addSetting(new NumberSetting("Line width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final NumberSetting scale = (NumberSetting)this.addSetting(new NumberSetting("Scale", 1.0D, 0.5D, 3.0D, 0.1D));
   private final ColorSetting color = (ColorSetting)this.addSetting(new ColorSetting("Color", -16776961));

   private static final List<Waypoint> POINTS = new ArrayList();
   private boolean wasDown;

   /**
    * A marker, scoped to where it was made.
    *
    * <p>Server and dimension are captured at creation so a waypoint from one
    * server's overworld doesn't hang in the sky on another server, or 8x out of
    * place in the nether. An empty server means singleplayer.
    */
   public static class Waypoint {
      public final String name;
      public final BlockPos pos;
      public final String server;
      public final int dimension;

      public Waypoint(String name, BlockPos pos, String server, int dimension) {
         this.name = name;
         this.pos = pos;
         this.server = server == null ? "" : server;
         this.dimension = dimension;
      }
   }

   /** Current server address, or "" in singleplayer. */
   private String currentServer() {
      net.minecraft.client.multiplayer.ServerData d = this.mc.getCurrentServerData();
      return d != null && d.serverIP != null ? d.serverIP : "";
   }

   private int currentDimension() {
      return this.mc.theWorld == null ? 0 : this.mc.theWorld.provider.getDimensionId();
   }

   /** Whether a waypoint belongs to where the player currently is. */
   private boolean inScope(Waypoint wp) {
      if(this.scopeServer.get() && !wp.server.equalsIgnoreCase(this.currentServer())) {
         return false;
      }

      return !this.scopeDimension.get() || wp.dimension == this.currentDimension();
   }

   public Waypoints() {
      super("Waypoints", "Show directions to important locations", ModuleCategory.MECHANIC);
   }

   public static List<Waypoint> getWaypoints() {
      return POINTS;
   }

   public static void add(String name, BlockPos pos, String server, int dimension) {
      POINTS.add(new Waypoint(name, pos, server, dimension));
   }

   public static void clear() {
      POINTS.clear();
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || !this.isEnabled()) {
         return;
      }

      if(this.mc.thePlayer == null || this.mc.currentScreen != null) {
         this.wasDown = false;
         return;
      }

      boolean down = BindUtil.isDown(this.createKey.getKeyCode());
      if(down && !this.wasDown) {
         BlockPos p = new BlockPos(this.mc.thePlayer);
         add("WP" + (POINTS.size() + 1), p, this.currentServer(), this.currentDimension());
         this.mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Ice] "
               + EnumChatFormatting.RESET + "Waypoint added at " + p.getX() + ", " + p.getY() + ", " + p.getZ()));
      }

      this.wasDown = down;
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(!this.isEnabled() || POINTS.isEmpty() || this.mc.thePlayer == null) {
         return;
      }

      int col = ColorUtil.withAlpha(this.color.getRGB(), 220);
      float w = (float)this.lineWidth.get();

      for(Waypoint wp : new ArrayList<Waypoint>(POINTS)) {
         // Skip markers made somewhere else entirely.
         if(!this.inScope(wp)) {
            continue;
         }

         BlockPos p = wp.pos;
         if(this.showBox.get()) {
            WorldRenderUtil.outlineBox(new AxisAlignedBB(p, p.add(1, 1, 1)), col, w);
         }

         if(this.drawBeacon.get()) {
            int beamCol = ColorUtil.withAlpha(this.color.getRGB(), (int)(this.beamAlpha.get() * 255.0D));
            WorldRenderUtil.beam((double)p.getX() + 0.5D, (double)p.getY() + this.beamOffsetY.get(), (double)p.getZ() + 0.5D,
                  this.beamHeight.get(), this.beamWidth.get(), beamCol, this.beamThroughWalls.get());
         }

         String label = wp.name;
         if(this.showDistance.get()) {
            int d = (int)this.mc.thePlayer.getDistance((double)p.getX() + 0.5D, (double)p.getY(), (double)p.getZ() + 0.5D);
            label = label + " [" + d + "m]";
         }

         WorldRenderUtil.text3d(label, (double)p.getX() + 0.5D, (double)p.getY() + 1.4D, (double)p.getZ() + 0.5D,
               col, 0.025F * (float)this.scale.get());
      }

   }
}
