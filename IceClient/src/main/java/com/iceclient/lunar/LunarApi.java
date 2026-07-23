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

      // Waypoints. The field numbers below come from Apollo's own
      // WaypointModuleImpl (MIT, Moonsworth): the builder sets them in
      // declaration order, which is what protobuf numbers them by.
      //
      //   DisplayWaypointMessage
      //     1 name          5 hidden          9 style
      //     2 location      6 show_beam
      //     3 color         7 highlight_block
      //     4 prevent_removal  8 highlight_block_line_width
      //
      //   BlockLocation: 1 world, 2 x, 3 y, 4 z
      //   Color:         1 color (packed ARGB int)
      //
      // Everything is read defensively -- a message that does not match is
      // ignored rather than half-applied, and Debug Packets still dumps the
      // real shape so this can be corrected against a live server.
      if("DisplayWaypointMessage".equals(shortName)) {
         readApolloWaypoint(body);
      } else if("RemoveWaypointMessage".equals(shortName)) {
         String n = ProtoReader.string(body, 1);
         if(n != null) {
            WAYPOINTS.remove(n);
         }
      } else if("ResetWaypointsMessage".equals(shortName)) {
         WAYPOINTS.clear();
      } else if("HideWaypointMessage".equals(shortName)
            || "ShowWaypointMessage".equals(shortName)) {
         setVisible(ProtoReader.string(body, 1), !shortName.startsWith("Hide"));
      } else if("DisplayCooldownMessage".equals(shortName)) {
         readApolloCooldown(body);
      } else if("RemoveCooldownMessage".equals(shortName)) {
         String n = ProtoReader.string(body, 1);
         if(n != null) {
            COOLDOWNS.remove(n);
         }
      } else if("ClearCooldownsMessage".equals(shortName)) {
         COOLDOWNS.clear();
      }
   }

   /**
    * A server-sent cooldown -- ability timers, pet summons, anything the server
    * wants counted down on the HUD.
    *
    * <p>Field numbers from Apollo's CooldownModuleImpl (MIT, Moonsworth):
    * DisplayCooldownMessage is 1 name, 2 duration, 3 icon, 4 style, and the
    * duration is a {@code google.protobuf.Duration} of 1 seconds, 2 nanos.
    */
   private static void readApolloCooldown(Map<Integer, List<Object>> body) {
      String name = ProtoReader.string(body, 1);
      byte[] durBytes = ProtoReader.bytes(body, 2);

      if(name == null || durBytes == null) {
         if(debug) {
            IceClient.LOGGER.warn("Apollo: cooldown with no name or duration; ignored");
         }
         return;
      }

      Map<Integer, List<Object>> dur =
            ProtoReader.scan(io.netty.buffer.Unpooled.wrappedBuffer(durBytes));

      long seconds = ProtoReader.number(dur, 1, 0L);
      long nanos = ProtoReader.number(dur, 2, 0L);
      long ms = seconds * 1000L + nanos / 1000000L;

      if(ms <= 0L) {
         COOLDOWNS.remove(name);
         return;
      }

      // Reuses the same Cooldown the legacy channel produces, so the HUD has one
      // source regardless of which protocol the server speaks -- and totalMs
      // comes free, which is what a progress bar needs.
      COOLDOWNS.put(name, new Cooldown(name, System.currentTimeMillis() + ms, ms, 0));

      if(debug) {
         IceClient.LOGGER.info("Apollo: cooldown '" + name + "' for " + ms + "ms");
      }
   }

   /** Rebuilds a waypoint with a new visibility, since Waypoint is immutable. */
   private static void setVisible(String name, boolean visible) {
      Waypoint w = name == null ? null : WAYPOINTS.get(name);
      if(w != null) {
         WAYPOINTS.put(name,
               new Waypoint(w.name, w.world, w.color, w.x, w.y, w.z, w.forced, visible));
      }
   }

   /** Turns a DisplayWaypointMessage body into one of our waypoints. */
   private static void readApolloWaypoint(Map<Integer, List<Object>> body) {
      String name = ProtoReader.string(body, 1);
      byte[] locBytes = ProtoReader.bytes(body, 2);

      if(name == null || locBytes == null) {
         if(debug) {
            IceClient.LOGGER.warn("Apollo: waypoint with no name or location; ignored");
         }
         return;
      }

      Map<Integer, List<Object>> loc =
            ProtoReader.scan(io.netty.buffer.Unpooled.wrappedBuffer(locBytes));

      // MIN_VALUE as the sentinel rather than 0, since 0 is a real coordinate.
      long x = ProtoReader.number(loc, 2, Long.MIN_VALUE);
      long y = ProtoReader.number(loc, 3, Long.MIN_VALUE);
      long z = ProtoReader.number(loc, 4, Long.MIN_VALUE);

      if(x == Long.MIN_VALUE || y == Long.MIN_VALUE || z == Long.MIN_VALUE) {
         if(debug) {
            IceClient.LOGGER.warn("Apollo: waypoint '" + name + "' had no coordinates; ignored");
         }
         return;
      }

      // Signed coordinates arrive zigzag-encoded (sint32), which is how negative
      // values survive a varint. Reading them raw makes every westward waypoint
      // land billions of blocks away.
      int wx = zigzag(x);
      int wy = zigzag(y);
      int wz = zigzag(z);

      int color = 0xFF5CC6FF;
      byte[] colBytes = ProtoReader.bytes(body, 3);
      if(colBytes != null) {
         Map<Integer, List<Object>> col =
               ProtoReader.scan(io.netty.buffer.Unpooled.wrappedBuffer(colBytes));
         long argb = ProtoReader.number(col, 1, Long.MIN_VALUE);
         if(argb != Long.MIN_VALUE) {
            color = (int)argb | 0xFF000000;
         }
      }

      boolean forced = ProtoReader.number(body, 4, 0L) != 0L;   // prevent_removal
      boolean visible = ProtoReader.number(body, 5, 0L) == 0L;  // hidden -> inverted

      WAYPOINTS.put(name, new Waypoint(name, ProtoReader.string(loc, 1),
            color, wx, wy, wz, forced, visible));

      if(debug) {
         IceClient.LOGGER.info("Apollo: waypoint '" + name + "' at "
               + wx + ", " + wy + ", " + wz);
      }
   }

   /**
    * Undoes protobuf's zigzag encoding for signed integers.
    *
    * <p>Values that look implausible are passed through unchanged: some servers
    * send plain int32 rather than sint32, and a coordinate in the millions is a
    * surer sign of the wrong encoding than of a real position.
    */
   private static int zigzag(long v) {
      long dec = (v >>> 1) ^ -(v & 1L);
      return Math.abs(dec) <= 30000000L ? (int)dec : (int)v;
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
         // Hex, not just a byte count. The layouts below were never confirmed,
         // and a count alone cannot tell you whether field three is an int, a
         // long or a string -- the actual bytes can. This is what makes a log
         // from a real server enough to correct the parser from.
         IceClient.LOGGER.info("Lunar (legacy): '" + id + "' " + hex(buf));
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

   /**
    * The still-unread bytes as hex plus their printable form, without consuming
    * them -- reading the buffer here would leave nothing for the real parser.
    */
   private static String hex(ByteBuf buf) {
      int n = Math.min(buf.readableBytes(), 96);
      if(n <= 0) {
         return "(empty)";
      }

      StringBuilder h = new StringBuilder(n * 3);
      StringBuilder t = new StringBuilder(n);

      for(int i = 0; i < n; ++i) {
         int b = buf.getByte(buf.readerIndex() + i) & 255;
         h.append(String.format("%02X ", Integer.valueOf(b)));
         t.append(b >= 32 && b < 127 ? (char)b : '.');
      }

      String more = buf.readableBytes() > n ? " ...+" + (buf.readableBytes() - n) : "";
      return "[" + buf.readableBytes() + "B] " + h.toString().trim() + more + "  |" + t + "|";
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
