package io.github.fishgames.vectrum.core.util;

/** Saturating and clamping number helpers. */
public final class SaturatedMath {
    private SaturatedMath() {
    }

    /** Clamps to the {@code int} range. */
    public static int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    /** Clamps to {@code 0..Integer.MAX_VALUE}. */
    public static int clampToNonNegativeInt(long value) {
        if (value <= 0) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    /** Sum saturating at {@link Long#MIN_VALUE} / {@link Long#MAX_VALUE}. */
    public static long addSaturated(long a, long b) {
        long result = a + b;
        // Overflow check
        if (((a ^ result) & (b ^ result)) < 0) {
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return result;
    }

    /** Product saturating at {@link Long#MIN_VALUE} / {@link Long#MAX_VALUE}. */
    public static long multiplySaturated(long a, long b) {
        long high = Math.multiplyHigh(a, b);
        long low = a * b;
        if ((high == 0 && low >= 0) || (high == -1 && low < 0)) {
            return low;
        }
        return (a ^ b) < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
    }
}
