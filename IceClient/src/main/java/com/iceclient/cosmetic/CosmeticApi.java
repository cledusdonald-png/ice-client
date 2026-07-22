package com.iceclient.cosmetic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Talks to the Ice server about points and unlocks.
 *
 * <p><b>Why any of this is on a server.</b> The balance used to live in
 * {@code iceclient.json}, which anyone can open in a text editor -- so "points"
 * were worth exactly nothing and a giveaway would have been theatre. Balances,
 * ownership and redemptions are held on the VPS instead; the client asks, and
 * the local config keeps only a copy so the wardrobe still renders while
 * offline.
 *
 * <p>This is not real security -- the client still says who it is, and nobody is
 * signing anything, so a determined person could claim to be someone else. It is
 * enough that points cannot be minted by editing a file, which is the actual
 * problem. Anything stronger needs the server to verify a session token, which
 * is worth doing only if this ever involves money.
 *
 * <p>Every call runs off-thread and fails silently into a callback: the game
 * must never hitch because a VPS is slow, and a wardrobe that works offline is
 * better than one that errors.
 */
public final class CosmeticApi {

   private static final String BASE = "http://5.175.213.69/api";
   private static final int TIMEOUT_MS = 5000;

   private CosmeticApi() {
   }

   /** Called on the main thread once a request finishes. */
   public interface Callback {
      void done(boolean ok, String message);
   }

   private static String selfName() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc.thePlayer == null ? null : mc.thePlayer.getName();
   }

   /**
    * Pulls the balance and owned list for this player.
    *
    * <p>The server is authoritative: whatever it says replaces local state. A
    * failed sync leaves the cached values alone rather than zeroing them, so a
    * server outage does not look like being robbed.
    */
   public static void sync(final Callback cb) {
      final String name = selfName();
      if(name == null) {
         return;
      }

      run(new Runnable() {
         public void run() {
            try {
               JsonObject res = get("/points?name=" + enc(name));
               if(res == null) {
                  post(cb, false, "Couldn't reach the Ice server.");
                  return;
               }

               applyState(res);
               post(cb, true, null);
            } catch (Exception e) {
               post(cb, false, "Sync failed: " + e.getMessage());
            }
         }
      });
   }

   /** Redeems a giveaway code. */
   public static void redeem(final String code, final Callback cb) {
      final String name = selfName();
      if(name == null || code == null || code.trim().isEmpty()) {
         return;
      }

      run(new Runnable() {
         public void run() {
            try {
               JsonObject body = new JsonObject();
               body.addProperty("name", name);
               body.addProperty("code", code.trim());

               JsonObject res = postJson("/redeem", body);
               if(res == null) {
                  post(cb, false, "Couldn't reach the Ice server.");
                  return;
               }

               if(res.has("error")) {
                  post(cb, false, res.get("error").getAsString());
                  return;
               }

               applyState(res);
               int gained = res.has("gained") ? res.get("gained").getAsInt() : 0;
               post(cb, true, "Redeemed — +" + gained + " frost.");
            } catch (Exception e) {
               post(cb, false, "Redeem failed: " + e.getMessage());
            }
         }
      });
   }

   /** Buys an item; the server checks the price and the balance, not us. */
   public static void purchase(final String id, final Callback cb) {
      final String name = selfName();
      if(name == null) {
         return;
      }

      run(new Runnable() {
         public void run() {
            try {
               JsonObject body = new JsonObject();
               body.addProperty("name", name);
               body.addProperty("item", id);

               JsonObject res = postJson("/buy", body);
               if(res == null) {
                  post(cb, false, "Couldn't reach the Ice server.");
                  return;
               }

               if(res.has("error")) {
                  post(cb, false, res.get("error").getAsString());
                  return;
               }

               applyState(res);
               post(cb, true, "Purchased.");
            } catch (Exception e) {
               post(cb, false, "Purchase failed: " + e.getMessage());
            }
         }
      });
   }

   /** Copies a server response into local state. */
   private static void applyState(JsonObject res) {
      if(res.has("balance")) {
         CosmeticManager.setBalance(res.get("balance").getAsInt());
      }

      if(res.has("owned")) {
         java.util.Set<String> ids = new java.util.HashSet<String>();
         for(com.google.gson.JsonElement e : res.getAsJsonArray("owned")) {
            ids.add(e.getAsString());
         }
         CosmeticManager.setOwned(ids);
      }
   }

   // ------------------------------------------------------------------
   // plumbing
   // ------------------------------------------------------------------

   private static void run(Runnable r) {
      Thread t = new Thread(r, "IceCosmeticApi");
      t.setDaemon(true);
      t.start();
   }

   /** Hops back to the main thread; Minecraft state is not thread-safe. */
   private static void post(final Callback cb, final boolean ok, final String msg) {
      if(cb == null) {
         return;
      }

      Minecraft.getMinecraft().addScheduledTask(new Runnable() {
         public void run() {
            cb.done(ok, msg);
         }
      });
   }

   private static String enc(String s) throws Exception {
      return java.net.URLEncoder.encode(s, "UTF-8");
   }

   private static JsonObject get(String path) throws Exception {
      HttpURLConnection c = open(path, "GET");
      return read(c);
   }

   private static JsonObject postJson(String path, JsonObject body) throws Exception {
      HttpURLConnection c = open(path, "POST");
      c.setDoOutput(true);
      c.setRequestProperty("Content-Type", "application/json");

      OutputStream out = c.getOutputStream();
      out.write(body.toString().getBytes(StandardCharsets.UTF_8));
      out.close();

      return read(c);
   }

   private static HttpURLConnection open(String path, String method) throws Exception {
      HttpURLConnection c = (HttpURLConnection) new URL(BASE + path).openConnection();
      c.setRequestMethod(method);
      c.setConnectTimeout(TIMEOUT_MS);
      c.setReadTimeout(TIMEOUT_MS);
      return c;
   }

   /** Reads the body whether the status was 2xx or an error, since the server
    *  reports refusals as JSON with an "error" field rather than a bare code. */
   private static JsonObject read(HttpURLConnection c) throws Exception {
      java.io.InputStream in = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
      if(in == null) {
         return null;
      }

      StringBuilder sb = new StringBuilder();
      BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      String line;
      while((line = r.readLine()) != null) {
         sb.append(line);
      }
      r.close();
      c.disconnect();

      com.google.gson.JsonElement parsed = new JsonParser().parse(sb.toString());
      return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
   }
}
