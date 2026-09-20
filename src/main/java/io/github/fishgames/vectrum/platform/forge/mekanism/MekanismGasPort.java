package io.github.fishgames.vectrum.platform.forge.mekanism;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.transfer.Port;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;

import java.util.function.Predicate;

/** {@link Port} for Mekanism gases. Unit: mB. */
final class MekanismGasPort implements Port {
    private final IGasHandler handler;

    MekanismGasPort(IGasHandler handler) {
        this.handler = handler;
    }

    @Override
    public long moveTo(Port target, long max, Predicate<String> filter) {
        if (max <= 0 || !(target instanceof MekanismGasPort other) || other.handler == handler) {
            return 0;
        }
        // 1. probe: source offer
        GasStack offered = offer(max, filter);
        if (offered.isEmpty()) {
            return 0;
        }
        // 2. probe: target acceptance
        long accepted = offered.getAmount() - other.handler.insertChemical(offered, Action.SIMULATE).getAmount();
        if (accepted <= 0) {
            return 0;
        }
        // 3. transfer
        GasStack taken = handler.extractChemical(new GasStack(offered, accepted), Action.EXECUTE);
        if (taken.isEmpty()) {
            return 0;
        }
        GasStack leftover = other.handler.insertChemical(taken, Action.EXECUTE);
        if (!leftover.isEmpty()) {
            GasStack lost = handler.insertChemical(leftover, Action.EXECUTE);
            if (!lost.isEmpty()) {
                Vectrum.LOGGER.warn("{} mB gas could be moved neither to the target nor back to the source", lost.getAmount());
            }
        }
        return taken.getAmount() - leftover.getAmount();
    }

    private GasStack offer(long max, Predicate<String> filter) {
        if (filter == ALL) {
            return handler.extractChemical(max, Action.SIMULATE);
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            GasStack inTank = handler.getChemicalInTank(tank);
            if (inTank.isEmpty() || !filter.test(inTank.getTypeRegistryName().toString())) {
                continue;
            }
            GasStack offered = handler.extractChemical(new GasStack(inTank, max), Action.SIMULATE);
            if (!offered.isEmpty()) {
                return offered;
            }
        }
        return GasStack.EMPTY;
    }

    @Override
    public double fillLevel() {
        long stored = 0;
        long capacity = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            stored += handler.getChemicalInTank(tank).getAmount();
            capacity += handler.getTankCapacity(tank);
        }
        return capacity <= 0 ? 1 : (double) stored / capacity;
    }
}
