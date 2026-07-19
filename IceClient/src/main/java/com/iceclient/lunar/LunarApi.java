package com.iceclient.lunar;

import com.iceclient.IceClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client half of Lunar's server APIs.
 *
 * <p>There are two, and the previous version of this class conflated them:
 *
 * <ul>
 *   <li><b>Apollo</b> -- current. Channel {@code lunar:apollo}, messages wrapped
 *       in {@code google.protobuf.Any}. Registering the channel is itself the
 *       handshake: Apollo detects a client by the {@code PlayerRegisterChannel}
 *       event, so a server sends nothing until we register.</li>
 *   <li><b>Legacy Bukkit API</b> -- pre-Apollo. Channel {@code Lunar-Client},
 *       a hand-rolled binary envelope. Still in use on older networks, so it is
 *       registered too.</li>
 * </ul>
 *
 * <p><b>What changed and why.</b> This class used to register only
 * {@code Lunar-Client} and parse it with a field layout that was, by its own
 * admission, never checked against a live server. Apollo traffic could not
 * arrive at all, and anything that did was decoded against invented offsets.
 * The envelope handling below is now the documented one; the per-module payload
 * layouts are <em>not</em> hardcoded, because those field numbers live in the
 * {@code .proto} schemas rather than in any published document. Instead each
 * message is scanned into numbered fields and, with {@code Debug Packets} on,
 * dumped -- which is what lets a real layout be worked out from real traffic
 * instead of guessed at a second time.
 *
 * @see <a href="https://lunarclient.dev/apollo/developers/lightweight/protobuf/player-detection">Apollo player detection</a>
 * @see <a href="https://buf.build/lunarclient/apollo">Apollo protobuf schemas</a>
 */
public final class LunarApi {

   /** Apollo. Registering this is what makes a server treat us as a Lunar client. */
   private static final String APOLLO_CHANNEL = "lunar:apollo";

   /** Pre-Apollo Lunar Bukkit API, still live on older networks. */
   private static final String LEGACY_CHANNEL = "Lunar-Client";

   /**
    * Apollo type URLs are {@code type.googleapis.com/lunarclient.apollo.<module>.v1.<Message>}.
    * Only the trailing message name is matched, so a change to the host or the
    * version segment does not break dispatch.
    */
   private static final String TYPE_URL_PREFIX = "type.googleapis.com/";

   /** Field numbers of {@code google.protobuf.Any}, which is a stable, public schema. */
   private static final int ANY_TYPE_URL = 1;
   private static final int ANY_VALUE = 2;

   private static final LunarApi INSTANCE = new LunarApi();

   private static FMLEventChannel apollo;
   private static FMLEventChannel legacy;

   private static volatile boolean debug;

   private static final Map<String, Cooldown> COOLDOWNS = new LinkedHashMap<String, Cooldown>();
   private static final Map<String, Waypoint> WAYPOINTS = new LinkedHashMap<String, Waypoint>();
   private static final Map<UUID, double[]> TEAMMATES = new LinkedHashMap<UUID, double[]>();

   /** Message names seen this session, for the debug readout. */
   private static final Map<String, String> SEEN = new LinkedHashMap<String, String>();

   private LunarApi() {
   }

   public static void init() {
      apollo = register(APOLLO_CHANNEL, apollo);
      legacy = register(LEGACY_CHANNEL, legacy);
   }

   private static FMLEventChannel register(String name, FMLEventChannel existing) {
      if(existing != null) {
         return existing;
      }

      try {
         FMLEventChannel c = NetworkRegistry.INSTANCE.newEventDrivenChannel(name);
         c.register(INSTANCE);
         IceClient.LOGGER.info("Lunar API: registered plugin channel '" + name + "'");
         return c;
      } catch (Throwable t) {
         IceClient.LOGGER.warn("Lunar API: could not register '" + name + "' -- " + t);
         return null;
      }
   }

   public static void setDebug(boolean on) {
      debug = on;
   }

   public static void clear() {
      COOLDOWNS.clear();
      WAYPOINTS.clear();
      TEAMMATES.clear();
      SEEN.clear();
   }

   /** Live cooldowns, expired entries dropped. */
   public static List<Cooldown> cooldowns() {
      long now = System.currentTimeMillis();
      COOLDOWNS.values().removeIf(c -> now >= c.expiresAt);
      return new ArrayList<Cooldown>(COOLDOWNS.values());
   }

   public static List<Waypoint> waypoints() {
      return new ArrayList<Waypoint>(WAYPOINTS.values());
   }

   public static Map<UUID, double[]> teammates() {
      return new LinkedHashMap<UUID, double[]>(TEAMMATES);
   }

   /** Apollo message names seen this session, mapped to their last field dump. */
   public static Map<String, String> seenMessages() {
      return new LinkedHashMap<String, String>(SEEN);
   }

   @SubscribeEvent
   public void onCustomPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
      ByteBuf raw = event.packet.payload();
      if(raw == null || !raw.isReadable()) {
         return;
      }

      String channel = event.packet.channel();

