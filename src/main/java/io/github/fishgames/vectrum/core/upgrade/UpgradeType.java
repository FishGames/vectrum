package io.github.fishgames.vectrum.core.upgrade;

import java.util.Locale;

/** Upgrade types with their maximum count per block. */
public enum UpgradeType {
    /** Throughput limit times 4 per upgrade. */
    THROUGHPUT(6),
    /** Transfer interval halved per upgrade. */
    SPEED(3),
    /** One more item type per transfer per upgrade. */
    TYPES(4),
    /** Unlocks the filter. */
    FILTER(1),
    /** Unlocks the priority setting. */
    PRIORITY(1),
    /** Wireless port only: allows links across dimensions. */
    DIMENSION(1, true);

    public static final UpgradeType[] VALUES = values();

    private final int maxCount;
    private final boolean wireless;

    UpgradeType(int maxCount) {
        this(maxCount, false);
    }

    UpgradeType(int maxCount, boolean wireless) {
        this.maxCount = maxCount;
        this.wireless = wireless;
    }

    /** {@code true}: fits the wireless port only; {@code false}: fits cables and endpoints only. */
    public boolean wireless() {
        return wireless;
    }

    public int maxCount() {
        return maxCount;
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
