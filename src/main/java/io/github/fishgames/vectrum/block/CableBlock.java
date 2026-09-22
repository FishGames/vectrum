package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.transport.TransportType;

import java.util.List;

/** Transport cable; sides touching an inventory act as source or target ports. */
public class CableBlock extends ConduitBlock {
    /** Placement shape while the wrench, a cable, a coder or a wireless port is held; matches the old fixed size. */
    private static final ShapeSpec WIDE_SELECTION = new ShapeSpec(3, 13, 3, 13, 2, 14, 3);
    /** Model, collision and default placement shape of a single-type cable. */
    private static final ShapeSpec COMPACT_SELECTION = new ShapeSpec(5, 11, 5, 11, 2, 14, 3);
    private static final ShapeSpec COLLISION = new ShapeSpec(5, 11, 5, 11, 4, 12, 3);

    /**
     * Single-type cable: 6 px model and collision, with the placement shape widening to 10 px while the wrench, a
     * cable, a coder or a wireless port is held, so an empty hand or an unrelated item can click past it instead.
     */
    public CableBlock(TransportType type, Properties properties) {
        super(List.of(type), properties, COMPACT_SELECTION, COLLISION, WIDE_SELECTION);
    }

    /** Universal cable carrying several types: unchanged fixed 10 px model, selection and placement shape. */
    public CableBlock(List<TransportType> types, Properties properties) {
        super(types, properties, WIDE_SELECTION, COLLISION);
    }

    @Override
    protected boolean alwaysActive() {
        return false;
    }
}
