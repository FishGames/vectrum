package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.core.transport.TransportType;

/**
 * Base throughput per transport type: amount per hand-over and source side without upgrades (units:
 * {@link io.github.fishgames.vectrum.transfer.Port}; interval: {@link io.github.fishgames.vectrum.block.ConduitBlock#INTERVAL}).
 */
public final class TransportDefaults {
    /** Items: 4 per hand-over. */
    public static final long ITEM_THROUGHPUT = 4;
    /** Fluids: 1000 mB per hand-over. */
    public static final long FLUID_THROUGHPUT = 1000;
    /** Energy: 2000 FE per hand-over. */
    public static final long ENERGY_THROUGHPUT = 2000;

    /** Gases (Mekanism): 1000 mB per hand-over. */
    public static final long GAS_THROUGHPUT = 1000;

    private TransportDefaults() {
    }

    /** Base throughput of the network layer with this id; unknown layers use the item value. */
    public static long baseThroughput(String layerId) {
        if (TransportType.FLUID.id().equals(layerId)) {
            return FLUID_THROUGHPUT;
        }
        if (TransportType.ENERGY.id().equals(layerId)) {
            return ENERGY_THROUGHPUT;
        }
        if (TransportType.GAS.id().equals(layerId)) {
            return GAS_THROUGHPUT;
        }
        return ITEM_THROUGHPUT;
    }

    /** Translation key of the unit name of a type. */
    public static String unitKey(TransportType type) {
        String path = type.id().substring(type.id().indexOf(':') + 1);
        return "unit.vectrum." + path;
    }
}
