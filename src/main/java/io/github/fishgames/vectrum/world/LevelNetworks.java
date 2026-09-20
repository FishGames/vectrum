package io.github.fishgames.vectrum.world;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.block.CoderBlock;
import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
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
import io.github.fishgames.vectrum.core.routing.PortSettingsTable;
import io.github.fishgames.vectrum.core.routing.ResourceFilter;
import io.github.fishgames.vectrum.core.routing.RoundRobinPointers;
import io.github.fishgames.vectrum.core.throughput.ThroughputLimits;
import io.github.fishgames.vectrum.core.upgrade.UpgradeEffects;
import io.github.fishgames.vectrum.core.upgrade.UpgradeTable;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.logistics.Target;
import io.github.fishgames.vectrum.logistics.TransportDefaults;
import io.github.fishgames.vectrum.transfer.Ports;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-dimension network state ({@code data/vectrum_networks.dat}): node graphs per layer, endpoint modes, coder
 * frequencies, signal outputs, throughput limits, port settings, round-robin pointers, upgrades, target caches and
 * back-off state. Bridges the core ({@link NetworkGraph}) and Minecraft.
 */
public final class LevelNetworks extends SavedData {
    private static final String DATA_NAME = "vectrum_networks";
    private static final String TAG_LAYERS = "layers";
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
    private static final String TAG_BLACKLIST = "blacklist";
    private static final String TAG_IDS = "ids";
    private static final String TAG_VALUE = "value";
    private static final String TAG_UPGRADES = "upgrades";
    private static final String TAG_COUNTS = "counts";
    private static final String TAG_FREQUENCIES = "frequencies";
    private static final byte UNSET_MODE = -1;
    private static final int ENDPOINT_FLAG = 0x40;
    private static final int MASK_BITS = 0x3F;

    /** Global change counter for networks and endpoint settings; cached target lists are valid for one value. */
    private static long epoch;

    private final String dimension;
    private final NetworkRegistry registry = new NetworkRegistry();
    /** Chosen roles of the port sides, per block that differs from the default. Key: BlockPos.asLong(). */
    private final Map<Long, EndpointMode[]> modes = new HashMap<>();

    /** Coder frequencies (non-zero only). Key: BlockPos.asLong(). */
    private final Map<Long, Integer> frequencies = new HashMap<>();

    /** Current redstone outputs (non-zero only, not persisted). Key: BlockPos.asLong(). */
    private final Map<Long, Integer> signalOutputs = new HashMap<>();

    /** Throughput limits per layer (key: layer id); own values only. */
    private final Map<String, ThroughputLimits> limits = new HashMap<>();

    /** Cached target list with expiry tick. */
    private record CachedTargets(List<Target> targets, long validUntil) {
    }

    /** Lifetime in ticks of an incomplete target list. */
    private static final long INCOMPLETE_LIFETIME = 20;

    /** Port side settings (priority, distribution mode, filter); non-default only. */
    private final PortSettingsTable settings = new PortSettingsTable();
    /** Upgrades per block (blocks with at least one upgrade). */
    private final UpgradeTable upgrades = new UpgradeTable();
    /** Round-robin pointer per source side. */
    private final RoundRobinPointers pointers = new RoundRobinPointers();

    /** Back-off key: source side plus target side. Not persisted. */
    private record SleepKey(long source, int sourceSide, int targetDimension, long target, int targetSide) {
    }

    /** Back-off state: failure count and wait-until tick. */
    private record Backoff(int failures, long until) {
    }

    /** Maximum back-off in ticks. */
    private static final long MAX_SLEEP_TICKS = 100;
    private final Map<SleepKey, Backoff> backoff = new HashMap<>();

    private long cacheEpoch = -1;
    /** Target cache. Key: layer id + network id. */
    private final Map<String, CachedTargets> targetCache = new HashMap<>();

    private LevelNetworks(String dimension) {
        this.dimension = dimension;
    }

    public static LevelNetworks get(ServerLevel level) {
        String dimension = level.dimension().location().toString();
        return level.getDataStorage().computeIfAbsent(
                tag -> load(dimension, tag), () -> new LevelNetworks(dimension), DATA_NAME);
    }

    /** Current value of the global change counter. */
    public static long epoch() {
        return epoch;
    }

    /** Increments the change counter, invalidating all caches. */
    public static void invalidateCaches() {
        epoch++;
    }

    // Nodes

    public BlockCoord coord(BlockPos pos) {
        return new BlockCoord(dimension, pos.getX(), pos.getY(), pos.getZ());
    }

