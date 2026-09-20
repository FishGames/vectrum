package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.transfer.Port;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Findet Fluidtanks ueber die Fluid-Handler-Capability des Blockentities (und vanilla Kessel). */
final class ForgeFluidPorts {
    private ForgeFluidPorts() {
    }

    static Port find(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            // Vanilla-Kessel haben keinen Blockentity und keine Capability; ein eigener Handler macht sie nutzbar.
            CauldronFluidHandler cauldron = CauldronFluidHandler.find(level, pos);
            return cauldron == null ? null : new ForgeFluidPort(cauldron);
        }
        return blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, side)
                .resolve()
                .<Port>map(ForgeFluidPort::new)
                .orElse(null);
    }
}
