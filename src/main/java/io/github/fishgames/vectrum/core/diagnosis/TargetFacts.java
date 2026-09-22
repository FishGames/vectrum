package io.github.fishgames.vectrum.core.diagnosis;

/**
 * Facts about a target side.
 *
 * @param networkPresent the block is registered in a network
 * @param sources        source sides in the reachable networks
 * @param ownFull        the storage at this side is full
 * @param receivedRecent goods arrived lately
 */
public record TargetFacts(boolean networkPresent, int sources, boolean ownFull, boolean receivedRecent) {
}
