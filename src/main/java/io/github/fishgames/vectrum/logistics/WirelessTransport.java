package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.routing.PortSettings;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import io.github.fishgames.vectrum.world.WirelessRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-over round of a wireless port.
 * <ul>
 * <li>Per sending type: take from the adjacent storages, give to all receivers of the same frequency (any dimension).</li>
 * <li>Throughput and item types are unlimited ({@link WirelessBlock#UNLIMITED}).</li>
 * <li>Routing cascade: filter, priority, distribution mode ({@link Transport}).</li>
 * </ul>
 */
public final class WirelessTransport {
    private WirelessTransport() {
    }

    /** @return {@code true} if the block sends at least one type */
    public static boolean run(ServerLevel level, BlockPos pos) {
        WirelessRegistry registry = WirelessRegistry.get(level);
        if (!registry.contains(level, pos)) {
            registry.register(level, pos);
        }
        int frequency = registry.frequency(level, pos);
        LevelNetworks networks = LevelNetworks.get(level);
        boolean sending = false;
        for (TransportType type : WirelessBlock.TYPES) {
            if (!registry.mode(level, pos, type).sends()) {
                continue;
            }
            sending = true;
            List<Target> targets = registry.receivers(level.getServer(), type, frequency, level,
                    networks.upgrades(pos)).targets();
            if (targets.isEmpty()) {
                continue;
            }
            Map<Target, Port> destinations = new HashMap<>();
            for (Direction side : Sides.ALL) {
                BlockPos sourcePos = pos.relative(side);
                if (!level.hasChunkAt(sourcePos)) {
                    continue;
                }
                Port source = Ports.find(type, level, sourcePos, side.getOpposite());
                if (source == null) {
                    continue;
                }
                PortSettings own = networks.effectiveSettings(pos, side, true);
                Transport.distribute(level, networks, type, pos, side, source, sourcePos, targets,
                        WirelessBlock.UNLIMITED, destinations, own, Integer.MAX_VALUE);
            }
        }
        return sending;
    }
}
