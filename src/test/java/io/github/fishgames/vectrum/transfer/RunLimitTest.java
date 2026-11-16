package io.github.fishgames.vectrum.transfer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Time limit of one run. */
class RunLimitTest {
    @Test
    void noLimitNeverExpires() throws Exception {
        RunLimit.start(0);
        RunLimit.progress();
        Thread.sleep(2);
        assertFalse(RunLimit.expired());
    }

    @Test
    void aRunAlwaysCompletesOneUnitOfWork() throws Exception {
        RunLimit.start(1);
        Thread.sleep(2);
        assertFalse(RunLimit.expired());
        RunLimit.progress();
        assertTrue(RunLimit.expired());
    }

    @Test
    void expiresOnceTheTimeIsSpent() throws Exception {
        RunLimit.start(1_000_000);
        RunLimit.progress();
        assertFalse(RunLimit.expired());
        RunLimit.start(1);
        RunLimit.progress();
        Thread.sleep(2);
        assertTrue(RunLimit.expired());
    }

    @Test
    void startingAgainClearsTheProgress() {
        RunLimit.start(1);
        RunLimit.progress();
        RunLimit.start(1);
        assertFalse(RunLimit.expired());
    }
}
