package io.github.fishgames.vectrum.item;

import io.github.fishgames.vectrum.block.CoderBlock;
import io.github.fishgames.vectrum.block.NetworkBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.world.WirelessRegistry;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.logistics.Signals;
import io.github.fishgames.vectrum.logistics.TransportDefaults;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Diagnostic tool. Right click on a cable, endpoint or wireless port shows network id, size, input and output counts
 * and throughput (or frequency information); changes nothing.
 */
public class DiagnosticItem extends Item {
    public DiagnosticItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (state.getBlock() instanceof WirelessBlock && level instanceof ServerLevel server) {
            if (!level.isClientSide && player != null) {
                WirelessRegistry registry = WirelessRegistry.get(server);
                int frequency = registry.frequency(server, pos);
                player.displayClientMessage(Component.translatable("message.vectrum.wireless_info", frequency,
                        registry.memberCount(frequency),
                        Component.translatable("link.vectrum." + registry.mode(server, pos, TransportType.ITEM).id()),
                        Component.translatable("link.vectrum." + registry.mode(server, pos, TransportType.FLUID).id()),
                        Component.translatable("link.vectrum." + registry.mode(server, pos, TransportType.ENERGY).id()),
                        WirelessBlock.gasSuffix(registry, server, pos)),
                        true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getBlock() instanceof NetworkBlock network) {
            if (!level.isClientSide && player != null && level instanceof ServerLevel server) {
                LevelNetworks networks = LevelNetworks.get(server);
                MutableComponent message = Component.empty();
                boolean typed = network.transportTypes().size() > 1;
                if (network instanceof CoderBlock) {
                    // Coder: transport networks in chat, digital network in the action bar
                    for (TransportType type : network.transportTypes()) {
                        if (type.behavior() != TransportType.Behavior.CONNECTION) {
                            player.sendSystemMessage(Component.translatable("type.vectrum." + shortName(type))
                                    .append(": ").append(describe(server, networks, type, pos)));
                        }
                    }
                    player.displayClientMessage(describe(server, networks, TransportType.DIGITAL, pos)
                            .copy().append(Component.literal("  |  ")
                                    .append(Component.translatable("message.vectrum.coder_frequency",
                                            networks.frequency(pos)))), true);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                for (TransportType type : network.transportTypes()) {
                    if (message.getSiblings().size() > 0) {
                        message.append(Component.literal("  |  "));
                    }
                    if (typed) {
                        message.append(Component.translatable("type.vectrum." + shortName(type))
                                .append(": "));
                    }
                    message.append(describe(server, networks, type, pos));
                }
                player.displayClientMessage(message, true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static String shortName(TransportType type) {
        return type.id().substring(type.id().indexOf(':') + 1);
    }

    private static Component describe(ServerLevel server, LevelNetworks networks, TransportType type, BlockPos pos) {
        Network found = networks.networkAt(type, pos);
        if (found == null) {
            return Component.translatable("message.vectrum.network_none");
        }
        if (type.behavior() == TransportType.Behavior.CONNECTION) {
            StringBuilder frequencies = new StringBuilder();
            for (int frequency : networks.digitalFrequencies(pos)) {
                frequencies.append(frequencies.length() == 0 ? "" : ", ").append(frequency);
            }
            return Component.translatable("message.vectrum.network_digital",
                    found.id(), found.size(), found.endpointCount(), frequencies.toString());
        }
        if (type.behavior() == TransportType.Behavior.SIGNAL) {
            int[] sides = Signals.countSides(server, found);
            return Component.translatable("message.vectrum.network_signal",
                    found.id(), found.size(), sides[0] + "/" + sides[1], Signals.valueAt(server, networks, pos));
        }
        int[] ports = networks.countPorts(server, type, found);
        return Component.translatable("message.vectrum.network",
                found.id(), found.size(), ports[0], ports[1], networks.throughput(type, pos),
                Component.translatable(TransportDefaults.unitKey(type)));
    }
}
