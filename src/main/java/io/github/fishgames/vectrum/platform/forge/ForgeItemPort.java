package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.transfer.ItemPort;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.function.Predicate;

/** {@link ItemPort} auf Basis der Forge-Item-Handler-Capability (gilt auch für NeoForge 1.20.1). */
final class ForgeItemPort implements ItemPort {
    private final IItemHandler handler;

    ForgeItemPort(IItemHandler handler) {
        this.handler = handler;
    }

    @Override
    public int moveTo(ItemPort target, Predicate<ItemStack> filter, int max) {
        if (max <= 0 || !(target instanceof ForgeItemPort other) || other.handler == handler) {
            return 0;
        }
        IItemHandler from = handler;
        IItemHandler to = other.handler;

        int moved = 0;
        for (int slot = 0; slot < from.getSlots() && moved < max; slot++) {
            // 1. Probe: Was könnte dieser Slot hergeben?
            ItemStack probe = from.extractItem(slot, max - moved, true);
            if (probe.isEmpty() || !filter.test(probe)) {
                continue;
            }
            // 2. Probe: Wie viel davon nimmt das Ziel?
            ItemStack rest = ItemHandlerHelper.insertItem(to, probe, true);
            int accepted = probe.getCount() - rest.getCount();
            if (accepted <= 0) {
                continue;
            }
            // Echte Übergabe
            ItemStack taken = from.extractItem(slot, accepted, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack leftover = ItemHandlerHelper.insertItem(to, taken, false);
            moved += taken.getCount() - leftover.getCount();
            if (!leftover.isEmpty()) {
                // Sollte nach der Probe nicht vorkommen. Zur Sicherheit zurück in die Quelle legen.
                ItemStack lost = ItemHandlerHelper.insertItem(from, leftover, false);
                if (!lost.isEmpty()) {
                    Vectrum.LOGGER.warn("{}x {} konnten weder ins Ziel noch zurück in die Quelle gelegt werden",
                            lost.getCount(), lost.getItem());
                }
            }
        }
        return moved;
    }
}
