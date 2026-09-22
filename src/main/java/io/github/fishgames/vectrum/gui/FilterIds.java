package io.github.fishgames.vectrum.gui;

import io.github.fishgames.vectrum.transfer.ResourceIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;

/** Conversion between filter ids and the stacks shown in filter slots. */
public final class FilterIds {
    private static Map<Item, Fluid> buckets;

    private FilterIds() {
    }

    /** Fluid held by a full bucket item, or {@code null}. */
    public static Fluid fluidOf(ItemStack stack) {
        if (buckets == null) {
            Map<Item, Fluid> found = new HashMap<>();
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                Item bucket = fluid.getBucket();
                if (bucket != Items.AIR && fluid.isSource(fluid.defaultFluidState())) {
                    found.putIfAbsent(bucket, fluid);
                }
            }
            buckets = found;
        }
        return buckets.get(stack.getItem());
    }

    /**
     * Filter id of a stack: the fluid id for a full bucket when {@code asFluid}, otherwise the item id.
     *
     * @return the id, or {@code null} for an empty stack
     */
    public static String idOf(ItemStack stack, boolean asFluid) {
        if (stack.isEmpty()) {
            return null;
        }
        if (asFluid) {
            Fluid fluid = fluidOf(stack);
            if (fluid != null) {
                return ResourceIds.of(fluid);
            }
        }
        return ResourceIds.of(stack.getItem());
    }

    /** Stack shown for an id: the item, a bucket of the fluid, or a named barrier for other ids. */
    public static ItemStack stackOf(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location != null && BuiltInRegistries.ITEM.containsKey(location)) {
            Item item = BuiltInRegistries.ITEM.get(location);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        ItemStack stack = new ItemStack(Items.BARRIER);
        if (location != null && BuiltInRegistries.FLUID.containsKey(location)) {
            Item bucket = BuiltInRegistries.FLUID.get(location).getBucket();
            if (bucket != Items.AIR) {
                stack = new ItemStack(bucket);
            }
        }
        return stack.setHoverName(Component.literal(id).withStyle(style -> style.withItalic(false)));
    }
}
