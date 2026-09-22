package io.github.fishgames.vectrum.datagen;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.registry.Registered;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Item tags {@code vectrum:cables} and {@code vectrum:upgrades}. */
public final class ModItemTagsProvider extends TagsProvider<Item> {
    public static final TagKey<Item> CABLES = TagKey.create(Registries.ITEM, Vectrum.id("cables"));
    public static final TagKey<Item> UPGRADES = TagKey.create(Registries.ITEM, Vectrum.id("upgrades"));

    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.ITEM, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (Registered<Item> cable : List.of(ModItems.ITEM_CABLE, ModItems.FLUID_CABLE, ModItems.ENERGY_CABLE,
                ModItems.REDSTONE_CABLE, ModItems.UNIVERSAL_CABLE, ModItems.DIGITAL_CABLE)) {
            tag(CABLES).add(ResourceKey.create(Registries.ITEM, cable.id()));
        }
        tag(CABLES).addOptional(Vectrum.id("gas_cable"));
        for (UpgradeType type : UpgradeType.VALUES) {
            tag(UPGRADES).add(ResourceKey.create(Registries.ITEM, ModItems.upgradeId(type)));
        }
    }
}
