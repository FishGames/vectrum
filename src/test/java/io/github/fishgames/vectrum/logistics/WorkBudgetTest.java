package io.github.fishgames.vectrum.logistics;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkBudgetTest {
    @Test
    void phaseStaysBelowTheIntervalAndIsFixedPerPosition() {
        for (int interval : new int[]{1, 2, 5, 10, 40}) {
            for (int i = 0; i < 500; i++) {
                BlockPos pos = new BlockPos(i * 7 - 900, i % 50, 3 * i);
                int phase = WorkBudget.phase(pos, interval);
                assertTrue(phase >= 0 && phase < interval, "phase " + phase + " for interval " + interval);
                assertEquals(phase, WorkBudget.phase(pos, interval));
            }
        }
    }

    @Test
    void neighbouringBlocksAreSpreadOverTheInterval() {
        Set<Integer> phases = new HashSet<>();
        for (int x = 0; x < 100; x++) {
            phases.add(WorkBudget.phase(new BlockPos(x, 64, 0), 10));
        }
        assertEquals(10, phases.size());
    }
}
