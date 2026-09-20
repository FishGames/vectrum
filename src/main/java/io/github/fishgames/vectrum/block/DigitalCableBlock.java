package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.transport.TransportType;

import java.util.List;

/** Digital cable: links coders in the digital layer; no ports, limit, upgrades or tick. */
public class DigitalCableBlock extends ConduitBlock {
    public DigitalCableBlock(Properties properties) {
        super(List.of(TransportType.DIGITAL), properties,
                new ShapeSpec(5, 11, 5, 11, 4, 12, 3),   // Selection shape
                new ShapeSpec(5, 11, 5, 11, 4, 12, 3));  // Collision shape
    }

    @Override
    protected boolean alwaysActive() {
        return false;
    }

    @Override
    protected boolean portless() {
        return true;
    }
}
