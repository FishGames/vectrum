package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.transfer.Port;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import team.reborn.energy.api.EnergyStorage;

/** Energy storage lookup on the Team Reborn Energy API. */
final class FabricEnergyPorts {
    private FabricEnergyPorts() {
    }

    static Port find(Level level, BlockPos pos, Direction side) {
        EnergyStorage storage = EnergyStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new FabricEnergyPort(storage);
    }
}
