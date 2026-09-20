package io.github.fishgames.vectrum.platform.fabric;

import io.github.fishgames.vectrum.transfer.ItemPort;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Findet Inventare über die Item-Lookup der Fabric Transfer API. */
final class FabricItemPorts {
    private FabricItemPorts() {
    }

    static ItemPort find(Level level, BlockPos pos, Direction side) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new FabricItemPort(storage);
    }
}
