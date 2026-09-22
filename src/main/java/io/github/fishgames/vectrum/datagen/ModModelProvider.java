package io.github.fishgames.vectrum.datagen;

import com.google.gson.JsonArray;
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

/** Block states plus block and item models for cube blocks, cables and plain items. */
public final class ModModelProvider implements DataProvider {
    /** Cable with its edge positions in sixteenths: {@code from} to {@code to}. */
    private record Cable(String name, int from, int to) {
    }

    /**
     * Cables in the world, including the gas cable that only exists with Mekanism. Single-type cables are 6 px thick
     * (their placement shape still widens to 10 px while a matching item is held, see
     * {@link io.github.fishgames.vectrum.block.CableBlock}); the universal cable stays 10 px since it keeps a fixed
     * shape.
     */
    private static final List<Cable> CABLES = List.of(
            new Cable("item_cable", 5, 11),
            new Cable("fluid_cable", 5, 11),
            new Cable("energy_cable", 5, 11),
            new Cable("gas_cable", 5, 11),
            new Cable("redstone_cable", 5, 11),
            new Cable("universal_cable", 3, 13),
            new Cable("digital_cable", 5, 11));

    private static final String[] DIRECTIONS = {"north", "east", "south", "west", "up", "down"};
    private static final int[][] ROTATIONS = {{0, 0}, {0, 90}, {0, 180}, {0, 270}, {270, 0}, {90, 0}};

    private final PackOutput.PathProvider models;
    private final PackOutput.PathProvider blockStates;

    public ModModelProvider(PackOutput output) {
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.blockStates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> writes = new ArrayList<>();

        for (Registered<Block> block : ModBlocks.cubes()) {
            String name = block.id().getPath();

            // Block state
            JsonObject variants = new JsonObject();
            variants.add("", model(Vectrum.MOD_ID + ":block/" + name));
            JsonObject blockState = new JsonObject();
            blockState.add("variants", variants);
            writes.add(DataProvider.saveStable(cache, blockState, blockStates.json(block.id())));

            // Block model
            JsonObject blockModel = new JsonObject();
            blockModel.addProperty("parent", "minecraft:block/cube_all");
            blockModel.add("textures", textures("all", Vectrum.MOD_ID + ":block/" + name));
            writes.add(DataProvider.saveStable(cache, blockModel, models.json(Vectrum.id("block/" + name))));

            // Block item model
            JsonObject itemModel = new JsonObject();
            itemModel.addProperty("parent", Vectrum.MOD_ID + ":block/" + name);
            writes.add(DataProvider.saveStable(cache, itemModel, models.json(Vectrum.id("item/" + name))));
        }

        for (Cable cable : CABLES) {
            cableFiles(cable, writes, cache);
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

    private void cableFiles(Cable cable, List<CompletableFuture<?>> writes, CachedOutput cache) {
        String name = cable.name();
        String texture = Vectrum.MOD_ID + ":block/" + name;
        int lo = cable.from();
        int hi = cable.to();

        writes.add(DataProvider.saveStable(cache, cableBlockState(name), blockStates.json(Vectrum.id(name))));
        writes.add(saveModel(cache, name + "_core", cableModel(texture, null,
                box(lo, lo, lo, hi, hi, hi, "#all", "down", "up", "north", "south", "west", "east"))));
        writes.add(saveModel(cache, name + "_arm", cableModel(texture, null,
                box(lo, lo, 0, hi, hi, lo, "#all", "down", "up", "west", "east"))));
        for (String port : new String[]{"in", "out"}) {
            writes.add(saveModel(cache, name + "_" + port, cableModel(texture,
                    Vectrum.MOD_ID + ":block/item_endpoint_" + port,
                    box(lo, lo, 0, hi, hi, lo, "#all", "down", "up", "west", "east"),
                    box(lo - 1, lo - 1, 0, hi + 1, hi + 1, 1, "#port", "down", "up", "south", "west", "east"))));
        }
        writes.add(saveModel(cache, name + "_inventory", cableModel(texture, null,
                box(lo, lo, 0, hi, hi, 16, "#all", "down", "up", "north", "south", "west", "east"))));

        JsonObject item = new JsonObject();
        item.addProperty("parent", Vectrum.MOD_ID + ":block/" + name + "_inventory");
        writes.add(DataProvider.saveStable(cache, item, models.json(Vectrum.id("item/" + name))));
    }

    private CompletableFuture<?> saveModel(CachedOutput cache, String name, JsonObject model) {
        return DataProvider.saveStable(cache, model, models.json(Vectrum.id("block/" + name)));
    }

    private static JsonObject cableModel(String texture, String port, JsonObject... boxes) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", texture);
        textures.addProperty("all", texture);
        if (port != null) {
            textures.addProperty("port", port);
        }
        json.add("textures", textures);
        JsonArray elements = new JsonArray();
        for (JsonObject box : boxes) {
            elements.add(box);
        }
        json.add("elements", elements);
        return json;
    }

    private static JsonObject box(int x1, int y1, int z1, int x2, int y2, int z2, String texture, String... faces) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(x1, y1, z1));
        element.add("to", numbers(x2, y2, z2));
        JsonObject faceObject = new JsonObject();
        for (String face : faces) {
            JsonObject entry = new JsonObject();
            entry.addProperty("texture", texture);
            faceObject.add(face, entry);
        }
        element.add("faces", faceObject);
        return element;
    }

    private static JsonArray numbers(int... values) {
        JsonArray array = new JsonArray();
        for (int value : values) {
            array.add(value);
        }
        return array;
    }

    private static JsonObject cableBlockState(String name) {
        JsonArray multipart = new JsonArray();
        JsonObject core = new JsonObject();
        core.add("apply", model(Vectrum.MOD_ID + ":block/" + name + "_core"));
        multipart.add(core);
        for (int i = 0; i < DIRECTIONS.length; i++) {
            for (String state : new String[]{"link", "in", "out"}) {
                JsonObject when = new JsonObject();
                when.addProperty(DIRECTIONS[i], state);
                JsonObject apply = model(Vectrum.MOD_ID + ":block/" + name + "_" + (state.equals("link") ? "arm" : state));
                if (ROTATIONS[i][0] != 0) {
                    apply.addProperty("x", ROTATIONS[i][0]);
                }
                if (ROTATIONS[i][1] != 0) {
                    apply.addProperty("y", ROTATIONS[i][1]);
                }
                JsonObject part = new JsonObject();
                part.add("when", when);
                part.add("apply", apply);
                multipart.add(part);
            }
        }
        JsonObject json = new JsonObject();
        json.add("multipart", multipart);
        return json;
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
