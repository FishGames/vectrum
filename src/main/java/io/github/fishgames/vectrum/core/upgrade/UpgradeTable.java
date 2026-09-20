package io.github.fishgames.vectrum.core.upgrade;

import io.github.fishgames.vectrum.core.network.BlockCoord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Sparse table of upgrades per block; only blocks with at least one upgrade are stored. */
public final class UpgradeTable {
    private final Map<BlockCoord, Upgrades> entries = new HashMap<>();

    public Upgrades get(BlockCoord pos) {
        return entries.getOrDefault(pos, Upgrades.EMPTY);
    }

    /** @return {@code true} when the stored value changed */
    public boolean set(BlockCoord pos, Upgrades upgrades) {
        Objects.requireNonNull(upgrades, "upgrades");
        Upgrades before = get(pos);
        if (upgrades.isEmpty()) {
            entries.remove(pos);
        } else {
            entries.put(pos, upgrades);
        }
        return !before.equals(upgrades);
    }

    /** Removes and returns the upgrades of the block. */
    public Upgrades take(BlockCoord pos) {
        Upgrades removed = entries.remove(pos);
        return removed == null ? Upgrades.EMPTY : removed;
    }

    public int size() {
        return entries.size();
    }

    public Map<BlockCoord, Upgrades> entries() {
        return Collections.unmodifiableMap(entries);
    }
}
