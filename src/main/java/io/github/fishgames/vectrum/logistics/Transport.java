package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.core.routing.PortSettings;
import io.github.fishgames.vectrum.core.routing.ResourceFilter;
import io.github.fishgames.vectrum.core.routing.Router;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Quantity transport (items, fluids, energy, gas): direct hand-over from source sides (input) to target sides (output)
 * of the same network; storage access goes through {@link Ports}.
 * <ul>
 * <li>Routing cascade: filter, priority, distribution mode ({@link Router}).</li>
 * <li>Source filter and target filter must both match; priority is a target setting, distribution mode a source
 * setting.</li>
 * <li>A target that accepts nothing while the source delivers elsewhere is skipped for a while
 * ({@link LevelNetworks#noteMiss}).</li>
 * </ul>
 */
public final class Transport {
    private Transport() {
    }

    /**
     * One hand-over round for a block.
     * <ul>
     * <li>1. for each quantity type of the block: look up network, throughput and target list</li>
     * <li>2. for each source side: distribute up to the throughput</li>
     * </ul>
     */
    public static void run(ServerLevel level, BlockPos pos, BlockState state, ConduitBlock block) {
        if (!block.hasSource(state)) {
            return;
        }
        LevelNetworks networks = LevelNetworks.get(level);
        for (TransportType type : block.transportTypes()) {
            if (type.behavior() != TransportType.Behavior.QUANTITY) {
                continue;
            }
            run(level, networks, type, pos, state, block);
        }
    }

    private static void run(ServerLevel level, LevelNetworks networks, TransportType type, BlockPos pos,
                            BlockState state, ConduitBlock block) {
        Network network = networks.networkAt(type, pos);
        if (network == null) {
            return;
        }
        // Throughput of the source block
        long budget = networks.throughput(type, pos);
        if (budget <= 0) {
            return;
        }
        List<Target> targets = networks.reachableTargets(level, type, network);
        if (targets.isEmpty()) {
            return;
        }

        // Destination ports
        Map<Target, Port> destinations = new HashMap<>();
        for (Direction side : Sides.ALL) {
            if (block.connection(state, side) != Connection.INPUT) {
                continue;
            }
            BlockPos sourcePos = pos.relative(side);
            if (!level.hasChunkAt(sourcePos)) {
                continue;
            }
            Port source = Ports.find(type, level, sourcePos, side.getOpposite());
            if (source == null) {
                continue;
            }
            distribute(level, networks, type, pos, side, source, sourcePos, targets, budget, destinations,
                    networks.effectiveSettings(pos, side), networks.maxTypes(pos));
        }
    }

    /**
     * Distributes the goods of one source side to the targets (also used by wireless blocks).
     * <ul>
     * <li>1. run the routing cascade over the targets</li>
     * <li>2. per target: skip same storage, unloaded chunks and sleeping targets, then move</li>
     * <li>3. record hits and misses</li>
     * </ul>
     *
     * @param own      effective settings of the source side (filter, distribution mode)
     * @param maxTypes item types per target
     */
    static void distribute(ServerLevel level, LevelNetworks networks, TransportType type, BlockPos pos,
                           Direction side, Port source, BlockPos sourcePos, List<Target> targets,
                           long budget, Map<Target, Port> destinations, PortSettings own, int maxTypes) {
        ResourceFilter sourceFilter = forType(type, own.filter());
        long now = level.getGameTime();

        Set<Target> asked = new HashSet<>();
        Set<Target> accepted = new HashSet<>();

        Router.Pointer pointer = new Router.Pointer() {
            @Override
            public int get() {
                return networks.pointer(pos, side);
            }

            @Override
            public void set(int value) {
                networks.setPointer(pos, side, value);
            }
        };

        long moved = Router.distribute(budget, targets,
                target -> target.settings().priority(),
                target -> fillLevel(level, type, target, destinations),
                own.mode(), pointer,
                (target, max) -> {
                    // 2. skip check
                    if ((target.level() == level && target.inventory().equals(sourcePos))
                            || !target.level().hasChunkAt(target.inventory())
                            || networks.isSleeping(pos, side, target, now)) {
                        return 0;
                    }
                    Port destination = destination(level, type, target, destinations);
                    if (destination == null) {
                        return 0;
                    }
                    asked.add(target);
                    long result = source.moveTo(destination, max, combine(sourceFilter, forType(type, target.settings().filter())), maxTypes);
                    if (result > 0) {
                        accepted.add(target);
                    }
                    return result;
                });

        // 3. hits and misses
        networks.noteTransfer(pos, side, type, now, budget, moved);
        for (Target target : accepted) {
            LevelNetworks.get(target.level()).noteReceived(target.endpoint(), target.side(), type, now);
        }
        if (moved > 0) {
            for (Target target : asked) {
                if (accepted.contains(target)) {
                    networks.noteHit(pos, side, target);
                } else {
                    networks.noteMiss(pos, side, target, now);
                }
            }
        }
    }

    private static ResourceFilter forType(TransportType type, ResourceFilter filter) {
        return Filters.forType(type, filter);
    }

    /** Combined source and target filter. */
    private static Predicate<String> combine(ResourceFilter source, ResourceFilter target) {
        if (source.isEmpty() && target.isEmpty()) {
            return Port.ALL;
        }
        return id -> source.matches(id) && target.matches(id);
    }

    private static Port destination(ServerLevel level, TransportType type, Target target,
                                    Map<Target, Port> destinations) {
        if (destinations.containsKey(target)) {
            return destinations.get(target);
        }
        Port port = target.level().hasChunkAt(target.inventory())
                ? Ports.find(type, target.level(), target.inventory(), target.side().getOpposite())
                : null;
        destinations.put(target, port);
        return port;
    }

    private static double fillLevel(ServerLevel level, TransportType type, Target target,
                                    Map<Target, Port> destinations) {
        Port port = destination(level, type, target, destinations);
        return port == null ? 1.0 : port.fillLevel();
    }
}
