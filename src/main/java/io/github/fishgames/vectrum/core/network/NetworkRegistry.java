package io.github.fishgames.vectrum.core.network;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Zentrale Übersicht aller Netz-Ebenen. Es gibt eine Ebene pro Transporttyp und eine für das digitale Netz. Jede
 * Ebene ist ein eigener {@link NetworkGraph}; Kabel desselben Blocks können dadurch in mehreren Ebenen liegen
 * (Universalkabel), ohne dass sich die Netze vermischen.
 */
public final class NetworkRegistry {
    /** Name der Ebene für das digitale Netz (Stufe 3). */
    public static final String DIGITAL_LAYER = "vectrum:digital";

    private final Map<String, NetworkGraph> graphs = new LinkedHashMap<>();

    /** Liefert die Ebene mit diesem Namen und legt sie beim ersten Zugriff an. */
    public NetworkGraph graph(String layer) {
        Objects.requireNonNull(layer, "layer");
        return graphs.computeIfAbsent(layer, NetworkGraph::new);
    }

    /** Das Netz an dieser Position in der angegebenen Ebene oder {@code null}. */
    public Network networkAt(String layer, BlockCoord pos) {
        NetworkGraph graph = graphs.get(layer);
        return graph == null ? null : graph.networkAt(pos);
    }

    public Collection<NetworkGraph> graphs() {
        return Collections.unmodifiableCollection(graphs.values());
    }
}
