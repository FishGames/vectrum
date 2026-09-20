package io.github.fishgames.vectrum.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Finds the storage of one transport type at a position. */
@FunctionalInterface
public interface PortFinder {
    /**
     * @param side side of the <em>storage block</em> that is accessed
     * @return the port, or {@code null} when there is no matching storage
     */
    Port find(Level level, BlockPos pos, Direction side);
}
