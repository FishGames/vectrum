package io.github.fishgames.vectrum.transfer;

import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/** Kennungen von Items und Fluiden, wie sie im Filter stehen ({@code namespace:pfad}). */
public final class ResourceIds {
    private ResourceIds() {
    }

    /**
     * Gehoert die Kennung zu einer Ware dieses Transporttyps? Items und Fluide werden in ihren Registries
     * nachgeschlagen; Energie und andere Typen kennen keine Kennungen.
     */
    public static boolean belongsTo(TransportType type, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return false;
        }
        if (TransportType.ITEM.equals(type)) {
            return BuiltInRegistries.ITEM.containsKey(location);
        }
        if (TransportType.FLUID.equals(type)) {
            return BuiltInRegistries.FLUID.containsKey(location);
        }
        return false;
    }

    public static String of(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static String of(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid).toString();
    }
}
