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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Block states plus block and item models for cube blocks, cables and plain items. */
public final class ModModelProvider implements DataProvider {
    /**
     * Cable with its edge positions in sixteenths: {@code from} to {@code to}; one solid box.
     *
     * @param pullPlate pull sides carry a plate in the cable's colour (state {@code in}); otherwise the old plates
     *                  of the item endpoint textures are drawn
     * @param junction  draws a box in the cable's colour around the core where three or more arms in one plane meet
     */
    private record Cable(String name, int from, int to, boolean pullPlate, boolean junction) {
    }

    /**
     * Cables in the world, including the gas, source and heat cables that only exist with their mods. Single-type cables are 6 px thick, the digital cable 4 px
     * (their placement shape still widens to 10 px while a matching item is held, see
     * {@link io.github.fishgames.vectrum.block.CableBlock}); the universal cable stays 10 px since it keeps a fixed
     * shape.
     */
    private static final List<Cable> CABLES = List.of(
            new Cable("item_cable", 5, 11, true, true),
            new Cable("fluid_cable", 5, 11, true, true),
            new Cable("energy_cable", 5, 11, true, false),
            new Cable("gas_cable", 5, 11, true, true),
            new Cable("source_cable", 5, 11, true, true),
            new Cable("heat_cable", 5, 11, true, false),
            new Cable("redstone_cable", 5, 11, false, false),
            new Cable("universal_cable", 3, 13, true, false),
            new Cable("digital_cable", 6, 10, false, false));

