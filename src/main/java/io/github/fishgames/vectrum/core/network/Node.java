package io.github.fishgames.vectrum.core.network;

/** Graph node; {@code links} holds the connected neighbour per direction (or {@code null}). */
final class Node {
    final BlockCoord pos;
    NodeKind kind;
    /** Bit per {@link Direction}: side is enabled for connections. */
    int sideMask;
    final Node[] links = new Node[Direction.VALUES.length];
    Network network;

    Node(BlockCoord pos, NodeKind kind, int sideMask) {
        this.pos = pos;
        this.kind = kind;
        this.sideMask = sideMask;
    }

    boolean enabled(Direction direction) {
        return (sideMask & direction.bit()) != 0;
    }

    @Override
    public String toString() {
        return kind + "@" + pos;
    }
}
