package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.transport.TransportType;

import java.util.List;

/**
 * Der Endpunkt-Block: ein etwas dickerer Netzknoten, der immer aktiv ist. Seit Kabel ihre Enden selbst übernehmen,
 * ist er für den Grundbetrieb nicht mehr nötig. Er bleibt vorerst erhalten und bekommt später eine eigene Aufgabe
 * (und dann auch einen anderen Namen).
 */
public class EndpointBlock extends ConduitBlock {
    public EndpointBlock(TransportType type, Properties properties) {
        super(List.of(type), properties,
                new ShapeSpec(2, 14, 3, 13, 4, 12, 2),
                new ShapeSpec(4, 12, 5, 11, 4, 12, 2));
    }

    @Override
    protected boolean alwaysActive() {
        return true;
    }
}