    /** Adds the node or updates its kind and open sides. */
    public void put(TransportType type, BlockPos pos, NodeKind kind, int sideMask) {
        NetworkGraph graph = registry.graph(type.id());
        BlockCoord coord = coord(pos);
        if (graph.contains(coord)) {
            graph.setSides(coord, sideMask);
            graph.setKind(coord, kind);
        } else {
            graph.add(coord, kind, sideMask);
        }
        changed();
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
            changed();
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
        changed();
    }

    /** Removes the chosen roles of a block. */
    public void clearModes(BlockPos pos) {
        if (modes.remove(pos.asLong()) != null) {
            changed();
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
            changed();
        }
    }

    public void clearFrequency(BlockPos pos) {
        if (frequencies.remove(pos.asLong()) != null) {
            changed();
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
            changed();
        }
    }

    /** Round-robin pointer of this source side. */
    public int pointer(BlockPos pos, Direction side) {
        return pointers.get(key(pos, side));
    }

    public void setPointer(BlockPos pos, Direction side, int value) {
        pointers.set(key(pos, side), value);
        setDirty();
    }

    /** Removes settings and round-robin pointers of a block. */
    public void clearSettings(BlockPos pos) {
        BlockCoord coord = coord(pos);
        boolean removedSettings = settings.clear(coord);
        boolean removedPointers = pointers.clear(coord);
        if (removedSettings) {
            changed();
        } else if (removedPointers) {
            setDirty();
        }
    }

    // Upgrades

    /** Upgrades of this block. */
    public Upgrades upgrades(BlockPos pos) {
        return upgrades.get(coord(pos));
    }

    /**
     * Sets the upgrades of a block.
     * <ul>
     * <li>1. no change: return</li>
     * <li>2. throughput upgrade count changed: store the throughput limit per type (base limit x 4^count)</li>
     * <li>3. invalidate caches</li>
     * </ul>
     */
    public void setUpgrades(List<TransportType> types, BlockPos pos, Upgrades value) {
        Upgrades before = upgrades.get(coord(pos));
        if (!upgrades.set(coord(pos), value)) {
            return;
        }
        if (before.count(UpgradeType.THROUGHPUT) != value.count(UpgradeType.THROUGHPUT)) {
            for (TransportType type : types) {
                setThroughput(type, pos, UpgradeEffects.throughput(baseLimit(type.id()), value));
            }
        }
        changed();
    }

    /** Removes and returns all upgrades of a block. */
    public Upgrades takeUpgrades(BlockPos pos) {
        Upgrades taken = upgrades.take(coord(pos));
        if (!taken.isEmpty()) {
            changed();
        }
        return taken;
    }

    /** Ticks between two transfers of this source. */
    public int interval(BlockPos pos) {
        return UpgradeEffects.interval(ConduitBlock.INTERVAL, upgrades(pos));
    }

    /** Maximum item types per transfer to one target. */
    public int maxTypes(BlockPos pos) {
        return UpgradeEffects.maxTypes(upgrades(pos));
    }

    /** Effective settings: no filter without filter upgrade, priority 0 without priority upgrade. */
    public PortSettings effectiveSettings(BlockPos pos, Direction side) {
        return effectiveSettings(pos, side, false);
    }

    /** Like {@link #effectiveSettings(BlockPos, Direction)}; {@code free}: filter and priority need no upgrade (wireless blocks). */
    public PortSettings effectiveSettings(BlockPos pos, Direction side, boolean free) {
        PortSettings stored = settings(pos, side);
        if (stored.isDefault() || free) {
            return stored;
        }
        Upgrades installed = upgrades(pos);
        PortSettings effective = stored;
        if (!UpgradeEffects.filterUnlocked(installed)) {
            effective = effective.withFilter(ResourceFilter.NONE);
        }
        if (!UpgradeEffects.priorityUnlocked(installed)) {
            effective = effective.withPriority(0);
        }
        return effective;
    }

    // Target back-off

    /** Whether the target is currently backed off for this source side. */
    public boolean isSleeping(BlockPos source, Direction sourceSide, Target target, long now) {
        syncEpoch();
        Backoff entry = backoff.get(sleepKey(source, sourceSide, target));
        return entry != null && now < entry.until();
    }

    /** Records a miss: back-off delay {@code min(100, 10 << min(failures, 10))} ticks. */
    public void noteMiss(BlockPos source, Direction sourceSide, Target target, long now) {
        syncEpoch();
        SleepKey key = sleepKey(source, sourceSide, target);
        Backoff before = backoff.get(key);
        int failures = before == null ? 1 : before.failures() + 1;
        long delay = Math.min(MAX_SLEEP_TICKS, 10L << Math.min(failures, 10));
        backoff.put(key, new Backoff(failures, now + delay));
    }

    /** Records a hit: clears the back-off. */
    public void noteHit(BlockPos source, Direction sourceSide, Target target) {
        if (!backoff.isEmpty()) {
            backoff.remove(sleepKey(source, sourceSide, target));
        }
    }

