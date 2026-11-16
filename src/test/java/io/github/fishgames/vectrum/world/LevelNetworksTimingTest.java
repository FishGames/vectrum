package io.github.fishgames.vectrum.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Waiting phase of idle sources and the periodic state check. */
class LevelNetworksTimingTest {
    private static final BlockPos POS = new BlockPos(4, 5, 6);

    private static LevelNetworks networks() throws Exception {
        Method m = LevelNetworks.class.getDeclaredMethod("load", String.class, CompoundTag.class);
        m.setAccessible(true);
        return (LevelNetworks) m.invoke(null, "overworld", new CompoundTag());
    }

    @Test
    void idleSourceWaitsLongerStepByStepUpToTheMaximum() throws Exception {
        LevelNetworks networks = networks();
        int[] expected = {10, 10, 10, 15, 22, 33, 40, 40};
        for (int delay : expected) {
            assertEquals(delay, networks.nextDelay(POS, false, 10));
        }
    }

    @Test
    void aFastSourceKeepsItsIntervalDuringTheGracePeriod() throws Exception {
        LevelNetworks networks = networks();
        for (int i = 0; i < 30; i++) {
            assertEquals(1, networks.nextDelay(POS, false, 1));
        }
        assertEquals(15, networks.nextDelay(POS, false, 1));
    }

    @Test
    void aTransferEndsTheWaitingPhase() throws Exception {
        LevelNetworks networks = networks();
        for (int i = 0; i < 8; i++) {
            networks.nextDelay(POS, false, 10);
        }
        assertEquals(10, networks.nextDelay(POS, true, 10));
        assertEquals(10, networks.nextDelay(POS, false, 10));
    }

    @Test
    void wakingEndsTheWaitingPhase() throws Exception {
        LevelNetworks networks = networks();
        for (int i = 0; i < 8; i++) {
            networks.nextDelay(POS, false, 10);
        }
        networks.wakeIdle(POS);
        assertEquals(10, networks.nextDelay(POS, false, 10));
    }

    @Test
    void intervalAtOrAboveTheMaximumIsNeverStretched() throws Exception {
        LevelNetworks networks = networks();
        for (int i = 0; i < 10; i++) {
            assertEquals(40, networks.nextDelay(POS, false, 40));
        }
    }

    @Test
    void stateIsCheckedOncePerPeriod() throws Exception {
        LevelNetworks networks = networks();
        assertTrue(networks.refreshDue(POS, 1000));
        assertFalse(networks.refreshDue(POS, 1050));
        assertTrue(networks.refreshDue(POS, 1100));
        networks.noteRefreshed(POS, 1150);
        assertFalse(networks.refreshDue(POS, 1200));
    }
}
