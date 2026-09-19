package io.github.fishgames.vectrum.datagen;

import io.github.fishgames.vectrum.registry.ModBlocks;
import io.github.fishgames.vectrum.registry.Registered;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

/** Alle Bloecke der Mod: mit der Spitzhacke abbaubar, Werkzeugstufe Stein. */
public final class ModBlockTagsProvider extends TagsProvider<Block> {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.BLOCK, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (Registered<Block> block : ModBlocks.all()) {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, block.id());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(key);
            tag(BlockTags.NEEDS_STONE_TOOL).add(key);
        }
    }
}
