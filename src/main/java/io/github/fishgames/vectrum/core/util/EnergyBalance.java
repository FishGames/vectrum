package io.github.fishgames.vectrum.core.util;

/** Fill balancing between two energy storages that can both give and receive. */
public final class EnergyBalance {
    private EnergyBalance() {
    }

    /**
     * Largest amount that may move from one two-way storage to the other without overshooting: moving it leaves both
     * with the same fill level. Nothing may move when the source is not fuller than the target.
     *
     * @param max amount the caller would move without balancing
     * @return amount from 0 to {@code max}; {@code max} unchanged when a capacity is unknown (zero or less)
     */
    public static long limit(long storedFrom, long capacityFrom, long storedTo, long capacityTo, long max) {
        if (capacityFrom <= 0 || capacityTo <= 0) {
            return max;
        }
        double equalising = (storedFrom * (double) capacityTo - storedTo * (double) capacityFrom)
                / ((double) capacityFrom + capacityTo);
        if (equalising < 1) {
            return 0;
        }
        return Math.min(max, (long) Math.ceil(equalising));
    }
}
