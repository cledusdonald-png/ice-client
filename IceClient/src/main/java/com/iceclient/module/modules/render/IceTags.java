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
import net.minecraftforge.client.event.RenderPlayerEvent;
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

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Post event) {
        if (!isEnabled() || mc.thePlayer == null) return;
        EntityPlayer p = event.entityPlayer;
        double baseY = event.y + p.height + 0.5D;

        if (p == mc.thePlayer) {
            // vanilla never draws your own tag; do it ourselves in 3rd person/freelook
            if (!ownTag.get() || mc.gameSettings.thirdPersonView == 0) return;
            drawLabel(event.x, baseY, event.z, p.getName(), 0xFFFFFF);
            if (badge.get()) drawLabel(event.x, baseY + 0.32D, event.z, "ICE", ICE);
        } else {
            // vanilla already drew their name -- stack our badge above it
            if (!badge.get() || !isIceUser(p)) return;
            drawLabel(event.x, baseY + 0.32D, event.z, "ICE", ICE);
        }
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
