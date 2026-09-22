package io.github.fishgames.vectrum.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Loader-independent list of all data generation providers. */
public final class VectrumDataGen {
    @FunctionalInterface
    public interface ProviderFactory {
        DataProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries);
    }

    /** @param client true for client assets, false for server data */
    public record Entry(boolean client, ProviderFactory factory) {
    }

    public static final List<Entry> PROVIDERS = List.of(
            // assets/
            new Entry(true, (output, registries) -> new ModLanguageProvider(output, "en_us")),
            new Entry(true, (output, registries) -> new ModLanguageProvider(output, "de_de")),
            new Entry(true, (output, registries) -> new ModModelProvider(output)),
            // data/
            new Entry(false, (output, registries) -> new LootTableProvider(output, Set.of(), List.of(
                    new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)))),
            new Entry(false, (output, registries) -> new ModRecipeProvider(output)),
            new Entry(false, ModBlockTagsProvider::new),
            new Entry(false, ModItemTagsProvider::new)
    );

    private VectrumDataGen() {
    }
}
