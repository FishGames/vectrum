package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.config.VectrumConfig;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.transfer.RunLimit;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import io.github.fishgames.vectrum.world.WirelessRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-over round of a wireless port.
 * <ul>
 * <li>Per sending type: take from the adjacent storages, give to all receivers of the same frequency (any dimension).</li>
 * <li>Throughput and item types are unlimited ({@link WirelessBlock#UNLIMITED}); the {@link WorkBudget} of the tick
 * and the time limit of the run ({@link RunLimit}) still apply.</li>
 * <li>Routing cascade: filter, priority, distribution mode ({@link Transport}).</li>
 * </ul>
 */
public final class WirelessTransport {
    /** Result of {@link #run} for a block that sends nothing. */
    public static final long NOT_SENDING = -1;

    private WirelessTransport() {
    }

    /** @return units moved, or {@link #NOT_SENDING} when the block sends no type */
    public static long run(ServerLevel level, LevelNetworks networks, BlockPos pos) {
        WirelessRegistry registry = WirelessRegistry.get(level);
        if (!registry.contains(level, pos)) {
            registry.register(level, pos);
        }
        int frequency = registry.frequency(level, pos);
        RunLimit.start(VectrumConfig.get().runBudgetMicros());
        long moved = 0;
        boolean sending = false;
        for (TransportType type : WirelessBlock.TYPES) {
            if (!registry.mode(level, pos, type).sends()) {
                continue;
            }
            sending = true;
            List<List<Target>> groups = registry.receivers(level.getServer(), type, frequency, level,
                    networks.blockUpgrades(pos)).groups();
            if (groups.isEmpty()) {
                continue;
            }
            Map<Target, Port> destinations = null;
            for (Direction side : Sides.ALL) {
                BlockPos sourcePos = pos.relative(side);
                if (!level.hasChunkAt(sourcePos)) {
                    continue;
                }
                if (RunLimit.expired()) {
                    break;
                }
                Port source = Ports.find(type, level, sourcePos, side.getOpposite());
                if (source == null) {
                    continue;
                }
                if (destinations == null) {
                    destinations = new IdentityHashMap<>();
                }
                moved += Transport.distribute(level, networks, type, pos, side, source, sourcePos, groups,
                        WirelessBlock.UNLIMITED, destinations, networks.effectiveSettings(pos, side, true).mode(),
                        networks.typedFilter(type, pos, side, true), Integer.MAX_VALUE);
            }
        }
        return sending ? moved : NOT_SENDING;
    }
}
