package io.github.fishgames.vectrum.core.network;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** All network layers: one {@link NetworkGraph} per transport type plus the digital layer. */
public final class NetworkRegistry {
    /** Layer name of the digital network. */
    public static final String DIGITAL_LAYER = "vectrum:digital";

    private final Map<String, NetworkGraph> graphs = new LinkedHashMap<>();

    /** Graph of the layer, created on first access. */
    public NetworkGraph graph(String layer) {
        Objects.requireNonNull(layer, "layer");
        return graphs.computeIfAbsent(layer, NetworkGraph::new);
    }

    /** Whether the layer exists. */
    public boolean hasLayer(String layer) {
        return graphs.containsKey(layer);
    }

    /** Network at the position in the layer, or {@code null}. */
    public Network networkAt(String layer, BlockCoord pos) {
        NetworkGraph graph = graphs.get(layer);
        return graph == null ? null : graph.networkAt(pos);
    }

    public Collection<NetworkGraph> graphs() {
        return Collections.unmodifiableCollection(graphs.values());
    }
}
