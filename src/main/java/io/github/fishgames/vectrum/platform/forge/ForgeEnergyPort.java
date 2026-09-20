package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.transfer.Port;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.function.Predicate;

/** {@link Port} for energy on the Forge energy capability. Unit: FE; the filter is ignored. */
final class ForgeEnergyPort implements Port {
    private final IEnergyStorage storage;

    ForgeEnergyPort(IEnergyStorage storage) {
        this.storage = storage;
    }

    @Override
    public long moveTo(Port target, long max, Predicate<String> filter) {
        if (max <= 0 || !(target instanceof ForgeEnergyPort other) || other.storage == storage) {
            return 0;
        }
        if (!storage.canExtract() || !other.storage.canReceive()) {
            return 0;
        }
        int limit = SaturatedMath.clampToNonNegativeInt(max);

        // 1. probe: source offer
        int offered = storage.extractEnergy(limit, true);
        if (offered <= 0) {
            return 0;
        }
        int accepted = other.storage.receiveEnergy(offered, true);
        if (accepted <= 0) {
            return 0;
        }
        // 3. transfer
        int taken = storage.extractEnergy(accepted, false);
        if (taken <= 0) {
            return 0;
        }
        int received = other.storage.receiveEnergy(taken, false);
        if (received < taken) {
            // 4. return remainder to the source
            int back = storage.receiveEnergy(taken - received, false);
            if (back < taken - received) {
                Vectrum.LOGGER.warn("{} FE were lost during the transfer", taken - received - back);
            }
        }
        return received;
    }

    @Override
    public double fillLevel() {
        int capacity = storage.getMaxEnergyStored();
        return capacity <= 0 ? 1 : (double) storage.getEnergyStored() / capacity;
    }
}
