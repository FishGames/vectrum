package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.core.config.VectrumConfig;
import io.github.fishgames.vectrum.core.transport.TransportType;

/** Base amount per transfer and unit key per transport type. */
public final class TransportDefaults {
    private TransportDefaults() {
    }

    /** Configured amount per transfer and source side of the layer with this id, without upgrades. */
    public static long baseThroughput(String layerId) {
        return VectrumConfig.get().baseAmount(layerId);
    }

    /** Translation key of the unit name of a type. */
    public static String unitKey(TransportType type) {
        String path = type.id().substring(type.id().indexOf(':') + 1);
        return "unit.vectrum." + path;
    }
}
