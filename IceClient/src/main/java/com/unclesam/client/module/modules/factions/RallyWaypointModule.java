package com.unclesam.client.module.modules.factions;

import com.unclesam.client.module.Module;
import com.unclesam.client.module.ModuleCategory;
import com.unclesam.client.setting.BooleanSetting;
import com.unclesam.client.setting.ModeSetting;
import com.unclesam.client.setting.NumberSetting;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import com.unclesam.client.util.WorldRenderUtil;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class RallyWaypointModule extends Module {
   /**
    * Pulls coordinates out of a rally message.
    *
    * <p>Two fixes over the original {@code (-?\d{1,7})\D{1,4}...} form:
    * <ul>
    *   <li>the separator is {@code [^-\d]}, not {@code \D} -- {@code \D} happily
    *       consumed the minus sign of a negative coordinate, silently turning
    *       "-5678" into "+5678" and putting the waypoint on the wrong side of
    *       the map;
    *   <li>an optional {@code .\d+} is consumed after each number, so a decimal
    *       position like "1234.5, 64.0, -5678.2" no longer shifts every capture
    *       one field left (that used to parse as 1234, 5, 64).
    * </ul>
    * Y is widened to 4 digits so nether-roof / build-height coords still match.
    */
   private static final Pattern COORDS = Pattern.compile("(-?\\d{1,7})(?:\\.\\d+)?[^-\\d]{1,4}(-?\\d{1,4})(?:\\.\\d+)?[^-\\d]{1,4}(-?\\d{1,7})(?:\\.\\d+)?");
   /**
    * Minecadia's coords broadcast, captured whole:
    * {@code [!] <Helper> W1ffed's coords are 4843,243,4834}
    *
    * <p>Taking the name from the same match as the coordinates is deliberate --
    * scanning the message for a name separately picks up the chat prefix
    * ("Helper") instead of the player. Both straight and curly apostrophes are
    * accepted because servers differ.
    */
   private static final Pattern ANNOUNCE = Pattern.compile(
         "([A-Za-z0-9_]{3,16})['’]s\\s+coords\\s+are\\s+(-?\\d{1,7})\\s*,\\s*(-?\\d{1,4})\\s*,\\s*(-?\\d{1,7})",
         Pattern.CASE_INSENSITIVE);

   private static final Pattern NAME = Pattern.compile("\\b([A-Za-z0-9_]{3,16})\\b");
   private final NumberSetting minutes = (NumberSetting)this.addSetting(new NumberSetting("Minutes", 3.0D, 1.0D, 15.0D, 1.0D));
   private final BooleanSetting beam = (BooleanSetting)this.addSetting(new BooleanSetting("Beam", true));
   // Was 15 -- the beam vanished whenever you got close to the rally, which is
   // exactly when you're looking for it. 0 means always draw.
   private final NumberSetting beamFrom = (NumberSetting)this.addSetting(new NumberSetting("Beam past (m)", 0.0D, 0.0D, 100.0D, 5.0D));
   // The original only cleared the depth *mask*, so the beam still depth-tested
   // and any terrain in front hid it -- underground or inside a base it was
   // invisible at every range. The ring already draws through walls; this makes
   // the beam match.
   private final BooleanSetting beamThroughWalls = (BooleanSetting)this.addSetting(new BooleanSetting("Beam through walls", true));
   private final NumberSetting beamWidth = (NumberSetting)this.addSetting(new NumberSetting("Beam width", 0.35D, 0.1D, 2.0D, 0.05D));
   private final NumberSetting beamAlpha = (NumberSetting)this.addSetting(new NumberSetting("Beam alpha", 0.45D, 0.05D, 1.0D, 0.05D));
   private final NumberSetting beamHeight = (NumberSetting)this.addSetting(new NumberSetting("Beam height", 256.0D, 8.0D, 512.0D, 8.0D));
   private final NumberSetting beamOffsetY = (NumberSetting)this.addSetting(new NumberSetting("Beam start Y offset", 0.0D, -64.0D, 64.0D, 1.0D));
   private final ModeSetting color = (ModeSetting)this.addSetting(new ModeSetting("Color", "Red", new String[]{"Red", "Aqua", "Green", "Yellow"}));
   private final BooleanSetting announce = (BooleanSetting)this.addSetting(new BooleanSetting("Announce in chat", true));
   // Ground marker: the white ring + centre dot that sits at the rally spot.
   private final BooleanSetting ring = (BooleanSetting)this.addSetting(new BooleanSetting("Ground ring", true));
   private final NumberSetting ringRadius = (NumberSetting)this.addSetting(new NumberSetting("Ring radius", 1.5D, 0.5D, 8.0D, 0.5D));
   private final NumberSetting ringWidth = (NumberSetting)this.addSetting(new NumberSetting("Ring width", 2.0D, 1.0D, 5.0D, 0.5D));
   private final BooleanSetting centreDot = (BooleanSetting)this.addSetting(new BooleanSetting("Centre dot", true));
   private String rallyName;
   private int rx;
   private int ry;
   private int rz;
   private long expiresAt;

   public RallyWaypointModule() {
      super("Rally Waypoint", "Marks /f rally spots with a beam + distance", ModuleCategory.FACTIONS, 0);
   }

   protected void onDisable() {
      this.expiresAt = 0L;
   }

   private boolean active() {
      return this.expiresAt > System.currentTimeMillis();
   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent event) {
      if(this.isEnabled() && event.message != null) {
         String msg = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
         if(msg != null) {
            // Preferred path: Minecadia's actual broadcast, e.g.
            //   [!] <Helper> W1ffed's coords are 4843,243,4834
            // It never contains the word "rally", so the keyword check below
            // never saw it. Capturing the name here also avoids extractName(),
            // which would otherwise pick "Helper" out of the chat prefix.
            Matcher r = ANNOUNCE.matcher(msg);
            if(r.find()) {
               try {
                  this.mark(r.group(1), Integer.parseInt(r.group(2)), Integer.parseInt(r.group(3)), Integer.parseInt(r.group(4)));
               } catch (NumberFormatException var7) {
               }

               return;
            }

            // Fallback for servers that do word it as a rally.
            String lower = msg.toLowerCase();
            if(lower.contains("rall")) {
               Matcher c = COORDS.matcher(msg);
               if(c.find()) {
                  try {
                     this.mark(this.extractName(msg), Integer.parseInt(c.group(1)), Integer.parseInt(c.group(2)), Integer.parseInt(c.group(3)));
                  } catch (NumberFormatException var6) {
                  }

               }
            }
         }
      }
   }

   /** Sets the active rally and announces it. */
   private void mark(String name, int x, int y, int z) {
      this.rallyName = name;
      this.rx = x;
      this.ry = y;
      this.rz = z;
      this.expiresAt = System.currentTimeMillis() + (long)(this.minutes.get() * 60000.0D);
      if(this.announce.get() && this.mc.thePlayer != null) {
         this.mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + "[Ice] " + EnumChatFormatting.RESET + "Rally marked: " + this.rallyName + " @ " + this.rx + ", " + this.ry + ", " + this.rz + " (" + this.minutes.getInt() + "m)"));
      }

   }

   private String extractName(String msg) {
      Matcher m = NAME.matcher(msg);

      while(m.find()) {
         String token = m.group(1);
         String t = token.toLowerCase();
         if(!t.startsWith("rall") && !t.equals("has") && !t.equals("the") && !t.equals("faction") && !t.equals("your") && !t.equals("team") && !t.equals("called") && !token.matches("-?\\d+")) {
            return token;
         }
      }

      return "Rally";
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent event) {
      if(this.isEnabled() && this.active() && this.mc.thePlayer != null) {
         double camX = this.mc.getRenderManager().viewerPosX;
         double camY = this.mc.getRenderManager().viewerPosY;
         double camZ = this.mc.getRenderManager().viewerPosZ;
         double dist = this.mc.thePlayer.getDistance((double)this.rx + 0.5D, (double)this.ry, (double)this.rz + 0.5D);
         float[] rgb = this.rgb();
         if(this.beam.get() && dist >= this.beamFrom.get()) {
            // Shared with Waypoints so both markers look the same and gain new
            // options together.
            int beamCol = this.argbOf(rgb, (int)(this.beamAlpha.get() * 255.0D));
            WorldRenderUtil.beam((double)this.rx + 0.5D, (double)this.ry + this.beamOffsetY.get(), (double)this.rz + 0.5D,
                  this.beamHeight.get(), this.beamWidth.get(), beamCol, this.beamThroughWalls.get());
         }

         // Ground marker. These helpers apply the camera offset themselves, so
         // they take absolute world coords rather than the cam-relative ones
         // the beam/label below use.
         if(this.ring.get()) {
            WorldRenderUtil.horizontalCircle((double)this.rx + 0.5D, (double)this.ry + 0.05D, (double)this.rz + 0.5D,
                  this.ringRadius.get(), -1, (float)this.ringWidth.get(), 48);
         }

         if(this.centreDot.get()) {
            WorldRenderUtil.drawPoint((double)this.rx + 0.5D, (double)this.ry + 0.05D, (double)this.rz + 0.5D, -16777216, 5.0F);
         }

         long secondsLeft = (this.expiresAt - System.currentTimeMillis()) / 1000L;
         String label = this.rallyName + " [" + (int)dist + "m]";
         String sub = secondsLeft >= 60L?secondsLeft / 60L + "m" + String.format("%02d", new Object[]{Long.valueOf(secondsLeft % 60L)}) + "s":secondsLeft + "s";
         this.drawLabel((double)this.rx + 0.5D - camX, (double)this.ry + 2.2D - camY, (double)this.rz + 0.5D - camZ, label, sub, dist, rgb);
      }
   }

   /** Packs a float rgb triple + alpha into the ARGB int the render helpers take. */
   private int argbOf(float[] rgb, int alpha) {
      return (alpha & 255) << 24
            | ((int)(rgb[0] * 255.0F) & 255) << 16
            | ((int)(rgb[1] * 255.0F) & 255) << 8
            | ((int)(rgb[2] * 255.0F) & 255);
   }

   private void drawBeam(double x, double y, double z, float[] rgb) {
      double s = this.beamWidth.get() / 2.0D;
      double top = this.beamHeight.get();
      boolean seeThrough = this.beamThroughWalls.get();
      GlStateManager.pushMatrix();
      GlStateManager.translate(x, y, z);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.disableCull();
      GlStateManager.depthMask(false);
      if(seeThrough) {
         GlStateManager.disableDepth();
      }

      GlStateManager.color(rgb[0], rgb[1], rgb[2], (float)this.beamAlpha.get());
      GL11.glBegin(7);
      GL11.glVertex3d(-s, 0.0D, -s);
      GL11.glVertex3d(-s, top, -s);
      GL11.glVertex3d(s, top, -s);
      GL11.glVertex3d(s, 0.0D, -s);
      GL11.glVertex3d(s, 0.0D, s);
      GL11.glVertex3d(s, top, s);
      GL11.glVertex3d(-s, top, s);
      GL11.glVertex3d(-s, 0.0D, s);
      GL11.glVertex3d(-s, 0.0D, s);
      GL11.glVertex3d(-s, top, s);
      GL11.glVertex3d(-s, top, -s);
      GL11.glVertex3d(-s, 0.0D, -s);
      GL11.glVertex3d(s, 0.0D, -s);
      GL11.glVertex3d(s, top, -s);
      GL11.glVertex3d(s, top, s);
      GL11.glVertex3d(s, 0.0D, s);
      GL11.glEnd();
      if(seeThrough) {
         GlStateManager.enableDepth();
      }

      GlStateManager.depthMask(true);
      GlStateManager.enableCull();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   private void drawLabel(double x, double y, double z, String label, String sub, double dist, float[] rgb) {
      float scale = (float)(0.025D * Math.max(1.0D, dist / 18.0D));
      GlStateManager.pushMatrix();
      GlStateManager.translate(x, y, z);
      GlStateManager.rotate(-this.mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(this.mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
      GlStateManager.scale(-scale, -scale, scale);
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.enableTexture2D();
      int w = this.mc.fontRendererObj.getStringWidth(label);
      int sw = this.mc.fontRendererObj.getStringWidth(sub);
      int half = Math.max(w, sw) / 2 + 3;
      Gui.drawRect(-half, -3, half, 20, -1879048192);
      int argb = -16777216 | (int)(rgb[0] * 255.0F) << 16 | (int)(rgb[1] * 255.0F) << 8 | (int)(rgb[2] * 255.0F);
      this.mc.fontRendererObj.drawStringWithShadow(label, (float)(-w) / 2.0F, 0.0F, argb);
      this.mc.fontRendererObj.drawStringWithShadow(sub, (float)(-sw) / 2.0F, 10.0F, -7697773);
      GlStateManager.disableBlend();
      GlStateManager.enableDepth();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   private float[] rgb() {
      return this.color.is("Aqua")?new float[]{0.2F, 0.9F, 1.0F}:(this.color.is("Green")?new float[]{0.2F, 1.0F, 0.3F}:(this.color.is("Yellow")?new float[]{1.0F, 0.9F, 0.2F}:new float[]{1.0F, 0.25F, 0.25F}));
   }
}
