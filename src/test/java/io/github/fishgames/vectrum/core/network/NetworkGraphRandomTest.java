package io.github.fishgames.vectrum.core.network;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Random test: applies thousands of random edits (add, remove, set sides, change kind) and compares the graph after
 * each edit with a reference that recomputes all networks from scratch.
 */
class NetworkGraphRandomTest {
    private static final String DIM = "minecraft:overworld";

    private record Cell(NodeKind kind, int mask) {
    }

    @Test
    void randomOperationsOnACubeMatchTheReference() {
        for (long seed = 1; seed <= 10; seed++) {
            run(seed, 7, 7, 3, 3000);
        }
    }

    @Test
    void randomOperationsOnAFlatGridMatchTheReference() {
        for (long seed = 100; seed <= 110; seed++) {
            run(seed, 14, 1, 14, 3000); // flat grid
        }
    }

    @Test
    void randomOperationsOnAThinLineMatchTheReference() {
        for (long seed = 200; seed <= 205; seed++) {
            run(seed, 40, 1, 1, 2000); // thin line
        }
    }

    private void run(long seed, int sizeX, int sizeY, int sizeZ, int steps) {
        Random random = new Random(seed);
        NetworkGraph graph = new NetworkGraph("random");
        Counts counts = new Counts();
        graph.addListener(counts);
        Map<BlockCoord, Cell> reference = new HashMap<>();

        for (int step = 0; step < steps; step++) {
            BlockCoord pos = new BlockCoord(DIM, random.nextInt(sizeX), random.nextInt(sizeY), random.nextInt(sizeZ));
            Cell existing = reference.get(pos);
            int roll = random.nextInt(10);

            if (existing == null) {
                if (roll < 7) {
                    Cell cell = new Cell(random.nextInt(5) == 0 ? NodeKind.ENDPOINT : NodeKind.CABLE, randomMask(random));
                    graph.add(pos, cell.kind(), cell.mask());
                    reference.put(pos, cell);
                }
            } else if (roll < 4) {
                graph.remove(pos);
                reference.remove(pos);
            } else if (roll < 8) {
                int mask = randomMask(random);
                graph.setSides(pos, mask);
                reference.put(pos, new Cell(existing.kind(), mask));
            } else if (roll < 9) {
                NodeKind kind = existing.kind() == NodeKind.CABLE ? NodeKind.ENDPOINT : NodeKind.CABLE;
                graph.setKind(pos, kind);
                reference.put(pos, new Cell(kind, existing.mask()));
            }

            String context = "Seed " + seed + ", step " + step;
            assertEquals(List.of(), graph.validate(), context);
            assertEquals(referencePartition(reference), NetworkGraphTest.partition(graph), context);
            assertEquals(reference.size(), graph.nodeCount(), context);
            assertEquals(graph.networkCount(), counts.expectedNetworkCount(), context + " (events)");
        }
    }

    /** Random side mask; usually all sides open. */
    private static int randomMask(Random random) {
        return random.nextInt(4) == 0 ? random.nextInt(Direction.ALL_MASK + 1) : Direction.ALL_MASK;
    }

    /** Reference partition: all networks recomputed by breadth-first search. */
    private static Set<Set<BlockCoord>> referencePartition(Map<BlockCoord, Cell> cells) {
        Set<Set<BlockCoord>> result = new HashSet<>();
        Set<BlockCoord> seen = new HashSet<>();
        for (BlockCoord start : cells.keySet()) {
            if (!seen.add(start)) {
                continue;
            }
            Set<BlockCoord> component = new HashSet<>();
            ArrayDeque<BlockCoord> queue = new ArrayDeque<>();
            component.add(start);
            queue.add(start);
            while (!queue.isEmpty()) {
                BlockCoord current = queue.poll();
                for (Direction direction : Direction.VALUES) {
                    BlockCoord next = current.offset(direction);
                    Cell nextCell = cells.get(next);
                    if (nextCell == null
                            || (cells.get(current).mask() & direction.bit()) == 0
                            || (nextCell.mask() & direction.opposite().bit()) == 0) {
                        continue;
                    }
                    if (seen.add(next)) {
                        component.add(next);
                        queue.add(next);
                    }
                }
            }
            result.add(component);
        }
        return result;
    }

    /** Network count derived from listener events. */
    private static final class Counts implements NetworkListener {
        int created;
        int merged;
        int splitParts;
        int dissolved;

        @Override
        public void onCreated(Network network) {
            created++;
        }

        @Override
        public void onMerged(Network survivor, Network absorbed) {
            merged++;
        }

        @Override
        public void onSplit(Network original, List<Network> newParts) {
            splitParts += newParts.size();
        }

        @Override
        public void onDissolved(Network network) {
            dissolved++;
        }

        int expectedNetworkCount() {
            return created + splitParts - merged - dissolved;
        }
    }
}
