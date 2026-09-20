package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.transfer.Port;
import net.minecraftforge.energy.IEnergyStorage;

/** {@link Port} fuer Energie auf Basis der Forge-Energy-Capability (gilt auch fuer NeoForge 1.20.1). Einheit: FE. */
final class ForgeEnergyPort implements Port {
    private final IEnergyStorage storage;

    ForgeEnergyPort(IEnergyStorage storage) {
        this.storage = storage;
    }

    @Override
    public long moveTo(Port target, long max) {
        if (max <= 0 || !(target instanceof ForgeEnergyPort other) || other.storage == storage) {
            return 0;
        }
        if (!storage.canExtract() || !other.storage.canReceive()) {
            return 0;
        }
        int limit = SaturatedMath.clampToNonNegativeInt(max);

        // 1. Probe: Was koennte die Quelle hergeben? 2. Probe: Wie viel davon nimmt das Ziel?
        int offered = storage.extractEnergy(limit, true);
        if (offered <= 0) {
            return 0;
        }
        int accepted = other.storage.receiveEnergy(offered, true);
        if (accepted <= 0) {
            return 0;
        }
        // Echte Uebergabe
        int taken = storage.extractEnergy(accepted, false);
        if (taken <= 0) {
            return 0;
        }
        int received = other.storage.receiveEnergy(taken, false);
        if (received < taken) {
            // Sollte nach der Probe nicht vorkommen. Zur Sicherheit zurueck in die Quelle geben.
            int back = storage.receiveEnergy(taken - received, false);
            if (back < taken - received) {
                Vectrum.LOGGER.warn("{} FE gingen bei der Uebergabe verloren", taken - received - back);
            }
        }
        return received;
    }
}
