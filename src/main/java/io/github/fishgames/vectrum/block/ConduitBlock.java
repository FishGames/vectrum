package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.network.NodeKind;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.logistics.Signals;
import io.github.fishgames.vectrum.logistics.Transport;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

    /** Ticks zwischen zwei Kontrollen eines Redstone-Bausteins (Aenderungen wirken sofort, dies ist nur die Absicherung). */
    public static final int SIGNAL_INTERVAL = 20;

    /** Maße in Pixeln (16 = ein Block), siehe {@link CableShapes#build}. */
    public record ShapeSpec(double coreLo, double coreHi, double armLo, double armHi,
                            double plateLo, double plateHi, double plateDepth) {
        VoxelShape build(int key) {
            return CableShapes.build(key & 63, key >> 6, coreLo, coreHi, armLo, armHi, plateLo, plateHi, plateDepth);
        }
    }

    private final List<TransportType> types;
    /** Fuehrt dieser Baustein Redstone-Signale (statt Mengen)? Redstone-Kabel fuehren nur dieses eine Signal. */
    private final boolean signal;
    private final ShapeSpec selectionSpec;
    private final ShapeSpec collisionSpec;
    private final ConcurrentMap<Integer, VoxelShape> selectionShapes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, VoxelShape> collisionShapes = new ConcurrentHashMap<>();

    protected ConduitBlock(List<TransportType> types, Properties properties, ShapeSpec selection, ShapeSpec collision) {
        super(properties);
        this.types = List.copyOf(types);
        this.signal = types.size() == 1 && types.get(0).behavior() == TransportType.Behavior.SIGNAL;
        this.selectionSpec = selection;
        this.collisionSpec = collision;
        BlockState state = stateDefinition.any();
        for (EnumProperty<Connection> property : SIDES) {
            state = state.setValue(property, Connection.NONE);
        }
        registerDefaultState(state);
    }

    @Override
    public List<TransportType> transportTypes() {
        return types;
    }

    /** Signal-Baustein (Redstone)? Er bewegt keine Mengen, hat kein Durchsatzlimit und keine Upgrades. */
    public boolean isSignalBlock() {
        return signal;
    }

    /** Nimmt der Baustein Upgrades an? Nur Mengen-Typen haben etwas zum Verbessern. */
    public boolean acceptsUpgrades() {
        return !signal;
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

    /** Hat dieser Baustein eine Seite mit angeschlossenem Inventar (Eingang oder Ausgang)? Nur dann nimmt er Upgrades. */
    public boolean hasPort(BlockState state) {
        return portMask(state) != 0;
    }

    public boolean hasSource(BlockState state) {
        return maskOf(state, Connection.INPUT) != 0;
    }

    /** Braucht der Baustein einen regelmaessigen Takt? Mengen-Typen: mit Quellseite; Redstone: mit jedem Anschluss. */
    private boolean needsTick(BlockState state) {
        return signal ? hasPort(state) : hasSource(state);
    }

    /** Ticks bis zum naechsten Takt. Redstone reagiert auf Aenderungen selbst; der Takt ist nur ein Sicherheitsnetz. */
    private int tickInterval(LevelNetworks networks, BlockPos pos) {
        return signal ? SIGNAL_INTERVAL : networks.interval(pos);
    }

    /**
     * Art des Knotens im Netz des Typs {@code type}: Endpunkt, sobald ein Inventar dieses Typs angeschlossen ist
     * (oder immer, siehe alwaysActive). Bei Universalkabeln zaehlt nur ein Speicher, der genau diesen Typ bietet;
     * Nachbarn in nicht geladenen Chunks gelten als moeglich.
     */
    public NodeKind nodeKind(Level level, BlockPos pos, BlockState state, TransportType type) {
        if (alwaysActive()) {
            return NodeKind.ENDPOINT;
        }
        if (types.size() == 1) {
            return portMask(state) != 0 ? NodeKind.ENDPOINT : NodeKind.CABLE;
        }
        for (Direction side : Sides.ALL) {
            Connection connection = connection(state, side);
            if (connection != Connection.INPUT && connection != Connection.OUTPUT) {
                continue;
            }
            BlockPos neighbour = pos.relative(side);
            if (!level.hasChunkAt(neighbour) || Ports.find(type, level, neighbour, side.getOpposite()) != null) {
                return NodeKind.ENDPOINT;
            }
        }
        return NodeKind.CABLE;
    }

    /**
     * Traegt den Baustein in die Netze aller seiner Typen ein (je Typ ein eigener Knoten).
     *
     * @param always {@code false}: nur eintragen, wenn sich Art oder Seiten gegenueber dem Netz geaendert haben
     *               (vermeidet unnoetiges Verwerfen von Zwischenspeichern)
     */
    private void syncGraph(ServerLevel level, BlockPos pos, BlockState state, boolean always) {
        LevelNetworks networks = LevelNetworks.get(level);
        int links = linkMask(state);
        for (TransportType type : types) {
            NodeKind kind = nodeKind(level, pos, state, type);
            if (always || !networks.matches(type, pos, kind, links)) {
                networks.put(type, pos, kind, links);
            }
        }
    }

    /** Ein Speicher an der Nachbarposition, der mindestens einen Typ dieses Bausteins bietet, oder {@code null}. */
    private Port findPort(Level level, BlockPos neighbour, Direction neighbourSide) {
        for (TransportType type : types) {
            Port port = Ports.find(type, level, neighbour, neighbourSide);
            if (port != null) {
                return port;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Rollen

    /**
     * Standardrolle einer Seite, solange der Spieler nichts gewaehlt hat. Mengen-Typen: Ausgang (so wird nichts
     * ungewollt aus einer Kiste gezogen). Redstone: Eingang neben einem Block, der Signale abgibt (Hebel, Fackel,
     * Redstone-Block ...), sonst aus; einen Ausgang stellt der Spieler mit dem Schluessel ein.
     */
    public EndpointMode defaultMode(Level level, BlockPos neighbour) {
        if (signal) {
            return level.getBlockState(neighbour).isSignalSource() ? EndpointMode.IN : EndpointMode.OFF;
        }
        return EndpointMode.DEFAULT;
    }

    /** Die gewaehlte oder, ohne Wahl, die Standardrolle der Seite. */
    public EndpointMode effectiveMode(LevelNetworks networks, Level level, BlockPos pos, Direction side) {
        EndpointMode stored = networks.storedMode(pos, side);
        return stored != null ? stored : defaultMode(level, pos.relative(side));
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
            if (NetworkBlock.connectsAny(level.getBlockState(neighbourPos), types)) {
                connection = Connection.LINK;
            } else if (networks != null) {
                EndpointMode mode = effectiveMode(networks, level, pos, side);
                if (mode != EndpointMode.OFF && findPort(level, neighbourPos, side.getOpposite()) != null) {
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
        } else if (level instanceof ServerLevel server) {
            LevelNetworks networks = LevelNetworks.get(server);
            if (signal) {
                // Ein Nachbar hat sich geaendert: Signalwert des Netzes neu bestimmen.
                Signals.update(server, networks, pos);
            }
            if (needsTick(state)) {
                // Sicherheitsnetz: Ein verlorener Takt wird bei jeder Aenderung in der Umgebung neu gestartet.
                server.scheduleTick(pos, this, tickInterval(networks, pos));
            }
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
            syncGraph(server, pos, state, true);
            // Doppelte Eintraege ignoriert Minecraft selbst: pro Block und Position gibt es hoechstens einen.
            if (signal) {
                Signals.update(server, LevelNetworks.get(server), pos);
            }
            if (needsTick(state)) {
                server.scheduleTick(pos, this, tickInterval(LevelNetworks.get(server), pos));
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
            for (TransportType type : types) {
                networks.remove(type, pos);
                networks.clearThroughput(type, pos);
            }
            networks.clearModes(pos);
            networks.clearSettings(pos);
            networks.setSignalOutput(pos, 0);
            // Upgrades stecken im Block und kommen beim Abbauen vollstaendig zurueck.
            Upgrades installed = networks.takeUpgrades(pos);
            for (UpgradeType upgrade : UpgradeType.VALUES) {
                int count = installed.count(upgrade);
                if (count > 0) {
                    Block.popResource(level, pos, new ItemStack(ModItems.upgrade(upgrade), count));
                }
            }
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
        for (TransportType type : types) {
            if (!networks.isRegistered(type, pos)) {
                syncGraph(level, pos, current, true);
                break;
            }
        }
        if (!needsTick(current)) {
            return;
        }
        if (!signal) {
            Transport.run(level, pos, current, this);
        }
        level.scheduleTick(pos, this, tickInterval(networks, pos));
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
        return level.hasChunkAt(neighbour) && findPort(level, neighbour, side.getOpposite()) != null;
    }

    /** Sprachschluessel der Rolle; Redstone-Bausteine sagen "Signal" statt "Ware". */
    public String roleKey(EndpointMode mode) {
        return signal && mode != EndpointMode.OFF ? mode.translationKey() + "_signal" : mode.translationKey();
    }

    /** Setzt die Rolle einer Seite (Wrench und Befehl) und passt den Baustein an. */
    public void setRole(ServerLevel level, BlockPos pos, Direction side, EndpointMode mode) {
        LevelNetworks.get(level).setMode(pos, side, mode);
        refresh(level, pos);
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
        EndpointMode next = effectiveMode(networks, level, pos, side).next();
        setRole(server, pos, side, next);
        player.displayClientMessage(Component.translatable("message.vectrum.endpoint_mode",
                sideName, Component.translatable(roleKey(next))), true);
    }

    // ------------------------------------------------------------------ Redstone-Ausgabe

    /** Redstone-Kabel geben nur an ihren Ausgangsseiten ein Signal ab (wie ein Redstone-Block, aber ueber das Netz). */
    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState state) {
        return signal && maskOf(state, Connection.OUTPUT) != 0;
    }

    /**
     * @param direction Richtung vom lesenden Block zu diesem Baustein; die Seite dieses Bausteins, die ihm zugewandt
     *                  ist, ist also {@code direction.getOpposite()}
     */
    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!signal || !(level instanceof ServerLevel server) || connection(state, direction.getOpposite()) != Connection.OUTPUT) {
            return 0;
        }
        return LevelNetworks.get(server).signalOutput(pos);
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
