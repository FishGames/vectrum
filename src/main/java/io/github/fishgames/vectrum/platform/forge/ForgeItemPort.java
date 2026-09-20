package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.transfer.ItemPort;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.ResourceIds;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** {@link ItemPort} on the Forge item handler capability. */
final class ForgeItemPort implements ItemPort {
    private final IItemHandler handler;

    ForgeItemPort(IItemHandler handler) {
        this.handler = handler;
    }

    @Override
    public long moveTo(Port target, long requested, Predicate<String> filter, int maxTypes) {
        if (requested <= 0 || !(target instanceof ForgeItemPort other) || other.handler == handler) {
            return 0;
        }
        int max = SaturatedMath.clampToNonNegativeInt(requested);
        IItemHandler from = handler;
        IItemHandler to = other.handler;

        int moved = 0;
        List<ItemStack> movedTypes = new ArrayList<>(); // item types moved so far
        for (int slot = 0; slot < from.getSlots() && moved < max; slot++) {
            // 1. probe: slot offer
            ItemStack probe = from.extractItem(slot, max - moved, true);
            if (probe.isEmpty() || (filter != ALL && !filter.test(ResourceIds.of(probe.getItem())))) {
                continue;
            }
            if (movedTypes.size() >= maxTypes && !containsType(movedTypes, probe)) {
                continue;
            }
            // 2. probe: target acceptance
            ItemStack rest = ItemHandlerHelper.insertItem(to, probe, true);
            int accepted = probe.getCount() - rest.getCount();
            if (accepted <= 0) {
                continue;
            }
            // 3. transfer
            ItemStack taken = from.extractItem(slot, accepted, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack leftover = ItemHandlerHelper.insertItem(to, taken, false);
            int delivered = taken.getCount() - leftover.getCount();
            moved += delivered;
            if (delivered > 0 && !containsType(movedTypes, taken)) {
                movedTypes.add(taken);
            }
            if (!leftover.isEmpty()) {
                // 4. return remainder to the source
                ItemStack lost = ItemHandlerHelper.insertItem(from, leftover, false);
                if (!lost.isEmpty()) {
                    Vectrum.LOGGER.warn("{}x {} could be moved neither to the target nor back to the source",
                            lost.getCount(), lost.getItem());
                }
            }
        }
        return moved;
    }

    private static boolean containsType(List<ItemStack> types, ItemStack stack) {
        for (ItemStack type : types) {
            if (ItemStack.isSameItemSameTags(type, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double fillLevel() {
        long stored = 0;
        long capacity = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            int perStack = stack.isEmpty() ? 64 : stack.getMaxStackSize();
            capacity += Math.min(handler.getSlotLimit(slot), perStack);
            stored += stack.getCount();
        }
        return capacity <= 0 ? 1 : (double) stored / capacity;
    }
}
