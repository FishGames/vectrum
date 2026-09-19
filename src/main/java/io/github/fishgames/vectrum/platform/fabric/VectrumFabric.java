package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.registry.Registration;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public final class VectrumFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Vectrum.init();

        // Reihenfolge: erst Bloecke, dann Items (Block-Items), dann Creative-Tabs.
        Registration.applyTo(Registries.BLOCK, BuiltInRegistries.BLOCK);
        Registration.applyTo(Registries.ITEM, BuiltInRegistries.ITEM);
        Registration.applyTo(Registries.CREATIVE_MODE_TAB, BuiltInRegistries.CREATIVE_MODE_TAB);
    }
}
