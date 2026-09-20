package io.github.fishgames.vectrum.transfer;

import java.util.function.Predicate;

/** Item inventory at a block side (a {@link Port} for items). */
public interface ItemPort extends Port {
    @Override
    long moveTo(Port target, long max, Predicate<String> filter, int maxTypes);

    /** Move without a limit on the number of item types. */
    @Override
    default long moveTo(Port target, long max, Predicate<String> filter) {
        return moveTo(target, max, filter, Integer.MAX_VALUE);
    }
}
