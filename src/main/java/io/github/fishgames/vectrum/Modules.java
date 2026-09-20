package io.github.fishgames.vectrum;

import io.github.fishgames.vectrum.core.transport.TransportType;

import java.util.ArrayList;
import java.util.List;

/** Optional modules that depend on other mods; every module is off until the loader enables it. */
public final class Modules {
    private static boolean gas;

    private Modules() {
    }

    /** Gas module (Mekanism). */
    public static boolean gas() {
        return gas;
    }

    public static void setGas(boolean enabled) {
        gas = enabled;
    }

    /** Quantity types in order: item, fluid, energy, and gas when the gas module is on. */
    public static List<TransportType> quantityTypes() {
        List<TransportType> types = new ArrayList<>(List.of(TransportType.ITEM, TransportType.FLUID, TransportType.ENERGY));
        if (gas) {
            types.add(TransportType.GAS);
        }
        return List.copyOf(types);
    }
}
