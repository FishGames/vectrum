package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.core.util.EnergyBalance;
import io.github.fishgames.vectrum.transfer.Port;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

import java.util.function.Predicate;

/** {@link Port} for energy on the Team Reborn Energy API (1 E = 1 FE); the filter is ignored. */
final class FabricEnergyPort implements Port {
    private final EnergyStorage storage;

    FabricEnergyPort(EnergyStorage storage) {
        this.storage = storage;
    }

    @Override
    public long moveTo(Port target, long max, Predicate<String> filter) {
        if (max <= 0 || !(target instanceof FabricEnergyPort other) || other.storage == storage) {
            return 0;
        }
        long limit = max;
        if (storage.supportsInsertion() && other.storage.supportsExtraction()
                && storage.getAmount() > 0 && other.storage.getAmount() > 0) {
            // two-way storages (batteries): move towards equal fill levels instead of back and forth
            limit = EnergyBalance.limit(storage.getAmount(), storage.getCapacity(), other.storage.getAmount(),
                    other.storage.getCapacity(), max);
            if (limit <= 0) {
                return 0;
            }
        }
        // Single transaction
        return EnergyStorageUtil.move(storage, other.storage, limit, null);
    }

    @Override
    public boolean canGive(Predicate<String> filter) {
        return storage.supportsExtraction() && storage.getAmount() > 0;
    }

    @Override
    public double fillLevel() {
        long capacity = storage.getCapacity();
        return capacity <= 0 ? 1 : (double) storage.getAmount() / capacity;
    }
}
