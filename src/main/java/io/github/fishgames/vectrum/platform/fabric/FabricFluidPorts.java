package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.transfer.Port;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Fluid storage lookup on the Fabric Transfer API. */
final class FabricFluidPorts {
    private FabricFluidPorts() {
    }

    static Port find(Level level, BlockPos pos, Direction side) {
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new FabricFluidPort(storage);
    }
}
