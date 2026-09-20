package io.github.fishgames.vectrum.core.routing;

import io.github.fishgames.vectrum.core.network.BlockCoord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Rundlaufzeiger je Quelle: Jede Quelle führt ihren eigenen Zeiger, mehrere Quellen im selben Netz beeinflussen
 * sich nicht. Nur Zeiger ungleich 0 werden gespeichert.
 */
public final class RoundRobinPointers {
    private final Map<PortKey, Integer> pointers = new HashMap<>();

    public int get(PortKey key) {
        return pointers.getOrDefault(key, 0);
    }

    public void set(PortKey key, int value) {
        if (value == 0) {
            pointers.remove(key);
        } else {
            pointers.put(key, value);
        }
    }

    /** Vergisst die Zeiger aller Seiten dieses Bausteins (beim Abbauen). */
    public boolean clear(BlockCoord pos) {
        return pointers.keySet().removeIf(key -> key.pos().equals(pos));
    }

    public Map<PortKey, Integer> entries() {
        return Collections.unmodifiableMap(pointers);
    }
}
