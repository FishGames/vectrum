package io.github.fishgames.vectrum.world;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.block.ConduitBlock;
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
 * Die Netzwerke einer Dimension. Kabel haben keinen Blockentity, deshalb merkt sich dieses Objekt, welche
 * Positionen Netzknoten sind. Es wird mit der Welt gespeichert (Datei {@code data/vectrum_networks.dat} im
 * Dimensionsordner) und beim Laden der Dimension in Millisekunden neu aufgebaut.
 *
 * <p>Die eigentliche Netzlogik (Verschmelzen, Teilen) steckt im Kern ({@link NetworkGraph}); diese Klasse ist nur
 * die Verbindung zwischen dem Kern und Minecraft.
 *
 * <p>Hinweis für neuere Minecraft-Versionen: ab 1.20.2 hat {@code computeIfAbsent} eine andere Signatur.
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
    private static final byte UNSET_MODE = -1;
    private static final int ENDPOINT_FLAG = 0x40;
    private static final int MASK_BITS = 0x3F;

    /**
     * Zählt jede Änderung an Netzen oder Endpunkt-Einstellungen hoch. Zwischengespeicherte Ziellisten gelten nur,
     * solange der Wert gleich bleibt. Bewusst statisch: Zugriff nur aus dem Server-Thread, und ein Aufruf darf nie
     * die Datenspeicherung anfassen (z. B. beim Entladen von Chunks).
     */
    private static long epoch;

    private final String dimension;
    private final NetworkRegistry registry = new NetworkRegistry();
    /** Gewählte Rollen der Anschlussseiten, nur für Bausteine, die vom Standard abweichen. Schlüssel: BlockPos.asLong(). */
    private final Map<Long, EndpointMode[]> modes = new HashMap<>();

    /** Aktuelle Redstone-Ausgaenge (nur Werte ungleich 0, nicht gespeichert: sie werden beim Takt neu berechnet). */
    private final Map<Long, Integer> signalOutputs = new HashMap<>();

    /** Durchsatzlimits je Ebene (Schluessel: Ebenen-Kennung). Nur Bausteine mit eigenem Wert stehen darin. */
    private final Map<String, ThroughputLimits> limits = new HashMap<>();

    /** Zwischengespeicherte Zielliste. Unvollstaendige Listen (Endpunkte in nicht geladenen Chunks) laufen ab. */
    private record CachedTargets(List<Target> targets, long validUntil) {
    }

    /** So lange (in Ticks) gilt eine unvollstaendige Zielliste, bevor sie neu berechnet wird. */
    private static final long INCOMPLETE_LIFETIME = 20;

    /** Einstellungen der Anschlussseiten (Prioritaet, Verteilmodus, Filter); nur Abweichungen vom Standard. */
    private final PortSettingsTable settings = new PortSettingsTable();
    /** Upgrades der Bausteine (nur Bausteine mit mindestens einem Upgrade). */
    private final UpgradeTable upgrades = new UpgradeTable();
    /** Rundlaufzeiger je Quellseite. */
    private final RoundRobinPointers pointers = new RoundRobinPointers();

    /** Wartezeit fuer ein Ziel, das trotz Lieferung an andere nichts angenommen hat. Fluechtig, nicht gespeichert. */
    private record SleepKey(long source, int sourceSide, long target, int targetSide) {
    }

    private record Backoff(int failures, long until) {
    }

    /** Ab dieser Zahl aufeinanderfolgender Fehlversuche wird die Wartezeit nicht weiter verdoppelt. */
    private static final long MAX_SLEEP_TICKS = 100;
    private final Map<SleepKey, Backoff> backoff = new HashMap<>();

    private long cacheEpoch = -1;
    /** Schluessel: Ebenen-Kennung + Netznummer (Netznummern gelten nur innerhalb einer Ebene). */
    private final Map<String, CachedTargets> targetCache = new HashMap<>();

    private LevelNetworks(String dimension) {
        this.dimension = dimension;
    }

    public static LevelNetworks get(ServerLevel level) {
        String dimension = level.dimension().location().toString();
        return level.getDataStorage().computeIfAbsent(
                tag -> load(dimension, tag), () -> new LevelNetworks(dimension), DATA_NAME);
    }

    /** Muss aufgerufen werden, wenn sich etwas ändert, das Zwischenspeicher ungültig macht. */
    public static void invalidateCaches() {
        epoch++;
    }

    // ------------------------------------------------------------------ Knoten

    public BlockCoord coord(BlockPos pos) {
        return new BlockCoord(dimension, pos.getX(), pos.getY(), pos.getZ());
    }

    /** Legt den Knoten an oder aktualisiert seine offenen Seiten. */
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

    /** Steht der Knoten schon genau so (Art und Seiten) im Netz des Typs? */
    public boolean matches(TransportType type, BlockPos pos, NodeKind kind, int sideMask) {
        NodeInfo info = registry.graph(type.id()).info(coord(pos));
        return info != null && info.kind() == kind && info.sideMask() == (sideMask & MASK_BITS);
    }

    public boolean isRegistered(TransportType type, BlockPos pos) {
        return registry.graph(type.id()).contains(coord(pos));
    }

    public void remove(TransportType type, BlockPos pos) {
        if (registry.graph(type.id()).remove(coord(pos))) {
            changed();
        }
    }

    // ------------------------------------------------------------------ Rollen der Anschlussseiten

    /**
     * Die vom Spieler gewaehlte Rolle der Seite eines Bausteins oder {@code null}, wenn er nichts gewaehlt hat. Dann
     * gilt der Standard des Bausteins (siehe {@code ConduitBlock#effectiveMode}).
     */
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

    /** Vergisst die gewaehlten Rollen dieses Bausteins (beim Abbauen). */
    public void clearModes(BlockPos pos) {
        if (modes.remove(pos.asLong()) != null) {
            changed();
        }
    }

    // ------------------------------------------------------------------ Redstone-Ausgaenge

    /** Signalstaerke, die dieser Baustein gerade an seinen Ausgangsseiten abgibt (0 bis 15, fluechtig). */
    public int signalOutput(BlockPos pos) {
        return signalOutputs.getOrDefault(pos.asLong(), 0);
    }

    /** @return {@code true}, wenn sich der Wert geaendert hat */
    public boolean setSignalOutput(BlockPos pos, int strength) {
        int before = signalOutput(pos);
        if (strength <= 0) {
            signalOutputs.remove(pos.asLong());
        } else {
            signalOutputs.put(pos.asLong(), strength);
        }
        return before != Math.max(0, strength);
    }

    // ------------------------------------------------------------------ Einstellungen der Anschlussseiten (Etappe 5)

    private PortKey key(BlockPos pos, Direction side) {
        return new PortKey(coord(pos), io.github.fishgames.vectrum.core.network.Direction.VALUES[side.get3DDataValue()]);
    }

    /** Prioritaet, Verteilmodus und Filter dieser Seite; ohne eigene Wahl der Standard. */
    public PortSettings settings(BlockPos pos, Direction side) {
        return settings.get(key(pos, side));
    }

    public void setSettings(BlockPos pos, Direction side, PortSettings value) {
        if (settings.set(key(pos, side), value)) {
            changed();
        }
    }

    /** Rundlaufzeiger dieser Quellseite. */
    public int pointer(BlockPos pos, Direction side) {
        return pointers.get(key(pos, side));
    }

    public void setPointer(BlockPos pos, Direction side, int value) {
        pointers.set(key(pos, side), value);
        setDirty(); // aendert kein Netz: Zwischenspeicher bleiben gueltig
    }

    /** Vergisst Einstellungen und Rundlaufzeiger dieses Bausteins (beim Abbauen). */
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

    // ------------------------------------------------------------------ Upgrades (Etappe 6)

    /** Die Upgrades dieses Bausteins. */
    public Upgrades upgrades(BlockPos pos) {
        return upgrades.get(coord(pos));
    }

    /**
     * Setzt die Upgrades eines Bausteins und rechnet ihre Wirkung einmal aus: Aendert sich die Zahl der
     * Durchsatz-Upgrades, wird das Durchsatzlimit neu gespeichert (Grundlimit x 4^Anzahl); Filter und Prioritaet
     * werden freigeschaltet oder gesperrt. Beim Takt wird nur nachgeschlagen (K3).
     */
    public void setUpgrades(List<TransportType> types, BlockPos pos, Upgrades value) {
        Upgrades before = upgrades.get(coord(pos));
        if (!upgrades.set(coord(pos), value)) {
            return;
        }
        if (before.count(UpgradeType.THROUGHPUT) != value.count(UpgradeType.THROUGHPUT)) {
            for (TransportType type : types) { // Universalkabel: jeder Typ hat sein eigenes Limit
                setThroughput(type, pos, UpgradeEffects.throughput(baseLimit(type.id()), value));
            }
        }
        changed(); // Filter und Prioritaet koennen frei- oder gesperrt worden sein: Zielliste neu berechnen
    }

    /** Entnimmt alle Upgrades eines Bausteins (beim Abbauen, damit sie zurueckgegeben werden koennen). */
    public Upgrades takeUpgrades(BlockPos pos) {
        Upgrades taken = upgrades.take(coord(pos));
        if (!taken.isEmpty()) {
            changed();
        }
        return taken;
    }

    /** Ticks zwischen zwei Uebergaben dieser Quelle (Grundwert, verkuerzt durch Speed-Upgrades). */
    public int interval(BlockPos pos) {
        return UpgradeEffects.interval(ConduitBlock.INTERVAL, upgrades(pos));
    }

    /** Wie viele Item-Sorten pro Uebergabe an ein Ziel bewegt werden duerfen. */
    public int maxTypes(BlockPos pos) {
        return UpgradeEffects.maxTypes(upgrades(pos));
    }

    /**
     * Die Einstellungen, die tatsaechlich wirken: Ohne Filter-Upgrade gilt kein Filter, ohne Prioritaets-Upgrade
     * Prioritaet 0. Die gespeicherten Werte bleiben erhalten und wirken wieder, sobald das Upgrade steckt.
     */
    public PortSettings effectiveSettings(BlockPos pos, Direction side) {
        PortSettings stored = settings(pos, side);
        if (stored.isDefault()) {
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

    // ------------------------------------------------------------------ Wartezeit fuer volle Ziele

    /** Wartet dieses Ziel gerade, weil es zuletzt nichts angenommen hat, waehrend andere Ziele beliefert wurden? */
    public boolean isSleeping(BlockPos source, Direction sourceSide, Target target, long now) {
        syncEpoch();
        Backoff entry = backoff.get(sleepKey(source, sourceSide, target));
        return entry != null && now < entry.until();
    }

    /** Das Ziel hat trotz Angebot nichts angenommen, obwohl die Quelle an andere lieferte: naechster Versuch spaeter. */
    public void noteMiss(BlockPos source, Direction sourceSide, Target target, long now) {
        syncEpoch();
        SleepKey key = sleepKey(source, sourceSide, target);
        Backoff before = backoff.get(key);
        int failures = before == null ? 1 : before.failures() + 1;
        long delay = Math.min(MAX_SLEEP_TICKS, 10L << Math.min(failures, 10)); // 20, 40, 80, 100, ...
        backoff.put(key, new Backoff(failures, now + delay));
    }

    /** Das Ziel hat etwas angenommen: die Wartezeit ist vorbei. */
    public void noteHit(BlockPos source, Direction sourceSide, Target target) {
        if (!backoff.isEmpty()) {
            backoff.remove(sleepKey(source, sourceSide, target));
        }
    }

    private static SleepKey sleepKey(BlockPos source, Direction sourceSide, Target target) {
        return new SleepKey(source.asLong(), sourceSide.get3DDataValue(), target.endpoint().asLong(),
                target.side().get3DDataValue());
    }

    // ------------------------------------------------------------------ Durchsatzlimit (K3)

    /** Grundlimit einer Ebene: Einheiten pro Uebergabe und Quellseite, solange kein Upgrade etwas anderes setzt. */
    private static long baseLimit(String layer) {
        return TransportDefaults.baseThroughput(layer);
    }

    private ThroughputLimits limits(String layer) {
        return limits.computeIfAbsent(layer, id -> new ThroughputLimits(baseLimit(id)));
    }

    /** Durchsatzlimit des Bausteins (ein einziger Nachschlag, keine Berechnung ueber das Netz). */
    public long throughput(TransportType type, BlockPos pos) {
        return limits(type.id()).limitOf(coord(pos));
    }

    /** Wie {@link #throughput}, fuer fremde Schnittstellen auf {@code int} geklemmt. */
    public int budget(TransportType type, BlockPos pos) {
        return limits(type.id()).budgetOf(coord(pos));
    }

    /** Setzt das Limit eines Bausteins. Das aendert kein Netz und macht keine Zwischenspeicher ungueltig. */
    public void setThroughput(TransportType type, BlockPos pos, long limit) {
        if (limits(type.id()).set(coord(pos), limit)) {
            setDirty();
        }
    }

    /** Vergisst das eigene Limit eines Bausteins (beim Abbauen). */
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

    /** Das Netz, in dem dieser Baustein liegt, oder {@code null}. */
    public Network networkAt(TransportType type, BlockPos pos) {
        return registry.networkAt(type.id(), coord(pos));
    }

    private void changed() {
        setDirty();
        invalidateCaches();
    }

    // ------------------------------------------------------------------ Ziele

    /**
     * Alle Ziele (Anschluss-Seiten mit Ausgang) des Netzes, nach Position sortiert. Die Liste wird zwischengespeichert
     * und nur neu berechnet, wenn sich seit dem letzten Aufruf etwas geaendert hat. Endpunkte in nicht geladenen
     * Chunks fehlen; solche unvollstaendigen Listen werden nach einer Sekunde neu berechnet, damit ein spaeter
     * geladener Endpunkt sicher auftaucht.
     */
    public List<Target> targets(ServerLevel level, TransportType type, Network network) {
        syncEpoch();
        long now = level.getGameTime();
        String key = type.id() + "#" + network.id();
        CachedTargets cached = targetCache.get(key);
        if (cached != null && now < cached.validUntil()) {
            return cached.targets();
        }

        boolean[] complete = {true};
        List<Target> targets = buildTargets(level, type, network, complete);
        targetCache.put(key, new CachedTargets(targets, complete[0] ? Long.MAX_VALUE : now + INCOMPLETE_LIFETIME));
        return targets;
    }

    /** Verwirft Zwischenspeicher und Wartezeiten, wenn sich seit dem letzten Aufruf etwas geaendert hat. */
    private void syncEpoch() {
        if (cacheEpoch != epoch) {
            targetCache.clear();
            backoff.clear();
            cacheEpoch = epoch;
        }
    }

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
                    result.add(new Target(pos, side, pos.relative(side), effectiveSettings(pos, side)));
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean hasPort(ServerLevel level, TransportType type, BlockPos neighbour, Direction neighbourSide) {
        return level.hasChunkAt(neighbour) && Ports.find(type, level, neighbour, neighbourSide) != null;
    }

    /**
     * Zaehlt fuer die Diagnose die Quell- und Zielseiten eines Netzes (nur in geladenen Chunks).
     *
     * @return {@code {Eingaenge, Ausgaenge}}
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

    // ------------------------------------------------------------------ Speichern

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
                // Aeltere Staende speicherten "unberuehrt" als Standard-Rolle; das verhaelt sich unveraendert.
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
        Vectrum.LOGGER.debug("Netzwerke von {} geladen: {} Bausteine, {} Anschlusseinstellungen",
                dimension, total, networks.modes.size());
        return networks;
    }
}
