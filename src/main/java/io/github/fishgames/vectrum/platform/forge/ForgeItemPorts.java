package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.transfer.ItemPort;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Findet Inventare über die Item-Handler-Capability des Blockentities. */
final class ForgeItemPorts {
    private ForgeItemPorts() {
    }

    static ItemPort find(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                .resolve()
                .<ItemPort>map(ForgeItemPort::new)
                .orElse(null);
    }
}
