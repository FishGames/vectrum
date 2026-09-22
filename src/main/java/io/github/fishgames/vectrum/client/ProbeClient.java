package io.github.fishgames.vectrum.client;

import io.github.fishgames.vectrum.net.VectrumNet;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Info lines of looked-at blocks for tooltip mods on the client.
 * <ul>
 *   <li>Asks the server at most every {@link #REFRESH} ms per block.</li>
 *   <li>Keeps the last reply per block for {@link #EXPIRY} ms.</li>
 * </ul>
 */
public final class ProbeClient {
    private static final long REFRESH = 400;
    private static final long EXPIRY = 3000;

    private record Reply(List<Component> lines, long time) {
    }

    private static final Map<Long, Reply> REPLIES = new HashMap<>();
    private static final Map<Long, Long> REQUESTS = new HashMap<>();

    private ProbeClient() {
    }

    /** Registers the handler of server replies. */
    public static void init() {
        VectrumNet.setClientHandler(ProbeClient::onMessage);
    }

    private static void onMessage(ResourceLocation id, FriendlyByteBuf payload) {
        if (VectrumNet.PROBE_REPLY.equals(id)) {
            BlockPos pos = payload.readBlockPos();
            REPLIES.put(pos.asLong(), new Reply(VectrumNet.readReply(payload), Util.getMillis()));
        }
    }

    /** Lines of the block, empty until the first reply arrives. */
    public static List<Component> lines(BlockPos pos, Vec3 hit, Direction face) {
        long now = Util.getMillis();
        long key = pos.asLong();
        Long requested = REQUESTS.get(key);
        if (requested == null || now - requested > REFRESH) {
            REQUESTS.put(key, now);
            VectrumNet.sendProbe(pos, hit, face);
        }
        Reply reply = REPLIES.get(key);
        if (reply == null || now - reply.time() > EXPIRY) {
            REPLIES.remove(key);
            return List.of();
        }
        return reply.lines();
    }
}
