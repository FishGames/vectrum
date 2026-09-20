package io.github.fishgames.vectrum.registry;

import io.github.fishgames.vectrum.item.WrenchItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    private static final List<Registered<Item>> ALL = new ArrayList<>();
    private static final List<Registered<Item>> PLAIN = new ArrayList<>();

    public static final Registered<Item> EXAMPLE_ITEM = plainItem("example_item", () -> new Item(new Item.Properties()));
    public static final Registered<Item> EXAMPLE_BLOCK_ITEM = blockItem(ModBlocks.EXAMPLE_BLOCK);
    public static final Registered<Item> ITEM_CABLE = blockItem(ModBlocks.ITEM_CABLE);
    public static final Registered<Item> ITEM_ENDPOINT = blockItem(ModBlocks.ITEM_ENDPOINT);
    public static final Registered<Item> WRENCH = plainItem("wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
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
