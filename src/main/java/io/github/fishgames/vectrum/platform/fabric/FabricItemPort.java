package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.transfer.ItemPort;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.ResourceIds;
import io.github.fishgames.vectrum.transfer.RunLimit;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** {@link ItemPort} on the Fabric Transfer API. */
final class FabricItemPort implements ItemPort {
    /** Items per transaction in an unlimited move; the run time limit is checked between chunks. */
    private static final long CHUNK = 4096;

    private final Storage<ItemVariant> storage;

    FabricItemPort(Storage<ItemVariant> storage) {
        this.storage = storage;
    }

    @Override
    public long moveTo(Port target, long max, Predicate<String> filter, int maxTypes) {
        if (max <= 0 || !(target instanceof FabricItemPort other) || other.storage == storage) {
            return 0;
        }
        Predicate<ItemVariant> allowed = filter == ALL
                ? variant -> true
                : variant -> filter.test(ResourceIds.of(variant.getItem()));
        if (maxTypes >= Integer.MAX_VALUE) {
            // One transaction per chunk
            long total = 0;
            while (total < max) {
                long request = Math.min(max - total, CHUNK);
                long step = StorageUtil.move(storage, other.storage, allowed, request, null);
                RunLimit.progress();
                total += step;
                if (step < request || RunLimit.expired()) {
                    break;
                }
            }
            return total;
        }

        // Type-limited move: one item type at a time
        Set<ItemVariant> moved = new HashSet<>();
        Set<ItemVariant> refused = new HashSet<>();
        long total = 0;
        while (total < max && !RunLimit.expired()) {
            ItemVariant candidate = StorageUtil.findExtractableResource(storage,
                    variant -> !refused.contains(variant) && allowed.test(variant)
                            && (moved.contains(variant) || moved.size() < maxTypes), null);
            if (candidate == null) {
                break;
            }
            long now = StorageUtil.move(storage, other.storage, variant -> variant.equals(candidate), max - total, null);
            RunLimit.progress();
            if (now > 0) {
                total += now;
                moved.add(candidate);
            } else {
                refused.add(candidate);
            }
        }
        return total;
    }

    @Override
    public boolean canGive(Predicate<String> filter) {
        for (StorageView<ItemVariant> view : storage) {
            if (!view.isResourceBlank() && view.getAmount() > 0
                    && (filter == ALL || filter.test(ResourceIds.of(view.getResource().getItem())))) {
                return true;
            }
        }
        return false;
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
