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
import io.github.fishgames.vectrum.core.throughput.ThroughputLimits;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.logistics.ItemTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

    /** Durchsatzlimits je Ebene (Schluessel: Ebenen-Kennung). Nur Bausteine mit eigenem Wert stehen darin. */
    private final Map<String, ThroughputLimits> limits = new HashMap<>();

    /** Zwischengespeicherte Zielliste. Unvollstaendige Listen (Endpunkte in nicht geladenen Chunks) laufen ab. */
    private record CachedTargets(List<ItemTarget> targets, long validUntil) {
    }

    /** So lange (in Ticks) gilt eine unvollstaendige Zielliste, bevor sie neu berechnet wird. */
    private static final long INCOMPLETE_LIFETIME = 20;

    /** Netznummern gelten nur innerhalb einer Ebene; der Praefix trennt die Ebenen (bisher gibt es nur Items). */
    private static final String ITEM_KEY_PREFIX = "item:";

    private long cacheEpoch = -1;
    private final Map<String, CachedTargets> itemTargets = new HashMap<>();

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

    public boolean isRegistered(TransportType type, BlockPos pos) {
        return registry.graph(type.id()).contains(coord(pos));
    }

    public void remove(TransportType type, BlockPos pos) {
        if (registry.graph(type.id()).remove(coord(pos))) {
            changed();
        }
    }

    // ------------------------------------------------------------------ Rollen der Anschlussseiten

    /** Rolle der Seite eines Bausteins; ohne eigene Wahl gilt der Standard. */
    public EndpointMode mode(BlockPos pos, Direction side) {
        EndpointMode[] stored = modes.get(pos.asLong());
        return stored == null ? EndpointMode.DEFAULT : stored[side.get3DDataValue()];
    }

    public void setMode(BlockPos pos, Direction side, EndpointMode mode) {
        long key = pos.asLong();
        EndpointMode[] stored = modes.get(key);
        if (stored == null) {
            stored = new EndpointMode[Sides.ALL.length];
            java.util.Arrays.fill(stored, EndpointMode.DEFAULT);
            modes.put(key, stored);
        }
        stored[side.get3DDataValue()] = mode;
        if (isDefault(stored)) {
            modes.remove(key);
        }
        changed();
    }

    /** Vergisst die gewählten Rollen dieses Bausteins (beim Abbauen). */
    public void clearModes(BlockPos pos) {
        if (modes.remove(pos.asLong()) != null) {
            changed();
        }
    }

    // ------------------------------------------------------------------ Durchsatzlimit (K3)

    /** Grundlimit einer Ebene: Einheiten pro Uebergabe und Quellseite, solange kein Upgrade etwas anderes setzt. */
    private static long baseLimit(String layer) {
        return ConduitBlock.BASE_THROUGHPUT;
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

    private static boolean isDefault(EndpointMode[] stored) {
        for (EndpointMode mode : stored) {
            if (mode != EndpointMode.DEFAULT) {
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
     * Alle Ziele (Endpunkt-Seiten mit Ausgang) des Netzes, nach Position sortiert. Die Liste wird zwischengespeichert
     * und nur neu berechnet, wenn sich seit dem letzten Aufruf etwas geaendert hat. Endpunkte in nicht geladenen
     * Chunks fehlen; solche unvollstaendigen Listen werden nach einer Sekunde neu berechnet, damit ein spaeter
     * geladener Endpunkt sicher auftaucht.
     */
    public List<ItemTarget> itemTargets(ServerLevel level, Network network) {
        if (cacheEpoch != epoch) {
            itemTargets.clear();
            cacheEpoch = epoch;
        }
        long now = level.getGameTime();
        CachedTargets cached = itemTargets.get(ITEM_KEY_PREFIX + network.id());
        if (cached != null && now < cached.validUntil()) {
            return cached.targets();
        }

        boolean[] complete = {true};
        List<ItemTarget> targets = buildItemTargets(level, network, complete);
        itemTargets.put(ITEM_KEY_PREFIX + network.id(), new CachedTargets(targets,
                complete[0] ? Long.MAX_VALUE : now + INCOMPLETE_LIFETIME));
        return targets;
    }

    private static List<ItemTarget> buildItemTargets(ServerLevel level, Network network, boolean[] complete) {
        List<BlockPos> endpoints = new ArrayList<>(network.endpointCount());
        for (BlockCoord coord : network.endpointPositions()) {
            endpoints.add(new BlockPos(coord.x(), coord.y(), coord.z()));
        }
        endpoints.sort((a, b) -> Long.compare(a.asLong(), b.asLong()));

        List<ItemTarget> result = new ArrayList<>();
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
                if (conduit.connection(state, side) == Connection.OUTPUT) {
                    result.add(new ItemTarget(pos, side, pos.relative(side)));
                }
            }
        }
        return List.copyOf(result);
    }

    /**
     * Zaehlt fuer die Diagnose die Quell- und Zielseiten eines Netzes (nur in geladenen Chunks).
     *
     * @return {@code {Eingaenge, Ausgaenge}}
     */
    public int[] countPorts(ServerLevel level, Network network) {
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
                stored[i] = (byte) entry.getValue()[i].ordinal();
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
        return tag;
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
                restored[side] = side < stored.length ? EndpointMode.byOrdinal(stored[side]) : EndpointMode.DEFAULT;
            }
            if (!isDefault(restored)) {
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
        Vectrum.LOGGER.debug("Netzwerke von {} geladen: {} Bausteine, {} Anschlusseinstellungen",
                dimension, total, networks.modes.size());
        return networks;
    }
}