    /** The cable painted from regions of one 32x32 texture ({@code item_cable.png}); see {@link #itemCableFiles}. */
    private static final String ITEM_CABLE = "item_cable";
    private static final int ATLAS_SIZE = 32;
    /** Regions of that texture in pixels {x1, y1, x2, y2}; x2 and y2 are exclusive. */
    private static final int[] REGION_ARM_POSITIVE = {11, 0, 16, 6};
    private static final int[] REGION_CORE_STRAIGHT = {5, 0, 11, 6};
    /**
     * Core regions by the arms seen on a face from the front: letters l(eft), r(ight), u(p), d(own), always in the
     * order "lrud"; "none" for no arm. The straight pieces "lr" and "ud" use {@link #REGION_CORE_STRAIGHT}.
     */
    private static final Map<String, int[]> CORE_REGIONS = Map.ofEntries(
            Map.entry("none", new int[]{6, 6, 12, 12}),
            Map.entry("lrud", new int[]{0, 6, 6, 12}),
            Map.entry("d", new int[]{0, 12, 6, 18}),
            Map.entry("l", new int[]{6, 12, 12, 18}),
            Map.entry("r", new int[]{0, 18, 6, 24}),
            Map.entry("u", new int[]{6, 18, 12, 24}),
            Map.entry("rd", new int[]{16, 0, 22, 6}),
            Map.entry("ld", new int[]{22, 0, 28, 6}),
            Map.entry("ru", new int[]{16, 6, 22, 12}),
            Map.entry("lu", new int[]{22, 6, 28, 12}),
            Map.entry("lrd", new int[]{16, 12, 22, 18}),
            Map.entry("lud", new int[]{22, 12, 28, 18}),
            Map.entry("rud", new int[]{16, 18, 22, 24}),
            Map.entry("lru", new int[]{22, 18, 28, 24}),
            Map.entry("lr", REGION_CORE_STRAIGHT),
            Map.entry("ud", REGION_CORE_STRAIGHT));
    private static final int[] REGION_ARM_NEGATIVE = {0, 0, 5, 6};
    /** Output plate: front [0,24]-[7,31], side strip [12,24]-[13,31] (x = 13 at the cable core). */
    private static final int[] REGION_PLATE_FRONT = {0, 24, 8, 32};
    private static final int[] REGION_PLATE_SIDE = {12, 24, 14, 32};
    private static final int PLATE_DEPTH = 2;
    /** Block-state values of the connection sides, see {@link io.github.fishgames.vectrum.block.Connection}. */
    private static final String PULL_STATE = "in";
    private static final String PUSH_STATE = "out";
    /** Pixels the junction box reaches beyond the cable on every side. */
    private static final int JUNCTION_GROWTH = 1;
    private static final String[][] OPPOSITE_PAIRS = {{"north", "south"}, {"east", "west"}, {"up", "down"}};
    /**
     * Corners of each face in the order Minecraft's {@code FaceInfo} lists them (0 = min, 1 = max per axis x, y, z);
     * together with {@code BlockFaceUV} this decides how a texture lies on the face.
     */
    private static final Map<String, int[][]> FACE_VERTICES = Map.of(
            "down", new int[][]{{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}},
            "up", new int[][]{{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
            "north", new int[][]{{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}},
            "south", new int[][]{{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}},
            "west", new int[][]{{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}},
            "east", new int[][]{{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}});

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
            if (cable.name().equals(ITEM_CABLE)) {
                itemCableFiles(cable, writes, cache);
            } else {
                cableFiles(cable, writes, cache);
            }
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
     * position-derived crop Minecraft uses by default. The design only makes sense painted on the full canvas; the
     * (untouched) in/out port plate keeps the old auto-mapped behaviour.
     */
    private static final int[] FULL_UV = {0, 0, 16, 16};

    // ---- shared cable generation ----

    private void cableFiles(Cable cable, List<CompletableFuture<?>> writes, CachedOutput cache) {
        String name = cable.name();
        String outer = Vectrum.MOD_ID + ":block/" + name;
        int lo = cable.from();
        int hi = cable.to();

        JsonArray multipart = new JsonArray();
        JsonObject core = new JsonObject();
        core.add("apply", model(Vectrum.MOD_ID + ":block/" + name + "_core"));
        multipart.add(core);
        writes.add(saveModel(cache, name + "_core", cableModel(outer, null,
                box(lo, lo, lo, hi, hi, hi, "#all", FULL_UV, "down", "up", "north", "south", "west", "east"))));

        if (cable.junction()) {
            writes.add(saveModel(cache, name + "_junction", cableModel(outer, null, solidJunctionBox(lo, hi))));
            junctionConditions(multipart, Vectrum.MOD_ID + ":block/" + name + "_junction");
        }
        writes.add(DataProvider.saveStable(cache, cableBlockState(multipart, name),
                blockStates.json(Vectrum.id(name))));

        writes.add(saveModel(cache, name + "_arm", cableModel(outer, null,
                box(lo, lo, 0, hi, hi, lo, "#all", FULL_UV, "down", "up", "west", "east"))));
        if (cable.pullPlate()) {
            // Pull: the arm with a plate in the cable's colour. Push: the plain arm.
            writes.add(saveModel(cache, name + "_" + PULL_STATE, cableModel(outer, null,
                    box(lo, lo, 0, hi, hi, lo, "#all", FULL_UV, "down", "up", "west", "east"),
                    box(lo - 1, lo - 1, 0, hi + 1, hi + 1, PLATE_DEPTH, "#all", FULL_UV,
                            "down", "up", "north", "south", "west", "east"))));
            writes.add(saveModel(cache, name + "_" + PUSH_STATE, cableModel(outer, null,
                    box(lo, lo, 0, hi, hi, lo, "#all", FULL_UV, "down", "up", "west", "east"))));
        } else {
            for (String port : new String[]{"in", "out"}) {
                writes.add(saveModel(cache, name + "_" + port, cableModel(outer,
                        Vectrum.MOD_ID + ":block/item_endpoint_" + port,
                        box(lo, lo, 0, hi, hi, lo, "#all", FULL_UV, "down", "up", "west", "east"),
                        box(lo - 1, lo - 1, 0, hi + 1, hi + 1, 1, "#port",
                                "down", "up", "south", "west", "east"))));
            }
        }
        writes.add(saveModel(cache, name + "_inventory", cableModel(outer, null,
                box(lo, lo, 0, hi, hi, 16, "#all", FULL_UV, "down", "up", "north", "south", "west", "east"))));

        JsonObject item = new JsonObject();
        item.addProperty("parent", Vectrum.MOD_ID + ":block/" + name + "_inventory");
        writes.add(DataProvider.saveStable(cache, item, models.json(Vectrum.id("item/" + name))));
    }

    /**
     * Item cable, painted from regions of {@code item_cable.png}. Every face is looked at from outside (never
     * mirrored, see {@link #plateRight} and {@link #plateUp}):
     * <ul>
     *   <li>every side face of an arm shows {@link #REGION_ARM_POSITIVE} or {@link #REGION_ARM_NEGATIVE}: the arm to
     *   the right of the core as painted (5 px along the arm, the core on its left), the arm to the left likewise;
     *   an arm above the core is the right arm turned 90 degrees counter-clockwise, an arm below the core the left arm
     *   turned the same way; arms to the right and above use the positive region, those to the left and below the
     *   negative one;</li>
     *   <li>a core face shows the region for the arms it has on its four sides, as seen from the front
     *   ({@link #CORE_REGIONS}); arms pointing towards or away from the viewer are not counted. The straight pieces
     *   (arms left and right, or above and below) use one region, turned like an arm when the arms are above and
     *   below.</li>
     * </ul>
     * Nothing else is drawn yet.
     */
    private void itemCableFiles(Cable cable, List<CompletableFuture<?>> writes, CachedOutput cache) {
        String name = cable.name();
        String texture = Vectrum.MOD_ID + ":block/" + name;
        int lo = cable.from();
        int hi = cable.to();

        JsonArray multipart = new JsonArray();
        for (String face : DIRECTIONS) {
            // Sorted: Map.of iterates in a different order in every run, which would reshuffle the generated blockstate.
            for (String pattern : new java.util.TreeSet<>(CORE_REGIONS.keySet())) {
                String model = name + "_core_" + face + "_" + pattern;
                writes.add(saveModel(cache, model,
                        cableModel(texture, null, coreFaceBox(face, pattern, lo, hi))));
                JsonObject when = new JsonObject();
                for (String direction : DIRECTIONS) {
                    if (axisOf(direction) == axisOf(face)) {
                        continue;
                    }
                    boolean connected = pattern.indexOf(screenLetter(face, direction)) >= 0;
                    when.addProperty(direction, connected ? "link|in|out" : "none");
                }
                JsonObject part = new JsonObject();
                part.add("when", when);
                part.add("apply", model(Vectrum.MOD_ID + ":block/" + model));
                multipart.add(part);
            }
        }

        for (String direction : DIRECTIONS) {
            // Link and push: the arm alone. Pull: the arm with a port plate.
            writes.add(saveModel(cache, name + "_arm_" + direction,
                    cableModel(texture, null, armBox(direction, lo, hi, true))));
            writes.add(saveModel(cache, name + "_" + PUSH_STATE + "_" + direction,
                    cableModel(texture, null, armBox(direction, lo, hi, true))));
            writes.add(saveModel(cache, name + "_" + PULL_STATE + "_" + direction,
                    cableModel(texture, null, armBox(direction, lo, hi, false), portPlate(direction, lo, hi))));
            for (String state : new String[]{"link", PUSH_STATE, PULL_STATE}) {
                JsonObject when = new JsonObject();
                when.addProperty(direction, state);
                String suffix = state.equals("link") ? "arm" : state;
                JsonObject part = new JsonObject();
                part.add("when", when);
                part.add("apply", model(Vectrum.MOD_ID + ":block/" + name + "_" + suffix + "_" + direction));
                multipart.add(part);
            }
        }

        // Junction box where three or more arms in one plane meet at the core
        String junction = name + "_junction";
        writes.add(saveModel(cache, junction, cableModel(texture, null, junctionBox(lo, hi))));
        junctionConditions(multipart, Vectrum.MOD_ID + ":block/" + junction);

        JsonObject blockState = new JsonObject();
        blockState.add("multipart", multipart);
        writes.add(DataProvider.saveStable(cache, blockState, blockStates.json(Vectrum.id(name))));

        // Held item: a straight piece (north-south), i.e. the core faces beside its two arms.
        List<JsonObject> inventory = new ArrayList<>();
        for (String face : new String[]{"up", "down", "east", "west"}) {
            String pattern = screenLetter(face, "north") == 'u' || screenLetter(face, "north") == 'd' ? "ud" : "lr";
            inventory.add(coreFaceBox(face, pattern, lo, hi));
        }
        inventory.add(armBox("north", lo, hi, false));
        inventory.add(armBox("south", lo, hi, false));
        writes.add(saveModel(cache, name + "_inventory",
                cableModel(texture, null, inventory.toArray(JsonObject[]::new))));

        JsonObject item = new JsonObject();
        item.addProperty("parent", Vectrum.MOD_ID + ":block/" + name + "_inventory");
        writes.add(DataProvider.saveStable(cache, item, models.json(Vectrum.id("item/" + name))));
    }

    /**
     * Arm of the item cable: the four faces along it, each with its own region and orientation. With {@code cap} the
     * end face is drawn as well, with the region of a core without arms ({@link #CORE_REGIONS}); an arm with a port
     * plate has none, since the plate covers its end.
     */
    private static JsonObject armBox(String direction, int lo, int hi, boolean cap) {
        int[] v = vector(direction);
        int[] from = new int[3];
        int[] to = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            from[axis] = v[axis] > 0 ? hi : v[axis] < 0 ? 0 : lo;
            to[axis] = v[axis] > 0 ? 16 : v[axis] < 0 ? lo : hi;
        }
        JsonObject faces = new JsonObject();
        for (String face : DIRECTIONS) {
            if (axisOf(face) != axisOf(direction)) {
                faces.add(face, armFace(face, direction));
            }
        }
        if (cap) {
            faces.add(direction, atlasFace(direction, CORE_REGIONS.get("none"), false));
        }
        return element(from, to, faces);
    }

    /**
     * Junction box around the core: {@link #JUNCTION_GROWTH} px larger than the cable on every side, all six faces
     * showing {@link #REGION_PLATE_FRONT} as a picture seen from outside.
     */
    private static JsonObject junctionBox(int lo, int hi) {
        JsonObject faces = new JsonObject();
        for (String face : DIRECTIONS) {
            faces.add(face, atlasFace(face, REGION_PLATE_FRONT, false));
        }
        int from = lo - JUNCTION_GROWTH;
        int to = hi + JUNCTION_GROWTH;
        return element(new int[]{from, from, from}, new int[]{to, to, to}, faces);
    }

    /** Junction box of a cable painted in one colour: {@link #JUNCTION_GROWTH} px larger than the cable on every side. */
    private static JsonObject solidJunctionBox(int lo, int hi) {
        return box(lo - JUNCTION_GROWTH, lo - JUNCTION_GROWTH, lo - JUNCTION_GROWTH, hi + JUNCTION_GROWTH,
                hi + JUNCTION_GROWTH, hi + JUNCTION_GROWTH, "#all", FULL_UV, "down", "up", "north", "south", "west",
                "east");
    }

    /**
     * Blockstate parts that show the junction box: for each of the three planes, the two directions across the plane
     * are unconnected and at least three of the four directions in the plane are connected (link,
     * input or output). One part per combination, since a part's condition cannot count.
     */
    private static void junctionConditions(JsonArray multipart, String model) {
        for (String[] across : OPPOSITE_PAIRS) {
            List<String> inPlane = new ArrayList<>();
            for (String direction : DIRECTIONS) {
                if (!direction.equals(across[0]) && !direction.equals(across[1])) {
                    inPlane.add(direction);
                }
            }
            // missing == -1: all four connected; otherwise exactly that one is not
            for (int missing = -1; missing < inPlane.size(); missing++) {
                JsonObject when = new JsonObject();
                when.addProperty(across[0], "none");
                when.addProperty(across[1], "none");
                for (int i = 0; i < inPlane.size(); i++) {
                    when.addProperty(inPlane.get(i), i == missing ? "none" : "link|in|out");
                }
                JsonObject part = new JsonObject();
                part.add("when", when);
                part.add("apply", model(model));
                multipart.add(part);
            }
        }
    }

    /** One face of the core, showing the region for the arms it has on its sides (see {@link #CORE_REGIONS}). */
    private static JsonObject coreFaceBox(String face, String pattern, int lo, int hi) {
        JsonObject faces = new JsonObject();
        faces.add(face, atlasFace(face, CORE_REGIONS.get(pattern), pattern.equals("ud")));
        return element(new int[]{lo, lo, lo}, new int[]{hi, hi, hi}, faces);
    }

    /**
     * Output plate at the outer end of an arm: {@link #PLATE_DEPTH} px deep and 2 px wider than the cable on every
     * side. The front (facing the inventory) and the back (towards the core) both show {@link #REGION_PLATE_FRONT}
     * like a picture seen from outside; the four side faces show {@link #REGION_PLATE_SIDE} with the texture's x axis
     * pointing towards the cable core (its right edge, x = 13, lies at the core, the left edge at the inventory).
     * Nothing is mirrored.
     */
    private static JsonObject portPlate(String direction, int lo, int hi) {
        int[] v = vector(direction);
        int[] from = new int[3];
        int[] to = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            from[axis] = v[axis] > 0 ? 16 - PLATE_DEPTH : v[axis] < 0 ? 0 : lo - 1;
            to[axis] = v[axis] > 0 ? 16 : v[axis] < 0 ? PLATE_DEPTH : hi + 1;
        }
        JsonObject faces = new JsonObject();
        faces.add(direction, atlasFace(direction, REGION_PLATE_FRONT, false));
        faces.add(opposite(direction), atlasFace(opposite(direction), REGION_PLATE_FRONT, false));
        for (String face : DIRECTIONS) {
            if (axisOf(face) != axisOf(direction)) {
                int[] towardsCore = negate(v);
                faces.add(face, atlasFace(face, REGION_PLATE_SIDE, towardsCore, cross(towardsCore, vector(face))));
            }
        }
        return element(from, to, faces);
    }

