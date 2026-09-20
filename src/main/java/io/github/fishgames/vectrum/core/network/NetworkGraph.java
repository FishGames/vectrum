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
 * In-memory graph of the cable and endpoint positions of one layer (a transport type or the digital network).
 *
 * <p>Two adjacent nodes are linked when both have the facing side enabled. Updates are incremental:
 * <ul>
 *   <li>Adding a node merges adjacent networks, smaller into larger.</li>
 *   <li>Removing a node or disabling a side runs simultaneous searches from the affected neighbours; searches that
 *       meet are united, a search that runs out of nodes becomes a separate network.</li>
 * </ul>
 * Not thread-safe.
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

    // Queries

    public boolean contains(BlockCoord pos) {
        return nodes.containsKey(pos);
    }

    /** Network at the position, or {@code null}. */
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

    /** Kind and enabled sides of the node at the position, or {@code null}. */
    public NodeInfo info(BlockCoord pos) {
        Node node = nodes.get(pos);
        return node == null ? null : new NodeInfo(node.pos, node.kind, node.sideMask);
    }

    /** State of all nodes. */
    public List<NodeInfo> snapshot() {
        List<NodeInfo> result = new ArrayList<>(nodes.size());
        for (Node node : nodes.values()) {
            result.add(new NodeInfo(node.pos, node.kind, node.sideMask));
        }
        return result;
    }

    // Mutations

    /**
     * Adds a node and links it to matching neighbours.
     *
     * <ul>
     * <li>1. link each enabled side to the neighbour when that neighbour faces back</li>
     * <li>2. no adjacent network: create a new network</li>
     * <li>3. otherwise: join the largest adjacent network and absorb the other adjacent networks</li>
     * </ul>
     *
     * @param sideMask bit per {@link Direction}; {@link Direction#ALL_MASK} = all sides enabled
     * @return the network the node belongs to
     * @throws IllegalStateException when the position is occupied
     */
    public Network add(BlockCoord pos, NodeKind kind, int sideMask) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(kind, "kind");
        if (nodes.containsKey(pos)) {
            throw new IllegalStateException("Position already occupied: " + pos);
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
     * Removes the node at the position.
     *
     * <ul>
     * <li>1. unlink all neighbours</li>
     * <li>2. empty network: dissolve it</li>
     * <li>3. two or more neighbours: run the split check from them</li>
     * </ul>
     *
     * @return {@code true} when a node was removed
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
        if (seeds.size() > 1) {
            resolveSplit(network, seeds);
        }
        return true;
    }

    /**
     * Changes the kind of a node in place and updates the endpoint count of its network.
     *
     * @throws IllegalArgumentException when there is no node at the position
     */
    public void setKind(BlockCoord pos, NodeKind kind) {
        Objects.requireNonNull(kind, "kind");
        Node node = nodes.get(pos);
        if (node == null) {
            throw new IllegalArgumentException("No node at position " + pos);
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
     * Changes the enabled sides of a node; may split or merge networks.
     *
     * <ul>
     * <li>1. drop links of disabled sides and run the split check from the node and the lost neighbours</li>
     * <li>2. link newly enabled sides</li>
     * <li>3. merge the networks reached through new links, smaller into larger</li>
     * </ul>
     *
     * @throws IllegalArgumentException when there is no node at the position
     */
    public void setSides(BlockCoord pos, int sideMask) {
        Node node = nodes.get(pos);
        if (node == null) {
            throw new IllegalArgumentException("No node at position " + pos);
        }
        int newMask = sideMask & Direction.ALL_MASK;
        if (node.sideMask == newMask) {
            return;
        }
        node.sideMask = newMask;

        // Phase 1: lost links
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

        // Phase 2: gained links
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

    // Internals

    /** Sets or clears the link between {@code node} and its neighbour in one direction according to the side masks. */
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

    /** Moves all nodes of {@code smaller} into {@code larger}. */
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

    /** Breadth-first search state started from one seed node. */
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
     * Split check of {@code network} after links were removed; {@code seeds} are the nodes at the removed links.
     *
     * <ul>
     * <li>1. start one breadth-first search per seed</li>
     * <li>2. advance all searches round-robin, one node each per round</li>
     * <li>3. a search that reaches the area of another is merged with it (smaller into larger)</li>
     * <li>4. a search with an empty queue has visited a whole part: it becomes a new network</li>
     * <li>5. stop when one search remains; its part stays in the original network</li>
     * <li>6. notify listeners with {@code onSplit} when new networks were created</li>
     * </ul>
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

    /** Merges two searches, the smaller into the larger. */
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

    /** Moves the nodes visited by the search into a new network. */
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

    // Validation

    /**
     * Checks membership, link symmetry, connectivity of each network and endpoint counts. Runs in time proportional
     * to the graph size.
     *
     * @return problem descriptions, empty when consistent
     */
    public List<String> validate() {
        List<String> problems = new ArrayList<>();

        int counted = 0;
        for (Network network : networks) {
            counted += network.nodes.size();
            if (network.nodes.isEmpty()) {
                problems.add(network + " is empty but still registered");
            }
            int endpoints = 0;
            for (Node node : network.nodes) {
                if (node.network != network) {
                    problems.add(node + " is in " + network + " but has " + node.network);
                }
                if (nodes.get(node.pos) != node) {
                    problems.add(node + " is missing from the position table");
                }
                if (node.kind == NodeKind.ENDPOINT) {
                    endpoints++;
                }
            }
            if (endpoints != network.endpointCount) {
                problems.add(network + " counts " + network.endpointCount + " endpoints, actual " + endpoints);
            }
            if (!network.nodes.isEmpty() && !isConnected(network)) {
                problems.add(network + " is not connected");
            }
        }
        if (counted != nodes.size()) {
            problems.add("Networks contain " + counted + " nodes, position table " + nodes.size());
        }

        for (Node node : nodes.values()) {
            for (Direction direction : Direction.VALUES) {
                Node other = nodes.get(node.pos.offset(direction));
                boolean expected = other != null && node.enabled(direction) && other.enabled(direction.opposite());
                Node actual = node.links[direction.ordinal()];
                if (expected != (actual != null) || (expected && actual != other)) {
                    problems.add("Link of " + node + " direction " + direction + " is wrong");
                }
                if (actual != null && actual.links[direction.opposite().ordinal()] != node) {
                    problems.add("Link of " + node + " direction " + direction + " is not symmetric");
                }
                if (actual != null && actual.network != node.network) {
                    problems.add(node + " is linked to " + actual + " but is in a different network");
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
