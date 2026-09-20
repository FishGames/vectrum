package io.github.fishgames.vectrum.transfer;

import java.util.function.Predicate;

/**
 * Loader-independent access to a storage at a block side (item inventory, fluid tank, energy storage).
 *
 * <p>Units per transport type: items in pieces, fluids in millibuckets (mB), energy in FE.
 *
 * <p>The filter receives resource ids (e.g. {@code "minecraft:cobblestone"}), see {@link ResourceIds}. Energy has no
 * resource types and ignores the filter.
 */
public interface Port {
    /** Filter that accepts everything. */
    Predicate<String> ALL = id -> true;

    /**
     * Moves up to {@code max} units from this port into {@code target}, limited to resources accepted by
     * {@code filter}.
     *
     * @return number of units actually moved
     */
    long moveTo(Port target, long max, Predicate<String> filter);

    /**
     * Like {@link #moveTo(Port, long, Predicate)}, limited to {@code maxTypes} distinct resource types (items only;
     * fluids and energy ignore the value).
     */
    default long moveTo(Port target, long max, Predicate<String> filter, int maxTypes) {
        return moveTo(target, max, filter);
    }

    /** Move without a filter. */
    default long moveTo(Port target, long max) {
        return moveTo(target, max, ALL);
    }

    /** Fill level from 0 (empty) to 1 (full); 0 when not determinable. */
    default double fillLevel() {
        return 0;
    }
}
