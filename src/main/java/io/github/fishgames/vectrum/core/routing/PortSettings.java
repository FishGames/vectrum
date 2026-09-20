package io.github.fishgames.vectrum.core.routing;

import java.util.Objects;

/**
 * Settings of a port side.
 *
 * @param priority higher = served first as target; default 0
 * @param mode     distribution mode when the side is a source
 * @param filter   applies to source (outgoing) and target (incoming)
 */
public record PortSettings(int priority, DistributionMode mode, ResourceFilter filter) {
    public static final PortSettings DEFAULT = new PortSettings(0, DistributionMode.DEFAULT, ResourceFilter.NONE);

    public PortSettings {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(filter, "filter");
    }

    public boolean isDefault() {
        return equals(DEFAULT);
    }

    public PortSettings withPriority(int priority) {
        return new PortSettings(priority, mode, filter);
    }

    public PortSettings withMode(DistributionMode mode) {
        return new PortSettings(priority, mode, filter);
    }

    public PortSettings withFilter(ResourceFilter filter) {
        return new PortSettings(priority, mode, filter);
    }
}
