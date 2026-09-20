package io.github.fishgames.vectrum.core.wireless;

/** Wireless port mode per transport type: off, receive, send or both. */
public enum LinkMode {
    OFF("off"),
    RECEIVE("receive"),
    SEND("send"),
    BOTH("both");

    /** Default mode. */
    public static final LinkMode DEFAULT = RECEIVE;

    private final String id;

    LinkMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean sends() {
        return this == SEND || this == BOTH;
    }

    public boolean receives() {
        return this == RECEIVE || this == BOTH;
    }

    /** Next mode in the cycle: receive, send, both, off. */
    public LinkMode next() {
        return switch (this) {
            case RECEIVE -> SEND;
            case SEND -> BOTH;
            case BOTH -> OFF;
            case OFF -> RECEIVE;
        };
    }

    public static LinkMode byId(String id) {
        for (LinkMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return null;
    }
}
