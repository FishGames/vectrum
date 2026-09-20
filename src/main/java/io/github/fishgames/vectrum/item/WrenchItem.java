package io.github.fishgames.vectrum.item;

import io.github.fishgames.vectrum.block.EndpointBlock;
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
 * Das Wrench-Werkzeug. Rechtsklick auf einen Endpunkt schaltet die Rolle der Seite mit angeschlossenem Inventar um (Ziel, Quelle, Aus).
 * Rechtsklick auf ein Kabel zeigt kurz, zu welchem Netz es gehört. Weitere Funktionen (Verbindungen abschalten,
 * Einstellungen kopieren) folgen in Etappe 12.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (state.getBlock() instanceof EndpointBlock endpoint && !(player != null && player.isSecondaryUseActive())) {
            if (!level.isClientSide && player != null) {
                endpoint.onWrench(level, pos, player,
                        endpoint.pickSide(level, pos, state, context.getClickLocation(), context.getClickedFace()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (state.getBlock() instanceof NetworkBlock network) {
            if (!level.isClientSide && player != null && level instanceof ServerLevel server) {
                Network found = LevelNetworks.get(server).networkAt(network.transportType(), pos);
                Component message = found == null
                        ? Component.translatable("message.vectrum.network_none")
                        : Component.translatable("message.vectrum.network", found.id(), found.size(),
                        found.endpointCount());
                player.displayClientMessage(message, true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
