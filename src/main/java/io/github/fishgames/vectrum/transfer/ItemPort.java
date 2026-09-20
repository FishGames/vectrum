package io.github.fishgames.vectrum.transfer;

import java.util.function.Predicate;

/**
 * Item-Inventar an einer Blockseite (ein {@link Port} für Items). Die Implementierungen liegen in
 * {@code platform/forge} (Item-Handler-Capability) und {@code platform/fabric} (Transfer API). Der Filter des
 * {@link Port} bekommt die Item-Kennung, z. B. {@code "minecraft:cobblestone"}. Eine Sorte ist Item plus NBT.
 */
public interface ItemPort extends Port {
    @Override
    long moveTo(Port target, long max, Predicate<String> filter, int maxTypes);

    /** Ohne Begrenzung der Sortenzahl. */
    @Override
    default long moveTo(Port target, long max, Predicate<String> filter) {
        return moveTo(target, max, filter, Integer.MAX_VALUE);
    }
}
