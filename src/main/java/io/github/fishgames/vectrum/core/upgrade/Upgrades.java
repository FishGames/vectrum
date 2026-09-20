package io.github.fishgames.vectrum.core.upgrade;

import java.util.Arrays;

/** Upgrade counts of a block, 0 to {@link UpgradeType#maxCount()} per type. Immutable. */
public final class Upgrades {
    public static final Upgrades EMPTY = new Upgrades(new int[UpgradeType.VALUES.length]);

    private final int[] counts;

    private Upgrades(int[] counts) {
        this.counts = counts;
    }

    /** Builds from stored counts; missing entries are 0, values are clamped to 0..max. */
    public static Upgrades of(int[] stored) {
        int[] counts = new int[UpgradeType.VALUES.length];
        for (int i = 0; i < counts.length && i < stored.length; i++) {
            counts[i] = Math.max(0, Math.min(stored[i], UpgradeType.VALUES[i].maxCount()));
        }
        return isAllZero(counts) ? EMPTY : new Upgrades(counts);
    }

    public int count(UpgradeType type) {
        return counts[type.ordinal()];
    }

    public boolean has(UpgradeType type) {
        return counts[type.ordinal()] > 0;
    }

    public boolean isEmpty() {
        return this == EMPTY || isAllZero(counts);
    }

    public int total() {
        int sum = 0;
        for (int count : counts) {
            sum += count;
        }
        return sum;
    }

    /** Copy with the count of one type set, clamped to 0..max. */
    public Upgrades with(UpgradeType type, int count) {
        int[] copy = counts.clone();
        copy[type.ordinal()] = Math.max(0, Math.min(count, type.maxCount()));
        return isAllZero(copy) ? EMPTY : new Upgrades(copy);
    }

    /** Remaining slots for the type. */
    public int freeSlots(UpgradeType type) {
        return type.maxCount() - count(type);
    }

    /** One count per type in {@link UpgradeType} order. */
    public int[] toArray() {
        return counts.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Upgrades that && Arrays.equals(counts, that.counts);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(counts);
    }

    @Override
    public String toString() {
        return Arrays.toString(counts);
    }

    private static boolean isAllZero(int[] values) {
        for (int value : values) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }
}
