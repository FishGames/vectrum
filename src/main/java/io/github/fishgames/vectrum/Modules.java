package io.github.fishgames.vectrum;

import io.github.fishgames.vectrum.core.transport.TransportType;

import java.util.ArrayList;
import java.util.List;

/** Optional modules that depend on other mods; every module is off until the loader enables it. */
public final class Modules {
    private static boolean gas;
    private static boolean source;
    private static boolean heat;

    private Modules() {
    }

    /** Gas module (Mekanism). */
    public static boolean gas() {
        return gas;
    }

    public static void setGas(boolean enabled) {
        gas = enabled;
    }

    /** Source module (Ars Nouveau). */
    public static boolean source() {
        return source;
    }

    public static void setSource(boolean enabled) {
        source = enabled;
    }

    /** Heat module (Mekanism). */
    public static boolean heat() {
        return heat;
    }

    public static void setHeat(boolean enabled) {
        heat = enabled;
    }

    /** Whether the module of an optional type is on; {@code false} for the types without a module. */
    public static boolean enabled(TransportType type) {
        if (TransportType.GAS.equals(type)) {
            return gas;
        }
        if (TransportType.SOURCE.equals(type)) {
            return source;
        }
        return TransportType.HEAT.equals(type) && heat;
    }

    /** Types of the modules that are on, in the order gas, source, heat. */
    public static List<TransportType> moduleTypes() {
        List<TransportType> types = new ArrayList<>();
        for (TransportType type : List.of(TransportType.GAS, TransportType.SOURCE, TransportType.HEAT)) {
            if (enabled(type)) {
                types.add(type);
            }
        }
        return List.copyOf(types);
    }

    /** Quantity types in order: item, fluid, energy, then gas, source and heat when their modules are on. */
    public static List<TransportType> quantityTypes() {
        List<TransportType> types = new ArrayList<>(List.of(TransportType.ITEM, TransportType.FLUID, TransportType.ENERGY));
        types.addAll(moduleTypes());
        return List.copyOf(types);
    }
}
