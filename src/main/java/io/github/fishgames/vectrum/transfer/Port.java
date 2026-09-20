package io.github.fishgames.vectrum.transfer;

/**
 * Loaderunabhängiger Zugang zu einem Speicher an einer Blockseite (Item-Inventar, Fluidtank, Energiespeicher). Der
 * gemeinsame Code braucht nur eine einzige Fähigkeit: Ware von einem Port zu einem anderen bewegen. Beide Ports
 * gehören immer zum selben Transporttyp und stammen vom selben Loader; die Umsetzungen liegen in
 * {@code platform/forge} und {@code platform/fabric}.
 *
 * <p>Einheiten je Transporttyp: Items in Stück, Fluide in Millibucket (mB), Energie in FE (bzw. E der Team Reborn
 * Energy API, 1:1).
 */
public interface Port {
    /**
     * Bewegt höchstens {@code max} Einheiten von diesem Port in {@code target}. Es geht nichts verloren: was das Ziel
     * nicht annimmt, bleibt in der Quelle.
     *
     * @return Anzahl der tatsächlich bewegten Einheiten
     */
    long moveTo(Port target, long max);
}
