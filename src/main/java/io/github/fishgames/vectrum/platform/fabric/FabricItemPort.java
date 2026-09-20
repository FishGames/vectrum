package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.transfer.ItemPort;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.ResourceIds;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;

import java.util.function.Predicate;

/** {@link ItemPort} auf Basis der Fabric Transfer API. Vanilla-Inventare werden von der API automatisch angepasst. */
final class FabricItemPort implements ItemPort {
    private final Storage<ItemVariant> storage;

    FabricItemPort(Storage<ItemVariant> storage) {
        this.storage = storage;
    }

    @Override
    public long moveTo(Port target, long max, Predicate<String> filter) {
        if (max <= 0 || !(target instanceof FabricItemPort other) || other.storage == storage) {
            return 0;
        }
        // StorageUtil.move arbeitet mit einer Transaktion: entweder komplett oder gar nicht, nichts geht verloren.
        Predicate<ItemVariant> allowed = filter == ALL
                ? variant -> true
                : variant -> filter.test(ResourceIds.of(variant.getItem()));
        return StorageUtil.move(storage, other.storage, allowed, max, null);
    }

    @Override
    public double fillLevel() {
        long stored = 0;
        long capacity = 0;
        for (StorageView<ItemVariant> view : storage) {
            stored += view.getAmount();
            capacity += view.getCapacity();
        }
        return capacity <= 0 ? 1 : (double) stored / capacity;
    }
}
