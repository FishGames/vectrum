package io.github.fishgames.vectrum.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Zentraler Zugang für den gemeinsamen Code. Der Loader trägt beim Start seine Umsetzung ein. */
public final class ItemPorts {
    private static ItemPortFinder finder;

    private ItemPorts() {
    }

    public static void setFinder(ItemPortFinder newFinder) {
        finder = newFinder;
    }

    /**
     * Das Inventar an {@code pos}, angesprochen von der Seite {@code side} dieses Blocks, oder {@code null}.
     * Der Chunk an {@code pos} muss geladen sein.
     */
    public static ItemPort find(Level level, BlockPos pos, Direction side) {
        return finder == null ? null : finder.find(level, pos, side);
    }
}
