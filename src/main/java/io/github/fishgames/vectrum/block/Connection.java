package io.github.fishgames.vectrum.block;

import net.minecraft.util.StringRepresentable;

/** Block-state value of one side of a conduit block. */
public enum Connection implements StringRepresentable {
    /** Nothing attached, or side switched off. */
    NONE("none"),
    /** Cable or endpoint of the same type (network link). */
    LINK("link"),
    /** Inventory attached, source side. */
    INPUT("in"),
    /** Inventory attached, target side. */
    OUTPUT("out");

    private final String name;

    Connection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
