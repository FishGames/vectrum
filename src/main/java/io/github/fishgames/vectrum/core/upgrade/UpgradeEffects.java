package io.github.fishgames.vectrum.core.upgrade;

import io.github.fishgames.vectrum.core.util.SaturatedMath;

/**
 * Was die Upgrades bewirken, als reine Rechnung. Alle Werte werden einmal beim Ändern der Upgrades berechnet und
 * gespeichert bzw. beim Takt nachgeschlagen, nie über das Netz berechnet (K3). Zwischenrechnungen sind gesättigt
 * (K5), ein Überlauf ist ausgeschlossen.
 */
public final class UpgradeEffects {
    /** Jedes Durchsatz-Upgrade multipliziert das Limit mit diesem Faktor. */
    public static final long THROUGHPUT_FACTOR = 4;
    /** Ohne Upgrade wird pro Übergabe an ein Ziel diese Zahl Item-Sorten bewegt. */
    public static final int BASE_TYPES = 1;

    private UpgradeEffects() {
    }

    /** Durchsatzlimit pro Übergabe: {@code base × 4^Anzahl}, nach oben bei {@link Long#MAX_VALUE} gedeckelt. */
    public static long throughput(long base, Upgrades upgrades) {
        long result = base;
        for (int i = 0; i < upgrades.count(UpgradeType.THROUGHPUT); i++) {
            result = SaturatedMath.multiplySaturated(result, THROUGHPUT_FACTOR);
        }
        return result;
    }

    /** Ticks zwischen zwei Übergaben: jedes Speed-Upgrade halbiert den Grundwert, mindestens 1. */
    public static int interval(int baseTicks, Upgrades upgrades) {
        int shift = upgrades.count(UpgradeType.SPEED);
        return Math.max(1, baseTicks >> shift);
    }

    /** Wie viele verschiedene Item-Sorten pro Übergabe an ein Ziel bewegt werden dürfen. */
    public static int maxTypes(Upgrades upgrades) {
        return BASE_TYPES + upgrades.count(UpgradeType.TYPES);
    }

    public static boolean filterUnlocked(Upgrades upgrades) {
        return upgrades.has(UpgradeType.FILTER);
    }

    public static boolean priorityUnlocked(Upgrades upgrades) {
        return upgrades.has(UpgradeType.PRIORITY);
    }
}
