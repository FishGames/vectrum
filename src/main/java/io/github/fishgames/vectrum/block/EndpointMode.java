package io.github.fishgames.vectrum.block;

/**
 * Role of a conduit side that touches an inventory (stored in LevelNetworks by ordinal; the ordinals never change).
 * The player-facing names are Pull ({@link #SOURCE}: the cable takes goods out of the block) and Push
 * ({@link #TARGET}: the cable puts goods into the block).
 */
public enum EndpointMode {
    /** Side switched off. */
    OFF("off"),
    /** Source: goods flow from the block into the network. Shown as "Pull". */
    SOURCE("out"),
    /** Target: goods flow from the network into the block. Shown as "Push". */
    TARGET("in");

    /** Default role of new sides: source. */
    public static final EndpointMode DEFAULT = SOURCE;

    private final String name;

    EndpointMode(String name) {
        this.name = name;
    }

    /** Next role in the wrench cycle: source, target, off. */
    public EndpointMode next() {
        return switch (this) {
            case SOURCE -> TARGET;
            case TARGET -> OFF;
            case OFF -> SOURCE;
        };
    }

    /** Short name used by commands and translation keys: {@code out} (source, shown Pull), {@code in} (target, shown Push), {@code off}. */
    public String shortName() {
        return name;
    }

    public String translationKey() {
        return "mode.vectrum." + name;
    }

    public static EndpointMode byOrdinal(int ordinal) {
        EndpointMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DEFAULT;
    }
}
