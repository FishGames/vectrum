package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.core.routing.ResourceFilter;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.ResourceIds;

/** Filter helpers shared by transfers and diagnosis. */
public final class Filters {
    private Filters() {
    }

    /** Filter entries of this type only ({@link ResourceFilter#restrictedTo}). */
    public static ResourceFilter forType(TransportType type, ResourceFilter filter) {
        return filter.isEmpty() ? filter : filter.restrictedTo(id -> ResourceIds.belongsTo(type, id));
    }
}
