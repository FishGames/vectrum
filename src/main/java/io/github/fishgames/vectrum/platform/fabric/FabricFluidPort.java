package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.transfer.Port;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;

/** {@link Port} fuer Fluide auf Basis der Fabric Transfer API. Fabric zaehlt in Tropfen, wir in mB (1 mB = 81 Tropfen). */
final class FabricFluidPort implements Port {
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private final Storage<FluidVariant> storage;

    FabricFluidPort(Storage<FluidVariant> storage) {
        this.storage = storage;
    }

    @Override
    public long moveTo(Port target, long max) {
        if (max <= 0 || !(target instanceof FabricFluidPort other) || other.storage == storage) {
            return 0;
        }
        // Eine Transaktion: entweder komplett oder gar nicht, nichts geht verloren.
        long droplets = SaturatedMath.multiplySaturated(max, DROPLETS_PER_MB);
        long moved = StorageUtil.move(storage, other.storage, variant -> true, droplets, null);
        return moved / DROPLETS_PER_MB;
    }
}
