package io.github.fishgames.vectrum.core.network;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Timing checks: build, rebuild, branch cut and node removal on a network of about 10,000 nodes. */
class NetworkGraphPerformanceTest {
    private static final String DIM = "minecraft:overworld";
    private static final int ALL = Direction.ALL_MASK;

    /** Solid cuboid of nodes. */
    private static List<NodeInfo> block(int sizeX, int sizeY, int sizeZ) {
        List<NodeInfo> result = new ArrayList<>(sizeX * sizeY * sizeZ);
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    NodeKind kind = (x + y + z) % 20 == 0 ? NodeKind.ENDPOINT : NodeKind.CABLE;
                    result.add(new NodeInfo(new BlockCoord(DIM, x, y, z), kind, ALL));
                }
            }
        }
        return result;
    }

    private static NetworkGraph build(List<NodeInfo> infos) {
        NetworkGraph graph = new NetworkGraph("perf");
        for (NodeInfo info : infos) {
            graph.add(info.pos(), info.kind(), info.sideMask());
        }
        return graph;
    }

    @Test
    void tenThousandNodesBuildAndRebuildQuickly() {
        List<NodeInfo> infos = block(22, 22, 21); // 10,164 nodes
        build(infos); // warm-up

        long start = System.nanoTime();
        NetworkGraph graph = build(infos);
        double buildMs = (System.nanoTime() - start) / 1e6;

        List<NodeInfo> snapshot = graph.snapshot();
        start = System.nanoTime();
        NetworkGraph rebuilt = build(snapshot);
        double rebuildMs = (System.nanoTime() - start) / 1e6;

        System.out.printf("[Performance] %d nodes: build %.1f ms, rebuild from snapshot %.1f ms%n",
                infos.size(), buildMs, rebuildMs);
        assertEquals(1, graph.networkCount());
        assertEquals(1, rebuilt.networkCount());
        assertEquals(infos.size(), rebuilt.nodeCount());
        assertTrue(rebuildMs < 1000, "Rebuild took " + rebuildMs + " ms");
    }

    @Test
    void cuttingOffASmallBranchOfAHugeNetworkIsCheap() {
        List<NodeInfo> infos = new ArrayList<>(block(22, 22, 21));
        for (int x = 22; x < 28; x++) { // branch of 6 cables at the block edge
            infos.add(new NodeInfo(new BlockCoord(DIM, x, 0, 0), NodeKind.CABLE, ALL));
        }
        NetworkGraph graph = build(infos);
        BlockCoord cut = new BlockCoord(DIM, 22, 0, 0);
        BlockCoord tip = new BlockCoord(DIM, 27, 0, 0);

        // warm-up, then repeated detach and reattach
        for (int i = 0; i < 200; i++) {
            graph.remove(cut);
            graph.add(cut, NodeKind.CABLE, ALL);
        }
        int rounds = 1000;
        long start = System.nanoTime();
        for (int i = 0; i < rounds; i++) {
            graph.remove(cut);
            graph.add(cut, NodeKind.CABLE, ALL);
        }
        double averageMs = (System.nanoTime() - start) / 1e6 / rounds;
        System.out.printf("[Performance] detach and reattach branch at %d nodes: %.4f ms per round%n",
                infos.size(), averageMs);

        graph.remove(cut);
        assertEquals(2, graph.networkCount());
        assertEquals(5, graph.networkAt(tip).size(), "detached branch");
        assertEquals(infos.size() - 6, graph.networkAt(new BlockCoord(DIM, 0, 0, 0)).size());
        assertEquals(List.of(), graph.validate());
        assertTrue(averageMs < 5, "a cut took on average " + averageMs + " ms");
    }

    @Test
    void removingInteriorNodesOfAHugeNetworkIsCheap() {
        NetworkGraph graph = build(block(22, 22, 21));
        List<BlockCoord> victims = new ArrayList<>();
        for (int i = 2; i < 20; i++) {
            victims.add(new BlockCoord(DIM, i, i, i));
        }

        long start = System.nanoTime();
        for (BlockCoord victim : victims) {
            graph.remove(victim);
        }
        double averageMs = (System.nanoTime() - start) / 1e6 / victims.size();
        System.out.printf("[Performance] remove interior nodes from %d nodes: %.4f ms per node%n", 10164, averageMs);

        assertEquals(1, graph.networkCount());
        assertEquals(List.of(), graph.validate());
        assertTrue(averageMs < 20, "Removal took on average " + averageMs + " ms");
    }
}
