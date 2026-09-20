package io.github.fishgames.vectrum.registry;

import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.item.DiagnosticItem;
import io.github.fishgames.vectrum.item.UpgradeItem;
import io.github.fishgames.vectrum.item.WrenchItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.function.Supplier;

/** Item registrations. */
public final class ModItems {
    private static final List<Registered<Item>> ALL = new ArrayList<>();
    private static final List<Registered<Item>> PLAIN = new ArrayList<>();

    public static final Registered<Item> EXAMPLE_ITEM = plainItem("example_item", () -> new Item(new Item.Properties()));
    public static final Registered<Item> EXAMPLE_BLOCK_ITEM = blockItem(ModBlocks.EXAMPLE_BLOCK);
    public static final Registered<Item> ITEM_CABLE = blockItem(ModBlocks.ITEM_CABLE);
    public static final Registered<Item> FLUID_CABLE = blockItem(ModBlocks.FLUID_CABLE);
    public static final Registered<Item> ENERGY_CABLE = blockItem(ModBlocks.ENERGY_CABLE);
    /** {@code null} unless the gas module is on. */
    public static final Registered<Item> GAS_CABLE = ModBlocks.GAS_CABLE == null ? null : blockItem(ModBlocks.GAS_CABLE);
    public static final Registered<Item> REDSTONE_CABLE = blockItem(ModBlocks.REDSTONE_CABLE);
    public static final Registered<Item> DIGITAL_CABLE = blockItem(ModBlocks.DIGITAL_CABLE);
    public static final Registered<Item> CODER = blockItem(ModBlocks.CODER);
    public static final Registered<Item> WIRELESS_PORT = blockItem(ModBlocks.WIRELESS_PORT);
    public static final Registered<Item> UNIVERSAL_CABLE = blockItem(ModBlocks.UNIVERSAL_CABLE);
    public static final Registered<Item> ITEM_ENDPOINT = blockItem(ModBlocks.ITEM_ENDPOINT);
    public static final Registered<Item> WRENCH = plainItem("wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1)));
    public static final Registered<Item> DIAGNOSTIC_TOOL = plainItem("diagnostic_tool",
            () -> new DiagnosticItem(new Item.Properties().stacksTo(1)));

    private static final Map<UpgradeType, Registered<Item>> UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        for (UpgradeType type : UpgradeType.VALUES) {
            UPGRADES.put(type, plainItem(type.id() + "_upgrade", () -> new UpgradeItem(type, new Item.Properties())));
        }
    }

    private ModItems() {
    }

    /** Registry id of an upgrade item. */
    public static net.minecraft.resources.ResourceLocation upgradeId(UpgradeType type) {
        return io.github.fishgames.vectrum.Vectrum.id(type.id() + "_upgrade");
    }

    /** Upgrade item of a type. */
    public static Item upgrade(UpgradeType type) {
        return UPGRADES.get(type).get();
    }

    /** Registers an item without a block. */
    private static Registered<Item> plainItem(String name, Supplier<Item> factory) {
        Registered<Item> entry = Registration.register(Registries.ITEM, name, factory);
        ALL.add(entry);
        PLAIN.add(entry);
        return entry;
    }

    /** Registers the block item of a block. */
    private static Registered<Item> blockItem(Registered<Block> block) {
        Registered<Item> entry = Registration.register(Registries.ITEM, block.id().getPath(),
                () -> new BlockItem(block.get(), new Item.Properties()));
        ALL.add(entry);
        return entry;
    }

    /** All mod items. */
    public static List<Registered<Item>> all() {
        return List.copyOf(ALL);
    }

    /** Items without a block. */
    public static List<Registered<Item>> plainItems() {
        return List.copyOf(PLAIN);
    }

    public static void init() {
    }
}