      // Copy first: netty reuses the payload buffer once this returns.
      ByteBuf buf = raw.copy();
      try {
         if(APOLLO_CHANNEL.equals(channel)) {
            this.handleApollo(buf);
         } else {
            this.handleLegacy(buf);
         }
      } catch (Throwable t) {
         // A malformed or unexpected packet must never take the client down.
         if(debug) {
            IceClient.LOGGER.warn("Lunar API: failed to parse packet on '" + channel + "' -- " + t);
         }
      } finally {
         buf.release();
      }
   }

   /**
    * Apollo: the payload is a {@code google.protobuf.Any}.
    *
    * <p>Field 1 is the type URL and field 2 the encoded message. Both are read
    * from the documented Any schema rather than assumed, which is the part that
    * was wrong before.
    */
   private void handleApollo(ByteBuf buf) {
      Map<Integer, List<Object>> any = ProtoReader.scan(buf);

      String typeUrl = ProtoReader.string(any, ANY_TYPE_URL);
      byte[] value = ProtoReader.bytes(any, ANY_VALUE);
      if(typeUrl == null) {
         return;
      }

      String name = typeUrl.startsWith(TYPE_URL_PREFIX)
            ? typeUrl.substring(TYPE_URL_PREFIX.length())
            : typeUrl;

      // Short name only: "lunarclient.apollo.waypoint.v1.WaypointMessage" is
      // mostly namespace, and the namespace is what changes between versions.
      int dot = name.lastIndexOf(46);
      String shortName = dot >= 0 ? name.substring(dot + 1) : name;

      Map<Integer, List<Object>> body = java.util.Collections.emptyMap();
      if(value != null && value.length > 0) {
         body = ProtoReader.scan(io.netty.buffer.Unpooled.wrappedBuffer(value));
      }

      String dump = ProtoReader.describe(body);
      SEEN.put(shortName, dump);

      if(debug) {
         IceClient.LOGGER.info("Apollo: " + shortName + " { " + dump + " }");
      }

      // Deliberately no per-module decoding here yet. The field numbers for
      // each Apollo message are defined in the .proto schemas, and inventing
      // them is exactly the mistake this rewrite exists to undo. Turn on Debug
      // Packets, note the numbers a real server sends, and decode from that.
   }

   /**
    * Legacy Lunar Bukkit API: a string id followed by per-packet fields.
    *
    * <p>Kept because some networks still run it, but treated as best-effort --
    * the layouts below were never confirmed against a live server, so every
    * read is bounded and a mismatch degrades to "packet ignored".
    */
   private void handleLegacy(ByteBuf buf) {
      PacketBuffer pb = new PacketBuffer(buf);
      String id = pb.readStringFromBuffer(64);

      if(debug) {
         IceClient.LOGGER.info("Lunar (legacy): '" + id + "' (" + buf.readableBytes() + " bytes left)");
      }

      if("cooldown".equals(id)) {
         readCooldown(pb);
      } else if("waypoint_add".equals(id)) {
         readWaypointAdd(pb);
      } else if("waypoint_remove".equals(id)) {
         WAYPOINTS.remove(pb.readStringFromBuffer(128));
      } else if("teammates".equals(id)) {
         readTeammates(pb);
      } else if(debug) {
         IceClient.LOGGER.info("Lunar (legacy): unhandled id '" + id + "'");
      }

   }

   private static void readCooldown(PacketBuffer pb) {
      String message = pb.readStringFromBuffer(256);
      long durationMs = pb.readLong();
      int iconId = pb.readableBytes() >= 4 ? pb.readInt() : 0;

      if(durationMs <= 0L) {
         COOLDOWNS.remove(message);
         return;
      }

      COOLDOWNS.put(message, new Cooldown(message, System.currentTimeMillis() + durationMs, durationMs, iconId));
   }

   private static void readWaypointAdd(PacketBuffer pb) {
      String name = pb.readStringFromBuffer(128);
      String world = pb.readStringFromBuffer(128);
      int color = pb.readInt();
      int x = pb.readInt();
      int y = pb.readInt();
      int z = pb.readInt();
      boolean forced = pb.readableBytes() > 0 && pb.readBoolean();
      boolean visible = pb.readableBytes() <= 0 || pb.readBoolean();
      WAYPOINTS.put(name, new Waypoint(name, world, color, x, y, z, forced, visible));
   }

   private static void readTeammates(PacketBuffer pb) {
      pb.readLong();
      pb.readLong();
      pb.readLong();
      int count = pb.readInt();
      TEAMMATES.clear();

      for(int i = 0; i < count && pb.readableBytes() >= 40; ++i) {
         UUID id = new UUID(pb.readLong(), pb.readLong());
         TEAMMATES.put(id, new double[]{pb.readDouble(), pb.readDouble(), pb.readDouble()});
      }

   }

   // ---------------------------------------------------------------------

   public static final class Cooldown {
      public final String message;
      public final long expiresAt;
      public final long totalMs;
      public final int iconId;

      Cooldown(String message, long expiresAt, long totalMs, int iconId) {
         this.message = message;
         this.expiresAt = expiresAt;
         this.totalMs = totalMs;
         this.iconId = iconId;
      }

      public long remainingMs() {
         return Math.max(0L, this.expiresAt - System.currentTimeMillis());
      }

      /** 1.0 at the moment it starts, 0.0 when it runs out. */
      public float fraction() {
         return this.totalMs <= 0L ? 0.0F
               : Math.max(0.0F, Math.min(1.0F, (float)this.remainingMs() / (float)this.totalMs));
      }
   }

   public static final class Waypoint {
      public final String name;
      public final String world;
      public final int color;
      public final int x;
      public final int y;
      public final int z;
      public final boolean forced;
      public final boolean visible;

      Waypoint(String name, String world, int color, int x, int y, int z, boolean forced, boolean visible) {
         this.name = name;
         this.world = world;
         this.color = color;
         this.x = x;
         this.y = y;
         this.z = z;
         this.forced = forced;
         this.visible = visible;
      }
   }
}
