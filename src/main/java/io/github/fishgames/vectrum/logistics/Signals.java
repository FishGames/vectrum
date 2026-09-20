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
 * Redstone network signals: network value and output propagation.
 * <ul>
 * <li>Network value: strongest signal at any input side of its blocks (0 to 15).</li>
 * <li>Every output side emits the network value.</li>
 * <li>Recalculated on {@code neighborChanged} of a signal block; output neighbours are notified when a stored output
 * changes.</li>
 * </ul>
 */
public final class Signals {
    private Signals() {
    }

    /**
     * Recalculates the network at {@code pos} and applies the value to all outputs.
     * <ul>
     * <li>1. collect signal blocks and the strongest input</li>
     * <li>2. store the output value of every block</li>
     * <li>3. notify neighbours of blocks whose output changed</li>
     * </ul>
     */
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

        // 2. store outputs
        List<Integer> changed = new ArrayList<>();
        for (int i = 0; i < endpoints.size(); i++) {
            int output = hasOutput(states.get(i), blocks.get(i)) ? value : 0;
            if (networks.setSignalOutput(endpoints.get(i), output)) {
                changed.add(i);
            }
        }
        // 3. notify neighbours
        for (int index : changed) {
            level.updateNeighborsAt(endpoints.get(index), blocks.get(index));
        }
    }

    /** Current network value; read-only. */
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

    /** Input and output side counts of the network: {@code {inputs, outputs}}. */
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

    /** Strongest signal at the input sides of this block. */
    private static int inputOf(ServerLevel level, BlockPos pos, BlockState state, ConduitBlock block) {
        int strongest = 0;
        for (Direction side : Sides.ALL) {
            if (block.connection(state, side) == Connection.INPUT) {
                BlockPos neighbour = pos.relative(side);
                if (level.hasChunkAt(neighbour)) {
                    strongest = Math.max(strongest, level.getSignal(neighbour, side));
                }
            }
        }
        return strongest;
    }
}
