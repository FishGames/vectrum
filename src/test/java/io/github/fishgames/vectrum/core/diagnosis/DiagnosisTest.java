package io.github.fishgames.vectrum.core.diagnosis;

import io.github.fishgames.vectrum.core.routing.ResourceFilter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosisTest {
    private static SourceFacts facts(int open, int full, int filtered, int unloaded, int missing, long moved, long budget) {
        return new SourceFacts(true, false, open, full, filtered, unloaded, missing, 0, false, moved, budget, true);
    }

    @Test
    void withoutNetworkNothingElseIsReported() {
        SourceFacts facts = new SourceFacts(false, false, 3, 0, 0, 0, 0, 0, false, 0, 0, false);
        assertEquals(List.of(FlowReason.NO_NETWORK), Diagnosis.forSource(facts));
    }

    @Test
    void noTargetsMeansNoTarget() {
        assertEquals(List.of(FlowReason.NO_TARGET), Diagnosis.forSource(facts(0, 0, 0, 0, 0, 0, 4)));
    }

    @Test
    void wirelessWithoutReceiversAndWithLockedOnes() {
        SourceFacts none = new SourceFacts(true, false, 0, 0, 0, 0, 0, 0, true, 0, 0, false);
        SourceFacts locked = new SourceFacts(true, false, 0, 0, 0, 0, 0, 2, true, 0, 0, false);
        assertEquals(List.of(FlowReason.NO_RECEIVER), Diagnosis.forSource(none));
        assertEquals(List.of(FlowReason.DIMENSION_LOCKED), Diagnosis.forSource(locked));
    }

    @Test
    void emptySourceWithoutRecentTransferIsReportedFirst() {
        SourceFacts facts = new SourceFacts(true, true, 2, 0, 0, 0, 0, 0, false, 0, 4, true);
        assertEquals(List.of(FlowReason.SOURCE_EMPTY), Diagnosis.forSource(facts));
    }

    @Test
    void openTargetsWithMovedGoodsFlow() {
        assertEquals(List.of(FlowReason.FLOWING), Diagnosis.forSource(facts(1, 0, 0, 0, 0, 2, 4)));
    }

    @Test
    void movedEqualToTheBudgetReportsTheLimit() {
        assertEquals(List.of(FlowReason.FLOWING, FlowReason.LIMIT_REACHED),
                Diagnosis.forSource(facts(1, 0, 0, 0, 0, 4, 4)));
    }

    @Test
    void openTargetsWithoutMovementAreIdle() {
        assertEquals(List.of(FlowReason.IDLE), Diagnosis.forSource(facts(1, 0, 0, 0, 0, 0, 4)));
    }

    @Test
    void noOpenTargetListsEveryKind() {
        assertEquals(List.of(FlowReason.TARGET_FULL, FlowReason.FILTER_BLOCKS_ALL, FlowReason.TARGET_UNLOADED),
                Diagnosis.forSource(facts(0, 1, 1, 1, 0, 0, 4)));
    }

    @Test
    void unloadedTargetsAreMentionedNextToOpenOnes() {
        assertEquals(List.of(FlowReason.FLOWING, FlowReason.TARGET_UNLOADED),
                Diagnosis.forSource(facts(1, 0, 0, 1, 0, 1, 4)));
    }

    @Test
    void targetSideReasons() {
        assertEquals(List.of(FlowReason.NO_NETWORK), Diagnosis.forTarget(new TargetFacts(false, 2, false, false)));
        assertEquals(List.of(FlowReason.NO_SOURCE), Diagnosis.forTarget(new TargetFacts(true, 0, false, false)));
        assertEquals(List.of(FlowReason.TARGET_FULL), Diagnosis.forTarget(new TargetFacts(true, 1, true, false)));
        assertEquals(List.of(FlowReason.FLOWING), Diagnosis.forTarget(new TargetFacts(true, 1, false, true)));
        assertEquals(List.of(FlowReason.IDLE), Diagnosis.forTarget(new TargetFacts(true, 1, false, false)));
    }

    @Test
    void filterCombinations() {
        ResourceFilter dirt = new ResourceFilter(false, Set.of("minecraft:dirt"));
        ResourceFilter stone = new ResourceFilter(false, Set.of("minecraft:stone"));
        ResourceFilter notDirt = new ResourceFilter(true, Set.of("minecraft:dirt"));
        ResourceFilter notStone = new ResourceFilter(true, Set.of("minecraft:stone"));
        assertTrue(Diagnosis.passNothing(dirt, stone));
        assertFalse(Diagnosis.passNothing(dirt, dirt));
        assertTrue(Diagnosis.passNothing(dirt, notDirt));
        assertTrue(Diagnosis.passNothing(notDirt, dirt));
        assertFalse(Diagnosis.passNothing(dirt, notStone));
        assertFalse(Diagnosis.passNothing(notDirt, notStone));
        assertFalse(Diagnosis.passNothing(ResourceFilter.NONE, dirt));
        assertFalse(Diagnosis.passNothing(dirt, ResourceFilter.NONE));
    }

    @Test
    void onlyRealStallsAreProblems() {
        assertFalse(FlowReason.FLOWING.isProblem());
        assertFalse(FlowReason.IDLE.isProblem());
        assertFalse(FlowReason.LIMIT_REACHED.isProblem());
        assertTrue(FlowReason.TARGET_FULL.isProblem());
    }
}
