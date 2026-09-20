package io.github.fishgames.vectrum.core.throughput;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.util.SaturatedMath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Throughput limits of the port blocks of one network layer.
 *
 * <ul>
 *   <li>Each block has one stored limit (units per transfer).</li>
 *   <li>Blocks without an entry use the base limit of the layer.</li>
 *   <li>Only blocks with a differing value are stored.</li>
 *   <li>{@link #limitOf} is a single map lookup.</li>
 *   <li>Values are {@code long}; {@link #budgetOf} clamps to {@code int}.</li>
 * </ul>
 */
public final class ThroughputLimits {
    private final long base;
    private final Map<BlockCoord, Long> overrides = new HashMap<>();

    /**
     * @param base base limit in units per transfer, not negative
     */
    public ThroughputLimits(long base) {
        if (base < 0) {
            throw new IllegalArgumentException("Base limit must not be negative: " + base);
        }
        this.base = base;
    }

    /** Base limit of the layer. */
    public long base() {
        return base;
    }

    /** Limit of the block: its own value or the base limit. */
    public long limitOf(BlockCoord pos) {
        Long own = overrides.get(pos);
        return own == null ? base : own;
    }

    /** Limit clamped to a non-negative {@code int}. */
    public int budgetOf(BlockCoord pos) {
        return SaturatedMath.clampToNonNegativeInt(limitOf(pos));
    }

    /**
     * Sets the limit of a block; a value equal to the base limit removes the entry.
     *
     * @return {@code true} when the limit changed
     * @throws IllegalArgumentException for a negative value
     */
    public boolean set(BlockCoord pos, long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must not be negative: " + limit);
        }
        long before = limitOf(pos);
        if (limit == base) {
            overrides.remove(pos);
        } else {
            overrides.put(pos, limit);
        }
        return before != limit;
    }

    /** Resets the block to the base limit. */
    public boolean reset(BlockCoord pos) {
        return overrides.remove(pos) != null;
    }

    /** Number of blocks with their own value. */
    public int overrideCount() {
        return overrides.size();
    }

    /** All own values. */
    public Map<BlockCoord, Long> overrides() {
        return Collections.unmodifiableMap(overrides);
    }
}
