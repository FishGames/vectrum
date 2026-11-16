package io.github.fishgames.vectrum.core.upgrade;

import io.github.fishgames.vectrum.core.network.BlockCoord;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Sparse table of upgrades per port; only blocks with at least one upgrade are stored.
 * A cable has one set per side (slots 0 to 5); a device without sides, such as the wireless port, uses
 * {@link #BLOCK}.
 */
public final class UpgradeTable {
    /** Slot of the upgrades that belong to the block itself. */
    public static final int BLOCK = 6;
    /** Number of slots per block: six sides and the block. */
    public static final int SLOTS = 7;

    private final Map<BlockCoord, Upgrades[]> entries = new HashMap<>();

    public Upgrades get(BlockCoord pos, int slot) {
        Upgrades[] sets = entries.get(pos);
        return sets == null ? Upgrades.EMPTY : sets[slot];
    }

    /** @return {@code true} when the stored value changed */
    public boolean set(BlockCoord pos, int slot, Upgrades upgrades) {
        Objects.requireNonNull(upgrades, "upgrades");
        Upgrades before = get(pos, slot);
        if (before.equals(upgrades)) {
            return false;
        }
        Upgrades[] sets = entries.get(pos);
        if (sets == null) {
            sets = new Upgrades[SLOTS];
            Arrays.fill(sets, Upgrades.EMPTY);
            entries.put(pos, sets);
        }
        sets[slot] = upgrades;
        if (upgrades.isEmpty() && isEmpty(sets)) {
            entries.remove(pos);
        }
        return true;
    }

    /** Removes the upgrades of all slots of the block and returns the total count per type. */
    public int[] take(BlockCoord pos) {
        int[] totals = new int[UpgradeType.VALUES.length];
        Upgrades[] removed = entries.remove(pos);
        if (removed != null) {
            for (Upgrades set : removed) {
                for (UpgradeType type : UpgradeType.VALUES) {
                    totals[type.ordinal()] += set.count(type);
                }
            }
        }
        return totals;
    }

    public int size() {
        return entries.size();
    }

    /** Stored sets per block, {@link #SLOTS} entries each. */
    public Map<BlockCoord, Upgrades[]> entries() {
        return Collections.unmodifiableMap(entries);
    }

    private static boolean isEmpty(Upgrades[] sets) {
        for (Upgrades set : sets) {
            if (!set.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
