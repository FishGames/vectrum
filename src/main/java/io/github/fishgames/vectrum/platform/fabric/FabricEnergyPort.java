package io.github.fishgames.vectrum.platform.fabric;

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
        // Single transaction
        return EnergyStorageUtil.move(storage, other.storage, max, null);
    }

    @Override
    public double fillLevel() {
        long capacity = storage.getCapacity();
        return capacity <= 0 ? 1 : (double) storage.getAmount() / capacity;
    }
}
