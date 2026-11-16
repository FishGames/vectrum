package io.github.fishgames.vectrum.core.config;

import io.github.fishgames.vectrum.core.upgrade.UpgradeEffects;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileTest {
    @AfterEach
    void restoreDefaults() {
        VectrumConfig.set(VectrumConfig.DEFAULTS);
    }

    @Test
    void renderedDefaultsParseBackToTheDefaults() {
        ConfigFile.Result result = ConfigFile.parse(List.of(ConfigFile.render(VectrumConfig.DEFAULTS).split("\n")));

        assertEquals(List.of(), result.warnings());
        assertEquals(ConfigFile.render(VectrumConfig.DEFAULTS), ConfigFile.render(result.config()));
    }

    @Test
    void emptyFileKeepsEveryDefault() {
        ConfigFile.Result result = ConfigFile.parse(List.of());

        assertEquals(ConfigFile.render(VectrumConfig.DEFAULTS), ConfigFile.render(result.config()));
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void sourceAndHeatAmountsHaveDefaultsAndAreRead() {
        assertEquals(1000, VectrumConfig.DEFAULTS.baseAmount("vectrum:source"));
        assertEquals(10_000, VectrumConfig.DEFAULTS.baseAmount("vectrum:heat"));

        ConfigFile.Result result = ConfigFile.parse(List.of("[amount]", "source = 250", "heat = 77"));

        assertEquals(List.of(), result.warnings());
        assertEquals(250, result.config().baseAmount("vectrum:source"));
        assertEquals(77, result.config().baseAmount("vectrum:heat"));
        assertEquals(1000, result.config().baseAmount("vectrum:gas"));
    }

    @Test
    void valuesAndTablesAreRead() {
        ConfigFile.Result result = ConfigFile.parse(List.of(
                "[amount]", "item = 8  # per transfer", "",
                "[upgrades]", "throughput = [1, 2, 3]", "speed = [20, 10]", "item_types = [2]",
                "[performance]", "idle_interval_max = 0", "target_pause = [5]", "time_budget_micros = 0",
                "run_budget_micros = 500"));
        VectrumConfig config = result.config();

        assertEquals(List.of(), result.warnings());
        assertEquals(8, config.baseAmount("vectrum:item"));
        assertEquals(1000, config.baseAmount("vectrum:fluid"));
        assertEquals(2, config.maxThroughputUpgrades());
        assertEquals(3, config.throughputFactor(2));
        assertEquals(3, config.throughputFactor(40));
        assertEquals(1, config.maxSpeedUpgrades());
        assertEquals(10, config.transferInterval(1));
        assertEquals(0, config.maxTypeUpgrades());
        assertEquals(2, config.itemTypesPerTransfer(5));
        assertEquals(0, config.idleIntervalMax());
        assertEquals(5, config.targetPause(1));
        assertEquals(5, config.targetPause(9));
        assertEquals(0, config.timeBudgetMicros());
        assertEquals(500, config.runBudgetMicros());
    }

    @Test
    void invalidEntriesFallBackWithAWarning() {
        ConfigFile.Result result = ConfigFile.parse(List.of(
                "[amount]", "item = 0", "fluid = abc",
                "[upgrades]", "throughput = [1, x]", "speed = []",
                "[nothing]", "key = 1", "garbage"));

        assertEquals(4, result.config().baseAmount("vectrum:item"));
        assertEquals(1000, result.config().baseAmount("vectrum:fluid"));
        assertEquals(6, result.config().maxThroughputUpgrades());
        assertEquals(3, result.config().maxSpeedUpgrades());
        assertEquals(6, result.warnings().size(), result.warnings().toString());
    }

    @Test
    void tablesDriveTheUpgradeEffects() {
        VectrumConfig.set(ConfigFile.parse(List.of(
                "[amount]", "item = 2",
                "[upgrades]", "throughput = [1, 10]", "speed = [8, 3]", "item_types = [1, 7]")).config());

        Upgrades full = Upgrades.EMPTY.with(UpgradeType.THROUGHPUT, 5).with(UpgradeType.SPEED, 5)
                .with(UpgradeType.TYPES, 5);

        assertEquals(1, UpgradeType.THROUGHPUT.maxCount());
        assertEquals(1, full.count(UpgradeType.THROUGHPUT));
        assertEquals(20, UpgradeEffects.throughput(VectrumConfig.get().baseAmount("vectrum:item"), full));
        assertEquals(3, UpgradeEffects.interval(full));
        assertEquals(7, UpgradeEffects.maxTypes(full));
        assertEquals(8, UpgradeEffects.interval(Upgrades.EMPTY));
    }

    @Test
    void loadWritesTheDefaultFileOnce(@TempDir Path dir) throws IOException {
        ConfigFile.Result first = ConfigFile.load(dir);
        Path file = dir.resolve(ConfigFile.FILE_NAME);

        assertTrue(Files.exists(file));
        assertEquals(List.of(), first.warnings());

        Files.writeString(file, "[amount]\nitem = 16\n");
        ConfigFile.Result second = ConfigFile.load(dir);

        assertEquals(16, second.config().baseAmount("vectrum:item"));
        assertEquals("[amount]\nitem = 16\n", Files.readString(file));
    }
}
