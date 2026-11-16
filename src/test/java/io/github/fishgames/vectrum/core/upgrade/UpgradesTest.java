package io.github.fishgames.vectrum.core.upgrade;

import io.github.fishgames.vectrum.core.config.VectrumConfig;
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
        assertEquals(10, UpgradeEffects.interval(none));
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
    void speedFollowsTheConfiguredTable() {
        assertEquals(10, UpgradeEffects.interval(Upgrades.EMPTY));
        assertEquals(5, UpgradeEffects.interval(Upgrades.EMPTY.with(UpgradeType.SPEED, 1)));
        assertEquals(2, UpgradeEffects.interval(Upgrades.EMPTY.with(UpgradeType.SPEED, 2)));
        assertEquals(1, UpgradeEffects.interval(Upgrades.EMPTY.with(UpgradeType.SPEED, 3)));
    }

    @Test
    void typesAddOnePerUpgrade() {
        assertEquals(5, UpgradeEffects.maxTypes(Upgrades.EMPTY.with(UpgradeType.TYPES, 4)));
    }

    @Test
    void storedArraysRoundTripAndTolerateGarbage() {
        Upgrades u = Upgrades.EMPTY.with(UpgradeType.SPEED, 2).with(UpgradeType.PRIORITY, 1);

        assertEquals(u, Upgrades.of(u.toArray()));
        assertEquals(VectrumConfig.MAX_UPGRADE_COUNT, Upgrades.of(new int[]{1000}).count(UpgradeType.THROUGHPUT));
        assertEquals(0, Upgrades.of(new int[]{-5, -5}).total());
    }

    @Test
    void tableKeepsSetsPerSlotAndTakesThemBack() {
        UpgradeTable table = new UpgradeTable();
        Upgrades filter = Upgrades.EMPTY.with(UpgradeType.FILTER, 1);
        Upgrades speed = Upgrades.EMPTY.with(UpgradeType.SPEED, 2);

        assertFalse(table.set(A, 0, Upgrades.EMPTY));
        assertTrue(table.set(A, 0, filter));
        assertFalse(table.set(A, 0, filter));
        assertTrue(table.set(A, 3, speed));
        assertEquals(1, table.size());
        assertEquals(filter, table.get(A, 0));
        assertEquals(speed, table.get(A, 3));
        assertTrue(table.get(A, 1).isEmpty());

        int[] taken = table.take(A);
        assertEquals(1, taken[UpgradeType.FILTER.ordinal()]);
        assertEquals(2, taken[UpgradeType.SPEED.ordinal()]);
        assertEquals(0, table.size());
        assertTrue(table.get(A, 0).isEmpty());
    }

    @Test
    void tableDropsBlockWhenLastSetIsCleared() {
        UpgradeTable table = new UpgradeTable();
        table.set(A, UpgradeTable.BLOCK, Upgrades.EMPTY.with(UpgradeType.DIMENSION, 1));

        assertEquals(1, table.size());
        assertTrue(table.set(A, UpgradeTable.BLOCK, Upgrades.EMPTY));
        assertEquals(0, table.size());
    }

    @Test
    void typeIdsRoundTrip() {
        for (UpgradeType type : UpgradeType.VALUES) {
            assertEquals(type, UpgradeType.byId(type.id()));
        }
        assertEquals(null, UpgradeType.byId("nope"));
    }

    @Test
    void dimensionUpgradeIsWirelessOnly() {
        assertTrue(UpgradeType.DIMENSION.wireless());
        for (UpgradeType type : UpgradeType.VALUES) {
            assertEquals(type == UpgradeType.DIMENSION, type.wireless());
        }
    }

    @Test
    void crossDimensionLinkNeedsBothEnds() {
        Upgrades with = Upgrades.EMPTY.with(UpgradeType.DIMENSION, 1);

        assertTrue(UpgradeEffects.canLink(true, Upgrades.EMPTY, Upgrades.EMPTY));
        assertFalse(UpgradeEffects.canLink(false, Upgrades.EMPTY, Upgrades.EMPTY));
        assertFalse(UpgradeEffects.canLink(false, with, Upgrades.EMPTY));
        assertFalse(UpgradeEffects.canLink(false, Upgrades.EMPTY, with));
        assertTrue(UpgradeEffects.canLink(false, with, with));
    }

    @Test
    void storedCountsWithoutTheNewestTypeStillLoad() {
        Upgrades old = Upgrades.of(new int[]{1, 2, 3, 1, 1});

        assertEquals(1, old.count(UpgradeType.PRIORITY));
        assertFalse(old.has(UpgradeType.DIMENSION));
    }
}
