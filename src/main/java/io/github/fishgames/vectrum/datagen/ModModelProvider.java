package io.github.fishgames.vectrum.datagen;

import com.google.gson.JsonObject;
import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.registry.ModBlocks;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.registry.Registered;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Schreibt Blockstates sowie Block- und Item-Modelle. Bewusst ohne loaderspezifische
 * Modell-Provider, damit derselbe Code auf Fabric, Forge und NeoForge laeuft.
 * Aktuell: jeder Block ist ein einfacher Wuerfel mit einer Textur ("cube_all"),
 * jedes einfache Item nutzt "item/generated".
 */
public final class ModModelProvider implements DataProvider {
    private final PackOutput.PathProvider models;
    private final PackOutput.PathProvider blockStates;

    public ModModelProvider(PackOutput output) {
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.blockStates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> writes = new ArrayList<>();

        for (Registered<Block> block : ModBlocks.all()) {
            String name = block.id().getPath();

            // Blockstate -> Blockmodell
            JsonObject variants = new JsonObject();
            variants.add("", model(Vectrum.MOD_ID + ":block/" + name));
            JsonObject blockState = new JsonObject();
            blockState.add("variants", variants);
            writes.add(DataProvider.saveStable(cache, blockState, blockStates.json(block.id())));

            // Blockmodell: ein Wuerfel mit einer Textur
            JsonObject blockModel = new JsonObject();
            blockModel.addProperty("parent", "minecraft:block/cube_all");
            blockModel.add("textures", textures("all", Vectrum.MOD_ID + ":block/" + name));
            writes.add(DataProvider.saveStable(cache, blockModel, models.json(Vectrum.id("block/" + name))));

            // Item-Modell des Block-Items -> zeigt das Blockmodell
            JsonObject itemModel = new JsonObject();
            itemModel.addProperty("parent", Vectrum.MOD_ID + ":block/" + name);
            writes.add(DataProvider.saveStable(cache, itemModel, models.json(Vectrum.id("item/" + name))));
        }

        for (Registered<Item> item : ModItems.plainItems()) {
            String name = item.id().getPath();

            JsonObject itemModel = new JsonObject();
            itemModel.addProperty("parent", "minecraft:item/generated");
            itemModel.add("textures", textures("layer0", Vectrum.MOD_ID + ":item/" + name));
            writes.add(DataProvider.saveStable(cache, itemModel, models.json(Vectrum.id("item/" + name))));
        }

        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private static JsonObject model(String model) {
        JsonObject json = new JsonObject();
        json.addProperty("model", model);
        return json;
    }

    private static JsonObject textures(String layer, String texture) {
        JsonObject json = new JsonObject();
        json.addProperty(layer, texture);
        return json;
    }

    @Override
    public String getName() {
        return "Vectrum Models";
    }
}
