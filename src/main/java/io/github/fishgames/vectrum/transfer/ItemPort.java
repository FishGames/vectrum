package io.github.fishgames.vectrum.transfer;

/**
 * Item-Inventar an einer Blockseite (ein {@link Port} für Items). Die Implementierungen liegen in
 * {@code platform/forge} (Item-Handler-Capability) und {@code platform/fabric} (Transfer API). Der Filter des
 * {@link Port} bekommt die Item-Kennung, z. B. {@code "minecraft:cobblestone"}.
 */
public interface ItemPort extends Port {
}
