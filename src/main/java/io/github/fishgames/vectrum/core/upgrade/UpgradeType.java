package io.github.fishgames.vectrum.core.upgrade;

import java.util.Locale;

/**
 * Die Upgrade-Sorten (ein einziger Satz für alle Transportkabel-Stufen). Jede Sorte darf höchstens
 * {@link #maxCount()} Mal in einem Baustein stecken.
 */
public enum UpgradeType {
    /** Höheres Durchsatzlimit pro Übergabe (jedes Upgrade vervierfacht es). */
    THROUGHPUT(6),
    /** Kürzerer Abstand zwischen zwei Übergaben (jedes Upgrade halbiert ihn). */
    SPEED(3),
    /** Mehr Item-Sorten gleichzeitig pro Übergabe (jedes Upgrade erlaubt eine Sorte mehr). */
    TYPES(4),
    /** Schaltet den Filter frei. */
    FILTER(1),
    /** Schaltet die Prioritätseinstellung frei. */
    PRIORITY(1);

    public static final UpgradeType[] VALUES = values();

    private final int maxCount;

    UpgradeType(int maxCount) {
        this.maxCount = maxCount;
    }

    public int maxCount() {
        return maxCount;
    }

    /** Kleingeschriebener Name, z. B. für Befehle und Speicherung. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Sucht nach dem Namen (Groß-/Kleinschreibung egal); {@code null}, wenn es ihn nicht gibt. */
    public static UpgradeType byId(String id) {
        for (UpgradeType type : VALUES) {
            if (type.id().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
