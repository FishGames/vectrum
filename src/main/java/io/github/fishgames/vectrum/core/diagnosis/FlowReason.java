package io.github.fishgames.vectrum.core.diagnosis;

/** Reason why goods flow or do not flow at a port side. */
public enum FlowReason {
    FLOWING("flowing"),
    IDLE("idle"),
    NO_NETWORK("no_network"),
    SIDE_OFF("side_off"),
    NO_TARGET("no_target"),
    NO_SOURCE("no_source"),
    NO_RECEIVER("no_receiver"),
    DIMENSION_LOCKED("dimension_locked"),
    SOURCE_EMPTY("source_empty"),
    TARGET_FULL("target_full"),
    FILTER_BLOCKS_ALL("filter_blocks_all"),
    TARGET_UNLOADED("target_unloaded"),
    LIMIT_REACHED("limit_reached");

    private final String id;

    FlowReason(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** Whether the reason explains a stall. */
    public boolean isProblem() {
        return this != FLOWING && this != IDLE && this != LIMIT_REACHED;
    }
}
