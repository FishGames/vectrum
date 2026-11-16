package io.github.fishgames.vectrum.block;

import net.minecraft.util.StringRepresentable;

/** Block-state value of one side of a conduit block. */
public enum Connection implements StringRepresentable {
    /** Nothing attached, or side switched off. */
    NONE("none"),
    /** Cable or endpoint of the same type (network link). */
    LINK("link"),
    /** Inventory attached, source (pull) side: the cable takes from the block; arm with plate (redstone: plain arm). The serialized name is older than the role names. */
    SOURCE("in"),
    /** Inventory attached, target (push) side: the cable delivers into the block; plain arm (redstone: arm with plate). */
    TARGET("out");

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
