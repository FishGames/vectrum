package io.github.fishgames.vectrum.registry;

import io.github.fishgames.vectrum.block.CableBlock;
import io.github.fishgames.vectrum.block.EndpointBlock;
import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModBlocks {
    private static final List<Registered<Block>> ALL = new ArrayList<>();
    private static final List<Registered<Block>> CUBES = new ArrayList<>();

    public static final Registered<Block> EXAMPLE_BLOCK = cubeBlock("example_block", () -> new Block(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST)));

    /** Transportkabel fuer Items (Stufe 1). Ohne Blockentity, Verbindungen stehen im BlockState. */
    public static final Registered<Block> ITEM_CABLE = block("item_cable", () -> new CableBlock(TransportType.ITEM,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.8F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Endpunkt fuer Items: vermittelt zwischen Inventaren und dem Kabelnetz. */
    public static final Registered<Block> ITEM_ENDPOINT = block("item_endpoint", () -> new EndpointBlock(TransportType.ITEM,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    private ModBlocks() {
    }

    private static Registered<Block> block(String name, Supplier<Block> factory) {
        Registered<Block> entry = Registration.register(Registries.BLOCK, name, factory);
        ALL.add(entry);
        return entry;
    }

    /** Block, dessen Modell der Datagen als einfacher Wuerfel erzeugt. Alle anderen haben eigene Modell-Dateien. */
    private static Registered<Block> cubeBlock(String name, Supplier<Block> factory) {
        Registered<Block> entry = block(name, factory);
        CUBES.add(entry);
        return entry;
    }

    /** Alle Bloecke dieser Mod (auch fuer den Datagen). */
    public static List<Registered<Block>> all() {
        return List.copyOf(ALL);
    }

    /** Nur die Bloecke mit einfachem Wuerfelmodell (der Datagen schreibt deren Modelle und Blockstates). */
    public static List<Registered<Block>> cubes() {
        return List.copyOf(CUBES);
    }

    public static void init() {
    }
}