    private static SleepKey sleepKey(BlockPos source, Direction sourceSide, Target target) {
        return new SleepKey(source.asLong(), sourceSide.get3DDataValue(), target.level().dimension().location().hashCode(),
                target.endpoint().asLong(), target.side().get3DDataValue());
    }

    // Throughput limit

    /** Base limit of a layer: units per transfer and source side. */
    private static long baseLimit(String layer) {
        return TransportDefaults.baseThroughput(layer);
    }

    private ThroughputLimits limits(String layer) {
        return limits.computeIfAbsent(layer, id -> new ThroughputLimits(baseLimit(id)));
    }

    /** Throughput limit of the block. */
    public long throughput(TransportType type, BlockPos pos) {
        return limits(type.id()).limitOf(coord(pos));
    }

    /** Like {@link #throughput}, clamped to {@code int}. */
    public int budget(TransportType type, BlockPos pos) {
        return limits(type.id()).budgetOf(coord(pos));
    }

    /** Sets the limit of a block. */
    public void setThroughput(TransportType type, BlockPos pos, long limit) {
        if (limits(type.id()).set(coord(pos), limit)) {
            setDirty();
        }
    }

    /** Removes the own limit of a block. */
    public void clearThroughput(TransportType type, BlockPos pos) {
        if (limits(type.id()).reset(coord(pos))) {
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

    private void changed() {
        setDirty();
        invalidateCaches();
    }

    // Targets

    /**
     * All targets (output sides) of the network, sorted by position, cached until the epoch changes. Incomplete
     * lists (endpoints in unloaded chunks) expire after {@code INCOMPLETE_LIFETIME} ticks.
     */
    public List<Target> targets(ServerLevel level, TransportType type, Network network) {
        return cachedTargets(level, type, network).targets();
    }

    private CachedTargets cachedTargets(ServerLevel level, TransportType type, Network network) {
        syncEpoch();
        long now = level.getGameTime();
        String key = type.id() + "#" + network.id();
        CachedTargets cached = targetCache.get(key);
        if (cached != null && now < cached.validUntil()) {
            return cached;
        }

        boolean[] complete = {true};
        List<Target> targets = buildTargets(level, type, network, complete);
        CachedTargets fresh = new CachedTargets(targets, complete[0] ? Long.MAX_VALUE : now + INCOMPLETE_LIFETIME);
        targetCache.put(key, fresh);
        return fresh;
    }

    /**
     * Targets of the network plus the networks coupled to it through coders (own network first).
     * <ul>
     * <li>1. no digital layer or non-quantity type: own targets</li>
     * <li>2. cache lookup under key {@code reach:<type>#<network>}</li>
     * <li>3. own targets plus targets of every partner network</li>
     * <li>4. validity is the minimum over all parts</li>
     * </ul>
     */
    private CachedTargets coupledTargets(ServerLevel level, TransportType type, Network network) {
        if (type.behavior() != TransportType.Behavior.QUANTITY || !registry.hasLayer(TransportType.DIGITAL.id())) {
            return cachedTargets(level, type, network);
        }
        syncEpoch();
        long now = level.getGameTime();
        String key = "reach:" + type.id() + "#" + network.id();
        CachedTargets cached = targetCache.get(key);
        if (cached != null && now < cached.validUntil()) {
            return cached;
        }

        CachedTargets own = cachedTargets(level, type, network);
        List<Network> partners = partnerNetworks(level, type, network);
        if (partners.isEmpty()) {
            targetCache.put(key, own);
            return own;
        }
        List<Target> all = new ArrayList<>(own.targets());
        long validUntil = own.validUntil();
        for (Network partner : partners) {
            CachedTargets part = cachedTargets(level, type, partner);
            all.addAll(part.targets());
            validUntil = Math.min(validUntil, part.validUntil());
        }
        CachedTargets combined = new CachedTargets(List.copyOf(all), validUntil);
        targetCache.put(key, combined);
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
        if (coupled.validUntil() != Long.MAX_VALUE) {
            complete[0] = false;
        }
        return coupled.targets();
    }

    /**
     * All targets a source in {@code network} can reach.
     * <ul>
     * <li>1. non-quantity types: {@link #targets}</li>
     * <li>2. cache lookup under key {@code full:<type>#<network>}</li>
     * <li>3. coupled targets (own network and coder partners)</li>
     * <li>4. per sending wireless port in these networks: receivers of its frequency</li>
     * <li>5. duplicates removed; validity is the minimum over all parts</li>
     * </ul>
     */
    public List<Target> reachableTargets(ServerLevel level, TransportType type, Network network) {
        if (type.behavior() != TransportType.Behavior.QUANTITY) {
            return targets(level, type, network);
        }
        syncEpoch();
        long now = level.getGameTime();
        String key = "full:" + type.id() + "#" + network.id();
        CachedTargets cached = targetCache.get(key);
        if (cached != null && now < cached.validUntil()) {
            return cached.targets();
        }

        CachedTargets coupled = coupledTargets(level, type, network);
        List<Network> networks = new ArrayList<>();
        networks.add(network);
        if (registry.hasLayer(TransportType.DIGITAL.id())) {
            networks.addAll(partnerNetworks(level, type, network));
        }
        java.util.Set<Target> all = new java.util.LinkedHashSet<>(coupled.targets());
        long validUntil = coupled.validUntil();
        WirelessRegistry wireless = WirelessRegistry.get(level);
        for (Network part : networks) {
            List<BlockPos> endpoints = new ArrayList<>();
            for (BlockCoord coord : part.endpointPositions()) {
                endpoints.add(toPos(coord));
            }
            endpoints.sort((a, b) -> Long.compare(a.asLong(), b.asLong()));
            for (BlockPos pos : endpoints) {
                if (!level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof WirelessBlock)
                        || !wireless.mode(level, pos, type).sends()) {
                    continue;
                }
                WirelessRegistry.Receivers receivers = wireless.receivers(level.getServer(), type,
                        wireless.frequency(level, pos), level, upgrades(pos));
                all.addAll(receivers.targets());
                if (!receivers.complete()) {
                    validUntil = Math.min(validUntil, now + INCOMPLETE_LIFETIME);
                }
            }
        }
        CachedTargets combined = new CachedTargets(List.copyOf(all), validUntil);
        targetCache.put(key, combined);
        return combined.targets();
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

    /** Clears target cache and back-off state when the epoch changed. */
    private void syncEpoch() {
        if (cacheEpoch != epoch) {
            targetCache.clear();
            backoff.clear();
            cacheEpoch = epoch;
        }
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
                if (conduit.connection(state, side) == Connection.OUTPUT
                        && (conduit.transportTypes().size() == 1
                        || hasPort(level, type, pos.relative(side), side.getOpposite()))) {
                    result.add(new Target(level, pos, side, pos.relative(side), effectiveSettings(pos, side)));
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
     * @return {@code {inputs, outputs}}
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
                    if (connection == Connection.INPUT) {
                        sources++;
                    } else if (connection == Connection.OUTPUT) {
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

        ListTag savedSettings = new ListTag();
        for (Map.Entry<PortKey, PortSettings> entry : settings.entries().entrySet()) {
            PortSettings value = entry.getValue();
            CompoundTag item = new CompoundTag();
            item.putLong(TAG_POS, toPos(entry.getKey().pos()).asLong());
            item.putByte(TAG_SIDE, (byte) entry.getKey().side().ordinal());
            item.putInt(TAG_PRIORITY, value.priority());
            item.putString(TAG_MODE, value.mode().id());
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
        for (Map.Entry<BlockCoord, Upgrades> entry : upgrades.entries().entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putLong(TAG_POS, toPos(entry.getKey()).asLong());
            item.putIntArray(TAG_COUNTS, entry.getValue().toArray());
            savedUpgrades.add(item);
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
        return tag;
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
     * <li>3. limits: restore non-negative own limits</li>
     * <li>4. settings: restore priority, mode (default when unknown) and filter; skip invalid sides</li>
     * <li>5. pointers, upgrades, frequencies (positive only): restore</li>
     * </ul>
     */
    private static LevelNetworks load(String dimension, CompoundTag tag) {
        LevelNetworks networks = new LevelNetworks(dimension);
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
                    target.set(networks.coord(BlockPos.of(positions[n])), values[n]);
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
            networks.settings.set(key, new PortSettings(item.getInt(TAG_PRIORITY),
                    mode == null ? DistributionMode.DEFAULT : mode,
                    new ResourceFilter(item.getBoolean(TAG_BLACKLIST), ids)));
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
            networks.upgrades.set(networks.coord(BlockPos.of(item.getLong(TAG_POS))),
                    Upgrades.of(item.getIntArray(TAG_COUNTS)));
        }
        ListTag savedFrequencies = tag.getList(TAG_FREQUENCIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < savedFrequencies.size(); i++) {
            CompoundTag item = savedFrequencies.getCompound(i);
            if (item.getInt(TAG_VALUE) > 0) {
                networks.frequencies.put(item.getLong(TAG_POS), item.getInt(TAG_VALUE));
            }
        }
        Vectrum.LOGGER.debug("Loaded networks of {}: {} nodes, {} port mode entries",
                dimension, total, networks.modes.size());
        return networks;
    }
}
