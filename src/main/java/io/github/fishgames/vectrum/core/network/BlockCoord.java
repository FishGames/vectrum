package io.github.fishgames.vectrum.core.network;

import java.util.Objects;

/**
 * Leichtgewichtige Position im Kern. Enthält die Dimension (z. B. {@code "minecraft:overworld"}), damit
 * gleiche Koordinaten in verschiedenen Dimensionen nie verwechselt werden.
 */
public record BlockCoord(String dimension, int x, int y, int z) {
    public BlockCoord {
        Objects.requireNonNull(dimension, "dimension");
    }

    public BlockCoord offset(Direction direction) {
        return new BlockCoord(dimension, x + direction.dx(), y + direction.dy(), z + direction.dz());
    }

    @Override
    public String toString() {
        return dimension + "@" + x + "," + y + "," + z;
    }
}
