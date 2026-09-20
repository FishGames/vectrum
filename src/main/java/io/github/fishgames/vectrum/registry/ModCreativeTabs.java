package io.github.fishgames.vectrum.registry;

import io.github.fishgames.vectrum.Vectrum;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Creative tab registrations. */
public final class ModCreativeTabs {
    public static final Registered<CreativeModeTab> MAIN = Registration.register(Registries.CREATIVE_MODE_TAB, "main",
            () -> tabBuilder()
                    .title(Component.translatable("itemGroup." + Vectrum.MOD_ID + ".main"))
                    .icon(() -> new ItemStack(ModItems.ITEM_CABLE.get()))
                    .displayItems((parameters, output) -> ModItems.all().forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }

    private static CreativeModeTab.Builder tabBuilder() {
        //? if <1.20.2 {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0);
        //?} else {
        /*return CreativeModeTab.builder();
        *///?}
    }

    public static void init() {
    }
}
