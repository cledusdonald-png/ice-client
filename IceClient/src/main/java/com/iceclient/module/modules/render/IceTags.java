package com.iceclient.module.modules.render;

import com.iceclient.module.Module;
import com.iceclient.module.ModuleCategory;
import com.iceclient.setting.BooleanSetting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ice Client identity tags, with live detection.
 *
 *  - Draws YOUR OWN nametag in third person / freelook (vanilla hides it).
 *  - Draws an "ICE" badge above other Ice Client users on the same server.
 *
 * Detection works like a presence service: every 20s we tell our server
 * "{name} is on {server}" and it replies with everyone else running Ice Client
 * on that same server. Runs on a background thread; if the server is
 * unreachable nothing shows and nothing blocks.
 *
 * Note: this sends your in-game name and the server address you're on to the
 * Ice Client server. That's what makes mutual detection possible.
 */
public class IceTags extends Module {

    private static final String PRESENCE_URL = "http://5.175.213.69/api/presence";
    private static final int ICE = 0x6FD6FF;
    private static final long BEAT_MS = 20000L;

    private final BooleanSetting ownTag = addBool("My nametag in 3rd person", true);
    private final BooleanSetting badge = addBool("Ice badge on users", true);

    /** Lower-cased names of Ice Client users seen on this server. */
    private final Set<String> iceUsers = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private long nextBeat = 0L;
    private volatile boolean beating = false;

    public IceTags() {
        super("Ice Tags", "Your nametag in 3rd person + live badge on other Ice Client users", ModuleCategory.HUD);
    }

    @Override
    protected void onDisable() {
        iceUsers.clear();
        nextBeat = 0L;
    }

