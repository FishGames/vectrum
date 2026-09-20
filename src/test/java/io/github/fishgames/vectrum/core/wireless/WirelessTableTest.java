package io.github.fishgames.vectrum.core.wireless;

import io.github.fishgames.vectrum.core.network.BlockCoord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WirelessTableTest {
    private static final String ITEM = "vectrum:item";
    private static final String FLUID = "vectrum:fluid";
    private static final BlockCoord A = new BlockCoord("minecraft:overworld", 0, 0, 0);
    private static final BlockCoord B = new BlockCoord("minecraft:overworld", 5, 0, 0);
    private static final BlockCoord C = new BlockCoord("minecraft:the_nether", 1, 0, 0);

    private final WirelessTable table = new WirelessTable();

    @Test
    void newBlocksStartOnFrequencyZeroAndOnlyReceive() {
        assertTrue(table.register(A));
        assertFalse(table.register(A));

        assertEquals(0, table.frequency(A));
        assertEquals(LinkMode.RECEIVE, table.mode(A, ITEM));
        assertEquals(List.of(A), table.receivers(0, ITEM));
    }

    @Test
    void membersOfOneFrequencyAreFoundAcrossDimensions() {
        for (BlockCoord pos : List.of(C, B, A)) {
            table.register(pos);
            table.setFrequency(pos, 7);
        }

        assertEquals(List.of(A, B, C), table.members(7)); // order: dimension, then position
        assertTrue(table.members(0).isEmpty());
    }

    @Test
    void changingTheFrequencyMovesTheBlock() {
        table.register(A);
        table.register(B);
        table.setFrequency(A, 3);

        assertEquals(List.of(B), table.members(0));
        assertEquals(List.of(A), table.members(3));
        assertFalse(table.setFrequency(A, 3));
    }

    @Test
    void modesArePerTypeAndDecideWhoReceives() {
        table.register(A);
        table.register(B);
        table.setMode(A, ITEM, LinkMode.SEND);
        table.setMode(B, FLUID, LinkMode.OFF);

        assertEquals(List.of(B), table.receivers(0, ITEM));
        assertEquals(List.of(A), table.receivers(0, FLUID));
        assertEquals(LinkMode.RECEIVE, table.mode(A, FLUID));
    }

    @Test
    void bothSendsAndReceives() {
        table.register(A);
        table.setMode(A, ITEM, LinkMode.BOTH);

        assertTrue(table.mode(A, ITEM).sends());
        assertTrue(table.mode(A, ITEM).receives());
        assertEquals(List.of(A), table.receivers(0, ITEM));
    }

    @Test
    void settingTheDefaultModeStoresNothing() {
        table.register(A);
        table.setMode(A, ITEM, LinkMode.SEND);
        assertEquals(1, table.entries().get(A).customModes().size());

        assertTrue(table.setMode(A, ITEM, LinkMode.DEFAULT));
        assertTrue(table.entries().get(A).customModes().isEmpty());
    }

    @Test
    void unregisterRemovesFromTheFrequencyIndex() {
        table.register(A);
        table.setFrequency(A, 9);

        assertTrue(table.unregister(A));
        assertFalse(table.unregister(A));
        assertTrue(table.members(9).isEmpty());
        assertEquals(0, table.size());
    }

    @Test
    void everyChangeBumpsTheVersionAndNoChangeDoesNot() {
        long start = table.version();
        table.register(A);
        long afterRegister = table.version();
        table.setFrequency(A, 0);
        table.setMode(A, ITEM, LinkMode.RECEIVE);

        assertTrue(afterRegister > start);
        assertEquals(afterRegister, table.version());
        table.setFrequency(A, 2);
        assertTrue(table.version() > afterRegister);
    }

    @Test
    void restoreReplacesAnExistingEntry() {
        table.register(A);
        table.restore(A, 4, Map.of(ITEM, LinkMode.SEND, FLUID, LinkMode.RECEIVE));

        assertEquals(4, table.frequency(A));
        assertEquals(LinkMode.SEND, table.mode(A, ITEM));
        assertEquals(1, table.entries().get(A).customModes().size()); // RECEIVE is the default mode
        assertEquals(List.of(A), table.members(4));
        assertTrue(table.members(0).isEmpty());
    }

    @Test
    void linkModeCyclesThroughAllFourStates() {
        LinkMode mode = LinkMode.RECEIVE;
        StringBuilder seen = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            seen.append(mode.id()).append(' ');
            mode = mode.next();
        }
        assertEquals("receive send both off ", seen.toString());
        assertEquals(LinkMode.RECEIVE, mode);
        assertEquals(LinkMode.BOTH, LinkMode.byId("both"));
    }
}
