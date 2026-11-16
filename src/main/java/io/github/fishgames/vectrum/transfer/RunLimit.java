package io.github.fishgames.vectrum.transfer;

/**
 * Time limit of one run of a source block. Ports check it between units of work (stacks, chunks) and stop early; the
 * rest of the goods waits for the next run. A run always completes at least one unit, so it makes progress however
 * small the limit is. Used from the server thread only.
 */
public final class RunLimit {
    private static boolean limited;
    private static long deadline;
    private static boolean progressed;

    private RunLimit() {
    }

    /** Starts the limit of a run; {@code micros} of 0 or less means no limit. */
    public static void start(long micros) {
        limited = micros > 0;
        deadline = limited ? System.nanoTime() + micros * 1000 : 0;
        progressed = false;
    }

    /** Notes that a unit of work was completed. */
    public static void progress() {
        progressed = true;
    }

    /** Whether the run should stop: its time is spent and it has completed a unit of work. */
    public static boolean expired() {
        return limited && progressed && System.nanoTime() - deadline >= 0;
    }
}
