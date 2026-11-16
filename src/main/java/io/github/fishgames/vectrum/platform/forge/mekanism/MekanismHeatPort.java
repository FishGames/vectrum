package io.github.fishgames.vectrum.platform.forge.mekanism;

import io.github.fishgames.vectrum.transfer.Port;
import mekanism.api.heat.IHeatHandler;

import java.util.function.Predicate;

/**
 * {@link Port} for Mekanism heat. Unit: heat units; the filter is ignored.
 * <ul>
 *   <li>Heat only flows from the hotter to the colder handler.</li>
 *   <li>One move carries at most the heat that equalises the two temperatures, and at most {@code max}.</li>
 * </ul>
 */
final class MekanismHeatPort implements Port {
    /** Temperature difference in kelvin below which nothing moves. */
    private static final double MIN_DIFFERENCE = 0.01;

    private final IHeatHandler handler;

    MekanismHeatPort(IHeatHandler handler) {
        this.handler = handler;
    }

    @Override
    public long moveTo(Port target, long max, Predicate<String> filter) {
        if (max <= 0 || !(target instanceof MekanismHeatPort other) || other.handler == handler) {
            return 0;
        }
        // 1. probe: temperatures and capacities
        double difference = handler.getTotalTemperature() - other.handler.getTotalTemperature();
        double capacity = handler.getTotalHeatCapacity();
        double targetCapacity = other.handler.getTotalHeatCapacity();
        if (difference < MIN_DIFFERENCE || capacity <= 0 || targetCapacity <= 0) {
            return 0;
        }
        // 2. heat that equalises the two temperatures, limited by max
        double equalising = difference * capacity * targetCapacity / (capacity + targetCapacity);
        double heat = Math.min(equalising, (double) max);
        if (!(heat > 0)) {
            return 0;
        }
        // 3. transfer
        handler.handleHeat(-heat);
        other.handler.handleHeat(heat);
        return Math.max(1, (long) Math.ceil(heat));
    }

    @Override
    public boolean canGive(Predicate<String> filter) {
        return handler.getTotalHeatCapacity() > 0;
    }
}
