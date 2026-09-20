package io.github.fishgames.vectrum.core.upgrade;

import io.github.fishgames.vectrum.core.util.SaturatedMath;

/** Effects of upgrades: throughput limit, transfer interval, item type count, feature unlocks. */
public final class UpgradeEffects {
    /** Limit factor per throughput upgrade. */
    public static final long THROUGHPUT_FACTOR = 4;
    /** Item types per transfer without upgrades. */
    public static final int BASE_TYPES = 1;

    private UpgradeEffects() {
    }

    /** Throughput limit per transfer: {@code base * 4^count}, saturating at {@link Long#MAX_VALUE}. */
    public static long throughput(long base, Upgrades upgrades) {
        long result = base;
        for (int i = 0; i < upgrades.count(UpgradeType.THROUGHPUT); i++) {
            result = SaturatedMath.multiplySaturated(result, THROUGHPUT_FACTOR);
        }
        return result;
    }

    /** Ticks between transfers: base halved per speed upgrade, at least 1. */
    public static int interval(int baseTicks, Upgrades upgrades) {
        int shift = upgrades.count(UpgradeType.SPEED);
        return Math.max(1, baseTicks >> shift);
    }

    /** Item types allowed per transfer to one target. */
    public static int maxTypes(Upgrades upgrades) {
        return BASE_TYPES + upgrades.count(UpgradeType.TYPES);
    }

    public static boolean filterUnlocked(Upgrades upgrades) {
        return upgrades.has(UpgradeType.FILTER);
    }

    public static boolean priorityUnlocked(Upgrades upgrades) {
        return upgrades.has(UpgradeType.PRIORITY);
    }

    public static boolean dimensionUnlocked(Upgrades upgrades) {
        return upgrades.has(UpgradeType.DIMENSION);
    }

    /** Whether two wireless ports can link: same dimension, or both have the dimension upgrade. */
    public static boolean canLink(boolean sameDimension, Upgrades sender, Upgrades receiver) {
        return sameDimension || dimensionUnlocked(sender) && dimensionUnlocked(receiver);
    }
}
