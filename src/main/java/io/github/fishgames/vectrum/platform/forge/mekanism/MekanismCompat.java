package io.github.fishgames.vectrum.platform.forge.mekanism;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.transfer.ResourceIds;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.registries.IForgeRegistry;

/** Mekanism gas integration: gas port finder and gas id check. */
public final class MekanismCompat {
    private static final Capability<IGasHandler> GAS_HANDLER = CapabilityManager.get(new CapabilityToken<>() {
    });

    private MekanismCompat() {
    }

    /** @return {@code true} if the gas module could be set up */
    public static boolean init() {
        try {
            Ports.setFinder(TransportType.GAS, MekanismCompat::find);
            ResourceIds.setGasIds(MekanismCompat::isGas);
            Vectrum.LOGGER.info("Mekanism found: gas module enabled");
            return true;
        } catch (LinkageError | RuntimeException e) {
            Vectrum.LOGGER.warn("Gas module disabled: unexpected Mekanism API ({})", e.toString());
            return false;
        }
    }

    private static Port find(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(GAS_HANDLER, side)
                .resolve()
                .<Port>map(MekanismGasPort::new)
                .orElse(null);
    }

    private static boolean isGas(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        IForgeRegistry<?> registry = MekanismAPI.gasRegistry();
        return location != null && registry != null && registry.containsKey(location);
    }
}
