package io.github.fishgames.vectrum.world;

import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.upgrade.UpgradeEffects;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.core.wireless.LinkMode;
import io.github.fishgames.vectrum.core.wireless.WirelessTable;
import io.github.fishgames.vectrum.logistics.Target;
import io.github.fishgames.vectrum.transfer.Ports;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Server-wide wireless block registry across all dimensions, stored in {@code data/vectrum_wireless.dat} of the
 * overworld. Wraps {@link WirelessTable}, persists it and caches the receiver lists per frequency.
 */
public final class WirelessRegistry extends SavedData {
    private static final String DATA_NAME = "vectrum_wireless";
    private static final String TAG_BLOCKS = "blocks";
    private static final String TAG_DIM = "dim";
    private static final String TAG_POS = "pos";
    private static final String TAG_FREQUENCY = "frequency";
    private static final String TAG_MODES = "modes";
    /** Lifetime in ticks of an incomplete target list (receivers in unloaded chunks). */
    private static final long INCOMPLETE_LIFETIME = 20;

    private final WirelessTable table = new WirelessTable();

    /** Cache key of a receiver list. */
    private record CacheKey(int frequency, String type, String dimension, boolean crossDimension) {
    }

    /** Receiving sides of a frequency; {@code complete} is {@code false} when receivers sit in unloaded chunks. */
    public record Receivers(List<Target> targets, boolean complete) {
    }

    /** Cached receiver list with validity data. */
    private record Cached(long tableVersion, long epoch, long validUntil, Receivers receivers) {
    }

    private final Map<CacheKey, Cached> cache = new HashMap<>();

    private WirelessRegistry() {
    }

