package io.github.fishgames.vectrum.item;

import io.github.fishgames.vectrum.block.NetworkBlock;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Das Diagnosewerkzeug. Rechtsklick auf ein Kabel oder einen Endpunkt zeigt, zu welchem Netz der Baustein gehört und
 * wie viele Eingänge und Ausgänge es hat. Es verändert nichts. Ausführlichere Diagnose (Fluss, Engpässe, Umriss des
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
                Network found = networks.networkAt(network.transportType(), pos);
                Component message;
                if (found == null) {
                    message = Component.translatable("message.vectrum.network_none");
                } else {
                    int[] ports = networks.countPorts(server, found);
                    message = Component.translatable("message.vectrum.network",
                            found.id(), found.size(), ports[0], ports[1]);
                }
                player.displayClientMessage(message, true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
