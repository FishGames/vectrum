package io.github.fishgames.vectrum.item;

import io.github.fishgames.vectrum.block.NetworkBlock;
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
 * Das Diagnosewerkzeug. Rechtsklick auf ein Kabel oder einen Endpunkt zeigt, zu welchem Netz der Baustein gehört und
 * wie viele Eingänge und Ausgänge es hat und wie hoch das Durchsatzlimit dieses Bausteins ist. Es verändert nichts. Ausführlichere Diagnose (Fluss, Engpässe, Umriss des
 * Netzes) kommt in Etappe 12 hierher.
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

        if (state.getBlock() instanceof NetworkBlock network) {
            if (!level.isClientSide && player != null && level instanceof ServerLevel server) {
                LevelNetworks networks = LevelNetworks.get(server);
                MutableComponent message = Component.empty();
                boolean typed = network.transportTypes().size() > 1;
                for (TransportType type : network.transportTypes()) {
                    if (message.getSiblings().size() > 0) {
                        message.append(Component.literal("  |  "));
                    }
                    if (typed) {
                        message.append(Component.translatable("type.vectrum." + type.id().substring(type.id().indexOf(':') + 1))
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

    private static Component describe(ServerLevel server, LevelNetworks networks, TransportType type, BlockPos pos) {
        Network found = networks.networkAt(type, pos);
        if (found == null) {
            return Component.translatable("message.vectrum.network_none");
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
