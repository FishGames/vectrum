package io.github.fishgames.vectrum.datagen;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.registry.ModBlocks;
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

public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> writer) {
        // Diamant + Redstone -> Beispielgegenstand
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.EXAMPLE_ITEM.get())
                .requires(Items.DIAMOND)
                .requires(Items.REDSTONE)
                .unlockedBy(getHasName(Items.DIAMOND), has(Items.DIAMOND))
                .save(writer);

        // 9 Beispielgegenstaende -> Beispielblock
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.EXAMPLE_BLOCK.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.EXAMPLE_ITEM.get())
                .unlockedBy(getHasName(ModItems.EXAMPLE_ITEM.get()), has(ModItems.EXAMPLE_ITEM.get()))
                .save(writer);

        // Beispielblock -> 9 Beispielgegenstaende
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.EXAMPLE_ITEM.get(), 9)
                .requires(ModBlocks.EXAMPLE_BLOCK.get())
                .unlockedBy(getHasName(ModBlocks.EXAMPLE_BLOCK.get()), has(ModBlocks.EXAMPLE_BLOCK.get()))
                .save(writer, Vectrum.id("example_item_from_example_block"));

        // Item-Kabel: 2 Eisen + 1 Kupfer -> 6 Kabel
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.ITEM_CABLE.get(), 6)
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                .save(writer);

        // Fluid-Kabel: 2 Eisen + 1 Glas -> 6 Kabel
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.FLUID_CABLE.get(), 6)
                .pattern("IGI")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy(getHasName(Items.GLASS), has(Items.GLASS))
                .save(writer);

        // Energie-Kabel: 2 Kupfer + 1 Redstone -> 6 Kabel
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.ENERGY_CABLE.get(), 6)
                .pattern("CRC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                .save(writer);

        // Redstone-Kabel: 2 Redstone + 1 Eisen -> 6 Kabel
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModItems.REDSTONE_CABLE.get(), 6)
                .pattern("RIR")
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                .save(writer);

        // Universalkabel (Stufe 2): verbraucht je ein Kabel der drei Einzeltypen, ergibt 2
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ModItems.UNIVERSAL_CABLE.get(), 2)
                .requires(ModItems.ITEM_CABLE.get())
                .requires(ModItems.FLUID_CABLE.get())
                .requires(ModItems.ENERGY_CABLE.get())
                .requires(Items.GOLD_INGOT)
                .unlockedBy(getHasName(ModItems.ITEM_CABLE.get()), has(ModItems.ITEM_CABLE.get()))
                .save(writer);

        // Item-Endpunkt: Trichter + Item-Kabel + Eisen
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ModItems.ITEM_ENDPOINT.get())
                .requires(Items.HOPPER)
                .requires(ModItems.ITEM_CABLE.get())
                .requires(Items.IRON_INGOT)
                .unlockedBy(getHasName(ModItems.ITEM_CABLE.get()), has(ModItems.ITEM_CABLE.get()))
                .save(writer);

        // Diagnosewerkzeug: Glasscheibe + Kupfer + Eisen
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
        // Upgrades (Platzhalter-Rezepte, das Balancing folgt spaeter)
        upgrade(writer, UpgradeType.THROUGHPUT, Items.HOPPER, Items.REDSTONE);
        upgrade(writer, UpgradeType.SPEED, Items.SUGAR, Items.REDSTONE);
        upgrade(writer, UpgradeType.TYPES, Items.CHEST, Items.REDSTONE);
        upgrade(writer, UpgradeType.FILTER, Items.PAPER, Items.REDSTONE);
        upgrade(writer, UpgradeType.PRIORITY, Items.GOLD_INGOT, Items.REDSTONE);
    }

    /** Ein Upgrade aus einem Kennzeichen-Item, Redstone, Kupfer und Eisen (formlos). */
    private void upgrade(Consumer<FinishedRecipe> writer, UpgradeType type, Item mark, Item dust) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.upgrade(type))
                .requires(mark)
                .requires(dust)
                .requires(Items.COPPER_INGOT)
                .requires(Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                .save(writer);
    }
}
