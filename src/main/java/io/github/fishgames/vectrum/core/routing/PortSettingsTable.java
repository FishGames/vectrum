package io.github.fishgames.vectrum.core.routing;

import io.github.fishgames.vectrum.core.network.BlockCoord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Sparse table of port settings; {@link #get} returns {@link PortSettings#DEFAULT} for absent sides. */
public final class PortSettingsTable {
    private final Map<PortKey, PortSettings> entries = new HashMap<>();

    public PortSettings get(PortKey key) {
        return entries.getOrDefault(key, PortSettings.DEFAULT);
    }

    /**
     * Stores the settings; default settings remove the entry.
     *
     * @return {@code true} when the stored value changed
     */
    public boolean set(PortKey key, PortSettings settings) {
        Objects.requireNonNull(settings, "settings");
        PortSettings before = get(key);
        if (settings.isDefault()) {
            entries.remove(key);
        } else {
            entries.put(key, settings);
        }
        return !before.equals(settings);
    }

    /** Removes the settings of all sides of the block at {@code pos}. */
    public boolean clear(BlockCoord pos) {
        return entries.keySet().removeIf(key -> key.pos().equals(pos));
    }

    public int size() {
        return entries.size();
    }

    /** All non-default settings. */
    public Map<PortKey, PortSettings> entries() {
        return Collections.unmodifiableMap(entries);
    }
}
