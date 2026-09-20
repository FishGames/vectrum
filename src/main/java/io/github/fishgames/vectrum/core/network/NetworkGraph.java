package io.github.fishgames.vectrum.core.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Graph aller Kabel- und Endpunkt-Positionen einer Ebene (ein Transporttyp oder das digitale Netz), im Speicher.
 *
 * <p>Änderungen sind <b>inkrementell</b>:
 * <ul>
 *   <li>Setzen eines Knotens verschmilzt angrenzende Netze, immer das kleinere in das größere.</li>
 *   <li>Entfernen eines Knotens (oder Sperren einer Seite) prüft nur die unmittelbar betroffenen Nachbarn. Dazu laufen
 *       Suchen gleichzeitig los; sobald sich zwei treffen, sind sie verbunden, und sobald eine Suche nichts mehr
 *       Neues findet, ist ihr Teil ein abgetrenntes Netz. Der Aufwand hängt vom <i>kleineren</i> Teil ab, nicht von
 *       der Größe des ganzen Netzes.</li>
 * </ul>
 *
 * <p>Zwei benachbarte Knoten sind verbunden, wenn beide die jeweils zugewandte Seite freigegeben haben. Die Klasse
 * ist nicht thread-sicher; Minecraft ruft sie ausschließlich aus dem Server-Thread auf.
 */
public final class NetworkGraph {
    private final String layer;
    private final Map<BlockCoord, Node> nodes = new HashMap<>();
    private final Set<Network> networks = new LinkedHashSet<>();
    private final List<NetworkListener> listeners = new ArrayList<>();
    private long nextNetworkId = 1;

    public NetworkGraph(String layer) {
        this.layer = Objects.requireNonNull(layer, "layer");
    }

    public String layer() {
        return layer;
    }

