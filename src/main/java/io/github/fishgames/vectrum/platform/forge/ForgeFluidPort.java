package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.ResourceIds;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.function.Predicate;

/** {@link Port} for fluids on the Forge fluid handler capability. Unit: mB. */
final class ForgeFluidPort implements Port {
    private final IFluidHandler handler;

    ForgeFluidPort(IFluidHandler handler) {
        this.handler = handler;
    }

    @Override
    public long moveTo(Port target, long max, Predicate<String> filter) {
        if (max <= 0 || !(target instanceof ForgeFluidPort other) || other.handler == handler) {
            return 0;
        }
        int limit = SaturatedMath.clampToNonNegativeInt(max);

        // 1. probe: source offer
        FluidStack offered = filter == ALL
                ? handler.drain(limit, IFluidHandler.FluidAction.SIMULATE)
                : firstAllowed(limit, filter);
        if (offered.isEmpty()) {
            return 0;
        }
        // 2. probe: target acceptance
        int accepted = other.handler.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }
        // 3. transfer
        FluidStack taken = handler.drain(new FluidStack(offered, accepted), IFluidHandler.FluidAction.EXECUTE);
        if (taken.isEmpty()) {
            return 0;
        }
        int filled = other.handler.fill(taken, IFluidHandler.FluidAction.EXECUTE);
        if (filled < taken.getAmount()) {
            // 4. return remainder to the source
            FluidStack rest = new FluidStack(taken, taken.getAmount() - filled);
            int back = handler.fill(rest, IFluidHandler.FluidAction.EXECUTE);
            if (back < rest.getAmount()) {
                Vectrum.LOGGER.warn("{} mB {} could be filled neither into the target nor back into the source",
                        rest.getAmount() - back, rest.getFluid());
            }
        }
        return filled;
    }

    /** First fluid in the source tanks that the filter allows (simulated drain). */
    private FluidStack firstAllowed(int limit, Predicate<String> filter) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack inTank = handler.getFluidInTank(tank);
            if (inTank.isEmpty() || !filter.test(ResourceIds.of(inTank.getFluid()))) {
                continue;
            }
            FluidStack offered = handler.drain(new FluidStack(inTank, limit), IFluidHandler.FluidAction.SIMULATE);
            if (!offered.isEmpty()) {
                return offered;
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public double fillLevel() {
        long stored = 0;
        long capacity = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            stored += handler.getFluidInTank(tank).getAmount();
            capacity += handler.getTankCapacity(tank);
        }
        return capacity <= 0 ? 1 : (double) stored / capacity;
    }
}
