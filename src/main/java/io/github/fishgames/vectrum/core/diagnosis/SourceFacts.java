package io.github.fishgames.vectrum.core.diagnosis;

/**
 * Facts about a source side.
 *
 * @param networkPresent  the block is registered in a network
 * @param sourceEmpty     the source storage holds nothing (known)
 * @param open            targets that can accept goods
 * @param full            targets that are full
 * @param filtered        targets whose filter passes nothing of the source
 * @param unloaded        targets in unloaded chunks
 * @param missing         targets without a storage
 * @param locked          wireless receivers excluded by the dimension rule
 * @param wireless        the source is a wireless port
 * @param lastMoved       units moved by the last transfer
 * @param lastBudget      throughput limit of the last transfer
 * @param recent          a transfer was recorded lately
 */
public record SourceFacts(boolean networkPresent, boolean sourceEmpty,
                          int open, int full, int filtered, int unloaded, int missing, int locked,
                          boolean wireless, long lastMoved, long lastBudget, boolean recent) {
    /** All targets. */
    public int total() {
        return open + full + filtered + unloaded + missing;
    }
}
