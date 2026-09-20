package io.github.fishgames.vectrum.transfer;

import java.util.function.Predicate;

/**
 * Loaderunabhängiger Zugang zu einem Speicher an einer Blockseite (Item-Inventar, Fluidtank, Energiespeicher). Der
 * gemeinsame Code braucht nur zwei Fähigkeiten: Ware von einem Port zu einem anderen bewegen und den Füllstand
 * nennen. Beide Ports gehören immer zum selben Transporttyp und stammen vom selben Loader; die Umsetzungen liegen in
 * {@code platform/forge} und {@code platform/fabric}.
 *
 * <p>Einheiten je Transporttyp: Items in Stück, Fluide in Millibucket (mB), Energie in FE (bzw. E der Team Reborn
 * Energy API, 1:1).
 *
 * <p>Ressourcen werden dem Filter als Kennung übergeben (z. B. {@code "minecraft:cobblestone"}, {@code "minecraft:water"}),
 * siehe {@link ResourceIds}. Energie kennt keine Sorten und ignoriert den Filter.
 */
public interface Port {
    /** Filter, der alles durchlässt. Wird per Identität erkannt, damit sich der schnelle Weg lohnt. */
    Predicate<String> ALL = id -> true;

    /**
     * Bewegt höchstens {@code max} Einheiten von diesem Port in {@code target}, nur Ware, die {@code filter} erfüllt.
     * Es geht nichts verloren: was das Ziel nicht annimmt, bleibt in der Quelle.
     *
     * @return Anzahl der tatsächlich bewegten Einheiten
     */
    long moveTo(Port target, long max, Predicate<String> filter);

    /** Ohne Filter. */
    default long moveTo(Port target, long max) {
        return moveTo(target, max, ALL);
    }

    /**
     * Füllstand von 0 (leer) bis 1 (voll). Wird nur für den Verteilmodus „Ausgleichen“ abgefragt, also selten.
     * Ist der Speicher nicht bestimmbar, gilt er als leer.
     */
    default double fillLevel() {
        return 0;
    }
}
