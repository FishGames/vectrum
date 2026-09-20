package io.github.fishgames.vectrum.transfer;

import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Predicate;

/** Item and fluid ids as they appear in a filter ({@code namespace:path}). */
public final class ResourceIds {
    private static Predicate<String> gasIds = id -> false;

    private ResourceIds() {
    }

    /** Whether the id names a resource of this transport type (registry lookup; gas via check; otherwise false). */
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
        if (TransportType.GAS.equals(type)) {
            return gasIds.test(id);
        }
        return false;
    }

    /** Sets the check for known gas ids. */
    public static void setGasIds(Predicate<String> check) {
        gasIds = check;
    }

    /** Id of an item. */
    public static String of(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** Id of a fluid. */
    public static String of(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid).toString();
    }
}
