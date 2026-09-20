package io.github.fishgames.vectrum.core.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DirectionAndCoordTest {
    @Test
    void oppositeIsItsOwnInverse() {
        for (Direction direction : Direction.VALUES) {
            assertEquals(direction, direction.opposite().opposite());
            assertNotEquals(direction, direction.opposite());
            assertEquals(-direction.dx(), direction.opposite().dx());
            assertEquals(-direction.dy(), direction.opposite().dy());
            assertEquals(-direction.dz(), direction.opposite().dz());
        }
    }

    @Test
    void bitsAreDistinctAndFillTheMask() {
        int all = 0;
        for (Direction direction : Direction.VALUES) {
            assertEquals(0, all & direction.bit());
            all |= direction.bit();
        }
        assertEquals(Direction.ALL_MASK, all);
        assertEquals(6, Direction.VALUES.length);
    }

    @Test
    void offsetMovesOneBlockAndKeepsDimension() {
        BlockCoord origin = new BlockCoord("minecraft:overworld", 10, 64, -3);
        assertEquals(new BlockCoord("minecraft:overworld", 10, 65, -3), origin.offset(Direction.UP));
        assertEquals(new BlockCoord("minecraft:overworld", 10, 64, -4), origin.offset(Direction.NORTH));
        assertEquals(origin, origin.offset(Direction.EAST).offset(Direction.WEST));
    }

    @Test
    void sameCoordinatesInDifferentDimensionsAreDifferent() {
        assertNotEquals(new BlockCoord("minecraft:overworld", 1, 2, 3), new BlockCoord("minecraft:the_nether", 1, 2, 3));
    }

    @Test
    void dimensionMustNotBeNull() {
        assertThrows(NullPointerException.class, () -> new BlockCoord(null, 0, 0, 0));
    }
}
