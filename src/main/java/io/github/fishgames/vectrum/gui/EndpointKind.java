package io.github.fishgames.vectrum.gui;

/** Kinds of blocks with an endpoint screen. */
public enum EndpointKind {
    CABLE(true),
    REDSTONE(true),
    WIRELESS(true),
    CODER(false);

    private final boolean sides;

    EndpointKind(boolean sides) {
        this.sides = sides;
    }

    /** Whether the screen shows settings per side. */
    public boolean hasSides() {
        return sides;
    }

    /** Whether the kind has filter and priority settings. */
    public boolean hasFilter() {
        return this == CABLE || this == WIRELESS;
    }

    public static EndpointKind byOrdinal(int ordinal) {
        EndpointKind[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CABLE;
    }
}
