package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.client.EndpointScreen;
import io.github.fishgames.vectrum.client.ProbeClient;
import io.github.fishgames.vectrum.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client setup: screen registration. */
@Mod.EventBusSubscriber(modid = Vectrum.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VectrumForgeClient {
    private VectrumForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ProbeClient.init();
        event.enqueueWork(() -> MenuScreens.register(ModMenus.ENDPOINT.get(), EndpointScreen::new));
    }
}
