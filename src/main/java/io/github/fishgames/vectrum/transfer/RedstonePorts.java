package io.github.fishgames.vectrum.transfer;

import io.github.fishgames.vectrum.block.NetworkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Redstone-"Anschluesse": Jeder Nachbarblock (ausser Luft und anderen Netzbausteinen) kann Signale liefern oder
 * empfangen, deshalb gibt es hier nur einen Marker ohne Inhalt. Ob eine Seite wirklich Eingang, Ausgang oder aus ist,
 * entscheidet die Rolle der Seite (siehe {@code ConduitBlock#defaultMode}). Redstone bewegt keine Mengen; der Marker
 * wird nur gebraucht, damit der Baustein weiss, wo ein Anschluss moeglich ist.
 */
public final class RedstonePorts {
    private static final Port MARKER = (target, max, filter) -> 0;

    private RedstonePorts() {
    }

    public static Port find(Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getBlock() instanceof NetworkBlock ? null : MARKER;
    }
}
