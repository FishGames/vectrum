package io.github.fishgames.vectrum.core.routing;

import java.util.Locale;

/** Distribution mode of a source across targets of equal priority. */
public enum DistributionMode {
    /** Fill the first target completely, then the next. */
    SEQUENTIAL,
    /** Each transfer starts after the target served first in the previous transfer. */
    ROUND_ROBIN,
    /** Emptier targets receive larger shares. */
    BALANCED;

    public static final DistributionMode DEFAULT = SEQUENTIAL;

    /** Lower-case name. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Case-insensitive lookup by name; {@code null} when unknown. */
    public static DistributionMode byId(String id) {
        for (DistributionMode mode : values()) {
            if (mode.id().equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return null;
    }
}