    private static JsonObject element(int[] from, int[] to, JsonObject faces) {
        JsonObject element = new JsonObject();
        element.add("from", numbers(from));
        element.add("to", numbers(to));
        element.add("faces", faces);
        return element;
    }

    /** Letter (l, r, u, d) of the side of {@code face} on which {@code direction} lies when looking at the face. */
    private static char screenLetter(String face, String direction) {
        int[] along = vector(direction);
        if (dot(along, plateRight(face)) > 0) {
            return 'r';
        }
        if (dot(along, plateRight(face)) < 0) {
            return 'l';
        }
        return dot(along, plateUp(face)) > 0 ? 'u' : 'd';
    }

    /**
     * Face entry for a side face of an arm along {@code direction}: the arm to the right or above the core uses the
     * positive region, the arm to the left or below the negative one, turned when the arm is above or below.
     */
    private static JsonObject armFace(String face, String direction) {
        char side = screenLetter(face, direction);
        boolean vertical = side == 'u' || side == 'd';
        boolean positive = side == 'r' || side == 'u';
        return atlasFace(face, positive ? REGION_ARM_POSITIVE : REGION_ARM_NEGATIVE, vertical);
    }

    /**
     * The face entry that lays a region on {@code face}. Seen from the front (right and up as in
     * {@link #plateRight}, {@link #plateUp}) the region appears as painted, or, when {@code turned}, turned 90 degrees
     * counter-clockwise (its x axis points up, its y axis to the right).
     */
    private static JsonObject atlasFace(String face, int[] region, boolean turned) {
        int[] right = plateRight(face);
        int[] up = plateUp(face);
        return atlasFace(face, region, turned ? up : right, turned ? right : negate(up));
    }

