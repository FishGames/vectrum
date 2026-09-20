package io.github.fishgames.vectrum.core.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Connected group of nodes; read-only view of a {@link NetworkGraph} component. */
public final class Network {
    private final long id;
    final Set<Node> nodes = new HashSet<>();
    int endpointCount;

    Network(long id) {
        this.id = id;
    }

    /** Network id, unique within a graph. */
    public long id() {
        return id;
    }

    /** Number of nodes (cables and endpoints). */
    public int size() {
        return nodes.size();
    }

    /** Number of endpoints. */
    public int endpointCount() {
        return endpointCount;
    }

    /** Copy of all node positions. */
    public Set<BlockCoord> positions() {
        Set<BlockCoord> result = new HashSet<>(nodes.size() * 2);
        for (Node node : nodes) {
            result.add(node.pos);
        }
        return Collections.unmodifiableSet(result);
    }

    /** Copy of all endpoint positions. */
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
        return "Network#" + id + "[" + nodes.size() + " nodes, " + endpointCount + " endpoints]";
    }
}
