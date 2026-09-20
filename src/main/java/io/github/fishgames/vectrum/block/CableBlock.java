package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.transport.TransportType;

/**
 * Ein Transportkabel. Es hat keinen Blockentity. An Stellen, wo es an ein Inventar grenzt, übernimmt es von selbst die
 * Aufgabe eines Anschlusses (Quelle oder Ziel, umschaltbar mit dem Wrench); dazwischen leitet es nur.
 * Maße: Kollision 6 px, Auswahl und Modell 10 px (siehe {@code docs/implementierungs-prompt.md}).
 */
public class CableBlock extends ConduitBlock {
    public CableBlock(TransportType type, Properties properties) {
        super(type, properties,
                new ShapeSpec(3, 13, 3, 13, 2, 14, 3),   // Auswahl: Kern und Arme 10 px, Anschlussplatte 12 px
                new ShapeSpec(5, 11, 5, 11, 4, 12, 3));  // Kollision: Kern und Arme 6 px
    }

    @Override
    protected boolean alwaysActive() {
        return false;
    }
}
