package io.github.fishgames.vectrum.world;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.block.EndpointBlock;
import io.github.fishgames.vectrum.core.network.BlockCoord;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.core.network.NetworkGraph;
import io.github.fishgames.vectrum.core.network.NetworkRegistry;
import io.github.fishgames.vectrum.core.network.NodeInfo;
import io.github.fishgames.vectrum.core.network.NodeKind;
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

    /** Zwischengespeicherte Zielliste. Unvollstaendige Listen (Endpunkte in nicht geladenen Chunks) laufen ab. */
    private record CachedTargets(List<ItemTarget> targets, long validUntil) {
    }

    /** So lange (in Ticks) gilt eine unvollstaendige Zielliste, bevor sie neu berechnet wird. */
    private static final long INCOMPLETE_LIFETIME = 20;

    private long cacheEpoch = -1;
    private final Map<Long, CachedTargets> itemTargets = new HashMap<>();

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
        } else {
            graph.add(coord, kind, sideMask);
        }
        changed();
    }

    public void remove(TransportType type, BlockPos pos) {
        if (registry.graph(type.id()).remove(coord(pos))) {
            changed();
        }
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
        CachedTargets cached = itemTargets.get(network.id());
        if (cached != null && now < cached.validUntil()) {
            return cached.targets();
        }

        boolean[] complete = {true};
        List<ItemTarget> targets = buildItemTargets(level, network, complete);
        itemTargets.put(network.id(), new CachedTargets(targets,
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
            if (!(state.getBlock() instanceof EndpointBlock endpoint)) {
                continue;
            }
            for (Direction side : Sides.ALL) {
                if (endpoint.connection(state, side) == Connection.OUTPUT) {
                    result.add(new ItemTarget(pos, side, pos.relative(side)));
                }
            }
        }
        return List.copyOf(result);
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
        Vectrum.LOGGER.debug("Netzwerke von {} geladen: {} Bausteine", dimension, total);
        return networks;
    }
}