    /**
     * The face entry that lays a region on {@code face} so that the region's x axis points in world direction
     * {@code texX} and its y axis in {@code texY}.
     */
    private static JsonObject atlasFace(String face, int[] region, int[] texX, int[] texY) {

        double scale = 16.0 / ATLAS_SIZE;
        double u1 = region[0] * scale;
        double v1 = region[1] * scale;
        double u2 = region[2] * scale;
        double v2 = region[3] * scale;
        int[][] vertices = FACE_VERTICES.get(face);
        for (int rotation = 0; rotation < 360; rotation += 90) {
            // Texture coordinate of each corner, the way Minecraft's BlockFaceUV assigns them.
            double[] u = new double[4];
            double[] v = new double[4];
            for (int i = 0; i < 4; i++) {
                int shifted = (i + rotation / 90) % 4;
                u[i] = shifted == 0 || shifted == 1 ? u1 : u2;
                v[i] = shifted == 0 || shifted == 3 ? v1 : v2;
            }
            if (java.util.Arrays.equals(growth(vertices, u, v), texX)
                    && java.util.Arrays.equals(growth(vertices, v, u), texY)) {
                JsonObject entry = new JsonObject();
                entry.addProperty("texture", "#all");
                entry.add("uv", numbers(u1, v1, u2, v2));
                if (rotation != 0) {
                    entry.addProperty("rotation", rotation);
                }
                return entry;
            }
        }
        throw new IllegalStateException("no UV orientation for " + face);
    }

