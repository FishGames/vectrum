package io.github.fishgames.vectrum.core.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * Routing cascade for one transfer: filter, priority, mode.
 *
 * <ol>
 *   <li>Filter: applied by the {@link Mover} per concrete resource.</li>
 *   <li>Priority: targets are grouped by priority, highest first; the next group is served when the group accepts
 *       nothing more.</li>
 *   <li>Mode: distribution inside a group of equal priority, see {@link DistributionMode}.</li>
 * </ol>
 */
public final class Router {
    /** Minimum weight of a target in balanced mode. */
    private static final double MIN_WEIGHT = 0.05;

    private Router() {
    }

    /** Performs the actual transfer. */
    @FunctionalInterface
    public interface Mover<T> {
        /** Offers at most {@code max} units to the target; returns the accepted amount (0 to max). */
        long move(T target, long max);
    }

    /** Round-robin pointer of the source. */
    public interface Pointer {
        int get();

        void set(int value);
    }

    /**
     * Distributes up to {@code budget} units across the targets.
     *
     * <ul>
     * <li>1. group targets by priority, highest first, keeping input order inside a group</li>
     * <li>2. per group, run the mode (sequential, round-robin or balanced) with the remaining budget</li>
     * <li>3. stop when the budget is used up</li>
     * </ul>
     *
     * @param targets   candidate targets in fixed order
     * @param priority  priority of a target (higher = first)
     * @param fillLevel fill level of a target from 0 (empty) to 1 (full); used in BALANCED only
     * @param pointer   round-robin pointer of the source; used in ROUND_ROBIN only
     * @return total moved amount, at most {@code budget}
     */
    public static <T> long distribute(long budget, List<T> targets, ToIntFunction<T> priority,
                                      ToDoubleFunction<T> fillLevel, DistributionMode mode, Pointer pointer,
                                      Mover<T> mover) {
        if (budget <= 0 || targets.isEmpty()) {
            return 0;
        }

        // Priority groups
        TreeMap<Integer, List<T>> groups = new TreeMap<>(Comparator.reverseOrder());
        for (T target : targets) {
            groups.computeIfAbsent(priority.applyAsInt(target), key -> new ArrayList<>()).add(target);
        }

        long remaining = budget;
        for (List<T> group : groups.values()) {
            if (remaining <= 0) {
                break;
            }
            long moved = switch (mode) {
                case SEQUENTIAL -> sequential(group, remaining, mover);
                case ROUND_ROBIN -> roundRobin(group, remaining, pointer, mover);
                case BALANCED -> balanced(group, remaining, fillLevel, mover);
            };
            remaining -= moved;
        }
        return budget - remaining;
    }

    private static <T> long sequential(List<T> group, long budget, Mover<T> mover) {
        long remaining = budget;
        for (T target : group) {
            if (remaining <= 0) {
                break;
            }
            remaining -= mover.move(target, remaining);
        }
        return budget - remaining;
    }

    private static <T> long roundRobin(List<T> group, long budget, Pointer pointer, Mover<T> mover) {
        int size = group.size();
        int start = Math.floorMod(pointer.get(), size);
        long remaining = budget;
        int firstServed = -1;
        for (int i = 0; i < size && remaining > 0; i++) {
            int index = (start + i) % size;
            long moved = mover.move(group.get(index), remaining);
            if (moved > 0) {
                remaining -= moved;
                if (firstServed < 0) {
                    firstServed = index;
                }
            }
        }
        if (firstServed >= 0) {
            pointer.set((firstServed + 1) % size);
        }
        return budget - remaining;
    }

    /**
     * Balanced distribution.
     *
     * <ul>
     * <li>1. read the fill level of each target once and sort by fill level, emptiest first</li>
     * <li>2. weight = max(minimum weight, 1 - fill level)</li>
     * <li>3. per round, offer each target its weighted share of the remaining budget (at least 1)</li>
     * <li>4. targets that accepted their full share stay in the next round</li>
     * <li>5. stop when the budget is used up or a round moves nothing</li>
     * </ul>
     */
    private static <T> long balanced(List<T> group, long budget, ToDoubleFunction<T> fillLevel, Mover<T> mover) {
        List<T> active = new ArrayList<>(group);
        double[] fill = new double[active.size()];
        // Fill levels
        List<Integer> order = new ArrayList<>(active.size());
        for (int i = 0; i < active.size(); i++) {
            fill[i] = clamp01(fillLevel.applyAsDouble(active.get(i)));
            order.add(i);
        }
        order.sort(Comparator.comparingDouble(i -> fill[i]));
        List<T> sorted = new ArrayList<>(active.size());
        List<Double> weights = new ArrayList<>(active.size());
        for (int i : order) {
            sorted.add(active.get(i));
            weights.add(Math.max(MIN_WEIGHT, 1.0 - fill[i]));
        }

        long remaining = budget;
        while (remaining > 0 && !sorted.isEmpty()) {
            double sum = 0;
            for (double weight : weights) {
                sum += weight;
            }
            long movedInRound = 0;
            List<T> nextTargets = new ArrayList<>(sorted.size());
            List<Double> nextWeights = new ArrayList<>(sorted.size());
            long roundBudget = remaining;
            for (int i = 0; i < sorted.size() && remaining > 0; i++) {
                long share = (long) Math.ceil(roundBudget * (weights.get(i) / sum));
                share = Math.max(1, Math.min(share, remaining));
                long moved = mover.move(sorted.get(i), share);
                movedInRound += moved;
                remaining -= moved;
                if (moved >= share) {
                    nextTargets.add(sorted.get(i));
                    nextWeights.add(weights.get(i));
                }
            }
            if (movedInRound == 0) {
                break;
            }
            sorted = nextTargets;
            weights = nextWeights;
        }
        return budget - remaining;
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        return Math.max(0, Math.min(1, value));
    }
}
