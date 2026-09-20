package io.github.fishgames.vectrum.core.routing;

import java.util.Locale;

/**
 * Wie eine Quelle ihre Ware auf gleichwertige Ziele (gleiche Priorität) verteilt. Wird an der Quelle eingestellt.
 */
public enum DistributionMode {
    /** Standard: das erste Ziel wird bis zum Anschlag gefüllt, dann das nächste. */
    SEQUENTIAL,
    /** Reihum: jede Übergabe beginnt beim Ziel hinter dem, das zuletzt beliefert wurde. */
    ROUND_ROBIN,
    /** Ausgleichen: leerere Ziele bekommen mehr, so dass sich die Füllstände angleichen. */
    BALANCED;

    public static final DistributionMode DEFAULT = SEQUENTIAL;

    /** Kleingeschriebener Name, z. B. für Befehle und Speicherung. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Sucht nach dem Namen (Groß-/Kleinschreibung egal); {@code null}, wenn es ihn nicht gibt. */
    public static DistributionMode byId(String id) {
        for (DistributionMode mode : values()) {
            if (mode.id().equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return null;
    }
}
