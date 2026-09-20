package io.github.fishgames.vectrum.core.network;

import java.util.Objects;

/** Block position with dimension id (for example {@code "minecraft:overworld"}). */
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
