package io.github.fishgames.vectrum.core.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * Die Routing-Kaskade für eine Übergabe: <b>Filter → Priorität → Modus</b>.
 *
 * <ol>
 *   <li><b>Filter</b> entscheidet, welche Ware ein Ziel überhaupt bekommt. Er wirkt beim Übergeben selbst
 *       (der {@link Mover} prüft ihn für jede konkrete Ware), denn erst dort ist bekannt, was die Quelle hergibt.</li>
 *   <li><b>Priorität</b>: Ziele mit höherer Zahl werden zuerst bedient. Erst wenn die Gruppe nichts mehr aufnimmt,
 *       kommt die nächste Gruppe an die Reihe.</li>
 *   <li><b>Modus</b> entscheidet innerhalb einer Gruppe gleicher Priorität, siehe {@link DistributionMode}.</li>
 * </ol>
 *
 * <p>Die Klasse kennt weder Minecraft noch die Ware: Sie sagt nur, wem sie wie viel anbietet, und bekommt vom
 * {@link Mover} zurück, wie viel tatsächlich ankam. Nichts geht verloren, weil der Mover nur bewegt, was das Ziel
 * annimmt. Sie ist deshalb ohne laufendes Spiel testbar.
 */
public final class Router {
    /** Untergrenze der Gewichtung beim Ausgleichen, damit auch fast volle Ziele noch etwas anbieten dürfen. */
    private static final double MIN_WEIGHT = 0.05;

    private Router() {
    }

    /** Führt die tatsächliche Übergabe aus. */
    @FunctionalInterface
    public interface Mover<T> {
        /** Bietet dem Ziel höchstens {@code max} Einheiten an und liefert, wie viel es angenommen hat (0 bis max). */
        long move(T target, long max);
    }

    /** Lese- und Schreibzugriff auf den Rundlaufzeiger der Quelle. */
    public interface Pointer {
        int get();

        void set(int value);
    }

    /**
     * Verteilt bis zu {@code budget} Einheiten auf die Ziele.
     *
     * @param targets   alle in Frage kommenden Ziele in fester Reihenfolge (z. B. nach Position sortiert); diese
     *                  Reihenfolge gilt bei Gleichstand
     * @param priority  Priorität eines Ziels (höher = zuerst)
     * @param fillLevel Füllstand eines Ziels von 0 (leer) bis 1 (voll); wird nur im Modus BALANCED abgefragt
     * @param pointer   Rundlaufzeiger der Quelle; wird nur im Modus ROUND_ROBIN gelesen und geschrieben
     * @return die insgesamt bewegte Menge, nie mehr als {@code budget}
     */
    public static <T> long distribute(long budget, List<T> targets, ToIntFunction<T> priority,
                                      ToDoubleFunction<T> fillLevel, DistributionMode mode, Pointer pointer,
                                      Mover<T> mover) {
        if (budget <= 0 || targets.isEmpty()) {
            return 0;
        }

        // Gruppen nach Priorität, höchste zuerst; innerhalb der Gruppe bleibt die Eingangsreihenfolge.
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
            pointer.set((firstServed + 1) % size); // nächste Übergabe beginnt hinter dem zuerst belieferten Ziel
        }
        return budget - remaining;
    }

    /**
     * Ausgleichen: Leerere Ziele bekommen einen größeren Anteil (Gewicht = 1 - Füllstand). Was ein Ziel nicht
     * aufnimmt, wird in der nächsten Runde auf die übrigen verteilt, bis nichts mehr geht.
     */
    private static <T> long balanced(List<T> group, long budget, ToDoubleFunction<T> fillLevel, Mover<T> mover) {
        List<T> active = new ArrayList<>(group);
        double[] fill = new double[active.size()];
        // Füllstand einmal je Übergabe abfragen (das kann teuer sein) und leerste Ziele zuerst.
        List<Integer> order = new ArrayList<>(active.size());
        for (int i = 0; i < active.size(); i++) {
            fill[i] = clamp01(fillLevel.applyAsDouble(active.get(i)));
            order.add(i);
        }
        order.sort(Comparator.comparingDouble(i -> fill[i])); // stabil: gleiche Füllstände behalten die Reihenfolge
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
                if (moved >= share) { // hat alles genommen: bleibt im Rennen
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
