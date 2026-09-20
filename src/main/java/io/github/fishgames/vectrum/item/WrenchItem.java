package io.github.fishgames.vectrum.item;

import io.github.fishgames.vectrum.block.ConduitBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Das Wrench-Werkzeug zum Einstellen. Rechtsklick auf ein Kabel oder einen Endpunkt neben einem Inventar schaltet die
 * Rolle dieser Seite um (Ausgang, Eingang, Aus). Weitere Funktionen (Verbindungen abschalten, Einstellungen kopieren)
 * folgen in Etappe 12. Zum Ansehen von Netzen dient das {@link DiagnosticItem}.
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

        if (state.getBlock() instanceof ConduitBlock conduit) {
            if (!level.isClientSide && player != null) {
                conduit.onWrench(level, pos, player,
                        conduit.pickSide(level, pos, state, context.getClickLocation(), context.getClickedFace()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
