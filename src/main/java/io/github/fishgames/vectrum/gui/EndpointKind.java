package io.github.fishgames.vectrum.gui;

/** Kinds of blocks with an endpoint screen. */
public enum EndpointKind {
    CABLE,
    REDSTONE,
    WIRELESS,
    CODER,
    /** Energy cable: like {@link #CABLE}, but its sides only switch on and off (energy flows both ways). */
    ENERGY_CABLE;

    /** Whether the kind has filter and priority settings. */
    public boolean hasFilter() {
        return this == CABLE || this == ENERGY_CABLE || this == WIRELESS;
    }

    public static EndpointKind byOrdinal(int ordinal) {
        EndpointKind[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CABLE;
    }
}
