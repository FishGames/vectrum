package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.core.routing.PortSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * Routing target: port side {@code side} of the network block {@code endpoint} in {@code level}, facing the storage at
 * {@code inventory}, with a snapshot of its port {@code settings}.
 */
public record Target(ServerLevel level, BlockPos endpoint, Direction side, BlockPos inventory, PortSettings settings) {
}
