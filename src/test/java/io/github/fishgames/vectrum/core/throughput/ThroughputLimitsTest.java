package io.github.fishgames.vectrum.core.throughput;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThroughputLimitsTest {
    private static final String DIM = "minecraft:overworld";
    private static final BlockCoord A = new BlockCoord(DIM, 0, 0, 0);
    private static final BlockCoord B = new BlockCoord(DIM, 1, 0, 0);

    private final ThroughputLimits limits = new ThroughputLimits(4);

    @Test
    void unknownBlocksUseTheBaseLimit() {
        assertEquals(4, limits.base());
        assertEquals(4, limits.limitOf(A));
        assertEquals(4, limits.budgetOf(A));
        assertEquals(0, limits.overrideCount());
    }

    @Test
    void ownValueOverridesTheBaseOnlyForThatBlock() {
        assertTrue(limits.set(A, 64));

        assertEquals(64, limits.limitOf(A));
        assertEquals(4, limits.limitOf(B));
        assertEquals(1, limits.overrideCount());
    }

    @Test
    void settingTheSameValueAgainReportsNoChange() {
        limits.set(A, 64);
        assertFalse(limits.set(A, 64));
    }

    @Test
    void settingTheBaseValueStoresNothing() {
        limits.set(A, 64);
        assertTrue(limits.set(A, 4));

        assertEquals(4, limits.limitOf(A));
        assertEquals(0, limits.overrideCount());
        assertFalse(limits.set(B, 4));
    }

    @Test
    void resetReturnsToTheBaseLimit() {
        limits.set(A, 64);

        assertTrue(limits.reset(A));
        assertEquals(4, limits.limitOf(A));
        assertFalse(limits.reset(A));
    }

    @Test
    void zeroIsAValidLimitAndBlocksTheTransfer() {
        limits.set(A, 0);

        assertEquals(0, limits.limitOf(A));
        assertEquals(0, limits.budgetOf(A));
        assertEquals(1, limits.overrideCount());
    }

    @Test
    void negativeValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> limits.set(A, -1));
        assertThrows(IllegalArgumentException.class, () -> new ThroughputLimits(-5));
        assertEquals(4, limits.limitOf(A));
    }

    @Test
    void hugeValuesStayExactInternallyAndAreClampedAtTheBoundary() {
        long huge = 5_000_000_000L; // mehr als ein int
        limits.set(A, huge);
        limits.set(B, Long.MAX_VALUE);

        assertEquals(huge, limits.limitOf(A));
        assertEquals(Integer.MAX_VALUE, limits.budgetOf(A));
        assertEquals(Long.MAX_VALUE, limits.limitOf(B));
        assertEquals(Integer.MAX_VALUE, limits.budgetOf(B));
    }

    @Test
    void overridesViewIsReadOnlyAndListsAllEntries() {
        limits.set(A, 8);
        limits.set(B, 16);

        assertEquals(Map.of(A, 8L, B, 16L), limits.overrides());
        assertThrows(UnsupportedOperationException.class, () -> limits.overrides().clear());
    }

    @Test
    void manyBlocksDoNotSlowDownTheLookup() {
        ThroughputLimits big = new ThroughputLimits(4);
        for (int x = 0; x < 100_000; x++) {
            big.set(new BlockCoord(DIM, x, 0, 0), 8 + (x % 7));
        }

        long start = System.nanoTime();
        long sum = 0;
        for (int x = 0; x < 100_000; x++) {
            sum += big.limitOf(new BlockCoord(DIM, x, 0, 0));
        }
        long micros = (System.nanoTime() - start) / 1000;

        assertTrue(sum > 0);
        System.out.println("100.000 Limit-Nachschlage: " + micros + " us");
        assertTrue(micros < 200_000, "Nachschlagen war zu langsam: " + micros + " us");
    }
}
