package io.github.fishgames.vectrum.core.network;

import io.github.fishgames.vectrum.core.transport.TransportType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkGraphTest {
    private static final String DIM = "minecraft:overworld";
    private static final int ALL = Direction.ALL_MASK;

    private final NetworkGraph graph = new NetworkGraph("test");
    private final Recorder events = new Recorder();

    NetworkGraphTest() {
        graph.addListener(events);
    }

    private static BlockCoord at(int x, int y, int z) {
        return new BlockCoord(DIM, x, y, z);
    }

    /** Kabelreihe entlang der X-Achse von {@code from} bis {@code to} (einschließlich). */
    private void line(int from, int to) {
        for (int x = from; x <= to; x++) {
            graph.add(at(x, 0, 0), NodeKind.CABLE, ALL);
        }
    }

    private void assertHealthy() {
        assertEquals(List.of(), graph.validate());
    }

    private static List<Integer> sizes(NetworkGraph graph) {
        List<Integer> result = new ArrayList<>();
        for (Network network : graph.networks()) {
            result.add(network.size());
        }
        result.sort(null);
        return result;
    }

    @Test
    void singleNodeCreatesItsOwnNetwork() {
        Network network = graph.add(at(0, 0, 0), NodeKind.CABLE, ALL);

        assertEquals(1, graph.networkCount());
        assertEquals(1, network.size());
        assertSame(network, graph.networkAt(at(0, 0, 0)));
        assertEquals(1, events.created);
        assertHealthy();
    }

    @Test
    void neighboursFormOneNetwork() {
        line(0, 4);

        assertEquals(1, graph.networkCount());
        assertEquals(5, graph.networkAt(at(2, 0, 0)).size());
        assertHealthy();
    }

    @Test
    void distantNodesStaySeparate() {
        graph.add(at(0, 0, 0), NodeKind.CABLE, ALL);
        graph.add(at(2, 0, 0), NodeKind.CABLE, ALL);
        graph.add(at(0, 0, 5), NodeKind.CABLE, ALL);

        assertEquals(3, graph.networkCount());
        assertNotSame(graph.networkAt(at(0, 0, 0)), graph.networkAt(at(2, 0, 0)));
        assertHealthy();
    }

    @Test
    void diagonalNeighboursAreNotConnected() {
        graph.add(at(0, 0, 0), NodeKind.CABLE, ALL);
        graph.add(at(1, 1, 0), NodeKind.CABLE, ALL);

        assertEquals(2, graph.networkCount());
    }

    @Test
    void bridgingNodeMergesTwoNetworks() {
        line(0, 2);
        line(4, 6);
        assertEquals(2, graph.networkCount());

        graph.add(at(3, 0, 0), NodeKind.CABLE, ALL);

        assertEquals(1, graph.networkCount());
        assertEquals(7, graph.networkAt(at(0, 0, 0)).size());
        assertEquals(1, events.merged);
        assertHealthy();
    }

    @Test
    void mergeKeepsTheLargerNetworkIdentity() {
        line(0, 4);
        Network large = graph.networkAt(at(0, 0, 0));
        graph.add(at(6, 0, 0), NodeKind.CABLE, ALL);
        Network small = graph.networkAt(at(6, 0, 0));

        graph.add(at(5, 0, 0), NodeKind.CABLE, ALL);

        assertSame(large, graph.networkAt(at(6, 0, 0)));
        assertEquals(large.id(), graph.networkAt(at(6, 0, 0)).id());
        assertEquals(0, small.size(), "das aufgenommene Netz ist leer");
        assertEquals(large, events.lastSurvivor);
        assertEquals(small, events.lastAbsorbed);
    }

    @Test
    void removingTheMiddleSplitsALine() {
        line(0, 6);

        assertTrue(graph.remove(at(3, 0, 0)));

        assertEquals(List.of(3, 3), sizes(graph));
        assertNotSame(graph.networkAt(at(0, 0, 0)), graph.networkAt(at(6, 0, 0)));
        assertEquals(1, events.splits);
        assertEquals(1, events.splitParts);
        assertHealthy();
    }

    @Test
    void removingAnEndDoesNotSplit() {
        line(0, 4);

        graph.remove(at(4, 0, 0));
        graph.remove(at(0, 0, 0));

        assertEquals(List.of(3), sizes(graph));
        assertEquals(0, events.splits);
        assertHealthy();
    }

    @Test
    void removingFromARingDoesNotSplit() {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (x != 1 || z != 1) {
                    graph.add(at(x, 0, z), NodeKind.CABLE, ALL);
                }
            }
        }
        assertEquals(List.of(8), sizes(graph));

        graph.remove(at(0, 0, 1));

        assertEquals(List.of(7), sizes(graph));
        assertEquals(0, events.splits);
        assertHealthy();
    }

    @Test
    void removingTheCentreOfAStarCreatesSixNetworks() {
        graph.add(at(0, 0, 0), NodeKind.CABLE, ALL);
        for (Direction direction : Direction.VALUES) {
            BlockCoord arm = at(0, 0, 0);
            for (int i = 0; i < 3; i++) {
                arm = arm.offset(direction);
                graph.add(arm, NodeKind.CABLE, ALL);
            }
        }
        assertEquals(List.of(19), sizes(graph));

        graph.remove(at(0, 0, 0));

        assertEquals(List.of(3, 3, 3, 3, 3, 3), sizes(graph));
        assertEquals(1, events.splits);
        assertEquals(5, events.splitParts, "fünf neue Netze, das sechste behält die alte Identität");
        assertHealthy();
    }

    @Test
    void removingTheLastNodeDissolvesTheNetwork() {
        graph.add(at(0, 0, 0), NodeKind.CABLE, ALL);

        graph.remove(at(0, 0, 0));

        assertEquals(0, graph.networkCount());
        assertEquals(0, graph.nodeCount());
        assertEquals(1, events.dissolved);
        assertNull(graph.networkAt(at(0, 0, 0)));
        assertFalse(graph.remove(at(0, 0, 0)), "zweites Entfernen ist wirkungslos");
    }

    @Test
    void bothNeighboursMustAllowTheConnection() {
        graph.add(at(0, 0, 0), NodeKind.CABLE, ALL);
        graph.add(at(1, 0, 0), NodeKind.CABLE, ALL & ~Direction.WEST.bit()); // B sperrt die Seite zu A

        assertEquals(2, graph.networkCount());
        assertHealthy();
    }

    @Test
    void enablingASideMergesAndDisablingItSplitsAgain() {
        graph.add(at(0, 0, 0), NodeKind.CABLE, ALL & ~Direction.EAST.bit());
        graph.add(at(1, 0, 0), NodeKind.CABLE, ALL);
        assertEquals(2, graph.networkCount());

        graph.setSides(at(0, 0, 0), ALL);
        assertEquals(1, graph.networkCount());
        assertHealthy();

        graph.setSides(at(1, 0, 0), ALL & ~Direction.WEST.bit());
        assertEquals(2, graph.networkCount());
        assertHealthy();
    }

    /** Regression: Seite A sperren und gleichzeitig Seite B freigeben darf keine Knoten fremder Netze umhängen. */
    @Test
    void swappingOneConnectionForAnotherInOneStepKeepsNetworksConsistent() {
        for (int x = -5; x <= -1; x++) {
            graph.add(at(x, 0, 0), NodeKind.CABLE, ALL);          // lange Kette links
        }
        graph.add(at(0, 0, 0), NodeKind.CABLE, ALL & ~Direction.EAST.bit()); // verbunden nach links, nicht nach rechts
        graph.add(at(1, 0, 0), NodeKind.CABLE, ALL);              // einzelner Knoten rechts
        assertEquals(List.of(1, 6), sizes(graph));

        graph.setSides(at(0, 0, 0), ALL & ~Direction.WEST.bit()); // links kappen, rechts verbinden

        assertEquals(List.of(2, 5), sizes(graph));
        assertSame(graph.networkAt(at(0, 0, 0)), graph.networkAt(at(1, 0, 0)));
        assertNotSame(graph.networkAt(at(0, 0, 0)), graph.networkAt(at(-1, 0, 0)));
        assertHealthy();
    }

    @Test
    void disablingASideInsideARingKeepsTheNetworkTogether() {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                if (x != 1 || z != 1) {
                    graph.add(at(x, 0, z), NodeKind.CABLE, ALL);
                }
            }
        }

        graph.setSides(at(0, 0, 0), ALL & ~Direction.SOUTH.bit()); // trennt nur eine Kante des Rings

        assertEquals(List.of(8), sizes(graph));
        assertEquals(0, events.splits);
        assertHealthy();
    }

    @Test
    void setSidesOnUnknownPositionFails() {
        assertThrows(IllegalArgumentException.class, () -> graph.setSides(at(0, 0, 0), ALL));
    }

    @Test
    void differentDimensionsNeverConnect() {
        graph.add(new BlockCoord("minecraft:overworld", 0, 0, 0), NodeKind.CABLE, ALL);
        graph.add(new BlockCoord("minecraft:the_nether", 1, 0, 0), NodeKind.CABLE, ALL);
        graph.add(new BlockCoord("minecraft:the_nether", 0, 0, 0), NodeKind.CABLE, ALL);

        assertEquals(2, graph.networkCount());
        assertEquals(List.of(1, 2), sizes(graph));
        assertHealthy();
    }

    @Test
    void endpointsAreCountedAcrossMergeAndSplit() {
        graph.add(at(0, 0, 0), NodeKind.ENDPOINT, ALL);
        line(1, 5);
        graph.add(at(6, 0, 0), NodeKind.ENDPOINT, ALL);
        assertEquals(2, graph.networkAt(at(3, 0, 0)).endpointCount());
        assertEquals(2, graph.networkAt(at(3, 0, 0)).endpointPositions().size());

        graph.remove(at(3, 0, 0));

        assertEquals(1, graph.networkAt(at(0, 0, 0)).endpointCount());
        assertEquals(1, graph.networkAt(at(6, 0, 0)).endpointCount());

        graph.remove(at(6, 0, 0));
        assertEquals(0, graph.networkAt(at(5, 0, 0)).endpointCount(), "ein Netz ohne Endpunkte bleibt bestehen, bleibt aber leer");
        assertHealthy();
    }

    @Test
    void changingTheKindOfANodeUpdatesTheEndpointCount() {
        line(0, 4);
        Network network = graph.networkAt(at(2, 0, 0));
        assertEquals(0, network.endpointCount());

        graph.setKind(at(1, 0, 0), NodeKind.ENDPOINT);
        graph.setKind(at(3, 0, 0), NodeKind.ENDPOINT);
        assertEquals(2, graph.networkAt(at(2, 0, 0)).endpointCount());
        assertEquals(1, graph.networkCount(), "die Art zu wechseln verbindet oder trennt nichts");

        graph.setKind(at(1, 0, 0), NodeKind.ENDPOINT); // gleiche Art: nichts ändert sich
        assertEquals(2, graph.networkAt(at(2, 0, 0)).endpointCount());

        graph.setKind(at(1, 0, 0), NodeKind.CABLE);
        assertEquals(1, graph.networkAt(at(2, 0, 0)).endpointCount());
        assertEquals(List.of(at(3, 0, 0)), graph.networkAt(at(2, 0, 0)).endpointPositions());

        graph.remove(at(2, 0, 0)); // Zähler bleibt nach dem Teilen richtig
        assertEquals(1, graph.networkAt(at(3, 0, 0)).endpointCount());
        assertEquals(0, graph.networkAt(at(0, 0, 0)).endpointCount());
        assertHealthy();
    }

    @Test
    void setKindOnUnknownPositionFails() {
        assertThrows(IllegalArgumentException.class, () -> graph.setKind(at(9, 9, 9), NodeKind.ENDPOINT));
    }

    @Test
    void endpointsConductLikeCables() {
        graph.add(at(0, 0, 0), NodeKind.CABLE, ALL);
        graph.add(at(1, 0, 0), NodeKind.ENDPOINT, ALL);
        graph.add(at(2, 0, 0), NodeKind.CABLE, ALL);

        assertEquals(List.of(3), sizes(graph));
    }

    @Test
    void addingTwiceAtTheSamePositionFails() {
        graph.add(at(0, 0, 0), NodeKind.CABLE, ALL);

        assertThrows(IllegalStateException.class, () -> graph.add(at(0, 0, 0), NodeKind.CABLE, ALL));
    }

    @Test
    void snapshotRebuildsTheSamePartition() {
        line(0, 4);
        graph.add(at(0, 1, 0), NodeKind.ENDPOINT, ALL);
        graph.add(at(10, 0, 0), NodeKind.CABLE, ALL & ~Direction.WEST.bit());
        graph.add(at(9, 0, 0), NodeKind.CABLE, ALL & ~Direction.EAST.bit());
        graph.remove(at(2, 0, 0));

        NetworkGraph rebuilt = new NetworkGraph("rebuilt");
        for (NodeInfo info : graph.snapshot()) {
            rebuilt.add(info.pos(), info.kind(), info.sideMask());
        }

        assertEquals(graph.nodeCount(), rebuilt.nodeCount());
        assertEquals(sizes(graph), sizes(rebuilt));
        assertEquals(partition(graph), partition(rebuilt));
        assertEquals(List.of(), rebuilt.validate());
    }

    @Test
    void universalCableKeepsOneNetworkPerLayerAtTheSamePosition() {
        NetworkRegistry registry = new NetworkRegistry();
        NetworkGraph items = registry.graph(TransportType.ITEM.id());
        NetworkGraph fluids = registry.graph(TransportType.FLUID.id());

        // Universalkabel: dieselben Positionen in beiden Ebenen, plus ein reines Item-Kabel
        for (int x = 0; x < 3; x++) {
            items.add(at(x, 0, 0), NodeKind.CABLE, ALL);
            fluids.add(at(x, 0, 0), NodeKind.CABLE, ALL);
        }
        items.add(at(3, 0, 0), NodeKind.CABLE, ALL);

        assertEquals(4, registry.networkAt(TransportType.ITEM.id(), at(0, 0, 0)).size());
        assertEquals(3, registry.networkAt(TransportType.FLUID.id(), at(0, 0, 0)).size());
        assertNull(registry.networkAt(TransportType.ENERGY.id(), at(0, 0, 0)));
        assertNotNull(registry.graph(NetworkRegistry.DIGITAL_LAYER));
        assertNotSame(registry.networkAt(TransportType.ITEM.id(), at(0, 0, 0)), registry.networkAt(TransportType.FLUID.id(), at(0, 0, 0)));

        items.remove(at(1, 0, 0));
        assertEquals(1, fluids.networkCount(), "das Fluid-Netz bleibt vom Item-Netz unberührt");
    }

    @Test
    void builtInTransportTypesHaveTheRightBehaviour() {
        assertEquals(TransportType.Behavior.SIGNAL, TransportType.REDSTONE.behavior());
        for (TransportType type : List.of(TransportType.ITEM, TransportType.FLUID, TransportType.ENERGY, TransportType.GAS)) {
            assertEquals(TransportType.Behavior.QUANTITY, type.behavior());
        }
    }

    // ------------------------------------------------------------------------------------------ Hilfsmittel

    static Set<Set<BlockCoord>> partition(NetworkGraph graph) {
        Set<Set<BlockCoord>> result = new HashSet<>();
        for (Network network : graph.networks()) {
            result.add(new HashSet<>(network.positions()));
        }
        return result;
    }

    /** Zählt Ereignisse mit, damit Tests das Verhalten des Graphen beobachten können. */
    static final class Recorder implements NetworkListener {
        int created;
        int merged;
        int splits;
        int splitParts;
        int dissolved;
        Network lastSurvivor;
        Network lastAbsorbed;

        @Override
        public void onCreated(Network network) {
            created++;
        }

        @Override
        public void onMerged(Network survivor, Network absorbed) {
            merged++;
            lastSurvivor = survivor;
            lastAbsorbed = absorbed;
        }

        @Override
        public void onSplit(Network original, List<Network> newParts) {
            splits++;
            splitParts += newParts.size();
        }

        @Override
        public void onDissolved(Network network) {
            dissolved++;
        }
    }
}
