package io.github.fishgames.vectrum.world;

import net.minecraft.core.Direction;

/**
 * Helpers for the six block sides. Order ({@code get3DDataValue}): down, up, north, south, west, east; identical
 * to {@code core.network.Direction}.
 */
public final class Sides {
    /** All sides. */
    public static final Direction[] ALL = Direction.values();

    private Sides() {
    }

    /** Side-mask bit of a side. */
    public static int bit(Direction side) {
        return 1 << side.get3DDataValue();
    }
}
