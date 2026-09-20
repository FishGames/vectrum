package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.ResourceIds;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;

import java.util.function.Predicate;

/** {@link Port} fuer Fluide auf Basis der Fabric Transfer API. Fabric zaehlt in Tropfen, wir in mB (1 mB = 81 Tropfen). */
final class FabricFluidPort implements Port {
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private final Storage<FluidVariant> storage;

    FabricFluidPort(Storage<FluidVariant> storage) {
        this.storage = storage;
    }

    @Override
    public long moveTo(Port target, long max, Predicate<String> filter) {
        if (max <= 0 || !(target instanceof FabricFluidPort other) || other.storage == storage) {
            return 0;
        }
        // Eine Transaktion: entweder komplett oder gar nicht, nichts geht verloren.
        long droplets = SaturatedMath.multiplySaturated(max, DROPLETS_PER_MB);
        Predicate<FluidVariant> allowed = filter == ALL
                ? variant -> true
                : variant -> filter.test(ResourceIds.of(variant.getFluid()));
        long moved = StorageUtil.move(storage, other.storage, allowed, droplets, null);
        return moved / DROPLETS_PER_MB;
    }

    @Override
    public double fillLevel() {
        long stored = 0;
        long capacity = 0;
        for (StorageView<FluidVariant> view : storage) {
            stored += view.getAmount();
            capacity += view.getCapacity();
        }
        return capacity <= 0 ? 1 : (double) stored / capacity;
    }
}
