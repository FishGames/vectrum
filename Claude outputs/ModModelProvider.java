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
    /**
     * Cable with its edge positions in sixteenths: {@code from} to {@code to}. A {@code layered} cable has a second,
     * inset "inner" box with its own texture; a cable that is not layered is one solid outer box.
     */
    private record Cable(String name, int from, int to, boolean layered) {
    }

    /**
     * Cables in the world, including the gas cable that only exists with Mekanism. Single-type cables are 6 px thick
     * (their placement shape still widens to 10 px while a matching item is held, see
     * {@link io.github.fishgames.vectrum.block.CableBlock}); the universal cable stays 10 px since it keeps a fixed
     * shape. The item cable is fully solid (no inner layer, no see-through texture parts).
     */
    private static final List<Cable> CABLES = List.of(
            new Cable("item_cable", 5, 11, false),
            new Cable("fluid_cable", 5, 11, true),
            new Cable("energy_cable", 5, 11, true),
            new Cable("gas_cable", 5, 11, true),
            new Cable("redstone_cable", 5, 11, true),
            new Cable("universal_cable", 3, 13, true),
            new Cable("digital_cable", 5, 11, true));

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

    /**
     * Every cable face is forced to show the whole 16x16 texture ({@code uv} 0,0 to 16,16) instead of the
     * position-derived crop Minecraft uses by default. The border/inner design only makes sense painted on the
     * full canvas; the (untouched) in/out port plate keeps the old auto-mapped behaviour.
     */
    private static final int[] FULL_UV = {0, 0, 16, 16};

    // ---- shared cable generation ----

    private void cableFiles(Cable cable, List<CompletableFuture<?>> writes, CachedOutput cache) {
        String name = cable.name();
        String outer = Vectrum.MOD_ID + ":block/" + name;
        // Only a layered cable has an inner texture; a solid cable binds no "inner" texture at all.
        String inner = cable.layered() ? Vectrum.MOD_ID + ":block/" + name + "_inner" : null;
        int lo = cable.from();
        int hi = cable.to();
        // Inner (glowing/animated) layer: inset 1 px on every side of the outer "glass" layer, e.g. 5..11 -> 6..10.
        int innerLo = lo + 1;
        int innerHi = hi - 1;

        JsonArray multipart = new JsonArray();
        JsonObject core = new JsonObject();
        core.add("apply", model(Vectrum.MOD_ID + ":block/" + name + "_core"));
        multipart.add(core);
        writes.add(saveModel(cache, name + "_core", cableModel(outer, inner, null, layers(cable,
                box(lo, lo, lo, hi, hi, hi, "#all", FULL_UV, "down", "up", "north", "south", "west", "east"),
                box(innerLo, innerLo, innerLo, innerHi, innerHi, innerHi, "#inner", FULL_UV,
                        "down", "up", "north", "south", "west", "east")))));

        writes.add(DataProvider.saveStable(cache, cableBlockState(multipart, name),
                blockStates.json(Vectrum.id(name))));

        writes.add(saveModel(cache, name + "_arm", cableModel(outer, inner, null, layers(cable,
                box(lo, lo, 0, hi, hi, lo, "#all", FULL_UV, "down", "up", "west", "east"),
                box(innerLo, innerLo, 0, innerHi, innerHi, lo, "#inner", FULL_UV, "down", "up", "west", "east")))));
        for (String port : new String[]{"in", "out"}) {
            writes.add(saveModel(cache, name + "_" + port, cableModel(outer, inner,
                    Vectrum.MOD_ID + ":block/item_endpoint_" + port, layers(cable,
                            box(lo, lo, 0, hi, hi, lo, "#all", FULL_UV, "down", "up", "west", "east"),
                            box(innerLo, innerLo, 0, innerHi, innerHi, lo, "#inner", FULL_UV,
                                    "down", "up", "west", "east"),
                            box(lo - 1, lo - 1, 0, hi + 1, hi + 1, 1, "#port",
                                    "down", "up", "south", "west", "east")))));
        }
        writes.add(saveModel(cache, name + "_inventory", cableModel(outer, inner, null, layers(cable,
                box(lo, lo, 0, hi, hi, 16, "#all", FULL_UV, "down", "up", "north", "south", "west", "east"),
                box(innerLo, innerLo, 0, innerHi, innerHi, 16, "#inner", FULL_UV,
                        "down", "up", "north", "south", "west", "east")))));

        JsonObject item = new JsonObject();
        item.addProperty("parent", Vectrum.MOD_ID + ":block/" + name + "_inventory");
        writes.add(DataProvider.saveStable(cache, item, models.json(Vectrum.id("item/" + name))));
    }

    private static List<JsonObject> jsonObjects(JsonArray array) {
        List<JsonObject> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            list.add(array.get(i).getAsJsonObject());
        }
        return list;
    }

    private CompletableFuture<?> saveModel(CachedOutput cache, String name, JsonObject model) {
        return DataProvider.saveStable(cache, model, models.json(Vectrum.id("block/" + name)));
    }

    /**
     * The boxes of one cable model: the outer box, then (only for a {@linkplain Cable#layered() layered} cable) the
     * inner box, then any further boxes such as the port plate.
     */
    private static JsonObject[] layers(Cable cable, JsonObject outerBox, JsonObject innerBox, JsonObject... more) {
        List<JsonObject> boxes = new ArrayList<>();
        boxes.add(outerBox);
        if (cable.layered()) {
            boxes.add(innerBox);
        }
        boxes.addAll(List.of(more));
        return boxes.toArray(JsonObject[]::new);
    }

    /**
     * @param innerTexture the wire/core layer shown through the (cutout) outer layer, see {@link #FULL_UV}; {@code null}
     *                     for a solid cable without an inner layer.
     */
    private static JsonObject cableModel(String outerTexture, String innerTexture, String portTexture,
                                         JsonObject... boxes) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", outerTexture);
        textures.addProperty("all", outerTexture);
        if (innerTexture != null) {
            textures.addProperty("inner", innerTexture);
        }
        if (portTexture != null) {
            textures.addProperty("port", portTexture);
        }
        json.add("textures", textures);
        JsonArray elements = new JsonArray();
        for (JsonObject box : boxes) {
            elements.add(box);
        }
        json.add("elements", elements);
        return json;
    }

    /** Box element with Minecraft's default, position-derived UV crop (used only for the in/out port plate). */
    private static JsonObject box(int x1, int y1, int z1, int x2, int y2, int z2, String texture, String... faces) {
        return box(x1, y1, z1, x2, y2, z2, texture, (int[]) null, faces);
    }

    /** Box element; {@code uv} (when given) is applied to every face instead of Minecraft's default crop. */
    private static JsonObject box(int x1, int y1, int z1, int x2, int y2, int z2, String texture, int[] uv,
                                  String... faces) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(x1, y1, z1));
        element.add("to", numbers(x2, y2, z2));
        JsonObject faceObject = new JsonObject();
        for (String face : faces) {
            JsonObject entry = new JsonObject();
            entry.addProperty("texture", texture);
            if (uv != null) {
                entry.add("uv", numbers(uv));
            }
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

    /**
     * @param extraParts already-built multipart entries applied unconditionally (one per cable: the single,
     *                   always-the-same "_core" entry).
     */
    private static JsonObject cableBlockState(JsonArray extraParts, String name) {
        JsonArray multipart = new JsonArray();
        for (JsonObject part : jsonObjects(extraParts)) {
            multipart.add(part);
        }
        for (int i = 0; i < DIRECTIONS.length; i++) {
            String direction = DIRECTIONS[i];
            for (String state : new String[]{"link", "in", "out"}) {
                JsonObject when = new JsonObject();
                when.addProperty(direction, state);
                String suffix = state.equals("link") ? "arm" : state;
                JsonObject apply = model(Vectrum.MOD_ID + ":block/" + name + "_" + suffix);
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
