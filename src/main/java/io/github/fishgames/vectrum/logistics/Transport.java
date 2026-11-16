package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.core.config.VectrumConfig;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.core.routing.DistributionMode;
import io.github.fishgames.vectrum.core.routing.RedstoneMode;
import io.github.fishgames.vectrum.core.routing.ResourceFilter;
import io.github.fishgames.vectrum.core.routing.Router;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.transfer.RunLimit;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * Quantity transport (items, fluids, energy, gas): direct hand-over from source sides to target sides of the same
 * network (energy: a push side is both); storage access goes through {@link Ports}.
 * <ul>
 * <li>Routing cascade: filter, priority, distribution mode ({@link Router}).</li>
 * <li>Source filter and target filter must both match; priority is a target setting, distribution mode a source
 * setting.</li>
 * <li>A source that holds nothing the filters let through moves nothing and costs no target lookups.</li>
 * <li>A target that refuses goods the source could give is skipped for a while ({@link LevelNetworks#noteMiss}).</li>
 * <li>Work stops for the tick when the {@link WorkBudget} is spent, and a run stops when its own time limit is spent
 * ({@link RunLimit}); the rest of the goods waits for the next run.</li>
 * </ul>
 */
public final class Transport {
    private Transport() {
    }

    /**
     * One hand-over round for a block.
     * <ul>
     * <li>1. for each quantity type of the block: look up network, throughput and target groups</li>
     * <li>2. for each source side that is due: distribute up to the throughput of that side</li>
     * </ul>
     *
     * @return units moved over all types and sides
     */
    public static long run(ServerLevel level, LevelNetworks networks, BlockPos pos, BlockState state,
                           ConduitBlock block) {
        if (!block.hasTransportSide(state)) {
            return 0;
        }
        RunLimit.start(VectrumConfig.get().runBudgetMicros());
        // Sides that are due now; each side waits for its own interval after a run
        long now = level.getGameTime();
        int due = 0;
        for (Direction side : Sides.ALL) {
            Connection connection = block.connection(state, side);
            if ((connection == Connection.SOURCE || connection == Connection.TARGET)
                    && networks.sideDue(pos, side, now)) {
                due |= 1 << side.get3DDataValue();
            }
        }
        if (due == 0) {
            return 0;
        }
        // Redstone control: a side with a mode other than ignore only runs while the signal allows it
        Boolean powered = null;
        for (Direction side : Sides.ALL) {
            int bit = 1 << side.get3DDataValue();
            if ((due & bit) == 0) {
                continue;
            }
            RedstoneMode redstone = networks.effectiveSettings(pos, side).redstone();
            if (redstone == RedstoneMode.IGNORE) {
                continue;
            }
            if (powered == null) {
                powered = level.hasNeighborSignal(pos);
            }
            if (!redstone.allows(powered)) {
                due &= ~bit;
            }
        }
        if (due == 0) {
            return 0;
        }
        long moved = 0;
        for (TransportType type : block.transportTypes()) {
            if (type.behavior() == TransportType.Behavior.QUANTITY) {
                moved += run(level, networks, type, pos, state, block, due);
            }
        }
        for (Direction side : Sides.ALL) {
            if ((due & (1 << side.get3DDataValue())) != 0) {
                networks.noteSideRun(pos, side, now);
            }
        }
        return moved;
    }

    private static long run(ServerLevel level, LevelNetworks networks, TransportType type, BlockPos pos,
                            BlockState state, ConduitBlock block, int due) {
        Network network = networks.networkAt(type, pos);
        if (network == null) {
            return 0;
        }
        List<List<Target>> groups = networks.reachableGroups(level, type, network);
        if (groups.isEmpty()) {
            return 0;
        }

        Map<Target, Port> destinations = null;
        long moved = 0;
        for (Direction side : Sides.ALL) {
            Connection connection = block.connection(state, side);
            // a push side also takes energy out of its storage
            if (connection != Connection.SOURCE
                    && !(connection == Connection.TARGET && TransportType.ENERGY.equals(type))) {
                continue;
            }
            if (RunLimit.expired()) {
                break;
            }
            // Every port side has its own interval and throughput (its upgrades)
            if ((due & (1 << side.get3DDataValue())) == 0) {
                continue;
            }
            long budget = networks.throughput(type, pos, side);
            if (budget <= 0) {
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
            if (destinations == null) {
                destinations = new IdentityHashMap<>();
            }
            moved += distribute(level, networks, type, pos, side, source, sourcePos, groups, budget, destinations,
                    networks.effectiveSettings(pos, side).mode(), networks.typedFilter(type, pos, side, false),
                    networks.maxTypes(pos, side));
        }
        return moved;
    }

    /**
     * Distributes the goods of one source side to the targets (also used by wireless blocks).
     * <ul>
     * <li>1. nothing to give: record the attempt and return</li>
     * <li>2. run the routing cascade over the target groups</li>
     * <li>3. per target: skip same storage, unloaded chunks and sleeping targets, then move</li>
     * <li>4. record hits and misses</li>
     * </ul>
     *
     * @param mode         distribution mode of the source side
     * @param sourceFilter filter of the source side, reduced to the transport type
     * @param maxTypes     item types per target
     * @return units moved
     */
    static long distribute(ServerLevel level, LevelNetworks networks, TransportType type, BlockPos pos,
                           Direction side, Port source, BlockPos sourcePos, List<List<Target>> groups,
                           long budget, Map<Target, Port> destinations, DistributionMode mode,
                           ResourceFilter sourceFilter, int maxTypes) {
        long now = level.getGameTime();
        Predicate<String> giveFilter = sourceFilter.isEmpty() ? Port.ALL : sourceFilter::matches;
        if (!source.canGive(giveFilter)) {
            networks.noteTransfer(pos, side, type, now, budget, 0);
            return 0;
        }

        Round round = new Round(level, networks, type, pos, side, source, sourcePos, sourceFilter, giveFilter,
                maxTypes, now, destinations);
        long moved = Router.distributeGroups(budget, groups, round, mode, round, round);
        networks.noteTransfer(pos, side, type, now, budget, moved);
        return moved;
    }

    /** State of one source side during a round; also the mover, pointer and fill level callbacks. */
    private static final class Round implements Router.Mover<Target>, Router.Pointer, ToDoubleFunction<Target> {
        private final ServerLevel level;
        private final LevelNetworks networks;
        private final TransportType type;
        private final BlockPos pos;
        private final Direction side;
        private final Port source;
        private final BlockPos sourcePos;
        private final ResourceFilter sourceFilter;
        private final Predicate<String> giveFilter;
        private final int maxTypes;
        private final long now;
        private final Map<Target, Port> destinations;
        private boolean stopped;

        Round(ServerLevel level, LevelNetworks networks, TransportType type, BlockPos pos, Direction side,
              Port source, BlockPos sourcePos, ResourceFilter sourceFilter, Predicate<String> giveFilter,
              int maxTypes, long now, Map<Target, Port> destinations) {
            this.level = level;
            this.networks = networks;
            this.type = type;
            this.pos = pos;
            this.side = side;
            this.source = source;
            this.sourcePos = sourcePos;
            this.sourceFilter = sourceFilter;
            this.giveFilter = giveFilter;
            this.maxTypes = maxTypes;
            this.now = now;
            this.destinations = destinations;
        }

        @Override
        public long move(Target target, long max) {
            if (stopped) {
                return 0;
            }
            ServerLevel targetLevel = target.level();
            if ((targetLevel == level && target.inventory().equals(sourcePos))
                    || !targetLevel.hasChunkAt(target.inventory())) {
                return 0;
            }
            if (WorkBudget.exhausted() || RunLimit.expired()) {
                stopped = true;
                return 0;
            }
            if (target.settings().redstone() != RedstoneMode.IGNORE
                    && !target.settings().redstone().allows(targetLevel.hasNeighborSignal(target.endpoint()))) {
                return 0;
            }
            if (networks.isSleeping(type, pos, side, target, now)) {
                return 0;
            }
            Port destination = destination(target);
            if (destination == null) {
                return 0;
            }
            ResourceFilter targetFilter = target.filter();
            Predicate<String> filter = giveFilter;
            if (!targetFilter.isEmpty()) {
                filter = sourceFilter.isEmpty() ? targetFilter::matches
                        : id -> sourceFilter.matches(id) && targetFilter.matches(id);
                if (!source.canGive(filter)) {
                    return 0;
                }
            }
            long moved = source.moveTo(destination, max, filter, maxTypes);
            RunLimit.progress();
            if (moved == 0 && RunLimit.expired()) {
                // cut short, not refused
                stopped = true;
                return 0;
            }
            if (moved > 0) {
                networks.noteHit(type, pos, side, target);
                LevelNetworks.get(targetLevel).noteReceived(target.endpoint(), target.side(), type, now);
            } else {
                networks.noteMiss(type, pos, side, target, now);
            }
            return moved;
        }

        @Override
        public int get() {
            return networks.pointer(pos, side);
        }

        @Override
        public void set(int value) {
            networks.setPointer(pos, side, value);
        }

        @Override
        public double applyAsDouble(Target target) {
            Port port = destination(target);
            return port == null ? 1.0 : port.fillLevel();
        }

        private Port destination(Target target) {
            Port port = destinations.get(target);
            if (port == null && !destinations.containsKey(target)) {
                port = target.level().hasChunkAt(target.inventory())
                        ? Ports.find(type, target.level(), target.inventory(), target.side().getOpposite())
                        : null;
                destinations.put(target, port);
            }
            return port;
        }
    }
}
