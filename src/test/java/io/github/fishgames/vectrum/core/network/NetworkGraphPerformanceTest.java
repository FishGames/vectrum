package io.github.fishgames.vectrum.core.network;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Richtwerte aus dem Konzept: Neuaufbau bei 10.000 Knoten unter 50 ms. Die Grenzen in den Prüfungen sind bewusst
 * großzügig, damit langsame Rechner den Test nicht zufällig scheitern lassen; die echten Zeiten werden ausgegeben.
 */
class NetworkGraphPerformanceTest {
    private static final String DIM = "minecraft:overworld";
    private static final int ALL = Direction.ALL_MASK;

    /** Massiver Quader, entspricht etwa 10.000 Knoten. */
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
        List<NodeInfo> infos = block(22, 22, 21); // 10.164 Knoten
        build(infos); // Aufwärmen der JVM

        long start = System.nanoTime();
        NetworkGraph graph = build(infos);
        double buildMs = (System.nanoTime() - start) / 1e6;

        List<NodeInfo> snapshot = graph.snapshot();
        start = System.nanoTime();
        NetworkGraph rebuilt = build(snapshot);
        double rebuildMs = (System.nanoTime() - start) / 1e6;

        System.out.printf("[Leistung] %d Knoten: Aufbau %.1f ms, Neuaufbau aus Speicherstand %.1f ms%n",
                infos.size(), buildMs, rebuildMs);
        assertEquals(1, graph.networkCount());
        assertEquals(1, rebuilt.networkCount());
        assertEquals(infos.size(), rebuilt.nodeCount());
        assertTrue(rebuildMs < 1000, "Neuaufbau dauerte " + rebuildMs + " ms");
    }

    @Test
    void cuttingOffASmallBranchOfAHugeNetworkIsCheap() {
        List<NodeInfo> infos = new ArrayList<>(block(22, 22, 21));
        for (int x = 22; x < 28; x++) { // Ast aus 6 Kabeln am Rand des Blocks
            infos.add(new NodeInfo(new BlockCoord(DIM, x, 0, 0), NodeKind.CABLE, ALL));
        }
        NetworkGraph graph = build(infos);
        BlockCoord cut = new BlockCoord(DIM, 22, 0, 0);
        BlockCoord tip = new BlockCoord(DIM, 27, 0, 0);

        // Aufwärmen, dann viele Male abtrennen und wieder verbinden
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
        System.out.printf("[Leistung] Ast abtrennen und wieder anschließen bei %d Knoten: %.4f ms pro Runde%n",
                infos.size(), averageMs);

        graph.remove(cut);
        assertEquals(2, graph.networkCount());
        assertEquals(5, graph.networkAt(tip).size(), "abgetrennter Ast");
        assertEquals(infos.size() - 6, graph.networkAt(new BlockCoord(DIM, 0, 0, 0)).size());
        assertEquals(List.of(), graph.validate());
        assertTrue(averageMs < 5, "ein Schnitt dauerte im Schnitt " + averageMs + " ms");
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
        System.out.printf("[Leistung] Innenknoten aus %d Knoten entfernen: %.4f ms pro Knoten%n", 10164, averageMs);

        assertEquals(1, graph.networkCount());
        assertEquals(List.of(), graph.validate());
        assertTrue(averageMs < 20, "Entfernen dauerte im Schnitt " + averageMs + " ms");
    }
}
