package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.command.VectrumCommands;
import io.github.fishgames.vectrum.registry.Registration;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.Ports;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public final class VectrumFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Ports.setFinder(TransportType.ITEM, FabricItemPorts::find);
        Ports.setFinder(TransportType.FLUID, FabricFluidPorts::find);
        Ports.setFinder(TransportType.ENERGY, FabricEnergyPorts::find);
        Vectrum.init();

        // Reihenfolge: erst Bloecke, dann Items (Block-Items), zuletzt Creative-Tabs.
        Registration.applyTo(Registries.BLOCK, BuiltInRegistries.BLOCK);
        Registration.applyTo(Registries.ITEM, BuiltInRegistries.ITEM);
        Registration.applyTo(Registries.CREATIVE_MODE_TAB, BuiltInRegistries.CREATIVE_MODE_TAB);

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> VectrumCommands.register(dispatcher));
    }
}
