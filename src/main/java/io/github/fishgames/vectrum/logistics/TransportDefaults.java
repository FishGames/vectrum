package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.core.transport.TransportType;

/**
 * Grundwerte je Transporttyp. Menge pro Uebergabe und Quellseite ohne Upgrades (Einheiten siehe
 * {@link io.github.fishgames.vectrum.transfer.Port}). Alle Typen takten gleich (siehe
 * {@link io.github.fishgames.vectrum.block.ConduitBlock#INTERVAL}), damit Upgrades spaeter nur an einer Stelle greifen.
 */
public final class TransportDefaults {
    /** Items: 4 Stueck (Wunsch des Projektinhabers). */
    public static final long ITEM_THROUGHPUT = 4;
    /** Fluide: 1000 mB, also ein Eimer pro Uebergabe. */
    public static final long FLUID_THROUGHPUT = 1000;
    /** Energie: 2000 FE pro Uebergabe (entspricht 200 FE pro Tick). */
    public static final long ENERGY_THROUGHPUT = 2000;

    private TransportDefaults() {
    }

    /** Grundlimit der Netz-Ebene mit dieser Kennung; unbekannte Ebenen bekommen den Item-Wert. */
    public static long baseThroughput(String layerId) {
        if (TransportType.FLUID.id().equals(layerId)) {
            return FLUID_THROUGHPUT;
        }
        if (TransportType.ENERGY.id().equals(layerId)) {
            return ENERGY_THROUGHPUT;
        }
        return ITEM_THROUGHPUT;
    }

    /** Schluessel des Einheitennamens fuer Meldungen ("Items", "mB", "FE"). */
    public static String unitKey(TransportType type) {
        String path = type.id().substring(type.id().indexOf(':') + 1);
        return "unit.vectrum." + path;
    }
}
