package io.github.fishgames.vectrum.datagen;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.registry.ModBlocks;
import io.github.fishgames.vectrum.registry.Registered;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.function.BiConsumer;

/**
 * Jeder Block dieser Mod droppt sich selbst. Vanillas BlockLootSubProvider wird bewusst nicht benutzt,
 * weil er ohne loaderspezifische Erweiterung ALLE registrierten Bloecke (auch fremde) pruefen wuerde.
 */
public final class ModBlockLoot implements LootTableSubProvider {
    @Override
    public void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
        for (Registered<Block> block : ModBlocks.all()) {
            output.accept(Vectrum.id("blocks/" + block.id().getPath()), dropSelf(block.get()));
        }
    }

    private static LootTable.Builder dropSelf(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(ExplosionCondition.survivesExplosion())
                .add(LootItem.lootTableItem(block)));
    }
}
