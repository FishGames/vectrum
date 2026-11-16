package io.github.fishgames.vectrum.core.upgrade;

import io.github.fishgames.vectrum.core.config.VectrumConfig;
import io.github.fishgames.vectrum.core.util.SaturatedMath;

/** Effects of upgrades: amount per transfer, transfer interval, item type count, feature unlocks. */
public final class UpgradeEffects {
    private UpgradeEffects() {
    }

    /** Amount per transfer: {@code base} times the configured factor, saturating at {@link Long#MAX_VALUE}. */
    public static long throughput(long base, Upgrades upgrades) {
        return SaturatedMath.multiplySaturated(base,
                VectrumConfig.get().throughputFactor(upgrades.count(UpgradeType.THROUGHPUT)));
    }

    /** Ticks between transfers from the configured speed table. */
    public static int interval(Upgrades upgrades) {
        return VectrumConfig.get().transferInterval(upgrades.count(UpgradeType.SPEED));
    }

    /** Item types allowed per transfer to one target. */
    public static int maxTypes(Upgrades upgrades) {
        return VectrumConfig.get().itemTypesPerTransfer(upgrades.count(UpgradeType.TYPES));
    }

    public static boolean filterUnlocked(Upgrades upgrades) {
        return upgrades.has(UpgradeType.FILTER);
    }

    public static boolean priorityUnlocked(Upgrades upgrades) {
        return upgrades.has(UpgradeType.PRIORITY);
    }

    public static boolean dimensionUnlocked(Upgrades upgrades) {
        return upgrades.has(UpgradeType.DIMENSION);
    }

    /** Whether two wireless ports can link: same dimension, or both have the dimension upgrade. */
    public static boolean canLink(boolean sameDimension, Upgrades sender, Upgrades receiver) {
        return sameDimension || dimensionUnlocked(sender) && dimensionUnlocked(receiver);
    }
}
