package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.core.routing.PortSettings;
import io.github.fishgames.vectrum.core.routing.ResourceFilter;
import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * Routing target: port side {@code side} of the network block {@code endpoint} in {@code level}, facing the storage at
 * {@code inventory}, with a snapshot of its port {@code settings} and the part of their filter that applies to the
 * transport type ({@code filter}). Two targets are equal when level, endpoint and side are equal.
 */
public record Target(ServerLevel level, BlockPos endpoint, Direction side, BlockPos inventory, PortSettings settings,
                     ResourceFilter filter) {
    public static Target of(TransportType type, ServerLevel level, BlockPos endpoint, Direction side,
                            BlockPos inventory, PortSettings settings) {
        return new Target(level, endpoint, side, inventory, settings, Filters.forType(type, settings.filter()));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Target that && level == that.level && side == that.side
                && endpoint.equals(that.endpoint);
    }

    @Override
    public int hashCode() {
        return (31 * System.identityHashCode(level) + endpoint.hashCode()) * 31 + side.ordinal();
    }
}
