package io.github.fishgames.vectrum.core.network;

import java.util.List;

/** Callbacks for network changes reported by a {@link NetworkGraph}. All methods are optional. */
public interface NetworkListener {
    /** A network was created. */
    default void onCreated(Network network) {
    }

    /** {@code absorbed} was merged into {@code survivor}. */
    default void onMerged(Network survivor, Network absorbed) {
    }

    /** {@code original} was split; {@code newParts} are the new networks. */
    default void onSplit(Network original, List<Network> newParts) {
    }

    /** The network lost its last node. */
    default void onDissolved(Network network) {
    }
}