    // ---------- presence ----------

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || beating) return;

        long now = System.currentTimeMillis();
        if (now < nextBeat) return;
        nextBeat = now + BEAT_MS;

        final String me = mc.thePlayer.getName();
        final String server = currentServer();
        beating = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try { heartbeat(me, server); } finally { beating = false; }
            }
        }, "IceTags-presence").start();
    }

    private String currentServer() {
        ServerData sd = mc.getCurrentServerData();
        String s = (sd != null && sd.serverIP != null) ? sd.serverIP : "singleplayer";
        return s.replaceAll("[\"\\\\]", "").toLowerCase(); // keep the JSON we build valid
    }

    /** Announce ourselves and take back the list of Ice users on this server. */
    private void heartbeat(String name, String server) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(PRESENCE_URL).openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(4000);
            c.setReadTimeout(4000);
            c.setRequestProperty("Content-Type", "application/json");

            String body = "{\"name\":\"" + name + "\",\"server\":\"" + server + "\"}";
            OutputStream out = c.getOutputStream();
            out.write(body.getBytes(StandardCharsets.UTF_8));
            out.close();

            if (c.getResponseCode() != 200) return;

            StringBuilder resp = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) resp.append(line);
            r.close();

            Set<String> found = new HashSet<String>();
            Matcher m = Pattern.compile("\"([A-Za-z0-9_]{3,16})\"").matcher(resp.toString());
            while (m.find()) found.add(m.group(1).toLowerCase());
            found.remove("users");

            iceUsers.clear();
            iceUsers.addAll(found);
        } catch (Exception ignored) {
            // offline / server down -- badges simply don't show
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private boolean isIceUser(EntityPlayer p) {
        if (p == mc.thePlayer) return true;
        return iceUsers.contains(p.getName().toLowerCase());
    }

    // ---------- rendering ----------

    /**
     * Replaces the vanilla nameplate for Ice users so the badge sits inline.
     *
     * <p>Stacking "ICE" on a second line above the name was the obvious approach
     * and it looks wrong -- it reads as two separate labels and doubles the
     * height of every tag in a fight. Lunar puts its mark on the same line, left
     * of the name, and the whole thing stays one nameplate. Doing that means
     * drawing the plate ourselves, so vanilla's is cancelled here.
     *
     * <p>The name is taken from {@code getDisplayName().getFormattedText()},
     * which is what vanilla uses -- so server rank prefixes and their colours
     * survive intact rather than being flattened to a plain username.
     */
    @SubscribeEvent
    public void onNameplate(net.minecraftforge.client.event.RenderLivingEvent.Specials.Pre event) {
        if (!isEnabled() || !badge.get() || mc.thePlayer == null) return;
        if (!(event.entity instanceof EntityPlayer)) return;

        EntityPlayer p = (EntityPlayer) event.entity;
        if (p == mc.thePlayer || !isIceUser(p)) return;

        // Match vanilla's own rules for showing a nameplate at all.
        if (p.isInvisibleToPlayer(mc.thePlayer) || p.isSneaking()) return;
        if (p.getDistanceSqToEntity(mc.getRenderViewEntity()) > 64.0D * 64.0D) return;

        // Cancel only. The replacement is drawn in onRenderWorldLast, after tile
        // entities -- drawing it here as well would double every tag up.
        event.setCanceled(true);
    }

    /**
     * Draws every tag after the world is finished, rather than during each
     * player's own render.
     *
     * <p>Two bugs share one cause here. Tags drawn inside the entity pass are
     * painted over by anything rendered later -- chests and other tile entities
     * come after players, so a chest in front would slice a name in half. And
     * the entity pass only runs for players Minecraft decided to draw, so during
     * FreeLook (which leaves your body still, and so leaves the visible-chunk
     * set stale) culled players lost their tags entirely.
     *
     * <p>{@code RenderWorldLastEvent} fires once per frame after all of that, so
     * neither applies: the tag is always drawn, and always on top.
     */
    @SubscribeEvent
    public void onRenderWorldLast(net.minecraftforge.client.event.RenderWorldLastEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        RenderManager rm = mc.getRenderManager();
        float pt = event.partialTicks;

        for (EntityPlayer p : mc.theWorld.playerEntities) {
            boolean self = p == mc.thePlayer;

            if (self) {
                if (!ownTag.get() || mc.gameSettings.thirdPersonView == 0) continue;
            } else {
                if (!badge.get() || !isIceUser(p)) continue;
                if (p.isInvisibleToPlayer(mc.thePlayer) || p.isSneaking()) continue;
            }

            if (p.getDistanceSqToEntity(mc.thePlayer) > 64.0D * 64.0D) continue;

            // Interpolated so the tag tracks a moving player smoothly, and
            // camera-relative because this event draws in world space.
            double x = p.prevPosX + (p.posX - p.prevPosX) * pt - rm.viewerPosX;
            double y = p.prevPosY + (p.posY - p.prevPosY) * pt - rm.viewerPosY + p.height + 0.5D;
            double z = p.prevPosZ + (p.posZ - p.prevPosZ) * pt - rm.viewerPosZ;

            if (badge.get()) {
                drawBadgedLabel(x, y, z, p.getDisplayName().getFormattedText());
            } else {
                drawLabel(x, y, z, p.getName(), 0xFFFFFF);
            }
        }
    }

    /** Nameplate with the ice mark inline, ahead of the name. */
    private void drawBadgedLabel(double x, double y, double z, String text) {
        RenderManager rm = mc.getRenderManager();
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;

        int textW = fr.getStringWidth(text);
        int mark = 7;              // width of the crystal
        int gapAfterMark = 3;
        int total = mark + gapAfterMark + textW;
        int left = -total / 2;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GL11.glNormal3f(0f, 1f, 0f);
        GlStateManager.rotate(-rm.playerViewY, 0f, 1f, 0f);
        GlStateManager.rotate(rm.playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-0.02666667f, -0.02666667f, 0.02666667f);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);

        GlStateManager.disableTexture2D();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        // Backing plate, sized to mark + name together.
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left - 2, -1, 0).color(0f, 0f, 0f, 0.25f).endVertex();
        wr.pos(left - 2, 8, 0).color(0f, 0f, 0f, 0.25f).endVertex();
        wr.pos(left + total + 2, 8, 0).color(0f, 0f, 0f, 0.25f).endVertex();
        wr.pos(left + total + 2, -1, 0).color(0f, 0f, 0f, 0.25f).endVertex();
        tess.draw();

        // The mark: a small crystal, drawn rather than a glyph so it does not
        // depend on the font having a snowflake character.
        float cx = left + mark / 2.0F;
        float cy = 3.5F;
        float r = mark / 2.0F;
        float ir = r * 0.55F;
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(cx, cy - r, 0).color(0.69f, 0.91f, 1f, 1f).endVertex();
        wr.pos(cx - ir, cy, 0).color(0.44f, 0.84f, 1f, 1f).endVertex();
        wr.pos(cx, cy + r, 0).color(0.18f, 0.50f, 0.65f, 1f).endVertex();
        wr.pos(cx + ir, cy, 0).color(0.44f, 0.84f, 1f, 1f).endVertex();
        tess.draw();

        GlStateManager.enableTexture2D();
        fr.drawString(text, left + mark + gapAfterMark, 0, 0xFFFFFF);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    /** Billboarded label at a world position, drawn like a vanilla nametag. */
    private void drawLabel(double x, double y, double z, String text, int color) {
        RenderManager rm = mc.getRenderManager();
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GL11.glNormal3f(0f, 1f, 0f);
        GlStateManager.rotate(-rm.playerViewY, 0f, 1f, 0f);
        GlStateManager.rotate(rm.playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-0.02666667f, -0.02666667f, 0.02666667f);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);

        int half = fr.getStringWidth(text) / 2;

        GlStateManager.disableTexture2D();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-half - 1, -1, 0).color(0f, 0f, 0f, 0.25f).endVertex();
        wr.pos(-half - 1, 8, 0).color(0f, 0f, 0f, 0.25f).endVertex();
        wr.pos(half + 1, 8, 0).color(0f, 0f, 0f, 0.25f).endVertex();
        wr.pos(half + 1, -1, 0).color(0f, 0f, 0f, 0.25f).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();

        fr.drawString(text, -half, 0, color);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }
}
