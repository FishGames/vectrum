package io.github.fishgames.vectrum.core.digital;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Coder links: which transport networks are connected through one digital network.
 *
 * <ul>
 * <li>Each coder has a frequency and belongs to one transport network.</li>
 * <li>Two transport networks are linked when each has a coder in the same digital network with the same frequency.</li>
 * <li>Links are single-hop (no forwarding).</li>
 * </ul>
 */
public final class CoderLinks {
    private CoderLinks() {
    }

    /**
     * Coder in a digital network.
     *
     * @param pos       packed position (sort key)
     * @param frequency coder frequency
     * @param network   transport network id, or {@code -1} when it is in none
     */
    public record Coder(long pos, int frequency, long network) {
    }

    /**
     * Partner coders of {@code ownNetwork}.
     *
     * <ul>
     * <li>1. collect the frequencies of the coders in {@code ownNetwork}</li>
     * <li>2. sort all coders by position</li>
     * <li>3. per other network with a matching frequency, keep the coder with the smallest position</li>
     * </ul>
     */
    public static List<Coder> partners(long ownNetwork, List<Coder> coders) {
        Set<Integer> frequencies = new HashSet<>();
        for (Coder coder : coders) {
            if (coder.network() == ownNetwork) {
                frequencies.add(coder.frequency());
            }
        }
        List<Coder> sorted = new ArrayList<>(coders);
        sorted.sort((a, b) -> Long.compare(a.pos(), b.pos()));

        List<Coder> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Coder coder : sorted) {
            if (coder.network() >= 0 && coder.network() != ownNetwork && frequencies.contains(coder.frequency())
                    && seen.add(coder.network())) {
                result.add(coder);
            }
        }
        return result;
    }
}
