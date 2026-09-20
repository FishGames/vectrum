package io.github.fishgames.vectrum.core.routing;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.network.Direction;

import java.util.Objects;

/** Port side: the network block at {@code pos} and the side facing the storage. */
public record PortKey(BlockCoord pos, Direction side) {
    public PortKey {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(side, "side");
    }
}
