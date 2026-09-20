package io.github.fishgames.vectrum.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** Lazily created registry entry; {@link #get()} returns the same instance on every call. */
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
