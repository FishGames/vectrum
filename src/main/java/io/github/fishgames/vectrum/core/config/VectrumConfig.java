package io.github.fishgames.vectrum.core.config;

import io.github.fishgames.vectrum.core.transport.TransportType;

/**
 * Immutable settings read from {@code config/vectrum.toml}.
 *
 * <p>The upgrade tables are indexed by the number of installed upgrades; a table with {@code n + 1} entries allows
 * at most {@code n} upgrades of that kind.
 */
public final class VectrumConfig {
    /** Hard limit for the number of installed upgrades of one kind. */
    public static final int MAX_UPGRADE_COUNT = 63;

    public static final VectrumConfig DEFAULTS = new VectrumConfig(
            4, 1000, 2000, 1000, 1000, 10_000,
            new long[]{1, 4, 16, 64, 256, 1024, 4096},
            new int[]{10, 5, 2, 1},
            new int[]{1, 2, 3, 4, 5},
            10, 20,
            40, new int[]{20, 40, 80, 100}, 10_000, 2_000, 100);

    private static volatile VectrumConfig current = DEFAULTS;

    private final long itemAmount;
    private final long fluidAmount;
    private final long energyAmount;
    private final long gasAmount;
    private final long sourceAmount;
    private final long heatAmount;
    private final long[] throughput;
    private final int[] speed;
    private final int[] itemTypes;
    private final int wirelessInterval;
    private final int redstoneInterval;
    private final int idleIntervalMax;
    private final int[] targetPause;
    private final long timeBudgetMicros;
    private final long runBudgetMicros;
    private final int refreshInterval;

    public VectrumConfig(long itemAmount, long fluidAmount, long energyAmount, long gasAmount,
                         long sourceAmount, long heatAmount, long[] throughput, int[] speed, int[] itemTypes,
                         int wirelessInterval, int redstoneInterval,
                         int idleIntervalMax, int[] targetPause, long timeBudgetMicros, long runBudgetMicros,
                         int refreshInterval) {
        this.itemAmount = itemAmount;
        this.fluidAmount = fluidAmount;
        this.energyAmount = energyAmount;
        this.gasAmount = gasAmount;
        this.sourceAmount = sourceAmount;
        this.heatAmount = heatAmount;
        this.throughput = throughput.clone();
        this.speed = speed.clone();
        this.itemTypes = itemTypes.clone();
        this.wirelessInterval = wirelessInterval;
        this.redstoneInterval = redstoneInterval;
        this.idleIntervalMax = idleIntervalMax;
        this.targetPause = targetPause.clone();
        this.timeBudgetMicros = timeBudgetMicros;
        this.runBudgetMicros = runBudgetMicros;
        this.refreshInterval = refreshInterval;
    }

    /** Settings in effect. */
    public static VectrumConfig get() {
        return current;
    }

    public static void set(VectrumConfig config) {
        current = config;
    }

    // Amounts

    /** Amount per transfer and source side without upgrades; unknown layers use the item amount. */
    public long baseAmount(String layerId) {
        if (TransportType.FLUID.id().equals(layerId)) {
            return fluidAmount;
        }
        if (TransportType.ENERGY.id().equals(layerId)) {
            return energyAmount;
        }
        if (TransportType.GAS.id().equals(layerId)) {
            return gasAmount;
        }
        if (TransportType.SOURCE.id().equals(layerId)) {
            return sourceAmount;
        }
        if (TransportType.HEAT.id().equals(layerId)) {
            return heatAmount;
        }
        return itemAmount;
    }

    // Upgrade tables

    /** Amount factor with {@code count} throughput upgrades. */
    public long throughputFactor(int count) {
        return throughput[Math.min(Math.max(count, 0), throughput.length - 1)];
    }

    /** Ticks between two transfers with {@code count} speed upgrades. */
    public int transferInterval(int count) {
        return speed[Math.min(Math.max(count, 0), speed.length - 1)];
    }

    /** Item types per transfer and target with {@code count} type upgrades. */
    public int itemTypesPerTransfer(int count) {
        return itemTypes[Math.min(Math.max(count, 0), itemTypes.length - 1)];
    }

    public int maxThroughputUpgrades() {
        return throughput.length - 1;
    }

    public int maxSpeedUpgrades() {
        return speed.length - 1;
    }

    public int maxTypeUpgrades() {
        return itemTypes.length - 1;
    }

    // Intervals

    public int wirelessInterval() {
        return wirelessInterval;
    }

    public int redstoneInterval() {
        return redstoneInterval;
    }

    // Performance

    /** Longest wait in ticks of a source that moves nothing; 0 keeps the regular interval. */
    public int idleIntervalMax() {
        return idleIntervalMax;
    }

    /** Wait in ticks of a target after its {@code failures}-th refusal in a row (1-based, last entry repeats). */
    public int targetPause(int failures) {
        return targetPause[Math.min(Math.max(failures, 1), targetPause.length) - 1];
    }

    /** Microseconds per server tick all transfers may use; 0 means unlimited. */
    public long timeBudgetMicros() {
        return timeBudgetMicros;
    }

    /** Microseconds one run of one source block may use; the rest waits for its next run; 0 means unlimited. */
    public long runBudgetMicros() {
        return runBudgetMicros;
    }

    /** Ticks between two state checks of a source; 0 checks on every tick. */
    public int refreshInterval() {
        return refreshInterval;
    }

    long[] throughputTable() {
        return throughput.clone();
    }

    int[] speedTable() {
        return speed.clone();
    }

    int[] itemTypesTable() {
        return itemTypes.clone();
    }

    int[] targetPauseTable() {
        return targetPause.clone();
    }

    long itemAmount() {
        return itemAmount;
    }

    long fluidAmount() {
        return fluidAmount;
    }

    long energyAmount() {
        return energyAmount;
    }

    long gasAmount() {
        return gasAmount;
    }

    long sourceAmount() {
        return sourceAmount;
    }

    long heatAmount() {
        return heatAmount;
    }
}
