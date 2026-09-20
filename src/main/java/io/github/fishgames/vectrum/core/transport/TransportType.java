package io.github.fishgames.vectrum.core.transport;

import java.util.Objects;

/**
 * Art der transportierten Ware. Jeder Typ bildet sein eigenes, getrenntes Netz (auch im Universalkabel).
 * Fremdmods können später über die API eigene Typen ergänzen, deshalb ist das hier ein Record und kein Enum.
 *
 * @param id       eindeutige Kennung mit Namensraum, z. B. {@code "vectrum:item"}; dient auch als Name der Netz-Ebene
 * @param behavior wie der Typ übertragen wird
 */
public record TransportType(String id, Behavior behavior) {
    public TransportType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(behavior, "behavior");
    }

    /** Übertragungsverhalten: entscheidet, ob der Kern Mengen verschiebt oder einen Wert spiegelt. */
    public enum Behavior {
        /** Menge wird aus der Quelle entnommen und im Ziel eingefügt (Items, Fluide, Energie, Gase). */
        QUANTITY,
        /** Ein Zustandswert wird gespiegelt, nichts wird verbraucht (Redstone-Signalstärke). */
        SIGNAL
    }

    public static final TransportType ITEM = new TransportType("vectrum:item", Behavior.QUANTITY);
    public static final TransportType FLUID = new TransportType("vectrum:fluid", Behavior.QUANTITY);
    public static final TransportType ENERGY = new TransportType("vectrum:energy", Behavior.QUANTITY);
    public static final TransportType REDSTONE = new TransportType("vectrum:redstone", Behavior.SIGNAL);
    /** Nur mit Mekanism vorhanden; die Konstante liegt hier, damit der Kern keine Sonderfälle braucht. */
    public static final TransportType GAS = new TransportType("vectrum:gas", Behavior.QUANTITY);
}
