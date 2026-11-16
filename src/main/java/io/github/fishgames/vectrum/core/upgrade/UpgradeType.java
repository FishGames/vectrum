package io.github.fishgames.vectrum.core.upgrade;

import io.github.fishgames.vectrum.core.config.VectrumConfig;

import java.util.Locale;

/** Upgrade types with their maximum count per block. */
public enum UpgradeType {
    /** Amount per transfer multiplied by the configured factor. */
    THROUGHPUT(false),
    /** Interval per transfer taken from the configured table. */
    SPEED(false),
    /** More item types per transfer. */
    TYPES(false),
    /** Unlocks the filter. */
    FILTER(false),
    /** Unlocks the priority setting. */
    PRIORITY(false),
    /** Wireless port only: allows links across dimensions. */
    DIMENSION(true);

    public static final UpgradeType[] VALUES = values();

    private final boolean wireless;

    UpgradeType(boolean wireless) {
        this.wireless = wireless;
    }

    /** {@code true}: fits the wireless port only; {@code false}: fits cables and endpoints only. */
    public boolean wireless() {
        return wireless;
    }

    /** Maximum number that can be installed. */
    public int maxCount() {
        VectrumConfig config = VectrumConfig.get();
        return switch (this) {
            case THROUGHPUT -> config.maxThroughputUpgrades();
            case SPEED -> config.maxSpeedUpgrades();
            case TYPES -> config.maxTypeUpgrades();
            case FILTER, PRIORITY, DIMENSION -> 1;
        };
    }

    /** Lower-case name. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Case-insensitive lookup by name; {@code null} when unknown. */
    public static UpgradeType byId(String id) {
        for (UpgradeType type : VALUES) {
            if (type.id().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
