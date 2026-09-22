package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.net.VectrumNet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Fabric transport of the server side messages. */
final class FabricNet {
    private FabricNet() {
    }

    static void init() {
        receive(VectrumNet.FILTER_ID);
        receive(VectrumNet.PROBE);
        VectrumNet.setServerSender((player, id, payload) -> ServerPlayNetworking.send(player, id, payload));
    }

    private static void receive(ResourceLocation id) {
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, responseSender) -> {
            FriendlyByteBuf copy = new FriendlyByteBuf(buf.copy());
            server.execute(() -> VectrumNet.receiveOnServer(player, id, copy));
        });
    }
}
