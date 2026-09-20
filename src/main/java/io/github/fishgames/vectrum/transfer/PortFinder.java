package io.github.fishgames.vectrum.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Sucht den Speicher eines Transporttyps an einer Position. Jeder Loader liefert dafür eigene Umsetzungen. */
@FunctionalInterface
public interface PortFinder {
    /**
     * @param side die Seite des <em>Speicherblocks</em>, von der aus zugegriffen wird
     * @return der Port oder {@code null}, wenn dort kein passender Speicher ist
     */
    Port find(Level level, BlockPos pos, Direction side);
}
