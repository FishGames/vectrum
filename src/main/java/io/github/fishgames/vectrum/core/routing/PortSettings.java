package io.github.fishgames.vectrum.core.routing;

import java.util.Objects;

/**
 * Settings of a port side.
 *
 * @param priority higher = served first as target; default 0
 * @param mode     distribution mode when the side is a source
 * @param filter   applies to source (outgoing) and target (incoming)
 * @param redstone redstone control of the side
 * @param name     name given by the player; empty = the default name of the block
 */
public record PortSettings(int priority, DistributionMode mode, ResourceFilter filter, RedstoneMode redstone,
                           String name) {
    /** Longest name a player can give. */
    public static final int MAX_NAME_LENGTH = 24;

    public static final PortSettings DEFAULT = new PortSettings(0, DistributionMode.DEFAULT, ResourceFilter.NONE,
            RedstoneMode.DEFAULT, "");

    public PortSettings {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(redstone, "redstone");
        name = normalize(name);
    }

    /** Trimmed and cut to {@link #MAX_NAME_LENGTH}; {@code null} becomes empty. */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.strip();
        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH).strip() : trimmed;
    }

    public boolean isDefault() {
        return equals(DEFAULT);
    }

    public PortSettings withPriority(int priority) {
        return new PortSettings(priority, mode, filter, redstone, name);
    }

    public PortSettings withMode(DistributionMode mode) {
        return new PortSettings(priority, mode, filter, redstone, name);
    }

    public PortSettings withFilter(ResourceFilter filter) {
        return new PortSettings(priority, mode, filter, redstone, name);
    }

    public PortSettings withRedstone(RedstoneMode redstone) {
        return new PortSettings(priority, mode, filter, redstone, name);
    }

    public PortSettings withName(String name) {
        return new PortSettings(priority, mode, filter, redstone, name);
    }
}
