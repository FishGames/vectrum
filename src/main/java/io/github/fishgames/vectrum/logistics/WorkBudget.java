package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.core.config.VectrumConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

/**
 * Time budget of all transfers within one server tick, shared by all dimensions. The first transfer of a tick starts
 * the clock; once the budget is spent, further transfers wait for the next tick. A budget of 0 disables the limit.
 * Also gives each block its start offset so that the runs of many blocks spread over the ticks.
 */
public final class WorkBudget {
    private static int tick = -1;
    private static long deadline;
    private static boolean limited;

    private WorkBudget() {
    }

    /** Starts the budget of the current server tick when it has not started yet. */
    public static void begin(MinecraftServer server) {
        int now = server.getTickCount();
        if (now == tick) {
            return;
        }
        tick = now;
        long micros = VectrumConfig.get().timeBudgetMicros();
        limited = micros > 0;
        deadline = limited ? System.nanoTime() + micros * 1000 : 0;
    }

    /**
     * Start offset of a block in ticks, from 0 up to {@code interval - 1}, fixed per position. Blocks placed or loaded
     * together then do not all run in the same tick.
     */
    public static int phase(BlockPos pos, int interval) {
        if (interval <= 1) {
            return 0;
        }
        long hash = pos.asLong() * 0x9E3779B97F4A7C15L;
        hash ^= hash >>> 32;
        hash *= 0xC2B2AE3D27D4EB4FL;
        hash ^= hash >>> 29;
        return (int) ((hash >>> 1) % interval);
    }

    /** Whether the budget of the current tick is spent. */
    public static boolean exhausted() {
        return limited && System.nanoTime() - deadline >= 0;
    }
}
