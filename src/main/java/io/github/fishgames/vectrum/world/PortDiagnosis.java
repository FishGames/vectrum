package io.github.fishgames.vectrum.world;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.block.EndpointMode;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.diagnosis.Diagnosis;
import io.github.fishgames.vectrum.core.diagnosis.FlowReason;
import io.github.fishgames.vectrum.core.diagnosis.SourceFacts;
import io.github.fishgames.vectrum.core.diagnosis.TargetFacts;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.core.routing.PortSettings;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.wireless.LinkMode;
import io.github.fishgames.vectrum.logistics.Filters;
import io.github.fishgames.vectrum.logistics.Target;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Flow diagnosis of port sides and wireless ports.
 * <ul>
 * <li>{@link #ofSide}: one entry per transport type with a storage at the side</li>
 * <li>{@link #ofWireless}: one entry per transport type whose link mode is not off</li>
 * <li>{@link #text}: reasons as a translatable line</li>
 * </ul>
 */
public final class PortDiagnosis {
    /** Ticks within which a transfer or delivery counts as recent. */
    private static final long RECENT = 100;
    /** Fill level from which a storage counts as full. */
    private static final double FULL = 0.999;

    private PortDiagnosis() {
    }

    /** Reasons for one transport type. */
    public record Entry(TransportType type, List<FlowReason> reasons) {
        public boolean hasProblem() {
            return reasons.stream().anyMatch(FlowReason::isProblem);
        }
    }

    /** Translatable line: {@code <type>: <reason>, <reason>}. */
    public static MutableComponent text(Entry entry) {
        MutableComponent line = Component.translatable("type.vectrum." + shortName(entry.type())).append(": ");
        for (int i = 0; i < entry.reasons().size(); i++) {
            if (i > 0) {
                line.append(", ");
            }
            line.append(Component.translatable("diagnosis.vectrum." + entry.reasons().get(i).id()));
        }
        return line;
    }

    private static String shortName(TransportType type) {
        return type.id().substring(type.id().indexOf(':') + 1);
    }

    /**
     * Diagnosis of a side of a cable or endpoint.
     * <ul>
     * <li>1. no storage at the side or a link side: empty list</li>
     * <li>2. role off: {@link FlowReason#SIDE_OFF}</li>
     * <li>3. role input: {@link Diagnosis#forSource}; role output: {@link Diagnosis#forTarget}</li>
     * </ul>
     */
    public static List<Entry> ofSide(ServerLevel level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        List<Entry> result = new ArrayList<>();
        if (!(state.getBlock() instanceof ConduitBlock conduit) || conduit.isSignalBlock()) {
            return result;
        }
        Connection connection = conduit.connection(state, side);
        BlockPos neighbour = pos.relative(side);
        if (connection == Connection.LINK || !level.hasChunkAt(neighbour)) {
            return result;
        }
        LevelNetworks networks = LevelNetworks.get(level);
        EndpointMode mode = conduit.effectiveMode(networks, level, pos, side);
        for (TransportType type : conduit.transportTypes()) {
            Port port = Ports.find(type, level, neighbour, side.getOpposite());
            if (port == null) {
                continue;
            }
            List<FlowReason> reasons;
            if (mode == EndpointMode.OFF) {
                reasons = List.of(FlowReason.SIDE_OFF);
            } else if (mode == EndpointMode.IN) {
                reasons = Diagnosis.forSource(sourceFacts(level, networks, type, pos, side, neighbour, port));
            } else {
                reasons = Diagnosis.forTarget(targetFacts(level, networks, type, pos, side, port));
            }
            result.add(new Entry(type, reasons));
        }
        return result;
    }

    private static SourceFacts sourceFacts(ServerLevel level, LevelNetworks networks, TransportType type,
                                           BlockPos pos, Direction side, BlockPos sourcePos, Port source) {
        Network network = networks.networkAt(type, pos);
        long now = level.getGameTime();
        LevelNetworks.TransferRecord last = networks.lastTransfer(pos, side, type);
        boolean recent = last != null && now - last.tick() <= RECENT;
        long moved = recent ? last.moved() : 0;
        long budget = recent ? last.budget() : 0;
        boolean known = TransportType.ITEM.equals(type) || TransportType.FLUID.equals(type)
                || TransportType.ENERGY.equals(type);
        boolean empty = known && source.fillLevel() <= 0;
        if (network == null) {
            return new SourceFacts(false, empty, 0, 0, 0, 0, 0, 0, false, moved, budget, recent);
        }
        PortSettings own = networks.effectiveSettings(pos, side);
        int[] counts = classify(level, type, sourcePos, own, networks.reachableTargets(level, type, network));
        int unloaded = counts[4] + networks.unloadedEndpoints(level, type, network);
        return new SourceFacts(true, empty, counts[0], counts[1], counts[2], unloaded, counts[3], 0, false,
                moved, budget, recent);
    }

    /** Counts targets by state: open, full, filtered, missing, unloaded. */
    private static int[] classify(ServerLevel level, TransportType type, BlockPos sourcePos, PortSettings own,
                                  List<Target> targets) {
        int[] counts = new int[5];
        for (Target target : targets) {
            if (target.level() == level && target.inventory().equals(sourcePos)) {
                continue;
            }
            if (!target.level().hasChunkAt(target.inventory())) {
                counts[4]++;
                continue;
            }
            Port destination = Ports.find(type, target.level(), target.inventory(), target.side().getOpposite());
            if (destination == null) {
                counts[3]++;
            } else if (Diagnosis.passNothing(Filters.forType(type, own.filter()),
                    Filters.forType(type, target.settings().filter()))) {
                counts[2]++;
            } else if (destination.fillLevel() >= FULL) {
                counts[1]++;
            } else {
                counts[0]++;
            }
        }
        return counts;
    }

    private static TargetFacts targetFacts(ServerLevel level, LevelNetworks networks, TransportType type,
                                           BlockPos pos, Direction side, Port own) {
        Network network = networks.networkAt(type, pos);
        long received = networks.lastReceived(pos, side, type);
        boolean recent = received >= 0 && level.getGameTime() - received <= RECENT;
        int sources = network == null ? 0 : networks.sourceCount(level, type, network);
        return new TargetFacts(network != null, sources, own.fillLevel() >= FULL, recent);
    }

    /**
     * Diagnosis of a wireless port: one entry per type with a link mode other than off.
     * <ul>
     * <li>sending or both: {@link Diagnosis#forSource} over the receivers of the frequency</li>
     * <li>receiving only: {@link FlowReason#NO_SOURCE} without another sender, otherwise recent deliveries</li>
     * </ul>
     */
    public static List<Entry> ofWireless(ServerLevel level, BlockPos pos) {
        List<Entry> result = new ArrayList<>();
        if (!(level.getBlockState(pos).getBlock() instanceof WirelessBlock)) {
            return result;
        }
        WirelessRegistry registry = WirelessRegistry.get(level);
        LevelNetworks networks = LevelNetworks.get(level);
        int frequency = registry.frequency(level, pos);
        long now = level.getGameTime();
        for (TransportType type : WirelessBlock.TYPES) {
            LinkMode mode = registry.mode(level, pos, type);
            if (mode == LinkMode.OFF) {
                result.add(new Entry(type, List.of(FlowReason.SIDE_OFF)));
                continue;
            }
            if (mode.sends()) {
                result.add(new Entry(type, Diagnosis.forSource(wirelessSource(level, registry, networks, type, pos,
                        frequency, now))));
                continue;
            }
            int senders = registry.senderCount(frequency, type, WirelessRegistry.coord(level, pos));
            boolean recent = false;
            for (Direction side : Sides.ALL) {
                long received = networks.lastReceived(pos, side, type);
                recent |= received >= 0 && now - received <= RECENT;
            }
            result.add(new Entry(type, Diagnosis.forTarget(new TargetFacts(true, senders, false, recent))));
        }
        return result;
    }

    private static SourceFacts wirelessSource(ServerLevel level, WirelessRegistry registry, LevelNetworks networks,
                                              TransportType type, BlockPos pos, int frequency, long now) {
        WirelessRegistry.Receivers receivers = registry.receivers(level.getServer(), type, frequency, level,
                networks.upgrades(pos));
        boolean anySource = false;
        boolean allEmpty = true;
        long moved = 0;
        boolean recent = false;
        boolean known = TransportType.ITEM.equals(type) || TransportType.FLUID.equals(type)
                || TransportType.ENERGY.equals(type);
        for (Direction side : Sides.ALL) {
            BlockPos neighbour = pos.relative(side);
            if (!level.hasChunkAt(neighbour)) {
                continue;
            }
            Port port = Ports.find(type, level, neighbour, side.getOpposite());
            if (port != null) {
                anySource = true;
                allEmpty &= known && port.fillLevel() <= 0;
            }
            LevelNetworks.TransferRecord last = networks.lastTransfer(pos, side, type);
            if (last != null && now - last.tick() <= RECENT) {
                recent = true;
                moved += last.moved();
                
            }
        }
        int[] counts = classify(level, type, pos, PortSettings.DEFAULT, receivers.targets());
        int unloaded = counts[4] + receivers.unloaded();
        return new SourceFacts(true, anySource && allEmpty, counts[0], counts[1], counts[2], unloaded, counts[3],
                receivers.locked(), true, moved, 0, recent);
    }
}
