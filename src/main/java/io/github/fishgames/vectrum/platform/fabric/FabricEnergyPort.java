package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.transfer.Port;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

/** {@link Port} fuer Energie auf Basis der Team Reborn Energy API (1 E = 1 FE). Wird mit der Mod mitgeliefert. */
final class FabricEnergyPort implements Port {
    private final EnergyStorage storage;

    FabricEnergyPort(EnergyStorage storage) {
        this.storage = storage;
    }

    @Override
    public long moveTo(Port target, long max) {
        if (max <= 0 || !(target instanceof FabricEnergyPort other) || other.storage == storage) {
            return 0;
        }
        // Eine Transaktion: entweder komplett oder gar nicht.
        return EnergyStorageUtil.move(storage, other.storage, max, null);
    }
}
