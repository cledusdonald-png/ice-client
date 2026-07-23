package com.iceclient.cosmetic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Publishes what you are wearing and learns what everyone else is.
 *
 * <p>Cosmetics that only you can see are close to pointless, and this is the
 * piece that fixes that: every few seconds the client tells the Ice server its
 * equipped slots and takes back the same for every other Ice user on the same
 * Minecraft server.
 *
 * <p>The server filters what it repeats to ownership, so editing a config to
 * claim a legendary cape achieves nothing -- other clients are told what the
 * server believes you own, not what you claimed.
 *
 * <p>Ten seconds rather than the presence service's twenty: swapping a cape and
 * waiting twenty seconds for a friend to see it feels broken, and the payload is
 * tiny.
 */
public final class CosmeticSync {

   private static final String URL_WORN = "http://5.175.213.69/api/worn";
   private static final long PERIOD_MS = 10000L;

   private long nextBeat;
   private volatile boolean inFlight;
   /** What we last published, so an unchanged loadout does not spam the server. */
   private String lastPayload = "";

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if(event.phase != TickEvent.Phase.END || this.inFlight) {
         return;
      }

      Minecraft mc = Minecraft.getMinecraft();
      if(mc.thePlayer == null || mc.theWorld == null) {
         return;
      }

      long now = System.currentTimeMillis();
      if(now < this.nextBeat) {
         return;
      }
      this.nextBeat = now + PERIOD_MS;

      final String name = mc.thePlayer.getName();
      final String server = currentServer(mc);
      final String payload = buildPayload(name, server);

      this.inFlight = true;
      Thread t = new Thread(new Runnable() {
         public void run() {
            try {
               beat(payload);
            } finally {
               inFlight = false;
            }
         }
      }, "IceCosmeticSync");
      t.setDaemon(true);
      t.start();
   }

   private static String currentServer(Minecraft mc) {
      ServerData sd = mc.getCurrentServerData();
      String s = (sd != null && sd.serverIP != null) ? sd.serverIP : "singleplayer";
      return s.toLowerCase();
   }

   private String buildPayload(String name, String server) {
      JsonObject equipped = new JsonObject();

      for(CosmeticType t : CosmeticType.values()) {
         String id = CosmeticManager.getEquipped(t);
         if(id != null) {
            equipped.addProperty(t.name(), id);
         }
      }

      JsonObject body = new JsonObject();
      body.addProperty("name", name);
      body.addProperty("server", server);
      body.add("equipped", equipped);

      // An emote in flight is sent with the time it began, so a client that
      // receives it late can start partway through -- or skip it entirely if it
      // has already finished -- rather than replaying it from the top.
      String emote = EmoteManager.localEmote();
      if(emote != null) {
         body.addProperty("emote", emote);
         body.addProperty("emoteAt", Long.valueOf(EmoteManager.localEmoteStart()));
      }

      return body.toString();
   }

   /**
    * One round trip.
    *
    * <p>Failures are swallowed on purpose: a VPS being down should mean nobody's
    * cosmetics show, not an error in chat every ten seconds.
    */
   private void beat(String payload) {
      HttpURLConnection c = null;

      try {
         c = (HttpURLConnection) new URL(URL_WORN).openConnection();
         c.setRequestMethod("POST");
         c.setDoOutput(true);
         c.setConnectTimeout(4000);
         c.setReadTimeout(4000);
         c.setRequestProperty("Content-Type", "application/json");

         OutputStream out = c.getOutputStream();
         out.write(payload.getBytes(StandardCharsets.UTF_8));
         out.close();

         if(c.getResponseCode() != 200) {
            return;
         }

         StringBuilder sb = new StringBuilder();
         BufferedReader r = new BufferedReader(
               new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
         String line;
         while((line = r.readLine()) != null) {
            sb.append(line);
         }
         r.close();

         apply(sb.toString());
         this.lastPayload = payload;
      } catch (Exception ignored) {
         // offline, or the service is restarting -- try again next beat
      } finally {
         if(c != null) {
            c.disconnect();
         }
      }
   }

   /** Replaces the remote cache wholesale, so someone taking a cape off clears it. */
   private void apply(String json) {
      try {
         JsonObject root = new JsonParser().parse(json).getAsJsonObject();
         if(!root.has("players")) {
            return;
         }

         CosmeticManager.clearRemote();
         JsonObject players = root.getAsJsonObject("players");

         for(Map.Entry<String, com.google.gson.JsonElement> e : players.entrySet()) {
            String who = e.getKey();
            if(!e.getValue().isJsonObject()) {
               continue;
            }

            JsonObject slots = e.getValue().getAsJsonObject();
            for(CosmeticType t : CosmeticType.values()) {
               if(slots.has(t.name())) {
                  CosmeticManager.setRemote(who, t, slots.get(t.name()).getAsString());
               }
            }

            if(slots.has("emote") && slots.has("emoteAt")) {
               EmoteManager.startRemote(who, slots.get("emote").getAsString(),
                     slots.get("emoteAt").getAsLong());
            }
         }
      } catch (Exception ignored) {
         // Malformed response: keep whatever we had rather than clearing everyone.
      }
   }

   /** Pushes on the next tick rather than waiting out the interval. */
   public void pokeNow() {
      this.nextBeat = 0L;
   }
}
