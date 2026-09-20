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

public final class ModItems {
    private static final List<Registered<Item>> ALL = new ArrayList<>();
    private static final List<Registered<Item>> PLAIN = new ArrayList<>();

    public static final Registered<Item> EXAMPLE_ITEM = plainItem("example_item", () -> new Item(new Item.Properties()));
    public static final Registered<Item> EXAMPLE_BLOCK_ITEM = blockItem(ModBlocks.EXAMPLE_BLOCK);
    public static final Registered<Item> ITEM_CABLE = blockItem(ModBlocks.ITEM_CABLE);
    public static final Registered<Item> FLUID_CABLE = blockItem(ModBlocks.FLUID_CABLE);
    public static final Registered<Item> ENERGY_CABLE = blockItem(ModBlocks.ENERGY_CABLE);
    public static final Registered<Item> REDSTONE_CABLE = blockItem(ModBlocks.REDSTONE_CABLE);
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

    /** Registrierungsname des Upgrade-Items einer Sorte. */
    public static net.minecraft.resources.ResourceLocation upgradeId(UpgradeType type) {
        return io.github.fishgames.vectrum.Vectrum.id(type.id() + "_upgrade");
    }

    /** Das Item zu einer Upgrade-Sorte. */
    public static Item upgrade(UpgradeType type) {
        return UPGRADES.get(type).get();
    }

    /** Einfacher Gegenstand mit flachem Item-Modell ("item/generated"). */
    private static Registered<Item> plainItem(String name, Supplier<Item> factory) {
        Registered<Item> entry = Registration.register(Registries.ITEM, name, factory);
        ALL.add(entry);
        PLAIN.add(entry);
        return entry;
    }

    /** Block-Item mit demselben Namen wie der Block. */
    private static Registered<Item> blockItem(Registered<Block> block) {
        Registered<Item> entry = Registration.register(Registries.ITEM, block.id().getPath(),
                () -> new BlockItem(block.get(), new Item.Properties()));
        ALL.add(entry);
        return entry;
    }

    /** Alle Items dieser Mod (Creative-Tab, Datagen). */
    public static List<Registered<Item>> all() {
        return List.copyOf(ALL);
    }

    /** Nur die Items ohne zugehoerigen Block (fuer die Item-Modelle im Datagen). */
    public static List<Registered<Item>> plainItems() {
        return List.copyOf(PLAIN);
    }

    public static void init() {
    }
}
