package io.github.fishgames.vectrum.core.network;

/** Speicherbarer Zustand eines Knotens. Aus einer Liste davon lässt sich ein Graph vollständig neu aufbauen. */
public record NodeInfo(BlockCoord pos, NodeKind kind, int sideMask) {
}
