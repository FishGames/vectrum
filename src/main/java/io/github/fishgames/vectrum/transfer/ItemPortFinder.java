package io.github.fishgames.vectrum.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Sucht das Item-Inventar an einer Position. Jeder Loader liefert dafür eine eigene Umsetzung. */
@FunctionalInterface
public interface ItemPortFinder {
    /**
     * @param side die Seite des <em>Inventarblocks</em>, von der aus zugegriffen wird
     * @return der Port oder {@code null}, wenn dort kein Inventar ist
     */
    ItemPort find(Level level, BlockPos pos, Direction side);
}
