package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Modules;
import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.platform.forge.mekanism.MekanismCompat;
import io.github.fishgames.vectrum.command.VectrumCommands;
import io.github.fishgames.vectrum.datagen.VectrumDataGen;
import io.github.fishgames.vectrum.registry.Registration;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.Ports;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

import java.util.concurrent.CompletableFuture;

/** Forge and NeoForge 1.20.1 entry point ({@code net.minecraftforge.*} API). */
@Mod(Vectrum.MOD_ID)
public final class VectrumForge {
    public VectrumForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        Ports.setFinder(TransportType.ITEM, ForgeItemPorts::find);
        Ports.setFinder(TransportType.FLUID, ForgeFluidPorts::find);
        Ports.setFinder(TransportType.ENERGY, ForgeEnergyPorts::find);
        // Gas module
        if (ModList.get().isLoaded("mekanism") && MekanismCompat.init()) {
            Modules.setGas(true);
        }
        Vectrum.init();
        modBus.addListener(VectrumForge::onRegister);
        modBus.addListener(VectrumForge::onGatherData);
        MinecraftForge.EVENT_BUS.addListener(VectrumForge::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        VectrumCommands.register(event.getDispatcher());
    }

    private static void onRegister(RegisterEvent event) {
        bind(event, Registries.BLOCK);
        bind(event, Registries.ITEM);
        bind(event, Registries.CREATIVE_MODE_TAB);
    }

    private static <T> void bind(RegisterEvent event, ResourceKey<? extends Registry<T>> registry) {
        event.register(registry, helper -> Registration.apply(registry, (id, value) -> helper.register(id, value)));
    }

    private static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();

        for (VectrumDataGen.Entry entry : VectrumDataGen.PROVIDERS) {
            boolean run = entry.client() ? event.includeClient() : event.includeServer();
            generator.addProvider(run, entry.factory().create(output, registries));
        }
    }
}
