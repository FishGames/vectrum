package io.github.fishgames.vectrum.core.routing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceFilterTest {
    @Test
    void emptyFilterLetsEverythingPass() {
        assertTrue(ResourceFilter.NONE.matches("minecraft:stone"));
        assertTrue(ResourceFilter.NONE.withBlacklist(true).matches("minecraft:stone"));
        assertTrue(ResourceFilter.NONE.isEmpty());
    }

    @Test
    void whitelistOnlyPassesListedIds() {
        ResourceFilter filter = ResourceFilter.NONE.with("minecraft:stone");

        assertTrue(filter.matches("minecraft:stone"));
        assertFalse(filter.matches("minecraft:dirt"));
    }

    @Test
    void blacklistPassesEverythingButListedIds() {
        ResourceFilter filter = ResourceFilter.NONE.withBlacklist(true).with("minecraft:stone");

        assertFalse(filter.matches("minecraft:stone"));
        assertTrue(filter.matches("minecraft:dirt"));
    }

    @Test
    void removingTheLastEntryTurnsTheFilterOff() {
        ResourceFilter filter = ResourceFilter.NONE.with("a:b").without("a:b");

        assertTrue(filter.isEmpty());
        assertTrue(filter.matches("x:y"));
    }

    @Test
    void clearedKeepsTheKindButDropsTheEntries() {
        ResourceFilter filter = ResourceFilter.NONE.withBlacklist(true).with("a:b").cleared();

        assertTrue(filter.blacklist());
        assertTrue(filter.isEmpty());
    }

    @Test
    void entriesAreSortedAndImmutable() {
        ResourceFilter filter = ResourceFilter.NONE.with("b:b").with("a:a");

        assertEquals("a:a", filter.ids().iterator().next());
        try {
            filter.ids().add("c:c");
            throw new AssertionError("Set muss unveränderlich sein");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    void restrictedFilterOnlyKeepsEntriesOfTheRelevantType() {
        ResourceFilter filter = ResourceFilter.NONE.with("minecraft:dirt").with("minecraft:water");

        ResourceFilter itemsOnly = filter.restrictedTo(id -> id.equals("minecraft:dirt"));
        assertTrue(itemsOnly.matches("minecraft:dirt"));
        assertFalse(itemsOnly.matches("minecraft:stone"));
    }

    @Test
    void filterWithNoRelevantEntriesLetsEverythingPass() {
        ResourceFilter whitelist = ResourceFilter.NONE.with("minecraft:dirt");
        ResourceFilter fluidsOnly = whitelist.restrictedTo(id -> id.equals("minecraft:water"));

        assertTrue(fluidsOnly.isEmpty());
        assertTrue(fluidsOnly.matches("minecraft:lava"));
        assertTrue(ResourceFilter.NONE.restrictedTo(id -> false).isEmpty());
    }
}
