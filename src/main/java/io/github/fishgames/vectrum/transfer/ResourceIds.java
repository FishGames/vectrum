package io.github.fishgames.vectrum.transfer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/** Kennungen von Items und Fluiden, wie sie im Filter stehen ({@code namespace:pfad}). */
public final class ResourceIds {
    private ResourceIds() {
    }

    public static String of(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static String of(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid).toString();
    }
}
