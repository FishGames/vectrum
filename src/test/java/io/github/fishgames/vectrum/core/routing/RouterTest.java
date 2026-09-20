package io.github.fishgames.vectrum.core.routing;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {
    /** Test target with capacity, fill level and priority. */
    private static final class Sink {
        final String name;
        final int priority;
        final long capacity;
        long stored;
        Sink(String name, int priority, long capacity, long stored) {
            this.name = name;
            this.priority = priority;
            this.capacity = capacity;
            this.stored = stored;
        }
        double fill() {
            return capacity == 0 ? 1 : (double) stored / capacity;
        }
        long accept(long max) {
            long taken = Math.min(max, capacity - stored);
            stored += taken;
            return taken;
        }
    }

    private static final class Ptr implements Router.Pointer {
        int value;
        @Override public int get() { return value; }
        @Override public void set(int value) { this.value = value; }
    }

    private static long run(long budget, List<Sink> sinks, DistributionMode mode, Ptr ptr) {
        return Router.distribute(budget, sinks, s -> s.priority, Sink::fill, mode, ptr, Sink::accept);
    }

    private static Sink sink(String name, int prio, long cap) {
        return new Sink(name, prio, cap, 0);
    }

    @Test
    void sequentialFillsTheFirstTargetBeforeTheNext() {
        Sink a = sink("a", 0, 5), b = sink("b", 0, 5), c = sink("c", 0, 5);
        long moved = run(8, List.of(a, b, c), DistributionMode.SEQUENTIAL, new Ptr());

        assertEquals(8, moved);
        assertEquals(5, a.stored);
        assertEquals(3, b.stored);
        assertEquals(0, c.stored);
    }

    @Test
    void higherPriorityGroupIsServedFirstAndOverflowGoesToTheNext() {
        Sink low = sink("low", 0, 10), high = sink("high", 5, 4), mid = sink("mid", 2, 10);
        long moved = run(9, List.of(low, high, mid), DistributionMode.SEQUENTIAL, new Ptr());

        assertEquals(9, moved);
        assertEquals(4, high.stored);
        assertEquals(5, mid.stored);
        assertEquals(0, low.stored);
    }

    @Test
    void negativePrioritiesComeLast() {
        Sink neg = sink("neg", -3, 10), zero = sink("zero", 0, 10);
        run(6, List.of(neg, zero), DistributionMode.SEQUENTIAL, new Ptr());

        assertEquals(6, zero.stored);
        assertEquals(0, neg.stored);
    }

    @Test
    void roundRobinStartsWithADifferentTargetEachTime() {
        Sink a = sink("a", 0, 100), b = sink("b", 0, 100), c = sink("c", 0, 100);
        Ptr ptr = new Ptr();
        List<Sink> all = List.of(a, b, c);

        run(1, all, DistributionMode.ROUND_ROBIN, ptr);
        run(1, all, DistributionMode.ROUND_ROBIN, ptr);
        run(1, all, DistributionMode.ROUND_ROBIN, ptr);
        run(1, all, DistributionMode.ROUND_ROBIN, ptr);

        assertEquals(2, a.stored);
        assertEquals(1, b.stored);
        assertEquals(1, c.stored);
    }

    @Test
    void roundRobinSkipsFullTargetsWithoutStalling() {
        Sink a = new Sink("a", 0, 5, 5), b = sink("b", 0, 100), c = sink("c", 0, 100);
        Ptr ptr = new Ptr();
        List<Sink> all = List.of(a, b, c);

        for (int i = 0; i < 4; i++) {
            assertEquals(1, run(1, all, DistributionMode.ROUND_ROBIN, ptr));
        }
        assertEquals(5, a.stored);
        assertEquals(2, b.stored);
        assertEquals(2, c.stored);
    }

    @Test
    void roundRobinPointerSurvivesAsPlainNumber() {
        Sink a = sink("a", 0, 100), b = sink("b", 0, 100);
        Ptr ptr = new Ptr();
        ptr.value = 1; // pointer after load
        run(1, List.of(a, b), DistributionMode.ROUND_ROBIN, ptr);

        assertEquals(0, a.stored);
        assertEquals(1, b.stored);
        assertEquals(0, ptr.value);
    }

    @Test
    void roundRobinHandlesPointerOutsideTheRange() {
        Sink a = sink("a", 0, 100), b = sink("b", 0, 100);
        Ptr ptr = new Ptr();
        ptr.value = 17; // out-of-range pointer
        assertEquals(1, run(1, List.of(a, b), DistributionMode.ROUND_ROBIN, ptr));
        ptr.value = -3;
        assertEquals(1, run(1, List.of(a, b), DistributionMode.ROUND_ROBIN, ptr));
    }

    @Test
    void balancedGivesEmptierTargetsMore() {
        Sink empty = sink("empty", 0, 1000), half = new Sink("half", 0, 1000, 500);
        run(100, List.of(half, empty), DistributionMode.BALANCED, new Ptr());

        assertTrue(empty.stored > half.stored - 500, "empty target must receive more");
        assertEquals(100, empty.stored + (half.stored - 500));
    }

    @Test
    void balancedRedistributesWhatAFullTargetCannotTake() {
        Sink nearlyFull = new Sink("full", 0, 100, 99), big = sink("big", 0, 1000);
        long moved = run(50, List.of(nearlyFull, big), DistributionMode.BALANCED, new Ptr());

        assertEquals(50, moved);
        assertEquals(100, nearlyFull.stored);
        assertEquals(49, big.stored);
    }

    @Test
    void balancedWithOnlyFullTargetsMovesNothing() {
        Sink a = new Sink("a", 0, 10, 10), b = new Sink("b", 0, 10, 10);
        assertEquals(0, run(50, List.of(a, b), DistributionMode.BALANCED, new Ptr()));
    }

    @Test
    void zeroBudgetAndNoTargetsMoveNothing() {
        Sink a = sink("a", 0, 10);
        for (DistributionMode mode : DistributionMode.values()) {
            assertEquals(0, run(0, List.of(a), mode, new Ptr()));
            assertEquals(0, run(-5, List.of(a), mode, new Ptr()));
            assertEquals(0, run(10, List.of(), mode, new Ptr()));
        }
        assertEquals(0, a.stored);
    }

    @Test
    void lowerGroupsAreOnlyUsedWhenHigherOnesAreFull() {
        Sink hi1 = sink("hi1", 1, 3), hi2 = sink("hi2", 1, 3), lo = sink("lo", 0, 100);
        run(10, List.of(lo, hi1, hi2), DistributionMode.ROUND_ROBIN, new Ptr());

        assertEquals(3, hi1.stored);
        assertEquals(3, hi2.stored);
        assertEquals(4, lo.stored);
    }

    @Test
    void nothingIsLostOrDuplicatedInAnyMode() {
        Random random = new Random(42);
        for (int round = 0; round < 2000; round++) {
            int count = 1 + random.nextInt(8);
            List<Sink> sinks = new ArrayList<>();
            long totalFree = 0;
            for (int i = 0; i < count; i++) {
                long cap = random.nextInt(200);
                long stored = cap == 0 ? 0 : random.nextInt((int) cap + 1);
                sinks.add(new Sink("s" + i, random.nextInt(4) - 1, cap, stored));
                totalFree += cap - stored;
            }
            long budget = random.nextInt(300);
            DistributionMode mode = DistributionMode.values()[random.nextInt(3)];
            Ptr ptr = new Ptr();
            ptr.value = random.nextInt(20);
            long before = sinks.stream().mapToLong(s -> s.stored).sum();

            long moved = run(budget, sinks, mode, ptr);

            long after = sinks.stream().mapToLong(s -> s.stored).sum();
            assertEquals(after - before, moved, "moved sum mismatch (" + mode + ")");
            assertTrue(moved <= budget);
            for (Sink s : sinks) {
                assertTrue(s.stored <= s.capacity && s.stored >= 0);
            }
            // budget not exhausted: every target is full
            if (moved < budget) {
                assertEquals(totalFree, moved, "free space remained with budget left (" + mode + ")");
            }
        }
    }
}
