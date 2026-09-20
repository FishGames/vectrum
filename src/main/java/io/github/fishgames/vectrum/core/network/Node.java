package io.github.fishgames.vectrum.core.network;

/**
 * Interner Knoten des Graphen. {@code links} speichert je Richtung den tatsächlich verbundenen Nachbarn
 * (oder {@code null}), damit Suchläufe reine Zeigersprünge sind und keine Koordinaten nachschlagen müssen.
 */
final class Node {
    final BlockCoord pos;
    final NodeKind kind;
    /** Bit pro {@link Direction}: Seite ist für Verbindungen freigegeben. */
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
