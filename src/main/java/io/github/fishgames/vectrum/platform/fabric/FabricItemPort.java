package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.transfer.ItemPort;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** {@link ItemPort} auf Basis der Fabric Transfer API. Vanilla-Inventare werden von der API automatisch angepasst. */
final class FabricItemPort implements ItemPort {
    private final Storage<ItemVariant> storage;

    FabricItemPort(Storage<ItemVariant> storage) {
        this.storage = storage;
    }

    @Override
    public int moveTo(ItemPort target, Predicate<ItemStack> filter, int max) {
        if (max <= 0 || !(target instanceof FabricItemPort other) || other.storage == storage) {
            return 0;
        }
        // StorageUtil.move arbeitet mit einer Transaktion: entweder komplett oder gar nicht, nichts geht verloren.
        long moved = StorageUtil.move(storage, other.storage, variant -> filter.test(variant.toStack()), max, null);
        return (int) moved;
    }
}
