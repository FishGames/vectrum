package io.github.fishgames.vectrum.platform.forge.ars;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Ars Nouveau source integration: source port finder. */
public final class ArsCompat {
    private ArsCompat() {
    }

    /** @return {@code true} if the source module could be set up */
    public static boolean init() {
        try {
            Ports.setFinder(TransportType.SOURCE, ArsCompat::find);
            Vectrum.LOGGER.info("Ars Nouveau found: source module enabled");
            return true;
        } catch (LinkageError | RuntimeException e) {
            Vectrum.LOGGER.warn("Source module disabled: unexpected Ars Nouveau API ({})", e.toString());
            return false;
        }
    }

    private static Port find(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof ISourceTile tile ? new ArsSourcePort(tile) : null;
    }
}
