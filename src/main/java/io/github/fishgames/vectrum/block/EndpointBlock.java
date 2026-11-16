package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.transport.TransportType;

import java.util.List;

/** Endpoint block: a thicker, always active network node. */
public class EndpointBlock extends ConduitBlock {
    public EndpointBlock(TransportType type, Properties properties) {
        super(List.of(type), properties,
                new ShapeSpec(2, 14, 3, 13),
                new ShapeSpec(4, 12, 5, 11));
    }

    @Override
    protected boolean alwaysActive() {
        return true;
    }
}
