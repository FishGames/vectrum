package io.github.fishgames.vectrum.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Ein Ziel im Netz: eine Endpunkt-Seite, die Ware in ein Inventar liefert.
 *
 * @param endpoint  Position des Endpunkts
 * @param side      Seite des Endpunkts, an der das Inventar liegt
 * @param inventory Position des Inventarblocks ({@code endpoint.relative(side)})
 */
public record ItemTarget(BlockPos endpoint, Direction side, BlockPos inventory) {
}
