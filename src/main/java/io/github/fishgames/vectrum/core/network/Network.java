package io.github.fishgames.vectrum.core.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Eine zusammenhängende Gruppe von Knoten. Netze werden ausschließlich vom {@link NetworkGraph} erzeugt und
 * verändert; von außen ist nur Lesen möglich.
 */
public final class Network {
    private final long id;
    final Set<Node> nodes = new HashSet<>();
    int endpointCount;

    Network(long id) {
        this.id = id;
    }

    /** Fortlaufende Nummer innerhalb eines Graphen. Beim Verschmelzen bleibt die Nummer des größeren Netzes. */
    public long id() {
        return id;
    }

    /** Anzahl aller Knoten (Kabel und Endpunkte). */
    public int size() {
        return nodes.size();
    }

    /** Anzahl der Endpunkte. Ein Netz ohne Endpunkte braucht keine Rechenzeit. */
    public int endpointCount() {
        return endpointCount;
    }

    /** Kopie aller Positionen des Netzes (für Tests, Speichern und Diagnose, nicht für heiße Pfade). */
    public Set<BlockCoord> positions() {
        Set<BlockCoord> result = new HashSet<>(nodes.size() * 2);
        for (Node node : nodes) {
            result.add(node.pos);
        }
        return Collections.unmodifiableSet(result);
    }

    /** Kopie der Positionen aller Endpunkte. */
    public List<BlockCoord> endpointPositions() {
        List<BlockCoord> result = new ArrayList<>(endpointCount);
        for (Node node : nodes) {
            if (node.kind == NodeKind.ENDPOINT) {
                result.add(node.pos);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public String toString() {
        return "Network#" + id + "[" + nodes.size() + " Knoten, " + endpointCount + " Endpunkte]";
    }
}
