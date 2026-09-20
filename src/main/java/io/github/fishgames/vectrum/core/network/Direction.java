package io.github.fishgames.vectrum.core.network;

/** The six block directions in Minecraft order: down, up, north, south, west, east. */
public enum Direction {
    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    /** All directions. */
    public static final Direction[] VALUES = values();

    /** Bit mask with all six directions. */
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
        return VALUES[ordinal() ^ 1];
    }

    /** Bit of this direction in a side mask. */
    public int bit() {
        return 1 << ordinal();
    }
}
