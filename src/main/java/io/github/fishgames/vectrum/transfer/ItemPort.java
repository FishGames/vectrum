package io.github.fishgames.vectrum.transfer;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Loaderunabhängiger Zugang zu einem Item-Inventar an einer Blockseite. Die Implementierungen liegen in
 * {@code platform/forge} (Item-Handler-Capability) und {@code platform/fabric} (Transfer API).
 *
 * <p>Der gemeinsame Code braucht nur eine einzige Fähigkeit: Ware von einem Port zu einem anderen bewegen.
 * Beide Ports stammen immer vom selben Loader.
 */
public interface ItemPort {
    /**
     * Bewegt höchstens {@code max} Items, die {@code filter} erfüllen, von diesem Port in {@code target}.
     * Es geht nichts verloren: was das Ziel nicht annimmt, bleibt in der Quelle.
     *
     * @return Anzahl der tatsächlich bewegten Items
     */
    int moveTo(ItemPort target, Predicate<ItemStack> filter, int max);
}
