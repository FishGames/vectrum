package io.github.fishgames.vectrum.transfer;

import io.github.fishgames.vectrum.core.util.SaturatedMath;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Item-Inventar an einer Blockseite (ein {@link Port} für Items). Die Implementierungen liegen in
 * {@code platform/forge} (Item-Handler-Capability) und {@code platform/fabric} (Transfer API).
 *
 * <p>Der gemeinsame Code braucht nur eine einzige Fähigkeit: Ware von einem Port zu einem anderen bewegen.
 * Beide Ports stammen immer vom selben Loader.
 */
public interface ItemPort extends Port {
    /** Filter, der alles durchlässt. */
    Predicate<ItemStack> ANY = stack -> true;

    /**
     * Bewegt höchstens {@code max} Items, die {@code filter} erfüllen, von diesem Port in {@code target}.
     * Es geht nichts verloren: was das Ziel nicht annimmt, bleibt in der Quelle.
     *
     * @return Anzahl der tatsächlich bewegten Items
     */
    int moveTo(ItemPort target, Predicate<ItemStack> filter, int max);

    /** Ohne Filter: alles, was das Ziel annimmt (Menge auf {@code int} geklemmt, siehe K5). */
    @Override
    default long moveTo(Port target, long max) {
        return target instanceof ItemPort other ? moveTo(other, ANY, SaturatedMath.clampToNonNegativeInt(max)) : 0;
    }
}
