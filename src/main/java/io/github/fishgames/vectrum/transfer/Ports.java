package io.github.fishgames.vectrum.transfer;

import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Port finder registry: one {@link PortFinder} per transport type. */
public final class Ports {
    private static final Map<String, PortFinder> FINDERS = new HashMap<>();

    private Ports() {
    }

    public static void setFinder(TransportType type, PortFinder finder) {
        FINDERS.put(type.id(), finder);
    }

    /**
     * Storage of this type at {@code pos}, accessed from {@code side}; {@code null} when none. The chunk at
     * {@code pos} must be loaded.
     */
    public static Port find(TransportType type, Level level, BlockPos pos, Direction side) {
        PortFinder finder = FINDERS.get(type.id());
        return finder == null ? null : finder.find(level, pos, side);
    }
}
