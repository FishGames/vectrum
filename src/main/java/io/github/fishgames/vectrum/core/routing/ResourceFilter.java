package io.github.fishgames.vectrum.core.routing;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Filter über Ressourcen-Kennungen (z. B. {@code "minecraft:cobblestone"}). Ein leerer Filter lässt alles durch
 * (Kernentscheidung K7). Sonst gilt je nach Art:
 * <ul>
 *   <li>Positivliste: nur die genannten Kennungen passen.</li>
 *   <li>Negativliste: alles außer den genannten passt.</li>
 * </ul>
 * Unveränderlich; {@code with...}-Methoden liefern eine neue Instanz.
 */
public record ResourceFilter(boolean blacklist, Set<String> ids) {
    /** Leerer Filter: lässt alles durch. */
    public static final ResourceFilter NONE = new ResourceFilter(false, Set.of());

    public ResourceFilter {
        ids = Collections.unmodifiableSet(new TreeSet<>(ids)); // sortiert: stabile Reihenfolge beim Speichern und Anzeigen
    }

    /** {@code true}, wenn nichts eingetragen ist (dann passt alles, egal ob Positiv- oder Negativliste). */
    public boolean isEmpty() {
        return ids.isEmpty();
    }

    /** Darf diese Ressource passieren? */
    public boolean matches(String id) {
        if (ids.isEmpty()) {
            return true;
        }
        return ids.contains(id) != blacklist;
    }

    public ResourceFilter with(String id) {
        Set<String> copy = new TreeSet<>(ids);
        copy.add(id);
        return new ResourceFilter(blacklist, copy);
    }

    public ResourceFilter without(String id) {
        Set<String> copy = new TreeSet<>(ids);
        copy.remove(id);
        return new ResourceFilter(blacklist, copy);
    }

    public ResourceFilter withBlacklist(boolean blacklist) {
        return new ResourceFilter(blacklist, ids);
    }

    /** Leert die Liste, behält aber die Art (Positiv- oder Negativliste). */
    public ResourceFilter cleared() {
        return new ResourceFilter(blacklist, Set.of());
    }
}
