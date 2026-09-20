package io.github.fishgames.vectrum.block.entity;

import io.github.fishgames.vectrum.block.EndpointMode;
import io.github.fishgames.vectrum.logistics.ItemTransport;
import io.github.fishgames.vectrum.registry.ModBlockEntities;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Daten und Takt eines Endpunkts. Hier steht nur, was der BlockState nicht tragen kann: die gewählte Rolle je Seite
 * und der Zeitgeber für die Übergaben. Was an einer Seite tatsächlich passiert, steht im BlockState.
 */
public class EndpointBlockEntity extends BlockEntity {
    /** Ticks zwischen zwei Übergaben. */
    public static final int BASE_INTERVAL = 10;
    /** Höchstmenge Items pro Übergabe und Quellseite. Das spätere Durchsatz-Upgrade erhöht diesen Wert. */
    public static final int BASE_THROUGHPUT = 16;

    private static final String TAG_MODES = "modes";

    private final EndpointMode[] modes = new EndpointMode[6];
    private int cooldown;

    public EndpointBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENDPOINT.get(), pos, state);
        Arrays.fill(modes, EndpointMode.DEFAULT);
        // Nicht alle Endpunkte im selben Tick arbeiten lassen.
        cooldown = 1 + Math.floorMod(pos.hashCode(), BASE_INTERVAL);
    }

    public EndpointMode mode(Direction side) {
        return modes[side.get3DDataValue()];
    }

    /** Schaltet die Rolle der Seite eine Stufe weiter und liefert die neue Rolle. */
    public EndpointMode cycleMode(Direction side) {
        int index = side.get3DDataValue();
        modes[index] = modes[index].next();
        setChanged();
        LevelNetworks.invalidateCaches();
        return modes[index];
    }

    public int interval() {
        return BASE_INTERVAL;
    }

    public int throughput() {
        return BASE_THROUGHPUT;
    }

    // ------------------------------------------------------------------ Takt

    public static void serverTick(Level level, BlockPos pos, BlockState state, EndpointBlockEntity endpoint) {
        if (level instanceof ServerLevel server) {
            endpoint.tick(server);
        }
    }

    private void tick(ServerLevel level) {
        if (--cooldown > 0) {
            return;
        }
        cooldown = interval();
        ItemTransport.run(level, this);
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        byte[] data = new byte[modes.length];
        for (int i = 0; i < modes.length; i++) {
            data[i] = (byte) modes[i].ordinal();
        }
        tag.putByteArray(TAG_MODES, data);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        byte[] data = tag.getByteArray(TAG_MODES);
        for (int i = 0; i < modes.length; i++) {
            modes[i] = i < data.length ? EndpointMode.byOrdinal(data[i]) : EndpointMode.DEFAULT;
        }
    }

    /** Beim Entladen (und Abbauen) fehlt dieser Endpunkt im Zwischenspeicher der Ziele. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            LevelNetworks.invalidateCaches();
        }
    }

    /** Beim Laden kommt dieser Endpunkt in die Ziellisten. */
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide) {
            LevelNetworks.invalidateCaches();
        }
    }
}
