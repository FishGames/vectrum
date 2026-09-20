package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.transfer.Port;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Findet Energiespeicher ueber die Energy-Capability des Blockentities. */
final class ForgeEnergyPorts {
    private ForgeEnergyPorts() {
    }

    static Port find(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(ForgeCapabilities.ENERGY, side)
                .resolve()
                .<Port>map(ForgeEnergyPort::new)
                .orElse(null);
    }
}
