package io.github.fishgames.vectrum.platform.forge.ars;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.common.block.tile.CreativeSourceJarTile;
import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.transfer.Port;

import java.util.function.Predicate;

/** {@link Port} for Ars Nouveau source tiles. Unit: source; the filter is ignored. */
final class ArsSourcePort implements Port {
    private final ISourceTile tile;

    ArsSourcePort(ISourceTile tile) {
        this.tile = tile;
    }

    @Override
    public long moveTo(Port target, long max, Predicate<String> filter) {
        if (max <= 0 || !(target instanceof ArsSourcePort other) || other.tile == tile) {
            return 0;
        }
        if (!other.tile.canAcceptSource()) {
            return 0;
        }
        // 1. probe: source offer and target room
        int amount = Math.min(Math.min(SaturatedMath.clampToNonNegativeInt(max), tile.getSource()),
                other.tile.getMaxSource() - other.tile.getSource());
        if (amount <= 0) {
            return 0;
        }
        // 2. take from the source
        int before = tile.getSource();
        tile.removeSource(amount);
        int taken = before - tile.getSource();
        if (taken <= 0 && tile instanceof CreativeSourceJarTile) {
            taken = amount; // the creative jar always reports a full jar, so nothing shows as taken
        }
        if (taken <= 0) {
            return 0;
        }
        // 3. give to the target
        int targetBefore = other.tile.getSource();
        other.tile.addSource(taken);
        int given = other.tile.getSource() - targetBefore;
        // 4. return the remainder to the source
        if (given < taken) {
            int sourceBefore = tile.getSource();
            tile.addSource(taken - given);
            int back = tile.getSource() - sourceBefore;
            if (back < taken - given) {
                Vectrum.LOGGER.warn("{} source were lost during the transfer", taken - given - back);
            }
        }
        return given;
    }

    @Override
    public boolean canGive(Predicate<String> filter) {
        return tile.getSource() > 0;
    }

    @Override
    public double fillLevel() {
        int capacity = tile.getMaxSource();
        return capacity <= 0 ? 1 : (double) tile.getSource() / capacity;
    }
}
