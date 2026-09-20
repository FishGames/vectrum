package io.github.fishgames.vectrum.registry;

import io.github.fishgames.vectrum.Vectrum;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Loader-independent registration queue ({@link #register}, {@link #apply}, {@link #applyTo}). */
public final class Registration {
    private static final Map<ResourceKey<? extends Registry<?>>, List<Registered<?>>> QUEUE = new LinkedHashMap<>();

    private Registration() {
    }

    public static <R, T extends R> Registered<T> register(ResourceKey<? extends Registry<R>> registry,
                                                          String name,
                                                          Supplier<T> factory) {
        Registered<T> entry = new Registered<>(Vectrum.id(name), factory);
        QUEUE.computeIfAbsent(registry, key -> new ArrayList<>()).add(entry);
        return entry;
    }

    /** Passes all queued entries of a registry to {@code sink}. */
    @SuppressWarnings("unchecked")
    public static <R> void apply(ResourceKey<? extends Registry<R>> registry, BiConsumer<ResourceLocation, R> sink) {
        for (Registered<?> entry : QUEUE.getOrDefault(registry, List.of())) {
            sink.accept(entry.id(), (R) entry.get());
        }
    }

    /** Registers all queued entries in an open registry. */
    public static <R> void applyTo(ResourceKey<? extends Registry<R>> registryKey, Registry<R> registry) {
        apply(registryKey, (id, value) -> Registry.register(registry, id, value));
    }
}
