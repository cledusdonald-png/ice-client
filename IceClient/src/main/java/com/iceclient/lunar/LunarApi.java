package com.iceclient.lunar;

import com.iceclient.IceClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client half of the Lunar / Apollo plugin-message protocol.
 *
 * <p>Servers running the Lunar Bukkit API (or Apollo in its legacy-compatible
 * mode) push cooldowns, waypoints and teammate positions down the
 * {@code Lunar-Client} plugin channel. Registering the channel is what tells the
 * server we speak it — without that, most servers never send anything.
 *
 * <p><b>Wire format caveat:</b> the legacy {@code LCPacket} envelope is a
 * length-prefixed packet id followed by per-packet fields. The field order below
 * follows Moonsworth's published BukkitAPI, but it has not been verified against
 * a live server from this codebase. Every read is bounded and wrapped — a
 * mismatch degrades to "packet ignored", never a crash. Turn on
 * {@code Debug Packets} in the LunarClientApi module to dump raw payloads so the
 * parser can be corrected against real traffic.
 */
public final class LunarApi {

    /** 1.8.9 caps plugin channel names at 16 chars; this is the legacy Lunar one. */
    private static final String CHANNEL = "Lunar-Client";

    private static final LunarApi INSTANCE = new LunarApi();
    private static FMLEventChannel channel;

    /** Set by the module so we don't spam the log when nobody asked for it. */
    private static volatile boolean debug;

    private static final Map<String, Cooldown> COOLDOWNS = new LinkedHashMap<>();
    private static final Map<String, Waypoint> WAYPOINTS = new LinkedHashMap<>();
    private static final Map<UUID, double[]> TEAMMATES = new LinkedHashMap<>();

    private LunarApi() {}

    public static void init() {
        if (channel != null) return;
        try {
            channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL);
            channel.register(INSTANCE);
            IceClient.LOGGER.info("Lunar API: registered plugin channel '" + CHANNEL + "'");
        } catch (Throwable t) {
            IceClient.LOGGER.warn("Lunar API: could not register channel — " + t);
        }
    }

    public static void setDebug(boolean on) { debug = on; }

    public static void clear() {
        COOLDOWNS.clear();
        WAYPOINTS.clear();
        TEAMMATES.clear();
    }

    /** Live cooldowns, expired entries dropped. */
    public static List<Cooldown> cooldowns() {
        long now = System.currentTimeMillis();
        COOLDOWNS.values().removeIf(c -> now >= c.expiresAt);
        return new ArrayList<>(COOLDOWNS.values());
    }

    public static List<Waypoint> waypoints() { return new ArrayList<>(WAYPOINTS.values()); }

    public static Map<UUID, double[]> teammates() { return new LinkedHashMap<>(TEAMMATES); }

    @SubscribeEvent
    public void onCustomPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        // 1.8.9 Forge exposes this as a public final field, not a getter.
        ByteBuf raw = event.packet.payload();
        if (raw == null || !raw.isReadable()) return;

        // Copy first: the payload buffer is reused by netty after this returns.
        ByteBuf buf = raw.copy();
        try {
            PacketBuffer pb = new PacketBuffer(buf);
            String id = pb.readStringFromBuffer(64);

            if (debug) {
                IceClient.LOGGER.info("Lunar API: packet '" + id + "' (" + buf.readableBytes() + " bytes left)");
            }

            switch (id) {
                case "cooldown":       readCooldown(pb); break;
                case "waypoint_add":   readWaypointAdd(pb); break;
                case "waypoint_remove":readWaypointRemove(pb); break;
                case "teammates":      readTeammates(pb); break;
                default:
                    if (debug) IceClient.LOGGER.info("Lunar API: unhandled packet id '" + id + "'");
            }
        } catch (Throwable t) {
            // A malformed or unexpected packet must never take the client down.
            if (debug) IceClient.LOGGER.warn("Lunar API: failed to parse packet — " + t);
        } finally {
            buf.release();
        }
    }

    private static void readCooldown(PacketBuffer pb) {
        String message = pb.readStringFromBuffer(256);
        long durationMs = pb.readLong();
        int iconId = pb.readableBytes() >= 4 ? pb.readInt() : 0;
        if (durationMs <= 0) {
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

    private static void readWaypointRemove(PacketBuffer pb) {
        WAYPOINTS.remove(pb.readStringFromBuffer(128));
    }

    private static void readTeammates(PacketBuffer pb) {
        pb.readLong();                       // leader uuid (high)
        pb.readLong();                       // leader uuid (low)
        pb.readLong();                       // lastUpdate
        int count = pb.readInt();
        TEAMMATES.clear();
        for (int i = 0; i < count && pb.readableBytes() >= 40; i++) {
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

        public long remainingMs() { return Math.max(0, expiresAt - System.currentTimeMillis()); }

        /** 1.0 at the moment it starts, 0.0 when it runs out. */
        public float fraction() {
            if (totalMs <= 0) return 0f;
            return Math.max(0f, Math.min(1f, remainingMs() / (float) totalMs));
        }
    }

    public static final class Waypoint {
        public final String name;
        public final String world;
        public final int color;
        public final int x, y, z;
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
