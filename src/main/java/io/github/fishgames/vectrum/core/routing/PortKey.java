package io.github.fishgames.vectrum.core.routing;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.network.Direction;

import java.util.Objects;

/** Eine Anschlussseite: der Netzbaustein an {@code pos} und die Seite, hinter der der Speicher liegt. */
public record PortKey(BlockCoord pos, Direction side) {
    public PortKey {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(side, "side");
    }
}
