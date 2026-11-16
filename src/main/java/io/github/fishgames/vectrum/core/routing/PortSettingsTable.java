package io.github.fishgames.vectrum.core.routing;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.network.Direction;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Sparse table of port settings, indexed by block; {@link #get} returns {@link PortSettings#DEFAULT} for absent
 * sides.
 */
public final class PortSettingsTable {
    private final Map<BlockCoord, PortSettings[]> byBlock = new HashMap<>();
    private int size;

    public PortSettings get(PortKey key) {
        PortSettings[] sides = byBlock.get(key.pos());
        PortSettings stored = sides == null ? null : sides[key.side().ordinal()];
        return stored == null ? PortSettings.DEFAULT : stored;
    }

    /**
     * Stores the settings; default settings remove the entry.
     *
     * @return {@code true} when the stored value changed
     */
    public boolean set(PortKey key, PortSettings settings) {
        Objects.requireNonNull(settings, "settings");
        PortSettings before = get(key);
        int side = key.side().ordinal();
        PortSettings[] sides = byBlock.get(key.pos());
        if (settings.isDefault()) {
            if (sides != null && sides[side] != null) {
                sides[side] = null;
                size--;
                if (isEmpty(sides)) {
                    byBlock.remove(key.pos());
                }
            }
        } else {
            if (sides == null) {
                sides = new PortSettings[Direction.VALUES.length];
                byBlock.put(key.pos(), sides);
            }
            if (sides[side] == null) {
                size++;
            }
            sides[side] = settings;
        }
        return !before.equals(settings);
    }

    /** Removes the settings of all sides of the block at {@code pos}. */
    public boolean clear(BlockCoord pos) {
        PortSettings[] sides = byBlock.remove(pos);
        if (sides == null) {
            return false;
        }
        for (PortSettings stored : sides) {
            if (stored != null) {
                size--;
            }
        }
        return true;
    }

    public int size() {
        return size;
    }

    /** All non-default settings. */
    public Map<PortKey, PortSettings> entries() {
        Map<PortKey, PortSettings> result = new LinkedHashMap<>();
        for (Map.Entry<BlockCoord, PortSettings[]> block : byBlock.entrySet()) {
            for (int side = 0; side < block.getValue().length; side++) {
                PortSettings stored = block.getValue()[side];
                if (stored != null) {
                    result.put(new PortKey(block.getKey(), Direction.VALUES[side]), stored);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static boolean isEmpty(PortSettings[] sides) {
        for (PortSettings stored : sides) {
            if (stored != null) {
                return false;
            }
        }
        return true;
    }
}
