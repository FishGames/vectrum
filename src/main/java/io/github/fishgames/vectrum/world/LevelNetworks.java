package io.github.fishgames.vectrum.world;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.block.CoderBlock;
import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.config.VectrumConfig;
import io.github.fishgames.vectrum.core.digital.CoderLinks;
import io.github.fishgames.vectrum.block.EndpointMode;
import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.core.network.NetworkGraph;
import io.github.fishgames.vectrum.core.network.NetworkRegistry;
import io.github.fishgames.vectrum.core.network.NodeInfo;
import io.github.fishgames.vectrum.core.network.NodeKind;
import io.github.fishgames.vectrum.core.routing.DistributionMode;
import io.github.fishgames.vectrum.core.routing.PortKey;
import io.github.fishgames.vectrum.core.routing.PortSettings;
import io.github.fishgames.vectrum.core.routing.RedstoneMode;
import io.github.fishgames.vectrum.core.routing.PortSettingsTable;
import io.github.fishgames.vectrum.core.routing.ResourceFilter;
import io.github.fishgames.vectrum.core.routing.RoundRobinPointers;
import io.github.fishgames.vectrum.core.routing.Router;
import io.github.fishgames.vectrum.core.throughput.ThroughputLimits;
import io.github.fishgames.vectrum.core.upgrade.UpgradeEffects;
import io.github.fishgames.vectrum.core.upgrade.UpgradeTable;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.logistics.Filters;
import io.github.fishgames.vectrum.logistics.Target;
import io.github.fishgames.vectrum.logistics.TransportDefaults;
import io.github.fishgames.vectrum.transfer.Ports;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-dimension network state ({@code data/vectrum_networks.dat}): node graphs per layer, endpoint modes, coder
 * frequencies, signal outputs, throughput limits, port settings, round-robin pointers, upgrades, target caches and
 * back-off state. Bridges the core ({@link NetworkGraph}) and Minecraft.
 * <ul>
 * <li>Caches are kept per layer and cleared when that layer changes; changes of the digital layer clear all layers.</li>
 * <li>Receiver lists of wireless ports span dimensions; they are checked against {@link #stamp()}.</li>
 * </ul>
 */
public final class LevelNetworks extends SavedData {
    private static final String DATA_NAME = "vectrum_networks";
    private static final String TAG_LAYERS = "layers";
    /** Present once the save was written with the source as the default role (see {@link #keepTargetDefault}). */
    private static final String TAG_ROLE_DEFAULT = "roleDefault";
    /** Present once the stored throughput limits are manual ones only (see {@link #dropLegacyLimits}). */
    private static final String TAG_LIMITS_MANUAL = "limitsManual";
    private static final String TAG_ID = "id";
    private static final String TAG_POS = "pos";
    private static final String TAG_DATA = "data";
    private static final String TAG_PORTS = "ports";
    private static final String TAG_MODES = "modes";
    private static final String TAG_LIMITS = "limits";
    private static final String TAG_VALUES = "values";
    private static final String TAG_SETTINGS = "settings";
    private static final String TAG_POINTERS = "pointers";
    private static final String TAG_SIDE = "side";
    private static final String TAG_PRIORITY = "priority";
    private static final String TAG_MODE = "mode";
    private static final String TAG_REDSTONE = "redstone";
    private static final String TAG_NAME = "name";
    private static final String TAG_BLACKLIST = "blacklist";
    private static final String TAG_IDS = "ids";
    private static final String TAG_VALUE = "value";
    private static final String TAG_UPGRADES = "upgrades";
    private static final String TAG_COUNTS = "counts";
    private static final String TAG_SLOT = "slot";
    private static final String TAG_FREQUENCIES = "frequencies";
    private static final String TAG_SEVERED = "severed";
    private static final byte UNSET_MODE = -1;
    private static final int ENDPOINT_FLAG = 0x40;
    private static final int MASK_BITS = 0x3F;
    /** Ticks a source may wait without a transfer before its interval grows. */
    private static final int IDLE_GRACE_TICKS = 30;
    /** Lifetime in ticks of an incomplete target list. */
    private static final long INCOMPLETE_LIFETIME = 20;
    private static final int SIDES = 6;

    private static final Map<ResourceKey<Level>, String> DIMENSION_NAMES = new ConcurrentHashMap<>();
    /** Dimension name for the factory methods of {@link #get}. */
    private static String requestedDimension;
    /** Counter of changes that can alter wireless receiver lists; shared by all dimensions. */
    private static long stamp;

    private String dimension;
    private final NetworkRegistry registry = new NetworkRegistry();
    /** Chosen roles of the port sides, per block that differs from the default. Key: BlockPos.asLong(). */
    private final Map<Long, EndpointMode[]> modes = new HashMap<>();

    /** Coder frequencies (non-zero only). Key: BlockPos.asLong(). */
    private final Map<Long, Integer> frequencies = new HashMap<>();

    /** Manually severed link sides, as a bit mask (non-zero only). Key: BlockPos.asLong(). */
    private final Map<Long, Integer> severed = new HashMap<>();

    /** Current redstone outputs (non-zero only, not persisted). Key: BlockPos.asLong(). */
    private final Map<Long, Integer> signalOutputs = new HashMap<>();

    /** Manually set throughput limits per layer (key: layer id). */
    private final Map<String, ThroughputLimits> limits = new HashMap<>();

    /** Port side settings (priority, distribution mode, filter); non-default only. */
    private final PortSettingsTable settings = new PortSettingsTable();
    /** Upgrades per block (blocks with at least one upgrade). */
    private final UpgradeTable upgrades = new UpgradeTable();
    /** Round-robin pointer per source side. */
    private final RoundRobinPointers pointers = new RoundRobinPointers();

    /** Cached target list with expiry tick. */
    private static final class CachedTargets {
        final List<Target> targets;
        final long validUntil;
        /** {@link #stamp} the list was built at; relevant when it contains wireless receivers. */
        final long builtAt;
        final boolean wireless;
        private List<List<Target>> groups;

        CachedTargets(List<Target> targets, long validUntil, long builtAt, boolean wireless) {
            this.targets = targets;
            this.validUntil = validUntil;
            this.builtAt = builtAt;
            this.wireless = wireless;
        }

        /** Targets grouped by priority, highest first. */
        List<List<Target>> groups() {
            List<List<Target>> result = groups;
            if (result == null) {
                result = Router.groupByPriority(targets, target -> target.settings().priority());
                groups = result;
            }
            return result;
        }

        boolean valid(long now) {
            return now < validUntil && (!wireless || builtAt == stamp);
        }
    }

    /** Back-off state of one target: refusals in a row and wait-until tick. */
    private static final class Sleep {
        int failures;
        long until;
    }

    /** Last transfer, last delivery and rate window of a port side. */
    private static final class Flow {
        long tick = -1;
        long budget;
        long moved;
        long windowStart;
        long windowMoved;
        double rate;
        long received = -1;
    }

    /** Caches and run-time state of one layer; cleared when the layer changes. */
    private static final class LayerState {
        /** Target lists by network id: own, with coder partners, and with wireless receivers. */
        final Long2ObjectOpenHashMap<CachedTargets> own = new Long2ObjectOpenHashMap<>();
        final Long2ObjectOpenHashMap<CachedTargets> coupled = new Long2ObjectOpenHashMap<>();
        final Long2ObjectOpenHashMap<CachedTargets> full = new Long2ObjectOpenHashMap<>();
        /** Back-off per source block and side, by target. */
        final Long2ObjectOpenHashMap<Map<Target, Sleep>[]> sleep = new Long2ObjectOpenHashMap<>();
        /** Flow records per block and side; kept when the layer changes. */
        final Long2ObjectOpenHashMap<Flow[]> flows = new Long2ObjectOpenHashMap<>();

        void clear() {
            own.clear();
            coupled.clear();
            full.clear();
            sleep.clear();
        }
    }

    /** Values derived from the stored data of a block, valid until the block changes. */
    private static final class SourceInfo {
        /** Upgrades per slot: the six sides, then the block itself ({@link UpgradeTable#BLOCK}). */
        final Upgrades[] upgrades = new Upgrades[UpgradeTable.SLOTS];
        /** Ticks between two transfers, per side. */
        final int[] interval = new int[SIDES];
        /** Item types per transfer, per side. */
        final int[] maxTypes = new int[SIDES];
        /** Game time from which a side may transfer again; not saved. */
        final long[] due = new long[SIDES];
        final PortSettings[] effective = new PortSettings[SIDES];
        final PortSettings[] free = new PortSettings[SIDES];
        /** Filters reduced to one transport type, by type id; sides 0 to 5 effective, 6 to 11 free. */
        Map<String, ResourceFilter[]> typed;

        SourceInfo(UpgradeTable table, BlockCoord coord) {
            for (int slot = 0; slot < UpgradeTable.SLOTS; slot++) {
                upgrades[slot] = table.get(coord, slot);
            }
            for (int side = 0; side < SIDES; side++) {
                interval[side] = UpgradeEffects.interval(upgrades[side]);
                maxTypes[side] = UpgradeEffects.maxTypes(upgrades[side]);
            }
        }
    }

    private final Map<String, LayerState> layerStates = new HashMap<>();
    private final Long2ObjectOpenHashMap<SourceInfo> sources = new Long2ObjectOpenHashMap<>();
    /** Ticks a source block has waited since its last transfer. */
    private final Long2IntOpenHashMap idleTicks = new Long2IntOpenHashMap();
    /** Port sides with a storage as last seen by the block, per block that watches them. */
    private final Long2IntOpenHashMap portMasks = new Long2IntOpenHashMap();
    /** Tick of the last state check, per source block. */
    private final Long2LongOpenHashMap refreshed = new Long2LongOpenHashMap();

    private LevelNetworks() {
        refreshed.defaultReturnValue(Long.MIN_VALUE / 2);
        portMasks.defaultReturnValue(-1);
    }

    public static LevelNetworks get(ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        String name = DIMENSION_NAMES.get(key);
        if (name == null) {
            name = key.location().toString();
            DIMENSION_NAMES.put(key, name);
        }
        requestedDimension = name;
        return level.getDataStorage().computeIfAbsent(LevelNetworks::fromTag, LevelNetworks::create, DATA_NAME);
    }

    private static LevelNetworks create() {
        LevelNetworks networks = new LevelNetworks();
        networks.dimension = requestedDimension;
        return networks;
    }

    private static LevelNetworks fromTag(CompoundTag tag) {
        return load(requestedDimension, tag);
    }

    /** Counter that changes whenever a wireless receiver list may have become outdated. */
    public static long stamp() {
        return stamp;
    }

    /** Marks all wireless receiver lists as outdated. */
    public static void touchWireless() {
        stamp++;
    }

    private LayerState layer(String id) {
        return layerStates.computeIfAbsent(id, key -> new LayerState());
    }

    // Nodes

    public BlockCoord coord(BlockPos pos) {
        return new BlockCoord(dimension, pos.getX(), pos.getY(), pos.getZ());
    }

    /** Adds the node or updates its kind and open sides; always clears the caches of the layer. */
    public void put(TransportType type, BlockPos pos, NodeKind kind, int sideMask) {
        NetworkGraph graph = registry.graph(type.id());
        BlockCoord coord = coord(pos);
        if (graph.contains(coord)) {
            graph.setSides(coord, sideMask);
            graph.setKind(coord, kind);
        } else {
            graph.add(coord, kind, sideMask);
        }
        changedLayer(type.id());
    }

    /** Whether the node is already registered with this kind and side mask. */
    public boolean matches(TransportType type, BlockPos pos, NodeKind kind, int sideMask) {
        NodeInfo info = registry.graph(type.id()).info(coord(pos));
        return info != null && info.kind() == kind && info.sideMask() == (sideMask & MASK_BITS);
    }

    /** Enabled link sides of the registered node, or 0 when unregistered. */
    public int sideMask(TransportType type, BlockPos pos) {
        NodeInfo info = registry.graph(type.id()).info(coord(pos));
        return info == null ? 0 : info.sideMask();
    }

    public boolean isRegistered(TransportType type, BlockPos pos) {
        return registry.graph(type.id()).contains(coord(pos));
    }

    public void remove(TransportType type, BlockPos pos) {
        if (registry.graph(type.id()).remove(coord(pos))) {
            changedLayer(type.id());
        }
    }

    // Port side roles

    /** Chosen role of a block side, or {@code null} when none is chosen. */
    public EndpointMode storedMode(BlockPos pos, Direction side) {
        EndpointMode[] stored = modes.get(pos.asLong());
        return stored == null ? null : stored[side.get3DDataValue()];
    }

    public void setMode(BlockPos pos, Direction side, EndpointMode mode) {
        long key = pos.asLong();
        EndpointMode[] stored = modes.get(key);
        if (stored == null) {
            stored = new EndpointMode[Sides.ALL.length];
            modes.put(key, stored);
        }
        stored[side.get3DDataValue()] = mode;
        if (isUnset(stored)) {
            modes.remove(key);
        }
        changedAt(pos);
    }

    /** Removes the chosen roles of a block. */
    public void clearModes(BlockPos pos) {
        if (modes.remove(pos.asLong()) != null) {
            changedAt(pos);
        }
    }

    // Coder frequencies

    /** Frequency of this coder (default 0). */
    public int frequency(BlockPos pos) {
        return frequencies.getOrDefault(pos.asLong(), 0);
    }

    public void setFrequency(BlockPos pos, int frequency) {
        int before = frequency(pos);
        if (frequency <= 0) {
            frequencies.remove(pos.asLong());
        } else {
            frequencies.put(pos.asLong(), frequency);
        }
        if (before != Math.max(0, frequency)) {
            changedAll();
        }
    }

    public void clearFrequency(BlockPos pos) {
        if (frequencies.remove(pos.asLong()) != null) {
            changedAll();
        }
    }

    // Manually severed link sides

    /** Whether this side was manually severed from its neighbour with the wrench. */
    public boolean isSevered(BlockPos pos, Direction side) {
        int mask = severed.getOrDefault(pos.asLong(), 0);
        return (mask & (1 << side.get3DDataValue())) != 0;
    }

    public void setSevered(BlockPos pos, Direction side, boolean value) {
        long key = pos.asLong();
        int mask = severed.getOrDefault(key, 0);
        int bit = 1 << side.get3DDataValue();
        int updated = value ? (mask | bit) : (mask & ~bit);
        if (updated == mask) {
            return;
        }
        if (updated == 0) {
            severed.remove(key);
        } else {
            severed.put(key, updated);
        }
        changedAt(pos);
    }

    /** Removes all severed sides of a block. */
    public void clearSevered(BlockPos pos) {
        if (severed.remove(pos.asLong()) != null) {
            changedAt(pos);
        }
    }

    // Redstone outputs

    /** Current output signal strength of this block (0 to 15). */
    public int signalOutput(BlockPos pos) {
        return signalOutputs.getOrDefault(pos.asLong(), 0);
    }

    /** @return {@code true} when the value changed */
    public boolean setSignalOutput(BlockPos pos, int strength) {
        int before = signalOutput(pos);
        if (strength <= 0) {
            signalOutputs.remove(pos.asLong());
        } else {
            signalOutputs.put(pos.asLong(), strength);
        }
        return before != Math.max(0, strength);
    }

    // Port side settings

    private PortKey key(BlockPos pos, Direction side) {
        return new PortKey(coord(pos), io.github.fishgames.vectrum.core.network.Direction.VALUES[side.get3DDataValue()]);
    }

    /** Priority, distribution mode and filter of this side. */
    public PortSettings settings(BlockPos pos, Direction side) {
        return settings.get(key(pos, side));
    }

    public void setSettings(BlockPos pos, Direction side, PortSettings value) {
        if (settings.set(key(pos, side), value)) {
            changedAt(pos);
        }
    }

    /** Round-robin pointer of this source side. */
    public int pointer(BlockPos pos, Direction side) {
        return pointers.get(key(pos, side));
    }

    /** Stores the pointer; it is written with the next save caused by any other change. */
    public void setPointer(BlockPos pos, Direction side, int value) {
        pointers.set(key(pos, side), value);
    }

    /** Removes settings, round-robin pointers and run-time records of a block. */
    public void clearSettings(BlockPos pos) {
        forget(pos);
        BlockCoord coord = coord(pos);
        boolean removedSettings = settings.clear(coord);
        boolean removedPointers = pointers.clear(coord);
        if (removedSettings) {
            changedAt(pos);
        } else if (removedPointers) {
            setDirty();
        }
    }

    // Upgrades

    /** Upgrades of one port side of a cable. */
    public Upgrades upgrades(BlockPos pos, Direction side) {
        return source(pos).upgrades[side.get3DDataValue()];
    }

    /** Upgrades of a device that has no sides of its own (wireless port). */
    public Upgrades blockUpgrades(BlockPos pos) {
        return source(pos).upgrades[UpgradeTable.BLOCK];
    }

    /** Sets the upgrades of a port side and clears what depends on them. */
    public void setUpgrades(BlockPos pos, Direction side, Upgrades value) {
        if (upgrades.set(coord(pos), side.get3DDataValue(), value)) {
            changedAt(pos);
        }
    }

    /** Sets the upgrades of a device that has no sides of its own. */
    public void setBlockUpgrades(BlockPos pos, Upgrades value) {
        if (upgrades.set(coord(pos), UpgradeTable.BLOCK, value)) {
            changedAt(pos);
        }
    }

    /**
     * Removes all upgrades of a block, sides and block alike.
     *
     * @return the total count per type in {@link UpgradeType} order
     */
    public int[] takeUpgrades(BlockPos pos) {
        int[] taken = upgrades.take(coord(pos));
        for (int count : taken) {
            if (count > 0) {
                changedAt(pos);
                break;
            }
        }
        return taken;
    }

    private SourceInfo source(BlockPos pos) {
        long key = pos.asLong();
        SourceInfo info = sources.get(key);
        if (info == null) {
            info = new SourceInfo(upgrades, coord(pos));
            sources.put(key, info);
        }
        return info;
    }

    /** Ticks between two transfers of this source side. */
    public int interval(BlockPos pos, Direction side) {
        return source(pos).interval[side.get3DDataValue()];
    }

    /** Maximum item types per transfer to one target, for this source side. */
    public int maxTypes(BlockPos pos, Direction side) {
        return source(pos).maxTypes[side.get3DDataValue()];
    }

    /** Whether the side may transfer at {@code now}; every side may after a change at its block. */
    public boolean sideDue(BlockPos pos, Direction side, long now) {
        return now >= source(pos).due[side.get3DDataValue()];
    }

    /** Marks the side as transferred at {@code now}: it waits for its own interval. */
    public void noteSideRun(BlockPos pos, Direction side, long now) {
        SourceInfo info = source(pos);
        int index = side.get3DDataValue();
        info.due[index] = now + info.interval[index];
    }

    /** Effective settings: no filter without filter upgrade, priority 0 without priority upgrade. */
    public PortSettings effectiveSettings(BlockPos pos, Direction side) {
        return effectiveSettings(pos, side, false);
    }

    /** Like {@link #effectiveSettings(BlockPos, Direction)}; {@code free}: filter and priority need no upgrade (wireless blocks). */
    public PortSettings effectiveSettings(BlockPos pos, Direction side, boolean free) {
        SourceInfo info = source(pos);
        PortSettings[] cache = free ? info.free : info.effective;
        int index = side.get3DDataValue();
        PortSettings result = cache[index];
        if (result == null) {
            result = computeSettings(info, pos, side, free);
            cache[index] = result;
        }
        return result;
    }

    /**
     * Filter of this side reduced to the entries of {@code type}; {@link ResourceFilter#NONE} when nothing is
     * filtered.
     */
    public ResourceFilter typedFilter(TransportType type, BlockPos pos, Direction side, boolean free) {
        ResourceFilter filter = effectiveSettings(pos, side, free).filter();
        if (filter.isEmpty()) {
            return filter;
        }
        SourceInfo info = source(pos);
        if (info.typed == null) {
            info.typed = new HashMap<>();
        }
        ResourceFilter[] cache = info.typed.computeIfAbsent(type.id(), id -> new ResourceFilter[2 * SIDES]);
        int index = side.get3DDataValue() + (free ? SIDES : 0);
        ResourceFilter result = cache[index];
        if (result == null) {
            result = Filters.forType(type, filter);
            cache[index] = result;
        }
        return result;
    }

    private PortSettings computeSettings(SourceInfo info, BlockPos pos, Direction side, boolean free) {
        PortSettings stored = settings(pos, side);
        if (stored.isDefault() || free) {
            return stored;
        }
        PortSettings effective = stored;
        Upgrades own = info.upgrades[side.get3DDataValue()];
        if (!UpgradeEffects.filterUnlocked(own)) {
            effective = effective.withFilter(ResourceFilter.NONE);
        }
        if (!UpgradeEffects.priorityUnlocked(own)) {
            effective = effective.withPriority(0);
        }
        return effective;
    }

    // Source timing

    /**
     * Ticks until the next run of a source block: the regular interval after a transfer and during the first
     * {@value #IDLE_GRACE_TICKS} ticks without one; after that half the idle time so far, at most the configured
     * idle maximum.
     */
    public int nextDelay(BlockPos pos, boolean transferred, int interval) {
        long key = pos.asLong();
        if (transferred) {
            if (!idleTicks.isEmpty()) {
                idleTicks.remove(key);
            }
            return interval;
        }
        int max = VectrumConfig.get().idleIntervalMax();
        if (max <= interval) {
            return interval;
        }
        int idle = idleTicks.get(key);
        int delay = idle < IDLE_GRACE_TICKS ? interval : Math.min(max, Math.max(interval, idle / 2));
        idleTicks.put(key, Math.min(idle + delay, 1 << 20));
        return delay;
    }

    /** Ends the waiting phase of an idle source block. */
    public void wakeIdle(BlockPos pos) {
        if (!idleTicks.isEmpty()) {
            idleTicks.remove(pos.asLong());
        }
    }

    /** Whether the block state is due for a check; starts the next period when it is. */
    public boolean refreshDue(BlockPos pos, long now) {
        int period = VectrumConfig.get().refreshInterval();
        if (period <= 0) {
            return true;
        }
        long key = pos.asLong();
        if (now - refreshed.get(key) < period) {
            return false;
        }
        refreshed.put(key, now);
        return true;
    }

    /**
     * Records which port sides of the block have a storage; a change clears the caches of the block's layers.
     *
     * @param mask one bit per type and side that has a storage
     */
    public void notePorts(BlockPos pos, int mask) {
        long key = pos.asLong();
        int before = portMasks.put(key, mask);
        if (before != mask) {
            changedAt(pos);
        }
    }

    /** Records a state check of the block. */
    public void noteRefreshed(BlockPos pos, long now) {
        refreshed.put(pos.asLong(), now);
    }

    // Target back-off

    /** Whether the target is currently backed off for this source side. */
    @SuppressWarnings("unchecked")
    public boolean isSleeping(TransportType type, BlockPos source, Direction sourceSide, Target target, long now) {
        LayerState state = layerStates.get(type.id());
        if (state == null || state.sleep.isEmpty()) {
            return false;
        }
        Map<Target, Sleep>[] sides = state.sleep.get(source.asLong());
        if (sides == null) {
            return false;
        }
        Map<Target, Sleep> map = sides[sourceSide.get3DDataValue()];
        if (map == null) {
            return false;
        }
        Sleep entry = map.get(target);
        return entry != null && now < entry.until;
    }

    /** Records a refusal: the target waits for the configured pause of its refusal count. */
    @SuppressWarnings("unchecked")
    public void noteMiss(TransportType type, BlockPos source, Direction sourceSide, Target target, long now) {
        LayerState state = layer(type.id());
        long key = source.asLong();
        Map<Target, Sleep>[] sides = state.sleep.get(key);
        if (sides == null) {
            sides = new Map[SIDES];
            state.sleep.put(key, sides);
        }
        int index = sourceSide.get3DDataValue();
        Map<Target, Sleep> map = sides[index];
        if (map == null) {
            map = new HashMap<>();
            sides[index] = map;
        }
        Sleep entry = map.get(target);
        if (entry == null) {
            entry = new Sleep();
            map.put(target, entry);
        }
        entry.failures = Math.min(entry.failures + 1, 1 << 20);
        entry.until = now + VectrumConfig.get().targetPause(entry.failures);
    }

    /** Records a delivery: clears the back-off of the target. */
    @SuppressWarnings("unchecked")
    public void noteHit(TransportType type, BlockPos source, Direction sourceSide, Target target) {
        LayerState state = layerStates.get(type.id());
        if (state == null || state.sleep.isEmpty()) {
            return;
        }
        Map<Target, Sleep>[] sides = state.sleep.get(source.asLong());
        if (sides == null) {
            return;
        }
        Map<Target, Sleep> map = sides[sourceSide.get3DDataValue()];
        if (map != null && !map.isEmpty()) {
            map.remove(target);
        }
    }

    // Flow records

    /**
     * Last transfer of a source side.
     *
     * @param tick        game tick of the transfer
     * @param budget      throughput limit of the transfer
     * @param moved       units moved
     * @param rate        units per second over the last full window
     */
    public record TransferRecord(long tick, long budget, long moved, long windowStart, long windowMoved, double rate) {
    }

    /** Length of a rate window in ticks. */
    private static final long RATE_WINDOW = 100;
    /** Ticks after which a rate counts as zero. */
    private static final long RATE_EXPIRY = 200;

    private Flow flow(TransportType type, BlockPos pos, Direction side, boolean create) {
        LayerState state = create ? layer(type.id()) : layerStates.get(type.id());
        if (state == null) {
            return null;
        }
        long key = pos.asLong();
        Flow[] sides = state.flows.get(key);
        if (sides == null) {
            if (!create) {
                return null;
            }
            sides = new Flow[SIDES];
            state.flows.put(key, sides);
        }
        int index = side.get3DDataValue();
        Flow flow = sides[index];
        if (flow == null && create) {
            flow = new Flow();
            sides[index] = flow;
        }
        return flow;
    }

    /** Records a transfer attempt of a source side. */
    public void noteTransfer(BlockPos pos, Direction side, TransportType type, long now, long budget, long moved) {
        Flow flow = flow(type, pos, side, true);
        if (flow.tick < 0) {
            flow.windowStart = now;
        }
        if (now - flow.windowStart >= RATE_WINDOW) {
            flow.rate = flow.windowMoved * 20.0 / (now - flow.windowStart);
            flow.windowStart = now;
            flow.windowMoved = 0;
        }
        flow.tick = now;
        flow.budget = budget;
        flow.moved = moved;
        flow.windowMoved += moved;
    }

    /** Last transfer of a source side, or {@code null}. */
    public TransferRecord lastTransfer(BlockPos pos, Direction side, TransportType type) {
        Flow flow = flow(type, pos, side, false);
        if (flow == null || flow.tick < 0) {
            return null;
        }
        return new TransferRecord(flow.tick, flow.budget, flow.moved, flow.windowStart, flow.windowMoved, flow.rate);
    }

    /** Units per second moved by a source side lately. */
    public double rate(BlockPos pos, Direction side, TransportType type, long now) {
        TransferRecord record = lastTransfer(pos, side, type);
        if (record == null || now - record.tick() > RATE_EXPIRY) {
            return 0;
        }
        long span = Math.max(1, now - record.windowStart());
        double current = record.windowMoved() * 20.0 / Math.max(span, RATE_WINDOW / 2);
        return Math.max(record.rate(), current);
    }

    /** Records that goods arrived at a target side. */
    public void noteReceived(BlockPos pos, Direction side, TransportType type, long now) {
        flow(type, pos, side, true).received = now;
    }

    /** Tick of the last delivery to a target side, or -1. */
    public long lastReceived(BlockPos pos, Direction side, TransportType type) {
        Flow flow = flow(type, pos, side, false);
        return flow == null ? -1 : flow.received;
    }

    /** Removes the run-time records of a block. */
    private void forget(BlockPos pos) {
        long key = pos.asLong();
        for (LayerState state : layerStates.values()) {
            state.flows.remove(key);
            state.sleep.remove(key);
        }
        sources.remove(key);
        idleTicks.remove(key);
        refreshed.remove(key);
        portMasks.remove(key);
    }

    // Throughput limit

    /** Base limit of a layer: units per transfer and source side. */
    private static long baseLimit(String layer) {
        return TransportDefaults.baseThroughput(layer);
    }

    private ThroughputLimits limits(String layer) {
        return limits.computeIfAbsent(layer, id -> new ThroughputLimits(baseLimit(id)));
    }

    /** Throughput limit of a port side: the manual value of its block, otherwise the base limit times the upgrade factor. */
    public long throughput(TransportType type, BlockPos pos, Direction side) {
        return throughput(type, pos, source(pos).upgrades[side.get3DDataValue()]);
    }

    /** Throughput limit of a device without sides (wireless port) or of the block as such. */
    public long throughput(TransportType type, BlockPos pos) {
        return throughput(type, pos, source(pos).upgrades[UpgradeTable.BLOCK]);
    }

    private long throughput(TransportType type, BlockPos pos, Upgrades upgrades) {
        ThroughputLimits own = limits.get(type.id());
        if (own != null && own.overrideCount() > 0) {
            Long manual = own.overrideOf(coord(pos));
            if (manual != null) {
                return manual;
            }
        }
        return UpgradeEffects.throughput(baseLimit(type.id()), upgrades);
    }

    /** Sets a manual limit for a block. */
    public void setThroughput(TransportType type, BlockPos pos, long limit) {
        if (limits(type.id()).put(coord(pos), limit)) {
            setDirty();
        }
    }

    /** Removes the manual limit of a block. */
    public void clearThroughput(TransportType type, BlockPos pos) {
        ThroughputLimits own = limits.get(type.id());
        if (own != null && own.reset(coord(pos))) {
            setDirty();
        }
    }

    private static boolean isUnset(EndpointMode[] stored) {
        for (EndpointMode mode : stored) {
            if (mode != null) {
                return false;
            }
        }
        return true;
    }

    /** Network containing this block, or {@code null}. */
    public Network networkAt(TransportType type, BlockPos pos) {
        return registry.networkAt(type.id(), coord(pos));
    }

    // Change tracking

    /** A change inside one layer: clears its caches. */
    private void changedLayer(String layerId) {
        setDirty();
        stamp++;
        if (TransportType.DIGITAL.id().equals(layerId)) {
            clearCaches();
        } else {
            layer(layerId).clear();
        }
    }

    /** A change of the data of one block: clears the derived values and the caches of the layers it belongs to. */
    private void changedAt(BlockPos pos) {
        setDirty();
        stamp++;
        long key = pos.asLong();
        sources.remove(key);
        idleTicks.remove(key);
        BlockCoord coord = coord(pos);
        for (NetworkGraph graph : registry.graphs()) {
            if (graph.contains(coord)) {
                layer(graph.layer()).clear();
            }
        }
    }

    /** A change that can reach several layers (coder frequencies). */
    private void changedAll() {
        setDirty();
        stamp++;
        sources.clear();
        clearCaches();
    }

    private void clearCaches() {
        for (LayerState state : layerStates.values()) {
            state.clear();
        }
    }

    // Targets

    /**
     * All targets (output sides) of the network, sorted by position, cached until the layer changes. Incomplete
     * lists (endpoints in unloaded chunks) expire after {@code INCOMPLETE_LIFETIME} ticks.
     */
    public List<Target> targets(ServerLevel level, TransportType type, Network network) {
        return cachedTargets(level, type, network).targets;
    }

    private CachedTargets cachedTargets(ServerLevel level, TransportType type, Network network) {
        LayerState state = layer(type.id());
        long now = level.getGameTime();
        CachedTargets cached = state.own.get(network.id());
        if (cached != null && cached.valid(now)) {
            return cached;
        }

        boolean[] complete = {true};
        List<Target> targets = buildTargets(level, type, network, complete);
        CachedTargets fresh = new CachedTargets(targets, complete[0] ? Long.MAX_VALUE : now + INCOMPLETE_LIFETIME,
                stamp, false);
        state.own.put(network.id(), fresh);
        return fresh;
    }

    /**
     * Targets of the network plus the networks coupled to it through coders (own network first).
     * <ul>
     * <li>1. no digital layer or non-quantity type: own targets</li>
     * <li>2. cache lookup by network id</li>
     * <li>3. own targets plus targets of every partner network</li>
     * <li>4. validity is the minimum over all parts</li>
     * </ul>
     */
    private CachedTargets coupledTargets(ServerLevel level, TransportType type, Network network) {
        if (type.behavior() != TransportType.Behavior.QUANTITY || !registry.hasLayer(TransportType.DIGITAL.id())) {
            return cachedTargets(level, type, network);
        }
        LayerState state = layer(type.id());
        long now = level.getGameTime();
        CachedTargets cached = state.coupled.get(network.id());
        if (cached != null && cached.valid(now)) {
            return cached;
        }

        CachedTargets own = cachedTargets(level, type, network);
        List<Network> partners = partnerNetworks(level, type, network);
        if (partners.isEmpty()) {
            state.coupled.put(network.id(), own);
            return own;
        }
        List<Target> all = new ArrayList<>(own.targets);
        long validUntil = own.validUntil;
        for (Network partner : partners) {
            CachedTargets part = cachedTargets(level, type, partner);
            all.addAll(part.targets);
            validUntil = Math.min(validUntil, part.validUntil);
        }
        CachedTargets combined = new CachedTargets(List.copyOf(all), validUntil, stamp, false);
        state.coupled.put(network.id(), combined);
        return combined;
    }

    /**
     * Targets of the network at {@code pos} including coder partners; used for wireless receivers.
     *
     * @param complete set to {@code false} when the list is incomplete
     */
    List<Target> attachedTargets(ServerLevel level, TransportType type, BlockPos pos, boolean[] complete) {
        Network network = networkAt(type, pos);
        if (network == null) {
            return List.of();
        }
        CachedTargets coupled = coupledTargets(level, type, network);
        if (coupled.validUntil != Long.MAX_VALUE) {
            complete[0] = false;
        }
        return coupled.targets;
    }

    /**
     * All targets a source in {@code network} can reach.
     * <ul>
     * <li>1. non-quantity types: {@link #targets}</li>
     * <li>2. cache lookup by network id</li>
     * <li>3. coupled targets (own network and coder partners)</li>
     * <li>4. per sending wireless port in these networks: receivers of its frequency</li>
     * <li>5. duplicates removed; validity is the minimum over all parts</li>
     * </ul>
     */
    public List<Target> reachableTargets(ServerLevel level, TransportType type, Network network) {
        return reach(level, type, network).targets;
    }

    /** Like {@link #reachableTargets}, grouped by priority (highest first); cached with the list. */
    public List<List<Target>> reachableGroups(ServerLevel level, TransportType type, Network network) {
        return reach(level, type, network).groups();
    }

    private CachedTargets reach(ServerLevel level, TransportType type, Network network) {
        if (type.behavior() != TransportType.Behavior.QUANTITY) {
            return cachedTargets(level, type, network);
        }
        LayerState state = layer(type.id());
        long now = level.getGameTime();
        CachedTargets cached = state.full.get(network.id());
        if (cached != null && cached.valid(now)) {
            return cached;
        }

        CachedTargets coupled = coupledTargets(level, type, network);
        List<Network> networks = new ArrayList<>();
        networks.add(network);
        if (registry.hasLayer(TransportType.DIGITAL.id())) {
            networks.addAll(partnerNetworks(level, type, network));
        }
        java.util.Set<Target> all = new java.util.LinkedHashSet<>(coupled.targets);
        long validUntil = coupled.validUntil;
        boolean wirelessUsed = false;
        WirelessRegistry wireless = null;
        for (Network part : networks) {
            List<BlockPos> endpoints = new ArrayList<>();
            for (BlockCoord coord : part.endpointPositions()) {
                endpoints.add(toPos(coord));
            }
            endpoints.sort((a, b) -> Long.compare(a.asLong(), b.asLong()));
            for (BlockPos pos : endpoints) {
                if (!level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof WirelessBlock)) {
                    continue;
                }
                wirelessUsed = true;
                if (wireless == null) {
                    wireless = WirelessRegistry.get(level);
                }
                if (!wireless.mode(level, pos, type).sends()) {
                    continue;
                }
                WirelessRegistry.Receivers receivers = wireless.receivers(level.getServer(), type,
                        wireless.frequency(level, pos), level, blockUpgrades(pos));
                all.addAll(receivers.targets());
                if (!receivers.complete()) {
                    validUntil = Math.min(validUntil, now + INCOMPLETE_LIFETIME);
                }
            }
        }
        CachedTargets combined = new CachedTargets(List.copyOf(all), validUntil, stamp, wirelessUsed);
        state.full.put(network.id(), combined);
        return combined;
    }

    /**
     * Transport networks of the same type coupled to {@code network} through coders.
     * <ul>
     * <li>1. collect coder endpoints of the network in loaded chunks, sorted by position</li>
     * <li>2. per digital network (visited once): list all coders with frequency and transport network id</li>
     * <li>3. resolve partner coders via {@link CoderLinks#partners} to their transport networks</li>
     * </ul>
     */
    private List<Network> partnerNetworks(ServerLevel level, TransportType type, Network network) {
        List<Network> result = new ArrayList<>();
        java.util.Set<Long> visited = new java.util.HashSet<>();
        List<BlockPos> endpoints = new ArrayList<>();
        for (BlockCoord coord : network.endpointPositions()) {
            endpoints.add(toPos(coord));
        }
        endpoints.sort((a, b) -> Long.compare(a.asLong(), b.asLong()));
        for (BlockPos pos : endpoints) {
            if (!level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof CoderBlock)) {
                continue;
            }
            Network digital = registry.networkAt(TransportType.DIGITAL.id(), coord(pos));
            if (digital == null || !visited.add(digital.id())) {
                continue;
            }
            List<CoderLinks.Coder> coders = new ArrayList<>();
            for (BlockCoord coord : digital.endpointPositions()) {
                BlockPos other = toPos(coord);
                Network transport = registry.networkAt(type.id(), coord);
                coders.add(new CoderLinks.Coder(other.asLong(), frequency(other), transport == null ? -1 : transport.id()));
            }
            for (CoderLinks.Coder partner : CoderLinks.partners(network.id(), coders)) {
                Network found = registry.networkAt(type.id(), coord(BlockPos.of(partner.pos())));
                if (found != null && !result.contains(found)) {
                    result.add(found);
                }
            }
        }
        return result;
    }

    /** Networks of {@code type} reachable from {@code network}: the network itself and its coder partners. */
    public List<Network> coupledNetworks(ServerLevel level, TransportType type, Network network) {
        List<Network> networks = new ArrayList<>();
        networks.add(network);
        if (type.behavior() == TransportType.Behavior.QUANTITY && registry.hasLayer(TransportType.DIGITAL.id())) {
            networks.addAll(partnerNetworks(level, type, network));
        }
        return networks;
    }

    /** Endpoints of the coupled networks that lie in unloaded chunks. */
    public int unloadedEndpoints(ServerLevel level, TransportType type, Network network) {
        int count = 0;
        for (Network part : coupledNetworks(level, type, network)) {
            for (BlockCoord coord : part.endpointPositions()) {
                if (!level.hasChunkAt(toPos(coord))) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Source sides in the coupled networks (loaded chunks only). */
    public int sourceCount(ServerLevel level, TransportType type, Network network) {
        int count = 0;
        for (Network part : coupledNetworks(level, type, network)) {
            count += countPorts(level, type, part)[0];
        }
        return count;
    }

    /** Sorted distinct frequencies of all coders in the digital network of this block. */
    public List<Integer> digitalFrequencies(BlockPos pos) {
        Network digital = registry.networkAt(TransportType.DIGITAL.id(), coord(pos));
        java.util.TreeSet<Integer> result = new java.util.TreeSet<>();
        if (digital != null) {
            for (BlockCoord coord : digital.endpointPositions()) {
                result.add(frequency(toPos(coord)));
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * Builds the target list of a network.
     * <ul>
     * <li>1. endpoints sorted by position</li>
     * <li>2. unloaded chunk: mark incomplete; non-conduit block: skip</li>
     * <li>3. per side with OUTPUT connection (and a matching port for multi-type conduits): add a target</li>
     * </ul>
     */
    private List<Target> buildTargets(ServerLevel level, TransportType type, Network network, boolean[] complete) {
        List<BlockPos> endpoints = new ArrayList<>(network.endpointCount());
        for (BlockCoord coord : network.endpointPositions()) {
            endpoints.add(new BlockPos(coord.x(), coord.y(), coord.z()));
        }
        endpoints.sort((a, b) -> Long.compare(a.asLong(), b.asLong()));

        List<Target> result = new ArrayList<>();
        for (BlockPos pos : endpoints) {
            if (!level.hasChunkAt(pos)) {
                complete[0] = false;
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ConduitBlock conduit)) {
                continue;
            }
            for (Direction side : Sides.ALL) {
                if (conduit.connection(state, side) == Connection.TARGET
                        && (conduit.transportTypes().size() == 1
                        || hasPort(level, type, pos.relative(side), side.getOpposite()))) {
                    result.add(Target.of(type, level, pos, side, pos.relative(side), effectiveSettings(pos, side)));
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean hasPort(ServerLevel level, TransportType type, BlockPos neighbour, Direction neighbourSide) {
        return level.hasChunkAt(neighbour) && Ports.find(type, level, neighbour, neighbourSide) != null;
    }

    /**
     * Counts source and target sides of a network (loaded chunks only).
     *
     * @return {@code {sources, targets}}; an energy push side counts as both
     */
    public int[] countPorts(ServerLevel level, TransportType type, Network network) {
        int sources = 0;
        int targets = 0;
        for (BlockCoord coord : network.endpointPositions()) {
            BlockPos pos = new BlockPos(coord.x(), coord.y(), coord.z());
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ConduitBlock conduit) {
                for (Direction side : Sides.ALL) {
                    Connection connection = conduit.connection(state, side);
                    if (conduit.transportTypes().size() > 1 && connection != Connection.NONE
                            && connection != Connection.LINK
                            && !hasPort(level, type, pos.relative(side), side.getOpposite())) {
                        continue;
                    }
                    if (TransportType.ENERGY.equals(type) && connection == Connection.TARGET) {
                        sources++; // a push side also takes energy out of its storage
                        targets++;
                    } else if (connection == Connection.SOURCE) {
                        sources++;
                    } else if (connection == Connection.TARGET) {
                        targets++;
                    }
                }
            }
        }
        return new int[]{sources, targets};
    }

    // Persistence

    /**
     * Writes all state to NBT.
     * <ul>
     * <li>{@code layers}: per graph layer, node positions and data bytes (endpoint flag + side mask)</li>
     * <li>{@code ports}: per block, side role bytes (unset = -1)</li>
     * <li>{@code limits}: per layer, own throughput limits as position and value arrays</li>
     * <li>{@code settings}: per port key, side, priority, mode, blacklist flag, filter ids</li>
     * <li>{@code pointers}: per port key, side, round-robin value</li>
     * <li>{@code upgrades}: per block, upgrade counts</li>
     * <li>{@code frequencies}: per coder, frequency</li>
     * <li>{@code severed}: per block, manually severed side mask</li>
     * </ul>
     * Signal outputs, back-off state and caches are not written.
     */
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag layers = new ListTag();
        for (NetworkGraph graph : registry.graphs()) {
            List<NodeInfo> nodes = graph.snapshot();
            long[] positions = new long[nodes.size()];
            byte[] data = new byte[nodes.size()];
            for (int i = 0; i < nodes.size(); i++) {
                NodeInfo node = nodes.get(i);
                positions[i] = new BlockPos(node.pos().x(), node.pos().y(), node.pos().z()).asLong();
                data[i] = (byte) ((node.kind() == NodeKind.ENDPOINT ? ENDPOINT_FLAG : 0) | (node.sideMask() & MASK_BITS));
            }
            CompoundTag layer = new CompoundTag();
            layer.putString(TAG_ID, graph.layer());
            layer.putLongArray(TAG_POS, positions);
            layer.putByteArray(TAG_DATA, data);
            layers.add(layer);
        }
        tag.put(TAG_LAYERS, layers);
        tag.putInt(TAG_ROLE_DEFAULT, 1);

        ListTag ports = new ListTag();
        for (Map.Entry<Long, EndpointMode[]> entry : modes.entrySet()) {
            byte[] stored = new byte[entry.getValue().length];
            for (int i = 0; i < stored.length; i++) {
                EndpointMode mode = entry.getValue()[i];
                stored[i] = (byte) (mode == null ? UNSET_MODE : mode.ordinal());
            }
            CompoundTag port = new CompoundTag();
            port.putLong(TAG_POS, entry.getKey());
            port.putByteArray(TAG_MODES, stored);
            ports.add(port);
        }
        tag.put(TAG_PORTS, ports);

        ListTag saved = new ListTag();
        for (Map.Entry<String, ThroughputLimits> entry : limits.entrySet()) {
            Map<BlockCoord, Long> own = entry.getValue().overrides();
            if (own.isEmpty()) {
                continue;
            }
            long[] positions = new long[own.size()];
            long[] values = new long[own.size()];
            int i = 0;
            for (Map.Entry<BlockCoord, Long> limit : own.entrySet()) {
                BlockCoord c = limit.getKey();
                positions[i] = new BlockPos(c.x(), c.y(), c.z()).asLong();
                values[i] = limit.getValue();
                i++;
            }
            CompoundTag layer = new CompoundTag();
            layer.putString(TAG_ID, entry.getKey());
            layer.putLongArray(TAG_POS, positions);
            layer.putLongArray(TAG_VALUES, values);
            saved.add(layer);
        }
        tag.put(TAG_LIMITS, saved);
        tag.putInt(TAG_LIMITS_MANUAL, 1);

        ListTag savedSettings = new ListTag();
        for (Map.Entry<PortKey, PortSettings> entry : settings.entries().entrySet()) {
            PortSettings value = entry.getValue();
            CompoundTag item = new CompoundTag();
            item.putLong(TAG_POS, toPos(entry.getKey().pos()).asLong());
            item.putByte(TAG_SIDE, (byte) entry.getKey().side().ordinal());
            item.putInt(TAG_PRIORITY, value.priority());
            item.putString(TAG_MODE, value.mode().id());
            item.putString(TAG_REDSTONE, value.redstone().id());
            item.putString(TAG_NAME, value.name());
            item.putBoolean(TAG_BLACKLIST, value.filter().blacklist());
            ListTag ids = new ListTag();
            for (String id : value.filter().ids()) {
                ids.add(StringTag.valueOf(id));
            }
            item.put(TAG_IDS, ids);
            savedSettings.add(item);
        }
        tag.put(TAG_SETTINGS, savedSettings);

        ListTag savedPointers = new ListTag();
        for (Map.Entry<PortKey, Integer> entry : pointers.entries().entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putLong(TAG_POS, toPos(entry.getKey().pos()).asLong());
            item.putByte(TAG_SIDE, (byte) entry.getKey().side().ordinal());
            item.putInt(TAG_VALUE, entry.getValue());
            savedPointers.add(item);
        }
        tag.put(TAG_POINTERS, savedPointers);

        ListTag savedUpgrades = new ListTag();
        for (Map.Entry<BlockCoord, Upgrades[]> entry : upgrades.entries().entrySet()) {
            for (int slot = 0; slot < UpgradeTable.SLOTS; slot++) {
                Upgrades set = entry.getValue()[slot];
                if (set.isEmpty()) {
                    continue;
                }
                CompoundTag item = new CompoundTag();
                item.putLong(TAG_POS, toPos(entry.getKey()).asLong());
                item.putInt(TAG_SLOT, slot);
                item.putIntArray(TAG_COUNTS, set.toArray());
                savedUpgrades.add(item);
            }
        }
        tag.put(TAG_UPGRADES, savedUpgrades);

        ListTag savedFrequencies = new ListTag();
        for (Map.Entry<Long, Integer> entry : frequencies.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putLong(TAG_POS, entry.getKey());
            item.putInt(TAG_VALUE, entry.getValue());
            savedFrequencies.add(item);
        }
        tag.put(TAG_FREQUENCIES, savedFrequencies);

        ListTag savedSevered = new ListTag();
        for (Map.Entry<Long, Integer> entry : severed.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putLong(TAG_POS, entry.getKey());
            item.putInt(TAG_VALUE, entry.getValue());
            savedSevered.add(item);
        }
        tag.put(TAG_SEVERED, savedSevered);
        return tag;
    }

    /**
     * Saves written before the source became the default role let every side without a chosen role act as a target.
     * To leave such worlds as they were, every side of every endpoint of a quantity layer that has no chosen role
     * gets the role target stored; sides of cables placed afterwards use the new default.
     */
    private void keepTargetDefault() {
        java.util.Set<String> quantityLayers = java.util.Set.of(TransportType.ITEM.id(), TransportType.FLUID.id(),
                TransportType.ENERGY.id(), TransportType.GAS.id());
        for (NetworkGraph graph : registry.graphs()) {
            if (!quantityLayers.contains(graph.layer())) {
                continue;
            }
            for (NodeInfo node : graph.snapshot()) {
                if (node.kind() != NodeKind.ENDPOINT) {
                    continue;
                }
                EndpointMode[] stored = modes.computeIfAbsent(toPos(node.pos()).asLong(),
                        key -> new EndpointMode[Sides.ALL.length]);
                for (int side = 0; side < stored.length; side++) {
                    if (stored[side] == null) {
                        stored[side] = EndpointMode.TARGET;
                    }
                }
            }
        }
        setDirty();
    }

    /**
     * Saves written before limits were derived from the upgrades stored the derived value of every upgraded block as
     * its limit; those entries are dropped, leaving the manually set ones.
     */
    private void dropLegacyLimits() {
        for (Map.Entry<String, ThroughputLimits> entry : limits.entrySet()) {
            long base = baseLimit(entry.getKey());
            ThroughputLimits limit = entry.getValue();
            for (Map.Entry<BlockCoord, Long> own : new ArrayList<>(limit.overrides().entrySet())) {
                int count = upgrades.get(own.getKey(), UpgradeTable.BLOCK).count(UpgradeType.THROUGHPUT);
                long legacy = count > 30 ? Long.MAX_VALUE
                        : SaturatedMath.multiplySaturated(base, 1L << (2 * count));
                if (own.getValue() == legacy) {
                    limit.reset(own.getKey());
                }
            }
        }
        setDirty();
    }

    private static BlockPos toPos(BlockCoord c) {
        return new BlockPos(c.x(), c.y(), c.z());
    }

    private static PortKey readKey(LevelNetworks networks, CompoundTag item) {
        int side = item.getByte(TAG_SIDE);
        if (side < 0 || side >= io.github.fishgames.vectrum.core.network.Direction.VALUES.length) {
            return null;
        }
        return new PortKey(networks.coord(BlockPos.of(item.getLong(TAG_POS))),
                io.github.fishgames.vectrum.core.network.Direction.VALUES[side]);
    }

    /**
     * Reads all state from NBT.
     * <ul>
     * <li>1. layers: rebuild each graph from positions and data bytes (endpoint flag, side mask)</li>
     * <li>2. ports: restore side roles; drop all-unset entries</li>
     * <li>3. limits: restore non-negative manual limits</li>
     * <li>4. settings: restore priority, mode (default when unknown) and filter; skip invalid sides</li>
     * <li>5. pointers, upgrades, frequencies (positive only): restore</li>
     * </ul>
     */
    private static LevelNetworks load(String dimension, CompoundTag tag) {
        LevelNetworks networks = new LevelNetworks();
        networks.dimension = dimension;
        ListTag layers = tag.getList(TAG_LAYERS, Tag.TAG_COMPOUND);
        int total = 0;
        for (int i = 0; i < layers.size(); i++) {
            CompoundTag layer = layers.getCompound(i);
            NetworkGraph graph = networks.registry.graph(layer.getString(TAG_ID));
            long[] positions = layer.getLongArray(TAG_POS);
            byte[] data = layer.getByteArray(TAG_DATA);
            int count = Math.min(positions.length, data.length);
            for (int n = 0; n < count; n++) {
                BlockPos pos = BlockPos.of(positions[n]);
                NodeKind kind = (data[n] & ENDPOINT_FLAG) != 0 ? NodeKind.ENDPOINT : NodeKind.CABLE;
                BlockCoord coord = networks.coord(pos);
                if (!graph.contains(coord)) {
                    graph.add(coord, kind, data[n] & MASK_BITS);
                }
            }
            total += count;
        }
        ListTag ports = tag.getList(TAG_PORTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < ports.size(); i++) {
            CompoundTag port = ports.getCompound(i);
            byte[] stored = port.getByteArray(TAG_MODES);
            EndpointMode[] restored = new EndpointMode[Sides.ALL.length];
            for (int side = 0; side < restored.length; side++) {
                restored[side] = side < stored.length && stored[side] != UNSET_MODE
                        ? EndpointMode.byOrdinal(stored[side]) : null;
            }
            if (!isUnset(restored)) {
                networks.modes.put(port.getLong(TAG_POS), restored);
            }
        }
        ListTag savedLimits = tag.getList(TAG_LIMITS, Tag.TAG_COMPOUND);
        for (int i = 0; i < savedLimits.size(); i++) {
            CompoundTag layer = savedLimits.getCompound(i);
            ThroughputLimits target = networks.limits(layer.getString(TAG_ID));
            long[] positions = layer.getLongArray(TAG_POS);
            long[] values = layer.getLongArray(TAG_VALUES);
            for (int n = 0; n < Math.min(positions.length, values.length); n++) {
                if (values[n] >= 0) {
                    target.put(networks.coord(BlockPos.of(positions[n])), values[n]);
                }
            }
        }
        ListTag savedSettings = tag.getList(TAG_SETTINGS, Tag.TAG_COMPOUND);
        for (int i = 0; i < savedSettings.size(); i++) {
            CompoundTag item = savedSettings.getCompound(i);
            PortKey key = readKey(networks, item);
            if (key == null) {
                continue;
            }
            DistributionMode mode = DistributionMode.byId(item.getString(TAG_MODE));
            java.util.Set<String> ids = new java.util.TreeSet<>();
            ListTag idList = item.getList(TAG_IDS, Tag.TAG_STRING);
            for (int n = 0; n < idList.size(); n++) {
                ids.add(idList.getString(n));
            }
            RedstoneMode redstone = RedstoneMode.byId(item.getString(TAG_REDSTONE));
            networks.settings.set(key, new PortSettings(item.getInt(TAG_PRIORITY),
                    mode == null ? DistributionMode.DEFAULT : mode,
                    new ResourceFilter(item.getBoolean(TAG_BLACKLIST), ids),
                    redstone == null ? RedstoneMode.DEFAULT : redstone, item.getString(TAG_NAME)));
        }
        ListTag savedPointers = tag.getList(TAG_POINTERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < savedPointers.size(); i++) {
            CompoundTag item = savedPointers.getCompound(i);
            PortKey key = readKey(networks, item);
            if (key != null) {
                networks.pointers.set(key, item.getInt(TAG_VALUE));
            }
        }
        ListTag savedUpgrades = tag.getList(TAG_UPGRADES, Tag.TAG_COMPOUND);
        for (int i = 0; i < savedUpgrades.size(); i++) {
            CompoundTag item = savedUpgrades.getCompound(i);
            int slot = item.contains(TAG_SLOT) ? item.getInt(TAG_SLOT) : UpgradeTable.BLOCK;
            if (slot >= 0 && slot < UpgradeTable.SLOTS) {
                networks.upgrades.set(networks.coord(BlockPos.of(item.getLong(TAG_POS))), slot,
                        Upgrades.of(item.getIntArray(TAG_COUNTS)));
            }
        }
        ListTag savedFrequencies = tag.getList(TAG_FREQUENCIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < savedFrequencies.size(); i++) {
            CompoundTag item = savedFrequencies.getCompound(i);
            if (item.getInt(TAG_VALUE) > 0) {
                networks.frequencies.put(item.getLong(TAG_POS), item.getInt(TAG_VALUE));
            }
        }
        ListTag savedSevered = tag.getList(TAG_SEVERED, Tag.TAG_COMPOUND);
        for (int i = 0; i < savedSevered.size(); i++) {
            CompoundTag item = savedSevered.getCompound(i);
            int mask = item.getInt(TAG_VALUE) & MASK_BITS;
            if (mask != 0) {
                networks.severed.put(item.getLong(TAG_POS), mask);
            }
        }
        if (!tag.contains(TAG_ROLE_DEFAULT)) {
            networks.keepTargetDefault();
        }
        if (!tag.contains(TAG_LIMITS_MANUAL)) {
            networks.dropLegacyLimits();
        }
        Vectrum.LOGGER.debug("Loaded networks of {}: {} nodes, {} port mode entries",
                dimension, total, networks.modes.size());
        return networks;
    }
}
