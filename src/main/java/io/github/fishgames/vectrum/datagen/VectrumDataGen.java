package io.github.fishgames.vectrum.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Loaderunabhaengige Liste aller Datagen-Provider. Sie basiert nur auf Vanilla-Klassen;
 * Fabric ({@code VectrumFabricDataGenerator}) und Forge/NeoForge ({@code VectrumForge}) hängen sie
 * lediglich an ihren jeweiligen Generator an.
 *
 * <p>Ausgabe: {@code generated/<minecraft-version>/} im Projektstamm (wird als Ressourcenordner eingebunden).
 */
public final class VectrumDataGen {
    @FunctionalInterface
    public interface ProviderFactory {
        DataProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries);
    }

    /** @param client true = Client-Ressourcen (assets), false = Serverdaten (data) */
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
            new Entry(false, ModBlockTagsProvider::new)
    );

    private VectrumDataGen() {
    }
}
