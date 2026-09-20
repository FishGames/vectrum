package io.github.fishgames.vectrum.core.transport;

import java.util.Objects;

/**
 * Transport type; each type forms its own network layer.
 *
 * @param id       namespaced id, for example {@code "vectrum:item"}; also the layer name
 * @param behavior transfer behavior
 */
public record TransportType(String id, Behavior behavior) {
    public TransportType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(behavior, "behavior");
    }

    /** Transfer behavior. */
    public enum Behavior {
        /** Quantity moved from source to target (items, fluids, energy, gases). */
        QUANTITY,
        /** State value mirrored (redstone signal strength). */
        SIGNAL,
        /** Connection layer without transfer (digital network). */
        CONNECTION
    }

    public static final TransportType ITEM = new TransportType("vectrum:item", Behavior.QUANTITY);
    public static final TransportType FLUID = new TransportType("vectrum:fluid", Behavior.QUANTITY);
    public static final TransportType ENERGY = new TransportType("vectrum:energy", Behavior.QUANTITY);
    public static final TransportType REDSTONE = new TransportType("vectrum:redstone", Behavior.SIGNAL);
    /** Digital layer. */
    public static final TransportType DIGITAL = new TransportType("vectrum:digital", Behavior.CONNECTION);
    /** Gas type (Mekanism). */
    public static final TransportType GAS = new TransportType("vectrum:gas", Behavior.QUANTITY);
}
