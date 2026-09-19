package io.github.fishgames.vectrum.registry;

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

    public static final Registered<Block> EXAMPLE_BLOCK = block("example_block", () -> new Block(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST)));

    private ModBlocks() {
    }

    private static Registered<Block> block(String name, Supplier<Block> factory) {
        Registered<Block> entry = Registration.register(Registries.BLOCK, name, factory);
        ALL.add(entry);
        return entry;
    }

    /** Alle Bloecke dieser Mod (auch fuer den Datagen). */
    public static List<Registered<Block>> all() {
        return List.copyOf(ALL);
    }

    public static void init() {
    }
}