    public void addListener(NetworkListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    // ------------------------------------------------------------------------------------------ Abfragen

    public boolean contains(BlockCoord pos) {
        return nodes.containsKey(pos);
    }

    /** Das Netz an dieser Position oder {@code null}, wenn dort kein Knoten steht. */
    public Network networkAt(BlockCoord pos) {
        Node node = nodes.get(pos);
        return node == null ? null : node.network;
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int networkCount() {
        return networks.size();
    }

    public Collection<Network> networks() {
        return Collections.unmodifiableCollection(networks);
    }

    /** Zustand aller Knoten zum Speichern. Wiederherstellen: alle Einträge der Reihe nach mit {@link #add} setzen. */
    /** Art und freigegebene Seiten des Knotens an dieser Position oder {@code null}, wenn dort keiner steht. */
    public NodeInfo info(BlockCoord pos) {
        Node node = nodes.get(pos);
        return node == null ? null : new NodeInfo(node.pos, node.kind, node.sideMask);
    }

    public List<NodeInfo> snapshot() {
        List<NodeInfo> result = new ArrayList<>(nodes.size());
        for (Node node : nodes.values()) {
            result.add(new NodeInfo(node.pos, node.kind, node.sideMask));
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------ Änderungen

    /**
     * Setzt einen Knoten und verbindet ihn mit passenden Nachbarn.
     *
     * @param sideMask Bit pro {@link Direction}; {@link Direction#ALL_MASK} = alle Seiten offen
     * @return das Netz, zu dem der Knoten danach gehört
     * @throws IllegalStateException wenn an der Position schon ein Knoten steht
     */
    public Network add(BlockCoord pos, NodeKind kind, int sideMask) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(kind, "kind");
        if (nodes.containsKey(pos)) {
            throw new IllegalStateException("Position ist bereits belegt: " + pos);
        }

        Node node = new Node(pos, kind, sideMask & Direction.ALL_MASK);
        nodes.put(pos, node);
        for (Direction direction : Direction.VALUES) {
            updateLink(node, direction);
        }

        List<Network> adjacent = new ArrayList<>(Direction.VALUES.length);
        for (Node other : node.links) {
            if (other != null && !adjacent.contains(other.network)) {
                adjacent.add(other.network);
            }
        }

        if (adjacent.isEmpty()) {
            Network created = createNetwork();
            attach(node, created);
            for (NetworkListener listener : listeners) {
                listener.onCreated(created);
            }
            return created;
        }

        Network target = adjacent.get(0);
        for (Network candidate : adjacent) {
            if (candidate.size() > target.size()) {
                target = candidate;
            }
        }
        attach(node, target);
        for (Network other : adjacent) {
            if (other != target) {
                absorb(target, other);
            }
        }
        return target;
    }

    /**
     * Entfernt den Knoten an dieser Position. Kann das Netz aufspalten.
     *
     * @return {@code true}, wenn dort ein Knoten stand
     */
    public boolean remove(BlockCoord pos) {
        Node node = nodes.remove(pos);
        if (node == null) {
            return false;
        }

        Network network = node.network;
        List<Node> seeds = new ArrayList<>(Direction.VALUES.length);
        for (Direction direction : Direction.VALUES) {
            Node other = node.links[direction.ordinal()];
            if (other != null) {
                other.links[direction.opposite().ordinal()] = null;
                node.links[direction.ordinal()] = null;
                seeds.add(other);
            }
        }

        network.nodes.remove(node);
        if (node.kind == NodeKind.ENDPOINT) {
            network.endpointCount--;
        }
        node.network = null;

        if (network.nodes.isEmpty()) {
            networks.remove(network);
            for (NetworkListener listener : listeners) {
                listener.onDissolved(network);
            }
            return true;
        }
        if (seeds.size() > 1) { // mit höchstens einem Nachbarn kann ein Knoten nichts auftrennen
            resolveSplit(network, seeds);
        }
        return true;
    }

    /**
     * Ändert die Art eines Knotens, ohne ihn zu entfernen. Ein Kabel, das an ein Inventar grenzt, wird so zum
     * Endpunkt (und wieder zum Kabel, wenn das Inventar wegfällt). Netze verschmelzen oder teilen sich dabei nicht;
     * nur der Endpunkt-Zähler des Netzes wird angepasst.
     *
     * @throws IllegalArgumentException wenn an der Position kein Knoten steht
     */
    public void setKind(BlockCoord pos, NodeKind kind) {
        Objects.requireNonNull(kind, "kind");
        Node node = nodes.get(pos);
        if (node == null) {
            throw new IllegalArgumentException("Kein Knoten an Position " + pos);
        }
        if (node.kind == kind) {
            return;
        }
        if (node.kind == NodeKind.ENDPOINT) {
            node.network.endpointCount--;
        }
        node.kind = kind;
        if (kind == NodeKind.ENDPOINT) {
            node.network.endpointCount++;
        }
    }

    /**
     * Ändert, welche Seiten eines Knotens verbunden sein dürfen (z. B. per Wrench). Kann Netze auftrennen oder
     * verschmelzen.
     *
     * @throws IllegalArgumentException wenn an der Position kein Knoten steht
     */
    public void setSides(BlockCoord pos, int sideMask) {
        Node node = nodes.get(pos);
        if (node == null) {
            throw new IllegalArgumentException("Kein Knoten an Position " + pos);
        }
        int newMask = sideMask & Direction.ALL_MASK;
        if (node.sideMask == newMask) {
            return;
        }
        node.sideMask = newMask;

        // Phase 1: nur wegfallende Verbindungen kappen und prüfen, ob dadurch etwas abgetrennt wird.
        // Neue Verbindungen dürfen erst danach geknüpft werden, sonst würde die Suche in fremde Netze laufen.
        List<Node> lost = new ArrayList<>(Direction.VALUES.length);
        for (Direction direction : Direction.VALUES) {
            Node before = node.links[direction.ordinal()];
            if (before != null && !node.enabled(direction)) {
                updateLink(node, direction);
                lost.add(before);
            }
        }
        if (!lost.isEmpty()) {
            List<Node> seeds = new ArrayList<>(lost.size() + 1);
            seeds.add(node);
            seeds.addAll(lost);
            resolveSplit(node.network, seeds);
        }

        // Phase 2: neu freigegebene Seiten verbinden und die betroffenen Netze verschmelzen.
        List<Node> gained = new ArrayList<>(Direction.VALUES.length);
        for (Direction direction : Direction.VALUES) {
            if (node.links[direction.ordinal()] == null && node.enabled(direction)) {
                updateLink(node, direction);
                Node after = node.links[direction.ordinal()];
                if (after != null) {
                    gained.add(after);
                }
            }
        }
        for (Node other : gained) {
            if (other.network != node.network) {
                Network larger = other.network.size() > node.network.size() ? other.network : node.network;
                Network smaller = larger == node.network ? other.network : node.network;
                absorb(larger, smaller);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ Innenleben

    /** Stellt die Verbindung zwischen {@code node} und seinem Nachbarn in einer Richtung passend zu den Seitenmasken her. */
    private void updateLink(Node node, Direction direction) {
        int index = direction.ordinal();
        int opposite = direction.opposite().ordinal();
        Node other = nodes.get(node.pos.offset(direction));
        boolean shouldLink = other != null && node.enabled(direction) && other.enabled(direction.opposite());

        Node current = node.links[index];
        if (shouldLink) {
            node.links[index] = other;
            other.links[opposite] = node;
        } else if (current != null) {
            current.links[opposite] = null;
            node.links[index] = null;
        }
    }

    private Network createNetwork() {
        Network network = new Network(nextNetworkId++);
        networks.add(network);
        return network;
    }

    private static void attach(Node node, Network network) {
        node.network = network;
        network.nodes.add(node);
        if (node.kind == NodeKind.ENDPOINT) {
            network.endpointCount++;
        }
    }

    /** Nimmt alle Knoten von {@code smaller} in {@code larger} auf. */
    private void absorb(Network larger, Network smaller) {
        for (Node node : smaller.nodes) {
            node.network = larger;
        }
        larger.nodes.addAll(smaller.nodes);
        larger.endpointCount += smaller.endpointCount;
        smaller.nodes.clear();
        smaller.endpointCount = 0;
        networks.remove(smaller);
        for (NetworkListener listener : listeners) {
            listener.onMerged(larger, smaller);
        }
    }

    /** Suchlauf von einem Startknoten aus. Mehrere Läufe, die sich treffen, werden zu einem zusammengelegt. */
    private static final class Group {
        final ArrayDeque<Node> queue = new ArrayDeque<>();
        final Set<Node> visited = new HashSet<>();
        boolean dead;

        Group(Node seed) {
            queue.add(seed);
            visited.add(seed);
        }
    }

    /**
     * Prüft nach dem Wegfall einer oder mehrerer Verbindungen, ob {@code network} noch zusammenhängt.
     *
     * <p>{@code seeds} sind alle Knoten, die an einer weggefallenen Verbindung lagen. Jeder Teil, der nach dem Wegfall
     * übrig bleibt, enthält mindestens einen davon. Von jedem startet eine Suche, alle laufen reihum je einen Schritt.
     * Trifft eine Suche auf das Gebiet einer anderen, sind beide verbunden und werden vereint. Ist die Warteschlange
     * einer Suche leer, hat sie ihren Teil vollständig gesehen: Er ist abgetrennt und wird zu einem neuen Netz. Bleibt
     * nur noch eine Suche übrig, ist der Rest das ursprüngliche Netz und muss nicht mehr durchsucht werden.
     */
    private void resolveSplit(Network network, List<Node> seeds) {
        Map<Node, Group> owner = new HashMap<>();
        List<Group> groups = new ArrayList<>(seeds.size());
        for (Node seed : seeds) {
            Group group = new Group(seed);
            owner.put(seed, group);
            groups.add(group);
        }

        List<Network> parts = new ArrayList<>(2);
        while (groups.size() > 1) {
            for (Group original : new ArrayList<>(groups)) {
                if (groups.size() <= 1) {
                    break;
                }
                if (original.dead) {
                    continue;
                }
                if (original.queue.isEmpty()) {
                    groups.remove(original);
                    parts.add(splitOff(network, original));
                    continue;
                }

                Group current = original;
                Node node = current.queue.poll();
                for (Node next : node.links) {
                    if (next == null) {
                        continue;
                    }
                    Group other = owner.get(next);
                    if (other == null) {
                        owner.put(next, current);
                        current.visited.add(next);
                        current.queue.add(next);
                    } else if (other != current) {
                        current = merge(current, other, owner, groups);
                    }
                }
            }
        }

        if (!parts.isEmpty()) {
            for (NetworkListener listener : listeners) {
                listener.onSplit(network, Collections.unmodifiableList(parts));
            }
        }
    }

    /** Vereint zwei Suchläufe; der kleinere wird in den größeren übernommen. */
    private static Group merge(Group a, Group b, Map<Node, Group> owner, List<Group> groups) {
        Group big = a.visited.size() >= b.visited.size() ? a : b;
        Group small = big == a ? b : a;
        for (Node node : small.visited) {
            owner.put(node, big);
        }
        big.visited.addAll(small.visited);
        big.queue.addAll(small.queue);
        small.dead = true;
        groups.remove(small);
        return big;
    }

    /** Macht aus dem vollständig durchsuchten Teil ein eigenes Netz. */
    private Network splitOff(Network from, Group group) {
        Network part = createNetwork();
        for (Node node : group.visited) {
            from.nodes.remove(node);
            if (node.kind == NodeKind.ENDPOINT) {
                from.endpointCount--;
            }
            attach(node, part);
        }
        return part;
    }

    // ------------------------------------------------------------------------------------------ Selbstprüfung

    /**
     * Prüft alle inneren Annahmen (Zugehörigkeit, symmetrische Verbindungen, Zusammenhang jedes Netzes, Zähler).
     * Kostet Zeit proportional zur Gesamtgröße und ist für Tests und Debug-Befehle gedacht, nicht für den Spielbetrieb.
     *
     * @return Liste der gefundenen Probleme, leer wenn alles stimmt
     */
    public List<String> validate() {
        List<String> problems = new ArrayList<>();

        int counted = 0;
        for (Network network : networks) {
            counted += network.nodes.size();
            if (network.nodes.isEmpty()) {
                problems.add(network + " ist leer, existiert aber noch");
            }
            int endpoints = 0;
            for (Node node : network.nodes) {
                if (node.network != network) {
                    problems.add(node + " steht in " + network + " hat aber " + node.network);
                }
                if (nodes.get(node.pos) != node) {
                    problems.add(node + " fehlt in der Positionstabelle");
                }
                if (node.kind == NodeKind.ENDPOINT) {
                    endpoints++;
                }
            }
            if (endpoints != network.endpointCount) {
                problems.add(network + " zählt " + network.endpointCount + " Endpunkte, es sind " + endpoints);
            }
            if (!network.nodes.isEmpty() && !isConnected(network)) {
                problems.add(network + " ist nicht zusammenhängend");
            }
        }
        if (counted != nodes.size()) {
            problems.add("Netze enthalten " + counted + " Knoten, die Positionstabelle " + nodes.size());
        }

        for (Node node : nodes.values()) {
            for (Direction direction : Direction.VALUES) {
                Node other = nodes.get(node.pos.offset(direction));
                boolean expected = other != null && node.enabled(direction) && other.enabled(direction.opposite());
                Node actual = node.links[direction.ordinal()];
                if (expected != (actual != null) || (expected && actual != other)) {
                    problems.add("Verbindung von " + node + " Richtung " + direction + " ist falsch");
                }
                if (actual != null && actual.links[direction.opposite().ordinal()] != node) {
                    problems.add("Verbindung von " + node + " Richtung " + direction + " ist nicht symmetrisch");
                }
                if (actual != null && actual.network != node.network) {
                    problems.add(node + " ist mit " + actual + " verbunden, liegt aber in einem anderen Netz");
                }
            }
        }
        return problems;
    }

    private static boolean isConnected(Network network) {
        Node start = network.nodes.iterator().next();
        Set<Node> seen = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            for (Node next : queue.poll().links) {
                if (next != null && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return seen.size() == network.nodes.size();
    }
}
