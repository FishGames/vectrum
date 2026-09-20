package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.world.level.block.state.BlockState;

/** Ein Block, der Teil eines Vectrum-Netzes ist (Kabel oder Endpunkt) und genau einen Transporttyp führt. */
public interface NetworkBlock {
    TransportType transportType();

    /** Verbinden sich ein Baustein dieses Typs und der Nachbar miteinander? */
    static boolean connects(BlockState neighbour, TransportType type) {
        return neighbour.getBlock() instanceof NetworkBlock other && other.transportType().equals(type);
    }
}
