package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.client.EndpointScreen;
import io.github.fishgames.vectrum.client.ProbeClient;
import io.github.fishgames.vectrum.net.VectrumNet;
import io.github.fishgames.vectrum.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.FriendlyByteBuf;

/** Client entry point: screen registration and the client side messages. */
public final class VectrumFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.ENDPOINT.get(), EndpointScreen::new);
        ProbeClient.init();
        VectrumNet.setClientSender((id, payload) -> ClientPlayNetworking.send(id, payload));
        ClientPlayNetworking.registerGlobalReceiver(VectrumNet.PROBE_REPLY, (client, handler, buf, responseSender) -> {
            FriendlyByteBuf copy = new FriendlyByteBuf(buf.copy());
            client.execute(() -> VectrumNet.receiveOnClient(VectrumNet.PROBE_REPLY, copy));
        });
    }
}
