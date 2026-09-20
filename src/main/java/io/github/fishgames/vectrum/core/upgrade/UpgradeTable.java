package io.github.fishgames.vectrum.core.upgrade;

import io.github.fishgames.vectrum.core.network.BlockCoord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Dünn besetzte Tabelle: nur Bausteine mit mindestens einem Upgrade stehen darin. Nicht thread-sicher. */
public final class UpgradeTable {
    private final Map<BlockCoord, Upgrades> entries = new HashMap<>();

    public Upgrades get(BlockCoord pos) {
        return entries.getOrDefault(pos, Upgrades.EMPTY);
    }

    /** @return {@code true}, wenn sich dadurch etwas geändert hat */
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

    /** Entnimmt alle Upgrades des Bausteins (beim Abbauen) und vergisst sie. */
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
