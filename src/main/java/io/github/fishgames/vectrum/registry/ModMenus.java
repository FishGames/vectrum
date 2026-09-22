package io.github.fishgames.vectrum.registry;

import io.github.fishgames.vectrum.gui.EndpointMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

/** Menu types. The loader provides the factory before registration. */
public final class ModMenus {
    private static Supplier<MenuType<EndpointMenu>> endpointFactory = () -> {
        throw new IllegalStateException("No menu factory set");
    };

    public static final Registered<MenuType<EndpointMenu>> ENDPOINT =
            Registration.register(Registries.MENU, "endpoint", () -> endpointFactory.get());

    private ModMenus() {
    }

    /** Sets the loader-specific creation of the endpoint menu type. */
    public static void setEndpointFactory(Supplier<MenuType<EndpointMenu>> factory) {
        endpointFactory = factory;
    }

    public static void init() {
    }
}
