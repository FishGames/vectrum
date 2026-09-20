package io.github.fishgames.vectrum.core.network;

/**
 * Die sechs Blockrichtungen. Reihenfolge und Achsen entsprechen Minecraft (unten, oben, Norden, Süden, Westen, Osten).
 */
public enum Direction {
    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    /** Alle Richtungen, einmal angelegt (vermeidet das Kopieren bei {@code values()}). */
    public static final Direction[] VALUES = values();

    /** Bitmaske mit allen sechs Richtungen. */
    public static final int ALL_MASK = (1 << VALUES.length) - 1;

    private final int dx;
    private final int dy;
    private final int dz;

    Direction(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public int dz() {
        return dz;
    }

    public Direction opposite() {
        return VALUES[ordinal() ^ 1]; // Paare (0,1), (2,3), (4,5) liegen nebeneinander
    }

    /** Das Bit dieser Richtung in einer Seitenmaske. */
    public int bit() {
        return 1 << ordinal();
    }
}
