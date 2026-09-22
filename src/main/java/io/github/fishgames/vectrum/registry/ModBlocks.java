package io.github.fishgames.vectrum.registry;

import io.github.fishgames.vectrum.block.CableBlock;
import io.github.fishgames.vectrum.block.CoderBlock;
import io.github.fishgames.vectrum.block.DigitalCableBlock;
import io.github.fishgames.vectrum.block.EndpointBlock;
import io.github.fishgames.vectrum.Modules;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Block registrations. */
public final class ModBlocks {
    private static final List<Registered<Block>> ALL = new ArrayList<>();
    private static final List<Registered<Block>> CUBES = new ArrayList<>();

    /** Item cable. */
    public static final Registered<Block> ITEM_CABLE = block("item_cable", () -> new CableBlock(TransportType.ITEM,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.8F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Fluid cable. */
    public static final Registered<Block> FLUID_CABLE = block("fluid_cable", () -> new CableBlock(TransportType.FLUID,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.8F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Digital cable. */
    public static final Registered<Block> DIGITAL_CABLE = block("digital_cable", () -> new DigitalCableBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.8F)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()));

    /** Coder. */
    public static final Registered<Block> CODER = cubeBlock("coder", () -> new CoderBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F)
                    .sound(SoundType.METAL)));

    /** Wireless port. */
    public static final Registered<Block> WIRELESS_PORT = cubeBlock("wireless_port", () -> new WirelessBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.0F)
                    .sound(SoundType.METAL)));

    /** Redstone cable. */
    public static final Registered<Block> REDSTONE_CABLE = block("redstone_cable", () -> new CableBlock(TransportType.REDSTONE,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE)
                    .strength(0.8F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Energy cable. */
    public static final Registered<Block> ENERGY_CABLE = block("energy_cable", () -> new CableBlock(TransportType.ENERGY,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.8F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Universal cable. */
    public static final Registered<Block> UNIVERSAL_CABLE = block("universal_cable", () -> new CableBlock(
            Modules.quantityTypes(),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Gas cable; {@code null} unless {@link Modules#gas()}. */
    public static final Registered<Block> GAS_CABLE = Modules.gas() ? block("gas_cable", () -> new CableBlock(TransportType.GAS,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.8F)
                    .sound(SoundType.METAL)
                    .noOcclusion())) : null;

    /** Item endpoint. */
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

    /** Registers a cube block. */
    private static Registered<Block> cubeBlock(String name, Supplier<Block> factory) {
        Registered<Block> entry = block(name, factory);
        CUBES.add(entry);
        return entry;
    }

    /** All mod blocks. */
    public static List<Registered<Block>> all() {
        return List.copyOf(ALL);
    }

    /** Cube blocks only. */
    public static List<Registered<Block>> cubes() {
        return List.copyOf(CUBES);
    }

    public static void init() {
    }
}
