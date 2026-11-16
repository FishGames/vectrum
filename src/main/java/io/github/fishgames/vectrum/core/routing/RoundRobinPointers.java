package io.github.fishgames.vectrum.core.routing;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.network.Direction;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Round-robin pointer per source port, indexed by block; only non-zero pointers are stored. */
public final class RoundRobinPointers {
    private final Map<BlockCoord, int[]> byBlock = new HashMap<>();

    public int get(PortKey key) {
        int[] sides = byBlock.get(key.pos());
        return sides == null ? 0 : sides[key.side().ordinal()];
    }

    public void set(PortKey key, int value) {
        int[] sides = byBlock.get(key.pos());
        if (value == 0) {
            if (sides != null) {
                sides[key.side().ordinal()] = 0;
                if (isEmpty(sides)) {
                    byBlock.remove(key.pos());
                }
            }
            return;
        }
        if (sides == null) {
            sides = new int[Direction.VALUES.length];
            byBlock.put(key.pos(), sides);
        }
        sides[key.side().ordinal()] = value;
    }

    /** Removes the pointers of all sides of the block at {@code pos}. */
    public boolean clear(BlockCoord pos) {
        return byBlock.remove(pos) != null;
    }

    public Map<PortKey, Integer> entries() {
        Map<PortKey, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<BlockCoord, int[]> block : byBlock.entrySet()) {
            for (int side = 0; side < block.getValue().length; side++) {
                if (block.getValue()[side] != 0) {
                    result.put(new PortKey(block.getKey(), Direction.VALUES[side]), block.getValue()[side]);
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static boolean isEmpty(int[] sides) {
        for (int value : sides) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }
}