    /** World direction in which {@code along} grows on a face, measured on corners that share the same {@code other}. */
    private static int[] growth(int[][] vertices, double[] along, double[] other) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (i != j && other[i] == other[j] && along[j] > along[i]) {
                    return new int[]{vertices[j][0] - vertices[i][0], vertices[j][1] - vertices[i][1],
                            vertices[j][2] - vertices[i][2]};
                }
            }
        }
        return new int[3];
    }

    /**
     * The picture's "right" when {@code face} is looked at from outside: the direction of the view (into the face)
     * crossed with {@link #plateUp}, so the picture is never mirrored.
     */
    private static int[] plateRight(String face) {
        int[] look = negate(vector(face));
        int[] up = plateUp(face);
        return new int[]{look[1] * up[2] - look[2] * up[1], look[2] * up[0] - look[0] * up[2],
                look[0] * up[1] - look[1] * up[0]};
    }

    /** The picture's "up": north on the top face, south on the bottom face, otherwise up. */
    private static int[] plateUp(String face) {
        return switch (face) {
            case "up" -> vector("north");
            case "down" -> vector("south");
            default -> vector("up");
        };
    }

    private static String opposite(String direction) {
        for (String[] pair : OPPOSITE_PAIRS) {
            if (pair[0].equals(direction)) {
                return pair[1];
            }
            if (pair[1].equals(direction)) {
                return pair[0];
            }
        }
        throw new IllegalArgumentException(direction);
    }

    private static char axisOf(String direction) {
        return switch (direction) {
            case "east", "west" -> 'x';
            case "up", "down" -> 'y';
            default -> 'z';
        };
    }

    private static int[] vector(String direction) {
        return switch (direction) {
            case "north" -> new int[]{0, 0, -1};
            case "south" -> new int[]{0, 0, 1};
            case "east" -> new int[]{1, 0, 0};
            case "west" -> new int[]{-1, 0, 0};
            case "up" -> new int[]{0, 1, 0};
            default -> new int[]{0, -1, 0};
        };
    }

    private static int dot(int[] a, int[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static int[] cross(int[] a, int[] b) {
        return new int[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static int[] negate(int[] a) {
        return new int[]{-a[0], -a[1], -a[2]};
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

    private static JsonObject cableModel(String outerTexture, String portTexture, JsonObject... boxes) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", outerTexture);
        textures.addProperty("all", outerTexture);
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

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            array.add(value);
        }
        return array;
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
