package io.github.fishgames.vectrum.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyBalanceTest {
    @Test
    void movesHalfTheDifferenceBetweenEqualStorages() {
        assertEquals(200, EnergyBalance.limit(800, 1000, 400, 1000, 10_000));
    }

    @Test
    void nothingMovesFromTheEmptierToTheFullerStorage() {
        assertEquals(0, EnergyBalance.limit(400, 1000, 800, 1000, 10_000));
        assertEquals(0, EnergyBalance.limit(500, 1000, 500, 1000, 10_000));
    }

    @Test
    void equalFillLevelsAreTheTargetForDifferentCapacities() {
        // 500 of 1000 into an empty 3000: both end at 125/1000 and 375/3000
        assertEquals(375, EnergyBalance.limit(500, 1000, 0, 3000, 10_000));
    }

    @Test
    void theCallersMaximumStillApplies() {
        assertEquals(10, EnergyBalance.limit(800, 1000, 0, 1000, 10));
    }

    @Test
    void unknownCapacityLeavesTheAmountAlone() {
        assertEquals(77, EnergyBalance.limit(5, 0, 0, 1000, 77));
    }
}
