package io.github.fishgames.vectrum.block;

/** Rolle einer Endpunkt-Seite, die an ein Inventar grenzt. Wird im Blockentity gespeichert. */
public enum EndpointMode {
    /** Seite ist abgeschaltet: der Endpunkt fasst das Inventar nicht an. */
    OFF("off"),
    /** Quelle: entnimmt Ware aus dem Inventar und gibt sie ans Netz. */
    IN("in"),
    /** Ziel: nimmt Ware aus dem Netz und legt sie ins Inventar. */
    OUT("out");

    /** Standard für neue Seiten: Ziel. So wird nichts ungewollt aus einer Kiste gezogen. */
    public static final EndpointMode DEFAULT = OUT;

    private final String name;

    EndpointMode(String name) {
        this.name = name;
    }

    /** Nächste Rolle beim Umschalten mit dem Wrench: Ziel, Quelle, Aus, wieder Ziel. */
    public EndpointMode next() {
        return switch (this) {
            case OUT -> IN;
            case IN -> OFF;
            case OFF -> OUT;
        };
    }

    public String translationKey() {
        return "mode.vectrum." + name;
    }

    public static EndpointMode byOrdinal(int ordinal) {
        EndpointMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DEFAULT;
    }
}
