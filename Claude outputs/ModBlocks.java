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
    private static final List<Registered<Block>> CABLES = new ArrayList<>();

    /** Item cable. */
    public static final Registered<Block> ITEM_CABLE = cableBlock("item_cable", () -> new CableBlock(TransportType.ITEM,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.8F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Fluid cable. */
    public static final Registered<Block> FLUID_CABLE = cableBlock("fluid_cable", () -> new CableBlock(TransportType.FLUID,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.8F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Digital cable. */
    public static final Registered<Block> DIGITAL_CABLE = cableBlock("digital_cable", () -> new DigitalCableBlock(
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
    public static final Registered<Block> REDSTONE_CABLE = cableBlock("redstone_cable", () -> new CableBlock(TransportType.REDSTONE,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE)
                    .strength(0.8F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Energy cable. */
    public static final Registered<Block> ENERGY_CABLE = cableBlock("energy_cable", () -> new CableBlock(TransportType.ENERGY,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.8F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Universal cable. */
    public static final Registered<Block> UNIVERSAL_CABLE = cableBlock("universal_cable", () -> new CableBlock(
            Modules.quantityTypes(),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Gas cable; {@code null} unless {@link Modules#gas()}. */
    public static final Registered<Block> GAS_CABLE = Modules.gas() ? cableBlock("gas_cable", () -> new CableBlock(TransportType.GAS,
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

    /** Registers a cable whose models {@code ModModelProvider} generates. */
    private static Registered<Block> cableBlock(String name, Supplier<Block> factory) {
        Registered<Block> entry = block(name, factory);
        CABLES.add(entry);
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

    /**
     * All cables (item, fluid, energy, gas, redstone, universal, digital) - these render as {@code cutout} on the
     * client (hard-edged, like leaves/glass panes) so a layered cable's outer layer can leave gaps that show the
     * inner layer through, without real alpha blending. The item cable is solid and has no inner layer; cutout
     * does not change how it looks.
     */
    public static List<Registered<Block>> cables() {
        return List.copyOf(CABLES);
    }

    public static void init() {
    }
}
