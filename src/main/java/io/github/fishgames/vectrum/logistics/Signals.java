package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Redstone-Netze: Sie uebertragen nur, sie verarbeiten nichts. Der Wert eines Netzes ist die groesste Signalstaerke,
 * die an einem seiner Eingaenge anliegt; jeder Ausgang gibt genau diesen Wert ab (0 bis 15, nichts wird verbraucht).
 *
 * <p>Ereignisgesteuert: Aendert sich etwas in der Umgebung eines Redstone-Bausteins (das meldet Minecraft ueber
 * {@code neighborChanged}), wird der Wert seines Netzes neu bestimmt. Nur wenn er sich aendert, werden die
 * Nachbarn der Ausgaenge benachrichtigt, so dass keine Schleife entsteht. Ein seltener Takt der Bausteine ist nur die
 * Absicherung nach dem Laden der Welt.
 */
public final class Signals {
    private Signals() {
    }

    /** Bestimmt den Wert des Netzes, in dem {@code pos} liegt, und gibt ihn an alle Ausgaenge weiter. */
    public static void update(ServerLevel level, LevelNetworks networks, BlockPos pos) {
        Network network = networks.networkAt(TransportType.REDSTONE, pos);
        if (network == null) {
            return;
        }
        int value = 0;
        List<BlockPos> endpoints = new ArrayList<>();
        List<ConduitBlock> blocks = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        for (BlockCoord coord : network.endpointPositions()) {
            BlockPos endpoint = new BlockPos(coord.x(), coord.y(), coord.z());
            if (!level.hasChunkAt(endpoint)) {
                continue;
            }
            BlockState state = level.getBlockState(endpoint);
            if (state.getBlock() instanceof ConduitBlock block && block.isSignalBlock()) {
                endpoints.add(endpoint);
                blocks.add(block);
                states.add(state);
                value = Math.max(value, inputOf(level, endpoint, state, block));
            }
        }

        // Erst alle Werte speichern, dann melden: Beim Melden lesen andere Bausteine die neuen Werte schon.
        List<Integer> changed = new ArrayList<>();
        for (int i = 0; i < endpoints.size(); i++) {
            int output = hasOutput(states.get(i), blocks.get(i)) ? value : 0;
            if (networks.setSignalOutput(endpoints.get(i), output)) {
                changed.add(i);
            }
        }
        for (int index : changed) {
            level.updateNeighborsAt(endpoints.get(index), blocks.get(index));
        }
    }

    /** Der aktuelle Wert des Netzes, ohne etwas zu veraendern (fuer die Diagnose). */
    public static int valueAt(ServerLevel level, LevelNetworks networks, BlockPos pos) {
        Network network = networks.networkAt(TransportType.REDSTONE, pos);
        if (network == null) {
            return 0;
        }
        int value = 0;
        for (BlockCoord coord : network.endpointPositions()) {
            BlockPos endpoint = new BlockPos(coord.x(), coord.y(), coord.z());
            if (level.hasChunkAt(endpoint) && level.getBlockState(endpoint).getBlock() instanceof ConduitBlock block
                    && block.isSignalBlock()) {
                value = Math.max(value, inputOf(level, endpoint, level.getBlockState(endpoint), block));
            }
        }
        return value;
    }

    /** Anzahl der Eingangs- und Ausgangsseiten aller Bausteine des Netzes: {@code {Eingaenge, Ausgaenge}}. */
    public static int[] countSides(ServerLevel level, Network network) {
        int[] counts = new int[2];
        for (BlockCoord coord : network.endpointPositions()) {
            BlockPos endpoint = new BlockPos(coord.x(), coord.y(), coord.z());
            if (!level.hasChunkAt(endpoint)) {
                continue;
            }
            BlockState state = level.getBlockState(endpoint);
            if (state.getBlock() instanceof ConduitBlock block && block.isSignalBlock()) {
                for (Direction side : Sides.ALL) {
                    Connection connection = block.connection(state, side);
                    if (connection == Connection.INPUT) {
                        counts[0]++;
                    } else if (connection == Connection.OUTPUT) {
                        counts[1]++;
                    }
                }
            }
        }
        return counts;
    }

    private static boolean hasOutput(BlockState state, ConduitBlock block) {
        for (Direction side : Sides.ALL) {
            if (block.connection(state, side) == Connection.OUTPUT) {
                return true;
            }
        }
        return false;
    }

    /** Groesste Signalstaerke, die an den Eingangsseiten dieses Bausteins anliegt. */
    private static int inputOf(ServerLevel level, BlockPos pos, BlockState state, ConduitBlock block) {
        int strongest = 0;
        for (Direction side : Sides.ALL) {
            if (block.connection(state, side) == Connection.INPUT) {
                BlockPos neighbour = pos.relative(side);
                if (level.hasChunkAt(neighbour)) {
                    // getSignal(Nachbar, Richtung vom Leser zum Nachbarn): so liest auch ein Repeater.
                    strongest = Math.max(strongest, level.getSignal(neighbour, side));
                }
            }
        }
        return strongest;
    }
}
