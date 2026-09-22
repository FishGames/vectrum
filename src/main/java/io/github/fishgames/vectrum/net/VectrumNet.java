package io.github.fishgames.vectrum.net;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.gui.EndpointMenu;
import io.github.fishgames.vectrum.transfer.ResourceIds;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.world.ProbeLines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Messages between client and server; the loaders provide the transport.
 * <ul>
 *   <li>{@link #FILTER_ID} (client to server): filter entry dropped from a recipe viewer.</li>
 *   <li>{@link #PROBE} (client to server): block position and hit point of a tooltip mod look.</li>
 *   <li>{@link #PROBE_REPLY} (server to client): position and info lines.</li>
 * </ul>
 */
public final class VectrumNet {
    public static final ResourceLocation FILTER_ID = Vectrum.id("filter_id");
    public static final ResourceLocation PROBE = Vectrum.id("probe");
    public static final ResourceLocation PROBE_REPLY = Vectrum.id("probe_reply");

    /** Longest accepted filter id. */
    private static final int MAX_ID_LENGTH = 256;
    /** Maximum distance in blocks of a probe. */
    private static final double PROBE_RANGE = 12;
    /** Minimum milliseconds between two probes of one player. */
    private static final long PROBE_PAUSE = 150;

    /** Sends a message to the server (client side). */
    public interface ClientSender {
        void send(ResourceLocation id, FriendlyByteBuf payload);
    }

    /** Sends a message to a player (server side). */
    public interface ServerSender {
        void send(ServerPlayer player, ResourceLocation id, FriendlyByteBuf payload);
    }

    private static ClientSender clientSender = (id, payload) -> {
    };
    private static ServerSender serverSender = (player, id, payload) -> {
    };
    private static BiConsumer<ResourceLocation, FriendlyByteBuf> clientHandler = (id, payload) -> {
    };
    private static final Map<UUID, Long> LAST_PROBE = new HashMap<>();

    private VectrumNet() {
    }

    public static void setClientSender(ClientSender sender) {
        clientSender = sender;
    }

    public static void setServerSender(ServerSender sender) {
        serverSender = sender;
    }

    /** Sets the handler of messages received on the client. */
    public static void setClientHandler(BiConsumer<ResourceLocation, FriendlyByteBuf> handler) {
        clientHandler = handler;
    }

    // Client side

    /** Sends a message to the server. */
    public static void sendToServer(ResourceLocation id, Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf payload = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        writer.accept(payload);
        clientSender.send(id, payload);
    }

    /** Requests the info lines of a block. */
    public static void sendProbe(BlockPos pos, Vec3 hit, Direction face) {
        sendToServer(PROBE, payload -> {
            payload.writeBlockPos(pos);
            payload.writeDouble(hit.x);
            payload.writeDouble(hit.y);
            payload.writeDouble(hit.z);
            payload.writeByte(face.get3DDataValue());
        });
    }

    /** Sends a filter entry for the open endpoint screen. */
    public static void sendFilterId(String id) {
        sendToServer(FILTER_ID, payload -> payload.writeUtf(id, MAX_ID_LENGTH));
    }

    /** Called by the loader on the client thread. */
    public static void receiveOnClient(ResourceLocation id, FriendlyByteBuf payload) {
        clientHandler.accept(id, payload);
    }

    // Server side

    /** Called by the loader on the server thread. */
    public static void receiveOnServer(ServerPlayer player, ResourceLocation id, FriendlyByteBuf payload) {
        if (FILTER_ID.equals(id)) {
            String entry = payload.readUtf(MAX_ID_LENGTH);
            if (player.containerMenu instanceof EndpointMenu menu && isFilterId(entry)) {
                menu.addFilterId(entry);
            }
        } else if (PROBE.equals(id)) {
            BlockPos pos = payload.readBlockPos();
            Vec3 hit = new Vec3(payload.readDouble(), payload.readDouble(), payload.readDouble());
            Direction face = Direction.from3DDataValue(payload.readByte());
            probe(player, pos, hit, face);
        }
    }

    private static boolean isFilterId(String id) {
        return ResourceLocation.tryParse(id) != null && (ResourceIds.belongsTo(TransportType.ITEM, id)
                || ResourceIds.belongsTo(TransportType.FLUID, id) || ResourceIds.belongsTo(TransportType.GAS, id));
    }

    private static void probe(ServerPlayer player, BlockPos pos, Vec3 hit, Direction face) {
        long now = System.currentTimeMillis();
        Long last = LAST_PROBE.put(player.getUUID(), now);
        if (last != null && now - last < PROBE_PAUSE) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!level.hasChunkAt(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > PROBE_RANGE * PROBE_RANGE) {
            return;
        }
        List<Component> lines = new ArrayList<>(ProbeLines.of(level, pos, hit, face));
        serverSender.send(player, PROBE_REPLY, encodeReply(pos, lines));
    }

    private static FriendlyByteBuf encodeReply(BlockPos pos, List<Component> lines) {
        FriendlyByteBuf payload = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        payload.writeBlockPos(pos);
        payload.writeVarInt(lines.size());
        for (Component line : lines) {
            payload.writeComponent(line);
        }
        return payload;
    }

    /** Reads a reply written by the server. */
    public static List<Component> readReply(FriendlyByteBuf payload) {
        int count = payload.readVarInt();
        List<Component> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(payload.readComponent());
        }
        return lines;
    }
}
