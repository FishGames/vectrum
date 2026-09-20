package io.github.fishgames.vectrum.world;

import net.minecraft.core.Direction;

/**
 * Hilfen für die sechs Blockseiten. Die Reihenfolge ({@code get3DDataValue}) ist unten, oben, Norden, Süden,
 * Westen, Osten und stimmt mit {@code core.network.Direction} überein. Deshalb kann ein Bit einer Seitenmaske
 * direkt mit {@code 1 << side.get3DDataValue()} berechnet werden.
 */
public final class Sides {
    /** Alle Seiten, einmal angelegt (vermeidet das Kopieren bei {@code Direction.values()}). */
    public static final Direction[] ALL = Direction.values();

    private Sides() {
    }

    public static int bit(Direction side) {
        return 1 << side.get3DDataValue();
    }
}
