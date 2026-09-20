package io.github.fishgames.vectrum.block;

/** Role of a conduit side that touches an inventory (stored in LevelNetworks). */
public enum EndpointMode {
    /** Side switched off. */
    OFF("off"),
    /** Source: inventory to network. */
    IN("in"),
    /** Target: network to inventory. */
    OUT("out");

    /** Default role of new sides: target. */
    public static final EndpointMode DEFAULT = OUT;

    private final String name;

    EndpointMode(String name) {
        this.name = name;
    }

    /** Next role in the wrench cycle: target, source, off. */
    public EndpointMode next() {
        return switch (this) {
            case OUT -> IN;
            case IN -> OFF;
            case OFF -> OUT;
        };
    }

    public String translationKey() {
        return "mode.vectrum." + name;
    }

    public static EndpointMode byOrdinal(int ordinal) {
        EndpointMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DEFAULT;
    }
}
