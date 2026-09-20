package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.core.routing.PortSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Ein Ziel im Netz: die Anschlussseite {@code side} des Netzbausteins {@code endpoint}, hinter der der
 * Speicher an {@code inventory} liegt. {@code settings} ist ein Abbild der Einstellungen zum Zeitpunkt, als die
 * Zielliste berechnet wurde (jede Aenderung berechnet sie neu).
 */
public record Target(BlockPos endpoint, Direction side, BlockPos inventory, PortSettings settings) {
}
