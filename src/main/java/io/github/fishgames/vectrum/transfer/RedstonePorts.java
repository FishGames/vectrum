package io.github.fishgames.vectrum.transfer;

import io.github.fishgames.vectrum.block.NetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Redstone ports: a content-free marker for every neighbour that is neither air nor a network block, and the
 * detection of blocks that process redstone signals.
 */
public final class RedstonePorts {
    private static final Port MARKER = (target, max, filter) -> 0;

    private RedstonePorts() {
    }

    public static Port find(Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getBlock() instanceof NetworkBlock ? null : MARKER;
    }

    /**
     * Whether the block reads a redstone signal.
     * <ul>
     *   <li>Known classes: lamp, TNT, piston base, command block, repeater, comparator, redstone wire.</li>
     *   <li>Block states with the property powered, triggered or enabled (doors, trapdoors, gates, rails, note
     *       blocks, bells, dispensers, droppers, hoppers).</li>
     * </ul>
     */
    public static boolean processesSignal(BlockState state) {
        Block block = state.getBlock();
        return block instanceof RedstoneLampBlock || block instanceof TntBlock || block instanceof PistonBaseBlock
                || block instanceof CommandBlock || block instanceof DiodeBlock || block instanceof RedStoneWireBlock
                || state.hasProperty(BlockStateProperties.POWERED) || state.hasProperty(BlockStateProperties.TRIGGERED)
                || state.hasProperty(BlockStateProperties.ENABLED);
    }
}
