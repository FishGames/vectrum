package io.github.fishgames.vectrum.core.network;

/** Persistable node state. */
public record NodeInfo(BlockCoord pos, NodeKind kind, int sideMask) {
}
