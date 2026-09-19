package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.datagen.VectrumDataGen;
import io.github.fishgames.vectrum.registry.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Einstiegspunkt fuer Forge und - bis Minecraft 1.20.1 - auch fuer NeoForge,
 * denn NeoForge 1.20.1 nutzt noch dieselbe API ({@code net.minecraftforge.*}).
 */
@Mod(Vectrum.MOD_ID)
public final class VectrumForge {
    public VectrumForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        Vectrum.init();
        modBus.addListener(VectrumForge::onRegister);
        modBus.addListener(VectrumForge::onGatherData);
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