    public static WirelessRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WirelessRegistry::load, WirelessRegistry::new,
                DATA_NAME);
    }

    public static WirelessRegistry get(ServerLevel level) {
        return get(level.getServer());
    }

    public static BlockCoord coord(Level level, BlockPos pos) {
        return new BlockCoord(level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    // Blocks

    public boolean contains(Level level, BlockPos pos) {
        return table.contains(coord(level, pos));
    }

    public void register(Level level, BlockPos pos) {
        if (table.register(coord(level, pos))) {
            changed();
        }
    }

    public void unregister(Level level, BlockPos pos) {
        if (table.unregister(coord(level, pos))) {
            changed();
        }
    }

    public int frequency(Level level, BlockPos pos) {
        return table.frequency(coord(level, pos));
    }

    public void setFrequency(Level level, BlockPos pos, int frequency) {
        if (table.setFrequency(coord(level, pos), frequency)) {
            changed();
        }
    }

    public LinkMode mode(Level level, BlockPos pos, TransportType type) {
        return table.mode(coord(level, pos), type.id());
    }

    public void setMode(Level level, BlockPos pos, TransportType type, LinkMode mode) {
        if (table.setMode(coord(level, pos), type.id(), mode)) {
            changed();
        }
    }

    /** Number of blocks on the frequency. */
    public int memberCount(int frequency) {
        return table.members(frequency).size();
    }

    private void changed() {
        setDirty();
        LevelNetworks.invalidateCaches();
    }

    // Targets

    /**
     * All receiving sides of the frequency for this type, in any dimension: every side of a wireless block in
     * receive mode (RECEIVE or BOTH) that has a storage of this type, plus the targets of the cable network attached
     * to that block.
     * <ul>
     * <li>1. cache hit when table version, epoch and validity match</li>
     * <li>2. per receiver: skip unknown dimension; incomplete on unloaded chunk; queue stale entry when the block
     * is no longer a wireless block; skip when the link is not allowed by the upgrades</li>
     * <li>3. per side with a matching port: add a target with effective settings</li>
     * <li>4. add the targets of the attached network (coder partners included)</li>
     * <li>5. unregister stale entries; remove duplicates</li>
     * <li>6. cache the result (unbounded validity when complete, {@code INCOMPLETE_LIFETIME} ticks otherwise)</li>
     * </ul>
     */
    public Receivers receivers(MinecraftServer server, TransportType type, int frequency, Level senderLevel,
                                  Upgrades senderUpgrades) {
        long now = server.getTickCount();
        long epoch = LevelNetworks.epoch();
        String senderDimension = senderLevel.dimension().location().toString();
        CacheKey key = new CacheKey(frequency, type.id(), senderDimension,
                UpgradeEffects.dimensionUnlocked(senderUpgrades));
        Cached cached = cache.get(key);
        if (cached != null && cached.tableVersion() == table.version() && cached.epoch() == epoch
                && now < cached.validUntil()) {
            return cached.receivers();
        }

        boolean complete = true;
        List<Target> result = new ArrayList<>();
        List<BlockCoord> stale = new ArrayList<>();
        for (BlockCoord member : table.receivers(frequency, type.id())) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(member.dimension())));
            if (level == null) {
                continue;
            }
            BlockPos pos = new BlockPos(member.x(), member.y(), member.z());
            if (!level.hasChunkAt(pos)) {
                complete = false;
                continue;
            }
            if (!(level.getBlockState(pos).getBlock() instanceof WirelessBlock)) {
                stale.add(member);
                continue;
            }
            LevelNetworks networks = LevelNetworks.get(level);
            if (!UpgradeEffects.canLink(level.dimension().equals(senderLevel.dimension()), senderUpgrades,
                    networks.upgrades(pos))) {
                continue;
            }
            for (Direction side : Sides.ALL) {
                BlockPos neighbour = pos.relative(side);
                if (level.hasChunkAt(neighbour) && Ports.find(type, level, neighbour, side.getOpposite()) != null) {
                    result.add(new Target(level, pos, side, neighbour, networks.effectiveSettings(pos, side, true)));
                }
            }
            boolean[] attachedComplete = {true};
            result.addAll(networks.attachedTargets(level, type, pos, attachedComplete));
            if (!attachedComplete[0]) {
                complete = false;
            }
        }
        for (BlockCoord member : stale) {
            table.unregister(member);
        }
        if (!stale.isEmpty()) {
            changed();
        }
        Receivers found = new Receivers(List.copyOf(new LinkedHashSet<>(result)), complete);
        cache.put(key, new Cached(table.version(), LevelNetworks.epoch(),
                complete ? Long.MAX_VALUE : now + INCOMPLETE_LIFETIME, found));
        return found;
    }

    // Persistence

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag blocks = new ListTag();
        for (Map.Entry<BlockCoord, WirelessTable.Entry> entry : table.entries().entrySet()) {
            BlockCoord c = entry.getKey();
            CompoundTag item = new CompoundTag();
            item.putString(TAG_DIM, c.dimension());
            item.putLong(TAG_POS, new BlockPos(c.x(), c.y(), c.z()).asLong());
            item.putInt(TAG_FREQUENCY, entry.getValue().frequency());
            CompoundTag modes = new CompoundTag();
            for (Map.Entry<String, LinkMode> mode : entry.getValue().customModes().entrySet()) {
                modes.putString(mode.getKey(), mode.getValue().id());
            }
            item.put(TAG_MODES, modes);
            blocks.add(item);
        }
        tag.put(TAG_BLOCKS, blocks);
        return tag;
    }

    private static WirelessRegistry load(CompoundTag tag) {
        WirelessRegistry registry = new WirelessRegistry();
        ListTag blocks = tag.getList(TAG_BLOCKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag item = blocks.getCompound(i);
            BlockPos pos = BlockPos.of(item.getLong(TAG_POS));
            Map<String, LinkMode> modes = new HashMap<>();
            CompoundTag stored = item.getCompound(TAG_MODES);
            for (String typeId : stored.getAllKeys()) {
                LinkMode mode = LinkMode.byId(stored.getString(typeId));
                if (mode != null) {
                    modes.put(typeId, mode);
                }
            }
            registry.table.restore(new BlockCoord(item.getString(TAG_DIM), pos.getX(), pos.getY(), pos.getZ()),
                    item.getInt(TAG_FREQUENCY), modes);
        }
        return registry;
    }
}
