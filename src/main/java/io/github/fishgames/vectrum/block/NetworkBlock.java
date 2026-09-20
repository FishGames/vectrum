package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Block that is a node of a Vectrum network (cable or endpoint); one node per carried transport type. */
public interface NetworkBlock {
    /** All carried types (at least one, fixed order). */
    List<TransportType> transportTypes();

    /** First (main) type. */
    default TransportType transportType() {
        return transportTypes().get(0);
    }

    default boolean carries(TransportType type) {
        return transportTypes().contains(type);
    }

    /** Whether the neighbour is a network block carrying {@code type}. */
    static boolean connects(BlockState neighbour, TransportType type) {
        return neighbour.getBlock() instanceof NetworkBlock other && other.carries(type);
    }

    /** Whether the neighbour is a network block carrying at least one of {@code types}. */
    static boolean connectsAny(BlockState neighbour, List<TransportType> types) {
        if (!(neighbour.getBlock() instanceof NetworkBlock other)) {
            return false;
        }
        for (TransportType type : types) {
            if (other.carries(type)) {
                return true;
            }
        }
        return false;
    }
}
