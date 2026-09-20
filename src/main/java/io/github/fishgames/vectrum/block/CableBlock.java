package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.transport.TransportType;

import java.util.List;

/** Transport cable; sides touching an inventory act as source or target ports. */
public class CableBlock extends ConduitBlock {
    public CableBlock(TransportType type, Properties properties) {
        this(List.of(type), properties);
    }

    /** Universal cable carrying several types. */
    public CableBlock(List<TransportType> types, Properties properties) {
        super(types, properties,
                new ShapeSpec(3, 13, 3, 13, 2, 14, 3),   // Selection shape
                new ShapeSpec(5, 11, 5, 11, 4, 12, 3));  // Collision shape
    }

    @Override
    protected boolean alwaysActive() {
        return false;
    }
}
