package io.github.fishgames.vectrum.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Ein noch nicht (oder bereits) registriertes Objekt. {@link #get()} erzeugt das Objekt beim ersten Aufruf
 * und liefert danach immer dieselbe Instanz - egal ob der Zugriff vor oder waehrend der Registrierung passiert.
 */
public final class Registered<T> implements Supplier<T> {
    private final ResourceLocation id;
    private final Supplier<? extends T> factory;
    private T value;

    Registered(ResourceLocation id, Supplier<? extends T> factory) {
        this.id = id;
        this.factory = factory;
    }

    public ResourceLocation id() {
        return id;
    }

    @Override
    public synchronized T get() {
        if (value == null) {
            value = factory.get();
        }
        return value;
    }
}
