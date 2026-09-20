package io.github.fishgames.vectrum.core.upgrade;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradesTest {
    private static final BlockCoord A = new BlockCoord("minecraft:overworld", 0, 0, 0);

    @Test
    void emptyHasNothingAndDefaultEffects() {
        Upgrades none = Upgrades.EMPTY;

        assertTrue(none.isEmpty());
        assertEquals(0, none.total());
        assertEquals(4, UpgradeEffects.throughput(4, none));
        assertEquals(10, UpgradeEffects.interval(10, none));
        assertEquals(1, UpgradeEffects.maxTypes(none));
        assertFalse(UpgradeEffects.filterUnlocked(none));
        assertFalse(UpgradeEffects.priorityUnlocked(none));
    }

    @Test
    void countsAreClampedToTheMaximumOfEachType() {
        Upgrades u = Upgrades.EMPTY.with(UpgradeType.THROUGHPUT, 99).with(UpgradeType.FILTER, 5).with(UpgradeType.SPEED, -3);

        assertEquals(UpgradeType.THROUGHPUT.maxCount(), u.count(UpgradeType.THROUGHPUT));
        assertEquals(1, u.count(UpgradeType.FILTER));
        assertEquals(0, u.count(UpgradeType.SPEED));
        assertEquals(0, u.freeSlots(UpgradeType.FILTER));
    }

    @Test
    void allZeroCollapsesToTheSharedEmptyInstance() {
        Upgrades u = Upgrades.EMPTY.with(UpgradeType.TYPES, 2).with(UpgradeType.TYPES, 0);

        assertSame(Upgrades.EMPTY, u);
        assertSame(Upgrades.EMPTY, Upgrades.of(new int[]{0, 0}));
    }

    @Test
    void throughputMultipliesByFourPerUpgrade() {
        for (int n = 0; n <= UpgradeType.THROUGHPUT.maxCount(); n++) {
            Upgrades u = Upgrades.EMPTY.with(UpgradeType.THROUGHPUT, n);
            assertEquals(4L * (long) Math.pow(4, n), UpgradeEffects.throughput(4, u));
        }
    }

    @Test
    void throughputSaturatesInsteadOfOverflowing() {
        Upgrades u = Upgrades.EMPTY.with(UpgradeType.THROUGHPUT, UpgradeType.THROUGHPUT.maxCount());

        assertEquals(Long.MAX_VALUE, UpgradeEffects.throughput(Long.MAX_VALUE / 2, u));
        assertEquals(0, UpgradeEffects.throughput(0, u));
    }

    @Test
    void speedHalvesTheIntervalButNeverBelowOneTick() {
        assertEquals(5, UpgradeEffects.interval(10, Upgrades.EMPTY.with(UpgradeType.SPEED, 1)));
        assertEquals(2, UpgradeEffects.interval(10, Upgrades.EMPTY.with(UpgradeType.SPEED, 2)));
        assertEquals(1, UpgradeEffects.interval(10, Upgrades.EMPTY.with(UpgradeType.SPEED, 3)));
        assertEquals(1, UpgradeEffects.interval(1, Upgrades.EMPTY.with(UpgradeType.SPEED, 3)));
    }

    @Test
    void typesAddOnePerUpgrade() {
        assertEquals(5, UpgradeEffects.maxTypes(Upgrades.EMPTY.with(UpgradeType.TYPES, 4)));
    }

    @Test
    void storedArraysRoundTripAndTolerateGarbage() {
        Upgrades u = Upgrades.EMPTY.with(UpgradeType.SPEED, 2).with(UpgradeType.PRIORITY, 1);

        assertEquals(u, Upgrades.of(u.toArray()));
        assertEquals(UpgradeType.THROUGHPUT.maxCount(), Upgrades.of(new int[]{1000}).count(UpgradeType.THROUGHPUT));
        assertEquals(0, Upgrades.of(new int[]{-5, -5}).total());
    }

    @Test
    void tableStoresOnlyBlocksWithUpgradesAndTakesThemBack() {
        UpgradeTable table = new UpgradeTable();
        Upgrades u = Upgrades.EMPTY.with(UpgradeType.FILTER, 1);

        assertFalse(table.set(A, Upgrades.EMPTY));
        assertTrue(table.set(A, u));
        assertFalse(table.set(A, u));
        assertEquals(1, table.size());
        assertEquals(u, table.take(A));
        assertEquals(0, table.size());
        assertTrue(table.take(A).isEmpty());
    }

    @Test
    void typeIdsRoundTrip() {
        for (UpgradeType type : UpgradeType.VALUES) {
            assertEquals(type, UpgradeType.byId(type.id()));
        }
        assertEquals(null, UpgradeType.byId("nope"));
    }
}
