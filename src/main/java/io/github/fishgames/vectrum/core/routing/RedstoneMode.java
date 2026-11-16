package io.github.fishgames.vectrum.core.routing;

import java.util.Locale;

/** Redstone control of a port side: when the side may transfer. */
public enum RedstoneMode {
    /** Always active. */
    IGNORE,
    /** Active while the block receives a redstone signal. */
    HIGH,
    /** Active while the block receives no redstone signal. */
    LOW;

    public static final RedstoneMode DEFAULT = IGNORE;

    /** Whether the side may transfer given the signal at its block. */
    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORE -> true;
            case HIGH -> powered;
            case LOW -> !powered;
        };
    }

    /** Next mode in the cycle ignore, high, low. */
    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Lower-case name. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Case-insensitive lookup by name; {@code null} when unknown. */
    public static RedstoneMode byId(String id) {
        for (RedstoneMode mode : values()) {
            if (mode.id().equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return null;
    }
}
