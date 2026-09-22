package io.github.fishgames.vectrum.datagen;

import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/** Crafting recipes. */
public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> writer) {
        // Item cable
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.ITEM_CABLE.get(), 6)
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                .save(writer);

        // Fluid cable
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.FLUID_CABLE.get(), 6)
                .pattern("IGI")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy(getHasName(Items.GLASS), has(Items.GLASS))
                .save(writer);

        // Energy cable
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.ENERGY_CABLE.get(), 6)
                .pattern("CRC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                .save(writer);

        // Redstone cable
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.REDSTONE_CABLE.get(), 6)
                .pattern("RIR")
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                .save(writer);

        // Digital cable
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.DIGITAL_CABLE.get(), 4)
                .pattern("QAQ")
                .define('Q', Items.QUARTZ)
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy(getHasName(Items.AMETHYST_SHARD), has(Items.AMETHYST_SHARD))
                .save(writer);

        // Coder
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.CODER.get(), 1)
                .pattern("GDG")
                .pattern("EUE")
                .pattern("GDG")
                .define('G', Items.GOLD_INGOT)
                .define('D', Items.DIAMOND)
                .define('E', Items.ENDER_PEARL)
                .define('U', ModItems.UNIVERSAL_CABLE.get())
                .unlockedBy(getHasName(ModItems.UNIVERSAL_CABLE.get()), has(ModItems.UNIVERSAL_CABLE.get()))
                .save(writer);

        // Wireless port
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.WIRELESS_PORT.get(), 2)
                .pattern("GEG")
                .pattern("DND")
                .pattern("GEG")
                .define('G', Items.GOLD_INGOT)
                .define('E', Items.ENDER_EYE)
                .define('D', Items.DIAMOND)
                .define('N', Items.NETHER_STAR)
                .unlockedBy(getHasName(Items.NETHER_STAR), has(Items.NETHER_STAR))
                .save(writer);

        // Universal cable
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ModItems.UNIVERSAL_CABLE.get())
                .requires(ModItems.ITEM_CABLE.get())
                .requires(ModItems.FLUID_CABLE.get())
                .requires(ModItems.ENERGY_CABLE.get())
                .requires(Items.GOLD_INGOT)
                .unlockedBy(getHasName(ModItems.ITEM_CABLE.get()), has(ModItems.ITEM_CABLE.get()))
                .save(writer);

        // Item endpoint
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ModItems.ITEM_ENDPOINT.get())
                .requires(Items.HOPPER)
                .requires(ModItems.ITEM_CABLE.get())
                .requires(Items.IRON_INGOT)
                .unlockedBy(getHasName(ModItems.ITEM_CABLE.get()), has(ModItems.ITEM_CABLE.get()))
                .save(writer);

        // Diagnostic tool
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.DIAGNOSTIC_TOOL.get())
                .pattern("G")
                .pattern("C")
                .pattern("I")
                .define('G', Items.GLASS_PANE)
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                .save(writer);

        // Wrench
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.WRENCH.get())
                .pattern("I I")
                .pattern(" C ")
                .pattern(" I ")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                .save(writer);

        // Upgrades
        upgrade(writer, UpgradeType.THROUGHPUT, Items.IRON_INGOT, Items.HOPPER);
        upgrade(writer, UpgradeType.SPEED, Items.COPPER_INGOT, Items.CLOCK);
        upgrade(writer, UpgradeType.TYPES, Items.IRON_INGOT, Items.CHEST);
        upgrade(writer, UpgradeType.FILTER, Items.COPPER_INGOT, Items.PAPER);
        upgrade(writer, UpgradeType.PRIORITY, Items.COPPER_INGOT, Items.GOLD_INGOT);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.upgrade(UpgradeType.DIMENSION))
                .pattern("DGD")
                .pattern("GEG")
                .pattern("DGD")
                .define('D', Items.DIAMOND)
                .define('G', Items.GOLD_INGOT)
                .define('E', Items.ENDER_EYE)
                .unlockedBy(getHasName(Items.ENDER_EYE), has(Items.ENDER_EYE))
                .save(writer);
    }

    /** Upgrade ring: metal in the corners, redstone at the sides, the marker item in the centre. */
    private void upgrade(Consumer<FinishedRecipe> writer, UpgradeType type, Item metal, Item marker) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.upgrade(type))
                .pattern("MRM")
                .pattern("RXR")
                .pattern("MRM")
                .define('M', metal)
                .define('R', Items.REDSTONE)
                .define('X', marker)
                .unlockedBy(getHasName(metal), has(metal))
                .save(writer);
    }
}
