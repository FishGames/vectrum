package io.github.fishgames.vectrum.core.util;

/**
 * Zentrale Zahlen-Helfer (Kernentscheidung K5): intern wird mit {@code long} gerechnet, an der Grenze zu fremden
 * Schnittstellen (Forge Energy, FluidStack, ItemStack, ...) wird sauber auf {@code int} geklemmt.
 * Alle Funktionen laufen nie über, sondern sättigen am jeweiligen Grenzwert.
 */
public final class SaturatedMath {
    private SaturatedMath() {
    }

    /** Klemmt auf den Wertebereich von {@code int}. */
    public static int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    /** Klemmt auf {@code 0..Integer.MAX_VALUE}, negative Werte werden zu 0. */
    public static int clampToNonNegativeInt(long value) {
        if (value <= 0) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    /** Summe, die bei Überlauf am größten bzw. kleinsten {@code long}-Wert stehen bleibt. */
    public static long addSaturated(long a, long b) {
        long result = a + b;
        // Überlauf, wenn beide Summanden dasselbe Vorzeichen haben, das Ergebnis aber ein anderes
        if (((a ^ result) & (b ^ result)) < 0) {
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return result;
    }

    /** Produkt, das bei Überlauf am größten bzw. kleinsten {@code long}-Wert stehen bleibt. */
    public static long multiplySaturated(long a, long b) {
        long high = Math.multiplyHigh(a, b);
        long low = a * b;
        if ((high == 0 && low >= 0) || (high == -1 && low < 0)) {
            return low;
        }
        return (a ^ b) < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
    }
}
