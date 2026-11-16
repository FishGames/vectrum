package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.transport.TransportType;

import java.util.List;

/** Digital cable: links coders in the digital layer; no ports, limit, upgrades or tick. */
public class DigitalCableBlock extends ConduitBlock {
    public DigitalCableBlock(Properties properties) {
        super(List.of(TransportType.DIGITAL), properties,
                new ShapeSpec(6, 10, 6, 10),    // Selection shape (4 px, like the model)
                new ShapeSpec(6, 10, 6, 10),    // Collision shape (4 px)
                new ShapeSpec(5, 11, 5, 11));   // Placement shape while a cable, coder, wrench... is held (6 px)
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
