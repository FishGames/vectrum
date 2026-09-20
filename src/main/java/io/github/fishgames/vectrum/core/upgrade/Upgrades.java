package io.github.fishgames.vectrum.core.upgrade;

import java.util.Arrays;

/**
 * Die Upgrades, die in einem Baustein stecken: je Sorte eine Anzahl von 0 bis {@link UpgradeType#maxCount()}.
 * Unveränderlich; Änderungen liefern eine neue Instanz.
 */
public final class Upgrades {
    public static final Upgrades EMPTY = new Upgrades(new int[UpgradeType.VALUES.length]);

    private final int[] counts;

    private Upgrades(int[] counts) {
        this.counts = counts;
    }

    /** Baut aus gespeicherten Zahlen (z. B. NBT). Zu kurze Felder werden mit 0 aufgefüllt, ungültige Werte geklemmt. */
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

    /** Setzt die Anzahl einer Sorte, geklemmt auf 0 bis Höchstzahl. */
    public Upgrades with(UpgradeType type, int count) {
        int[] copy = counts.clone();
        copy[type.ordinal()] = Math.max(0, Math.min(count, type.maxCount()));
        return isAllZero(copy) ? EMPTY : new Upgrades(copy);
    }

    /** Wie viele weitere Upgrades dieser Sorte noch hineinpassen. */
    public int freeSlots(UpgradeType type) {
        return type.maxCount() - count(type);
    }

    /** Für die Speicherung: eine Zahl je Sorte in der Reihenfolge von {@link UpgradeType}. */
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
