package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.net.VectrumNet;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Forge transport: one channel with one message type that carries the message id and its bytes. */
final class ForgeNet {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Vectrum.id("main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    /** Message: id and payload bytes. */
    private record Raw(ResourceLocation id, byte[] data) {
        static void write(Raw message, FriendlyByteBuf buf) {
            buf.writeResourceLocation(message.id());
            buf.writeByteArray(message.data());
        }

        static Raw read(FriendlyByteBuf buf) {
            return new Raw(buf.readResourceLocation(), buf.readByteArray());
        }

        static Raw of(ResourceLocation id, FriendlyByteBuf payload) {
            byte[] bytes = new byte[payload.readableBytes()];
            payload.getBytes(payload.readerIndex(), bytes);
            return new Raw(id, bytes);
        }

        FriendlyByteBuf payload() {
            return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        }
    }

    private ForgeNet() {
    }

    static void init() {
        CHANNEL.registerMessage(0, Raw.class, Raw::write, Raw::read, ForgeNet::handle);
        VectrumNet.setServerSender((player, id, payload) ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), Raw.of(id, payload)));
        VectrumNet.setClientSender((id, payload) -> CHANNEL.sendToServer(Raw.of(id, payload)));
    }

    private static void handle(Raw message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender != null) {
                    VectrumNet.receiveOnServer(sender, message.id(), message.payload());
                }
            } else {
                VectrumNet.receiveOnClient(message.id(), message.payload());
            }
        });
        context.setPacketHandled(true);
    }
}
