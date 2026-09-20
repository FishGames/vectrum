package io.github.fishgames.vectrum.core.routing;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.network.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortSettingsTableTest {
    private static final String DIM = "minecraft:overworld";
    private static final BlockCoord A = new BlockCoord(DIM, 0, 0, 0);
    private static final BlockCoord B = new BlockCoord(DIM, 1, 0, 0);

    private final PortSettingsTable table = new PortSettingsTable();

    @Test
    void unknownPortsHaveDefaultSettings() {
        assertEquals(PortSettings.DEFAULT, table.get(new PortKey(A, Direction.UP)));
        assertTrue(PortSettings.DEFAULT.isDefault());
    }

    @Test
    void settingsAreStoredPerSide() {
        PortKey up = new PortKey(A, Direction.UP), down = new PortKey(A, Direction.DOWN);
        assertTrue(table.set(up, PortSettings.DEFAULT.withPriority(5)));

        assertEquals(5, table.get(up).priority());
        assertEquals(0, table.get(down).priority());
        assertEquals(1, table.size());
    }

    @Test
    void defaultValuesAreNotStoredAndReportChangeCorrectly() {
        PortKey key = new PortKey(A, Direction.UP);
        assertFalse(table.set(key, PortSettings.DEFAULT));
        assertEquals(0, table.size());

        table.set(key, PortSettings.DEFAULT.withMode(DistributionMode.BALANCED));
        assertFalse(table.set(key, PortSettings.DEFAULT.withMode(DistributionMode.BALANCED)));
        assertTrue(table.set(key, PortSettings.DEFAULT));
        assertEquals(0, table.size());
    }

    @Test
    void clearRemovesAllSidesOfOneBlockOnly() {
        table.set(new PortKey(A, Direction.UP), PortSettings.DEFAULT.withPriority(1));
        table.set(new PortKey(A, Direction.DOWN), PortSettings.DEFAULT.withPriority(2));
        table.set(new PortKey(B, Direction.UP), PortSettings.DEFAULT.withPriority(3));

        assertTrue(table.clear(A));
        assertFalse(table.clear(A));
        assertEquals(1, table.size());
        assertEquals(3, table.get(new PortKey(B, Direction.UP)).priority());
    }

    @Test
    void roundRobinPointersDropZeroAndClearPerBlock() {
        RoundRobinPointers pointers = new RoundRobinPointers();
        PortKey a = new PortKey(A, Direction.UP), b = new PortKey(B, Direction.UP);
        pointers.set(a, 3);
        pointers.set(b, 1);
        assertEquals(3, pointers.get(a));

        pointers.set(b, 0);
        assertEquals(1, pointers.entries().size());
        assertTrue(pointers.clear(A));
        assertEquals(0, pointers.get(a));
        assertTrue(pointers.entries().isEmpty());
    }

    @Test
    void distributionModeIdsRoundTrip() {
        for (DistributionMode mode : DistributionMode.values()) {
            assertEquals(mode, DistributionMode.byId(mode.id()));
        }
    }
}
