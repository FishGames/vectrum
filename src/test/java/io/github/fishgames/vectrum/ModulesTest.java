package io.github.fishgames.vectrum;

import io.github.fishgames.vectrum.core.transport.TransportType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModulesTest {
    @AfterEach
    void switchEverythingOff() {
        Modules.setGas(false);
        Modules.setSource(false);
        Modules.setHeat(false);
    }

    @Test
    void withoutModulesOnlyTheBuiltInQuantityTypesExist() {
        assertEquals(List.of(TransportType.ITEM, TransportType.FLUID, TransportType.ENERGY), Modules.quantityTypes());
        assertEquals(List.of(), Modules.moduleTypes());
    }

    @Test
    void enabledModulesAppendTheirTypesInTheOrderGasSourceHeat() {
        Modules.setHeat(true);
        Modules.setGas(true);
        Modules.setSource(true);

        assertEquals(List.of(TransportType.ITEM, TransportType.FLUID, TransportType.ENERGY, TransportType.GAS,
                TransportType.SOURCE, TransportType.HEAT), Modules.quantityTypes());
        assertEquals(List.of(TransportType.GAS, TransportType.SOURCE, TransportType.HEAT), Modules.moduleTypes());
    }

    @Test
    void eachModuleSwitchesOnlyItsOwnType() {
        Modules.setSource(true);

        assertTrue(Modules.enabled(TransportType.SOURCE));
        assertFalse(Modules.enabled(TransportType.GAS));
        assertFalse(Modules.enabled(TransportType.HEAT));
        assertFalse(Modules.enabled(TransportType.ITEM));
        assertEquals(List.of(TransportType.ITEM, TransportType.FLUID, TransportType.ENERGY, TransportType.SOURCE),
                Modules.quantityTypes());
    }
}
