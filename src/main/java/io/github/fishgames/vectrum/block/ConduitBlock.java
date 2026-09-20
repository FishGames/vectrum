package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.network.NodeKind;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.logistics.ItemTransport;
import io.github.fishgames.vectrum.transfer.ItemPorts;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Gemeinsame Grundlage von Kabel und Endpunkt. Beide sind Netzknoten ohne Blockentity:
 * <ul>
 *   <li>Der BlockState zeigt pro Seite, was dort ist ({@link Connection}): nichts, Kabel/Endpunkt (Teil des Netzes),
 *       oder ein angeschlossenes Inventar als Eingang (Quelle) oder Ausgang (Ziel).</li>
 *   <li>Die gewählte Rolle je Seite steht in {@link LevelNetworks}, weil nur wenige Bausteine eine haben.</li>
 *   <li>Wer mindestens ein Inventar angeschlossen hat, ist im Netz ein "Endpunkt" ({@link NodeKind#ENDPOINT}) und
 *       nimmt am Transport teil. Quellen takten sich selbst über geplante Block-Ticks.</li>
 * </ul>
 * Ein Kabel wird dadurch an seinen Enden von selbst zum Anschluss; ein Endpunkt-Block ist immer aktiv.
 */
public abstract class ConduitBlock extends Block implements NetworkBlock {
    public static final EnumProperty<Connection> DOWN = EnumProperty.create("down", Connection.class);
    public static final EnumProperty<Connection> UP = EnumProperty.create("up", Connection.class);
    public static final EnumProperty<Connection> NORTH = EnumProperty.create("north", Connection.class);
    public static final EnumProperty<Connection> SOUTH = EnumProperty.create("south", Connection.class);
    public static final EnumProperty<Connection> WEST = EnumProperty.create("west", Connection.class);
    public static final EnumProperty<Connection> EAST = EnumProperty.create("east", Connection.class);
    /** Reihenfolge wie {@link Sides#ALL}: unten, oben, Norden, Süden, Westen, Osten. */
    public static final List<EnumProperty<Connection>> SIDES = List.of(DOWN, UP, NORTH, SOUTH, WEST, EAST);

    /** Ticks zwischen zwei Übergaben einer Quelle. */
    public static final int INTERVAL = 10;
    /** Höchstmenge Items pro Übergabe und Quellseite im Basisbetrieb (spätere Durchsatz-Upgrades erhöhen sie). */
    public static final int BASE_THROUGHPUT = 4;

    /** Maße in Pixeln (16 = ein Block), siehe {@link CableShapes#build}. */
    public record ShapeSpec(double coreLo, double coreHi, double armLo, double armHi,
                            double plateLo, double plateHi, double plateDepth) {
        VoxelShape build(int key) {
            return CableShapes.build(key & 63, key >> 6, coreLo, coreHi, armLo, armHi, plateLo, plateHi, plateDepth);
        }
    }

    private final TransportType type;
    private final ShapeSpec selectionSpec;
    private final ShapeSpec collisionSpec;
    private final ConcurrentMap<Integer, VoxelShape> selectionShapes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, VoxelShape> collisionShapes = new ConcurrentHashMap<>();

    protected ConduitBlock(TransportType type, Properties properties, ShapeSpec selection, ShapeSpec collision) {
        super(properties);
        this.type = type;
        this.selectionSpec = selection;
        this.collisionSpec = collision;
        BlockState state = stateDefinition.any();
        for (EnumProperty<Connection> property : SIDES) {
            state = state.setValue(property, Connection.NONE);
        }
        registerDefaultState(state);
    }

    @Override
    public TransportType transportType() {
        return type;
    }

    /** {@code true}: der Block ist immer ein aktiver Endpunkt, auch ohne angeschlossenes Inventar. */
    protected abstract boolean alwaysActive();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    // ------------------------------------------------------------------ Zustand lesen

    public Connection connection(BlockState state, Direction side) {
        return state.getValue(SIDES.get(side.get3DDataValue()));
    }

    /** Bitmaske der Seiten, die mit dem Netz verbunden sind (Kabel oder Endpunkt), für den Netzwerk-Kern. */
    public int linkMask(BlockState state) {
        return maskOf(state, Connection.LINK);
    }

    private int portMask(BlockState state) {
        return maskOf(state, Connection.INPUT) | maskOf(state, Connection.OUTPUT);
    }

    private int maskOf(BlockState state, Connection wanted) {
        int mask = 0;
        for (int i = 0; i < SIDES.size(); i++) {
            if (state.getValue(SIDES.get(i)) == wanted) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    public boolean hasSource(BlockState state) {
        return maskOf(state, Connection.INPUT) != 0;
    }

    /** Art des Knotens im Netz: Endpunkt, sobald ein Inventar angeschlossen ist (oder immer, siehe alwaysActive). */
    public NodeKind nodeKind(BlockState state) {
        return alwaysActive() || portMask(state) != 0 ? NodeKind.ENDPOINT : NodeKind.CABLE;
    }

    // ------------------------------------------------------------------ Zustand berechnen

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    /**
     * Berechnet für jede Seite, was dort ist: Kabel/Endpunkt (LINK), ein Inventar mit der gewählten Rolle
     * (INPUT/OUTPUT) oder nichts. Nachbarn in nicht geladenen Chunks werden nicht angefasst; dort bleibt der alte
     * Wert (sonst würden Chunks geladen oder Verbindungen am Chunkrand verloren gehen). Auf dem Client werden keine
     * Inventare gesucht; dort zählt nur, was der Server schickt.
     */
    private BlockState computeState(Level level, BlockPos pos, BlockState state) {
        LevelNetworks networks = level instanceof ServerLevel server ? LevelNetworks.get(server) : null;
        for (int i = 0; i < SIDES.size(); i++) {
            Direction side = Sides.ALL[i];
            BlockPos neighbourPos = pos.relative(side);
            if (!level.hasChunkAt(neighbourPos)) {
                continue;
            }
            Connection connection = Connection.NONE;
            if (NetworkBlock.connects(level.getBlockState(neighbourPos), type)) {
                connection = Connection.LINK;
            } else if (networks != null) {
                EndpointMode mode = networks.mode(pos, side);
                if (mode != EndpointMode.OFF && ItemPorts.find(level, neighbourPos, side.getOpposite()) != null) {
                    connection = mode == EndpointMode.IN ? Connection.INPUT : Connection.OUTPUT;
                }
            }
            state = state.setValue(SIDES.get(i), connection);
        }
        return state;
    }

    /** Prüft die Umgebung neu und passt den BlockState an, falls sich etwas geändert hat. */
    public void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != this) {
            return;
        }
        BlockState updated = computeState(level, pos, state);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        } else if (hasSource(state) && level instanceof ServerLevel server) {
            // Sicherheitsnetz: Ein verlorener Takt wird bei jeder Aenderung in der Umgebung neu gestartet.
            server.scheduleTick(pos, this, INTERVAL);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        if (!level.isClientSide) {
            refresh(level, pos);
        }
    }

    // ------------------------------------------------------------------ Netz und Takt

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel server) {
            LevelNetworks.get(server).put(type, pos, nodeKind(state), linkMask(state));
            // Doppelte Eintraege ignoriert Minecraft selbst: pro Block und Position gibt es hoechstens einen.
            if (hasSource(state)) {
                server.scheduleTick(pos, this, INTERVAL);
            } else if (!oldState.is(this)) {
                // Neu gesetzt (z. B. per /setblock oder Struktur): einmal die Umgebung pruefen.
                server.scheduleTick(pos, this, 1);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            LevelNetworks networks = LevelNetworks.get(server);
            networks.remove(type, pos);
            networks.clearModes(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Takt: prueft die Umgebung, meldet den Baustein bei Bedarf im Netz an (Selbstheilung, falls die Netzdatei fehlt
     * oder veraltet ist), uebergibt Ware und plant den naechsten Takt, solange es noch eine Quellseite gibt.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        refresh(level, pos);
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() != this) {
            return;
        }
        LevelNetworks networks = LevelNetworks.get(level);
        if (!networks.isRegistered(type, pos)) {
            networks.put(type, pos, nodeKind(current), linkMask(current));
        }
        if (!hasSource(current)) {
            return;
        }
        ItemTransport.run(level, pos, current, this);
        level.scheduleTick(pos, this, INTERVAL);
    }

    // ------------------------------------------------------------------ Wrench

    /**
     * Welche Seite meint ein Klick mit dem Wrench? Die Seite zum Inventar liegt direkt an der Kiste oder Maschine und
     * lässt sich nicht anklicken. Deshalb gilt: Von allen Seiten mit angeschlossenem Inventar (auch abgeschaltete)
     * wird die genommen, die der Klickstelle am nächsten liegt. Gibt es keine, zählt die angeklickte Fläche.
     */
    public Direction pickSide(Level level, BlockPos pos, BlockState state, Vec3 hit, Direction clickedFace) {
        double x = hit.x - pos.getX() - 0.5;
        double y = hit.y - pos.getY() - 0.5;
        double z = hit.z - pos.getZ() - 0.5;

        Direction best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Direction side : Sides.ALL) {
            if (connection(state, side) == Connection.LINK || !hasInventory(level, pos, state, side)) {
                continue;
            }
            double score = x * side.getStepX() + y * side.getStepY() + z * side.getStepZ();
            if (score > bestScore) {
                bestScore = score;
                best = side;
            }
        }
        return best != null ? best : clickedFace;
    }

    private boolean hasInventory(Level level, BlockPos pos, BlockState state, Direction side) {
        if (connection(state, side) != Connection.NONE) {
            return connection(state, side) != Connection.LINK;
        }
        BlockPos neighbour = pos.relative(side);
        return level.hasChunkAt(neighbour) && ItemPorts.find(level, neighbour, side.getOpposite()) != null;
    }

    /** Schaltet die Rolle der Seite weiter (Ausgang, Eingang, Aus) und meldet das Ergebnis in der Aktionsleiste. */
    public void onWrench(Level level, BlockPos pos, Player player, Direction side) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        Component sideName = Component.translatable("direction.vectrum." + side.getName());
        if (connection(state, side) == Connection.LINK) {
            player.displayClientMessage(Component.translatable("message.vectrum.endpoint_link", sideName), true);
            return;
        }
        if (!hasInventory(level, pos, state, side)) {
            player.displayClientMessage(Component.translatable("message.vectrum.no_inventory"), true);
            return;
        }
        LevelNetworks networks = LevelNetworks.get(server);
        EndpointMode next = networks.mode(pos, side).next();
        networks.setMode(pos, side, next);
        refresh(level, pos);
        player.displayClientMessage(Component.translatable("message.vectrum.endpoint_mode",
                sideName, Component.translatable(next.translationKey())), true);
    }

    // ------------------------------------------------------------------ Form

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int key = linkMask(state) | (portMask(state) << 6);
        return selectionShapes.computeIfAbsent(key, selectionSpec::build);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int key = linkMask(state) | (portMask(state) << 6);
        return collisionShapes.computeIfAbsent(key, collisionSpec::build);
    }
}
