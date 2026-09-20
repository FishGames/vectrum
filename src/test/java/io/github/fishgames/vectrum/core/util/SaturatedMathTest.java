package io.github.fishgames.vectrum.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaturatedMathTest {
    @Test
    void clampToIntKeepsValuesInRange() {
        assertEquals(0, SaturatedMath.clampToInt(0));
        assertEquals(123, SaturatedMath.clampToInt(123));
        assertEquals(-123, SaturatedMath.clampToInt(-123));
        assertEquals(Integer.MAX_VALUE, SaturatedMath.clampToInt(Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, SaturatedMath.clampToInt(Integer.MIN_VALUE));
    }

    @Test
    void clampToIntSaturatesInsteadOfOverflowing() {
        assertEquals(Integer.MAX_VALUE, SaturatedMath.clampToInt((long) Integer.MAX_VALUE + 1));
        assertEquals(Integer.MAX_VALUE, SaturatedMath.clampToInt(Long.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, SaturatedMath.clampToInt((long) Integer.MIN_VALUE - 1));
        assertEquals(Integer.MIN_VALUE, SaturatedMath.clampToInt(Long.MIN_VALUE));
    }

    @Test
    void clampToNonNegativeIntTurnsNegativeIntoZero() {
        assertEquals(0, SaturatedMath.clampToNonNegativeInt(-5));
        assertEquals(0, SaturatedMath.clampToNonNegativeInt(Long.MIN_VALUE));
        assertEquals(7, SaturatedMath.clampToNonNegativeInt(7));
        assertEquals(Integer.MAX_VALUE, SaturatedMath.clampToNonNegativeInt(Long.MAX_VALUE));
    }

    @Test
    void addSaturatedStopsAtLimits() {
        assertEquals(5L, SaturatedMath.addSaturated(2, 3));
        assertEquals(-1L, SaturatedMath.addSaturated(2, -3));
        assertEquals(Long.MAX_VALUE, SaturatedMath.addSaturated(Long.MAX_VALUE, 1));
        assertEquals(Long.MAX_VALUE, SaturatedMath.addSaturated(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, SaturatedMath.addSaturated(Long.MIN_VALUE, -1));
        assertEquals(Long.MAX_VALUE - 1, SaturatedMath.addSaturated(Long.MAX_VALUE, -1));
    }

    @Test
    void multiplySaturatedStopsAtLimits() {
        assertEquals(12L, SaturatedMath.multiplySaturated(3, 4));
        assertEquals(-12L, SaturatedMath.multiplySaturated(3, -4));
        assertEquals(0L, SaturatedMath.multiplySaturated(0, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, SaturatedMath.multiplySaturated(Long.MAX_VALUE, 2));
        assertEquals(Long.MAX_VALUE, SaturatedMath.multiplySaturated(-3_000_000_000_000L, -4_000_000_000_000L));
        assertEquals(Long.MIN_VALUE, SaturatedMath.multiplySaturated(Long.MAX_VALUE, -2));
        assertEquals(Long.MIN_VALUE, SaturatedMath.multiplySaturated(Long.MIN_VALUE, 5));
        // Zwischenergebnis Basis * Faktor würde überlaufen, obwohl das Endergebnis eines Upgrade-Aufrufs sinnvoll ist
        assertEquals(1_000_000_000_000L, SaturatedMath.multiplySaturated(1_000_000L, 1_000_000L));
    }
}
