package io.github.fishgames.vectrum.core.routing;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Filter over resource ids (for example {@code "minecraft:cobblestone"}). An empty filter passes everything.
 * <ul>
 *   <li>Whitelist: only the listed ids pass.</li>
 *   <li>Blacklist: everything except the listed ids passes.</li>
 * </ul>
 * Immutable.
 */
public record ResourceFilter(boolean blacklist, Set<String> ids) {
    /** Empty filter. */
    public static final ResourceFilter NONE = new ResourceFilter(false, Set.of());

    public ResourceFilter {
        ids = Collections.unmodifiableSet(new TreeSet<>(ids));
    }

    /** Whether no id is listed. */
    public boolean isEmpty() {
        return ids.isEmpty();
    }

    /** Whether the resource passes. */
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

    /** Filter reduced to the ids matching {@code relevant}; {@link #NONE} when none remain. */
    public ResourceFilter restrictedTo(Predicate<String> relevant) {
        if (ids.isEmpty()) {
            return this;
        }
        Set<String> kept = new TreeSet<>();
        for (String id : ids) {
            if (relevant.test(id)) {
                kept.add(id);
            }
        }
        return kept.isEmpty() ? NONE : new ResourceFilter(blacklist, kept);
    }

    /** Same list type with no ids. */
    public ResourceFilter cleared() {
        return new ResourceFilter(blacklist, Set.of());
    }
}
