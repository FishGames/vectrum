package io.github.fishgames.vectrum.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Ein Ziel im Netz: die Anschlussseite {@code side} des Netzbausteins {@code endpoint}, hinter der der
 * Speicher an {@code inventory} liegt.
 */
public record Target(BlockPos endpoint, Direction side, BlockPos inventory) {
}
