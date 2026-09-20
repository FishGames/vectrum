package io.github.fishgames.vectrum.transfer;

import io.github.fishgames.vectrum.core.transport.TransportType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Zentraler Zugang für den gemeinsamen Code. Der Loader trägt beim Start je Transporttyp seine Umsetzung ein. */
public final class Ports {
    private static final Map<String, PortFinder> FINDERS = new HashMap<>();

    private Ports() {
    }

    public static void setFinder(TransportType type, PortFinder finder) {
        FINDERS.put(type.id(), finder);
    }

    /**
     * Der Speicher dieses Typs an {@code pos}, angesprochen von der Seite {@code side} dieses Blocks, oder
     * {@code null}. Der Chunk an {@code pos} muss geladen sein.
     */
    public static Port find(TransportType type, Level level, BlockPos pos, Direction side) {
        PortFinder finder = FINDERS.get(type.id());
        return finder == null ? null : finder.find(level, pos, side);
    }
}
