package io.github.fishgames.vectrum.core.transport;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.network.NetworkRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransportTypeTest {
    @Test
    void onlyRedstoneMirrorsAValueAllOthersMoveQuantities() {
        assertEquals(TransportType.Behavior.SIGNAL, TransportType.REDSTONE.behavior());
        for (TransportType type : new TransportType[]{TransportType.ITEM, TransportType.FLUID,
                TransportType.ENERGY, TransportType.GAS}) {
            assertEquals(TransportType.Behavior.QUANTITY, type.behavior(), type.id());
        }
    }

    @Test
    void redstoneNetworksAreSeparateFromItemNetworksAtTheSamePosition() {
        NetworkRegistry registry = new NetworkRegistry();
        BlockCoord pos = new BlockCoord("minecraft:overworld", 0, 0, 0);
        registry.graph(TransportType.REDSTONE.id()).add(pos, io.github.fishgames.vectrum.core.network.NodeKind.ENDPOINT, 0);

        assertNotNull(registry.networkAt(TransportType.REDSTONE.id(), pos));
        assertNull(registry.networkAt(TransportType.ITEM.id(), pos));
    }
}
