package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.transfer.ItemPort;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.ResourceIds;
import io.github.fishgames.vectrum.transfer.RunLimit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** {@link ItemPort} on the Forge item handler capability. */
final class ForgeItemPort implements ItemPort {
    /** Refused stacks remembered per move. */
    private static final int MAX_REMEMBERED_REFUSALS = 8;

    private final IItemHandler handler;
    /** Slots that held an item when first looked at; the port lives for one round of one source side. */
    private int[] occupied;

    ForgeItemPort(IItemHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean canGive(Predicate<String> filter) {
        for (int slot : occupiedSlots()) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && (filter == ALL || filter.test(ResourceIds.of(stack.getItem())))) {
                return true;
            }
        }
        if (occupiedSlots().length > 0) {
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack probe = handler.extractItem(slot, 1, true);
            if (!probe.isEmpty() && (filter == ALL || filter.test(ResourceIds.of(probe.getItem())))) {
                return true;
            }
        }
        return false;
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
        List<ItemStack> refused = null;                  // stacks the target took none of
        Map<Item, Boolean> verdicts = filter == ALL ? null : new IdentityHashMap<>();
        int[] slots = occupiedSlots();
        if (slots.length == 0) {
            slots = allSlots();
        }
        for (int slot : slots) {
            if (moved >= max || RunLimit.expired()) {
                break;
            }
            RunLimit.progress();
            // 1. probe: slot offer
            ItemStack probe = from.extractItem(slot, max - moved, true);
            if (probe.isEmpty() || (verdicts != null && !allowed(verdicts, filter, probe.getItem()))) {
                continue;
            }
            if (movedTypes.size() >= maxTypes && !containsType(movedTypes, probe)) {
                continue;
            }
            if (refused != null && containsType(refused, probe)) {
                continue;
            }
            // 2. probe: target acceptance
            ItemStack rest = ItemHandlerHelper.insertItem(to, probe, true);
            int accepted = probe.getCount() - rest.getCount();
            if (accepted <= 0) {
                if (refused == null) {
                    refused = new ArrayList<>(2);
                }
                if (refused.size() < MAX_REMEMBERED_REFUSALS) {
                    refused.add(probe);
                }
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
            if (delivered > 0) {
                refused = null;
                if (!containsType(movedTypes, taken)) {
                    movedTypes.add(taken);
                }
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

    private int[] occupiedSlots() {
        int[] slots = occupied;
        if (slots == null) {
            int count = handler.getSlots();
            int[] found = new int[count];
            int size = 0;
            for (int slot = 0; slot < count; slot++) {
                if (!handler.getStackInSlot(slot).isEmpty()) {
                    found[size++] = slot;
                }
            }
            slots = size == count ? found : java.util.Arrays.copyOf(found, size);
            occupied = slots;
        }
        return slots;
    }

    private int[] allSlots() {
        int[] all = new int[handler.getSlots()];
        for (int slot = 0; slot < all.length; slot++) {
            all[slot] = slot;
        }
        return all;
    }

    private static boolean allowed(Map<Item, Boolean> verdicts, Predicate<String> filter, Item item) {
        Boolean known = verdicts.get(item);
        if (known == null) {
            known = filter.test(ResourceIds.of(item));
            verdicts.put(item, known);
        }
        return known;
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
