package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.transfer.Port;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** {@link Port} fuer Fluide auf Basis der Forge-Fluid-Handler-Capability (gilt auch fuer NeoForge 1.20.1). Einheit: mB. */
final class ForgeFluidPort implements Port {
    private final IFluidHandler handler;

    ForgeFluidPort(IFluidHandler handler) {
        this.handler = handler;
    }

    @Override
    public long moveTo(Port target, long max) {
        if (max <= 0 || !(target instanceof ForgeFluidPort other) || other.handler == handler) {
            return 0;
        }
        int limit = SaturatedMath.clampToNonNegativeInt(max);

        // 1. Probe: Was koennte die Quelle hergeben?
        FluidStack offered = handler.drain(limit, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return 0;
        }
        // 2. Probe: Wie viel davon nimmt das Ziel?
        int accepted = other.handler.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }
        // Echte Uebergabe
        FluidStack taken = handler.drain(new FluidStack(offered, accepted), IFluidHandler.FluidAction.EXECUTE);
        if (taken.isEmpty()) {
            return 0;
        }
        int filled = other.handler.fill(taken, IFluidHandler.FluidAction.EXECUTE);
        if (filled < taken.getAmount()) {
            // Sollte nach der Probe nicht vorkommen. Zur Sicherheit zurueck in die Quelle fuellen.
            FluidStack rest = new FluidStack(taken, taken.getAmount() - filled);
            int back = handler.fill(rest, IFluidHandler.FluidAction.EXECUTE);
            if (back < rest.getAmount()) {
                Vectrum.LOGGER.warn("{} mB {} konnten weder ins Ziel noch zurueck in die Quelle gefuellt werden",
                        rest.getAmount() - back, rest.getFluid());
            }
        }
        return filled;
    }
}
