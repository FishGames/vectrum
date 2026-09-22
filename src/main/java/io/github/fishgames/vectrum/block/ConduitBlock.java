package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.network.NodeKind;
import io.github.fishgames.vectrum.gui.EndpointMenu;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.logistics.Signals;
import io.github.fishgames.vectrum.logistics.Transport;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.transfer.RedstonePorts;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base class of cable and endpoint blocks (network nodes without block entity).
 * <ul>
 *   <li>Block state: one {@link Connection} per side (none, link, input, output).</li>
 *   <li>Chosen role per side: stored in {@link LevelNetworks}.</li>
 *   <li>Node with at least one attached inventory: {@link NodeKind#ENDPOINT}.</li>
 *   <li>Sources tick through scheduled block ticks.</li>
 * </ul>
 */
public abstract class ConduitBlock extends Block implements NetworkBlock {
    public static final EnumProperty<Connection> DOWN = EnumProperty.create("down", Connection.class);
    public static final EnumProperty<Connection> UP = EnumProperty.create("up", Connection.class);
    public static final EnumProperty<Connection> NORTH = EnumProperty.create("north", Connection.class);
    public static final EnumProperty<Connection> SOUTH = EnumProperty.create("south", Connection.class);
    public static final EnumProperty<Connection> WEST = EnumProperty.create("west", Connection.class);
    public static final EnumProperty<Connection> EAST = EnumProperty.create("east", Connection.class);
    /** Side properties in {@link Sides#ALL} order: down, up, north, south, west, east. */
    public static final List<EnumProperty<Connection>> SIDES = List.of(DOWN, UP, NORTH, SOUTH, WEST, EAST);

    /** Ticks between two transfers of a source. */
    public static final int INTERVAL = 10;

    /** Ticks between two checks of a redstone block. */
    public static final int SIGNAL_INTERVAL = 20;

    /** Shape measures in pixels (16 = one block), see {@link CableShapes#build}. */
    public record ShapeSpec(double coreLo, double coreHi, double armLo, double armHi,
                            double plateLo, double plateHi, double plateDepth) {
        VoxelShape build(int key) {
            return CableShapes.build(key & 63, key >> 6, coreLo, coreHi, armLo, armHi, plateLo, plateHi, plateDepth);
        }
    }

    private final List<TransportType> types;
    /** Whether this block carries a redstone signal (no quantities). */
    private final boolean signal;
    private final ShapeSpec selectionSpec;
    private final ShapeSpec collisionSpec;
    /** Placement/interaction shape while a matching item is held, or {@code null} for a fixed shape. */
    private final ShapeSpec expandedSelectionSpec;
    private final ConcurrentMap<Integer, VoxelShape> selectionShapes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, VoxelShape> expandedSelectionShapes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, VoxelShape> collisionShapes = new ConcurrentHashMap<>();

    protected ConduitBlock(List<TransportType> types, Properties properties, ShapeSpec selection, ShapeSpec collision) {
        this(types, properties, selection, collision, null);
    }

    /**
     * @param expandedSelection placement/interaction shape used while the player holds the wrench, a cable, a coder
     *                          or a wireless port ({@link #isExpandTrigger}); {@code null} keeps {@code selection}
     *                          fixed regardless of the held item.
     */
    protected ConduitBlock(List<TransportType> types, Properties properties, ShapeSpec selection, ShapeSpec collision,
                           ShapeSpec expandedSelection) {
        super(properties);
        this.types = List.copyOf(types);
        this.signal = types.size() == 1 && types.get(0).behavior() == TransportType.Behavior.SIGNAL;
        this.selectionSpec = selection;
        this.collisionSpec = collision;
        this.expandedSelectionSpec = expandedSelection;
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

    /** Redstone signal block. */
    public boolean isSignalBlock() {
        return signal;
    }

    /** Whether the block accepts upgrades (quantity ports only). */
    public boolean acceptsUpgrades() {
        return !signal && !portless();
    }

    /** {@code true}: the block never has inventory port sides (digital cable, coder). */
    protected boolean portless() {
        return false;
    }

    /** {@code true}: the block is always an active endpoint. */
    protected abstract boolean alwaysActive();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    // State reading

    public Connection connection(BlockState state, Direction side) {
        return state.getValue(SIDES.get(side.get3DDataValue()));
    }

    /** Bit mask of the {@link Connection#LINK} sides. */
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

    /** Whether a side has an attached inventory (input or output). */
    public boolean hasPort(BlockState state) {
        return portMask(state) != 0;
    }

    public boolean hasSource(BlockState state) {
        return maskOf(state, Connection.INPUT) != 0;
    }

    /** Whether the block ticks: quantity types with a source side, redstone with any port. */
    private boolean needsTick(BlockState state) {
        if (portless()) {
            return false;
        }
        return signal ? hasPort(state) : hasSource(state);
    }

    /** Ticks until the next tick. */
    private int tickInterval(LevelNetworks networks, BlockPos pos) {
        return signal ? SIGNAL_INTERVAL : networks.interval(pos);
    }

    /**
     * Node kind in the network of {@code type}.
     * <ul>
     *   <li>{@code alwaysActive()}: endpoint.</li>
     *   <li>Single-type block: endpoint with any input/output side, otherwise cable.</li>
     *   <li>Multi-type block: endpoint when an input/output side faces a port of {@code type}
     *       (or an unloaded chunk), otherwise cable.</li>
     * </ul>
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
     * Registers the block as a node in the network of each of its types.
     *
     * @param always {@code false}: register only when kind or link sides differ from the network
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

    /** First port at the neighbour position offering one of this block's types, or {@code null}. */
    private Port findPort(Level level, BlockPos neighbour, Direction neighbourSide) {
        for (TransportType type : types) {
            Port port = Ports.find(type, level, neighbour, neighbourSide);
            if (port != null) {
                return port;
            }
        }
        return null;
    }

    // Roles

    /**
     * Default role of a side without a chosen role.
     * <ul>
     *   <li>Quantity types: {@link EndpointMode#DEFAULT}.</li>
     *   <li>Redstone: input next to a signal source, output next to a block that processes signals, otherwise
     *       off.</li>
     * </ul>
     */
    public EndpointMode defaultMode(Level level, BlockPos neighbour) {
        if (signal) {
            BlockState state = level.getBlockState(neighbour);
            if (state.isSignalSource()) {
                return EndpointMode.IN;
            }
            return RedstonePorts.processesSignal(state) ? EndpointMode.OUT : EndpointMode.OFF;
        }
        return EndpointMode.DEFAULT;
    }

    /** Chosen role of the side, or the default role. */
    public EndpointMode effectiveMode(LevelNetworks networks, Level level, BlockPos pos, Direction side) {
        EndpointMode stored = networks.storedMode(pos, side);
        return stored != null ? stored : defaultMode(level, pos.relative(side));
    }

    // State computation

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    /**
     * Computes the {@link Connection} of every side.
     * <ul>
     *   <li>Neighbour in an unloaded chunk: side keeps its value.</li>
     *   <li>Neighbour is a network block sharing a type and the side was not manually severed: LINK.</li>
     *   <li>Server only, not portless: role effective for the side; not OFF and port found: INPUT (role IN) or OUTPUT.</li>
     *   <li>Otherwise: NONE.</li>
     * </ul>
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
            if (NetworkBlock.connectsAny(level.getBlockState(neighbourPos), types)
                    && (networks == null || !networks.isSevered(pos, side))) {
                connection = Connection.LINK;
            } else if (networks != null && !portless()) {
                EndpointMode mode = effectiveMode(networks, level, pos, side);
                if (mode != EndpointMode.OFF && findPort(level, neighbourPos, side.getOpposite()) != null) {
                    connection = mode == EndpointMode.IN ? Connection.INPUT : Connection.OUTPUT;
                }
            }
            state = state.setValue(SIDES.get(i), connection);
        }
        return state;
    }

    /**
     * Recomputes the state.
     * <ul>
     *   <li>State changed: sets the new state.</li>
     *   <li>Otherwise, signal block: updates the network signal.</li>
     *   <li>Otherwise, ticking block: schedules a tick.</li>
     * </ul>
     */
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
                // Network signal
                Signals.update(server, networks, pos);
            }
            if (needsTick(state)) {
                // Tick
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

    // Network and tick

    /**
     * Placement.
     * <ul>
     *   <li>Registers the node in the network of each type.</li>
     *   <li>Signal block: updates the network signal.</li>
     *   <li>Ticking block: schedules a tick.</li>
     *   <li>Newly placed non-ticking block: schedules a tick after 1 game tick.</li>
     * </ul>
     */
    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel server) {
            syncGraph(server, pos, state, true);
            if (signal) {
                Signals.update(server, LevelNetworks.get(server), pos);
            }
            if (needsTick(state)) {
                server.scheduleTick(pos, this, tickInterval(LevelNetworks.get(server), pos));
            } else if (!oldState.is(this)) {
                // Initial check
                server.scheduleTick(pos, this, 1);
            }
        }
    }

    /**
     * Removal (block replaced by another block).
     * <ul>
     *   <li>Removes the node and throughput per type.</li>
     *   <li>Clears modes, frequency, severed sides, settings and signal output.</li>
     *   <li>Drops the installed upgrades.</li>
     * </ul>
     */
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
            networks.clearFrequency(pos);
            networks.clearSevered(pos);
            networks.clearSettings(pos);
            networks.setSignalOutput(pos, 0);
            // Upgrade drops
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
     * Tick.
     * <ul>
     *   <li>1. {@link #refresh}.</li>
     *   <li>2. Registers the node when a type is not registered in the network.</li>
     *   <li>3. Stops when the block does not need a tick.</li>
     *   <li>4. Non-signal block: runs the transfer.</li>
     *   <li>5. Schedules the next tick.</li>
     * </ul>
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

    // Wrench

    /**
     * Side targeted by a wrench click.
     * <ul>
     *   <li>Among the non-link sides with an inventory (including switched-off ones): the side closest to the hit point.</li>
     *   <li>No such side: the clicked face.</li>
     * </ul>
     */
    public Direction pickSide(Level level, BlockPos pos, BlockState state, Vec3 hit, Direction clickedFace) {
        Direction best = guiSide(level, pos, state, hit);
        return best != null ? best : clickedFace;
    }

    /** Non-link side with an inventory closest to the hit point, or {@code null} when there is none. */
    public Direction guiSide(Level level, BlockPos pos, BlockState state, Vec3 hit) {
        double x = hit.x - pos.getX() - 0.5;
        double y = hit.y - pos.getY() - 0.5;
        double z = hit.z - pos.getZ() - 0.5;

        Direction best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Direction side : portSides(level, pos, state)) {
            double score = x * side.getStepX() + y * side.getStepY() + z * side.getStepZ();
            if (score > bestScore) {
                bestScore = score;
                best = side;
            }
        }
        return best;
    }

    /** Non-link sides with an inventory (including switched-off ones), in {@link Sides#ALL} order. */
    public List<Direction> portSides(Level level, BlockPos pos, BlockState state) {
        List<Direction> result = new ArrayList<>();
        for (Direction side : Sides.ALL) {
            if (connection(state, side) != Connection.LINK && hasInventory(level, pos, state, side)) {
                result.add(side);
            }
        }
        return result;
    }

    public boolean hasInventory(Level level, BlockPos pos, BlockState state, Direction side) {
        if (connection(state, side) != Connection.NONE) {
            return connection(state, side) != Connection.LINK;
        }
        BlockPos neighbour = pos.relative(side);
        return level.hasChunkAt(neighbour) && findPort(level, neighbour, side.getOpposite()) != null;
    }

    /** Translation key of the role; signal blocks use the {@code _signal} variant. */
    public String roleKey(EndpointMode mode) {
        return signal && mode != EndpointMode.OFF ? mode.translationKey() + "_signal" : mode.translationKey();
    }

    /** Stores the role of a side and refreshes the block. */
    public void setRole(ServerLevel level, BlockPos pos, Direction side, EndpointMode mode) {
        LevelNetworks.get(level).setMode(pos, side, mode);
        refresh(level, pos);
    }

    /** Wrench click while sneaking; no action by default. */
    public void onWrenchSneak(Level level, BlockPos pos, Player player) {
    }

    /**
     * Sets or clears the manually severed flag of a link side. When the neighbour at that side is a matching
     * {@link NetworkBlock}, the flag is set symmetrically on its opposite-facing side too, so the two blocks agree on
     * the link. Refreshes both blocks afterwards so the network graph and the rendered state pick up the change.
     */
    private void setSevered(ServerLevel level, BlockPos pos, Direction side, boolean severed) {
        LevelNetworks networks = LevelNetworks.get(level);
        networks.setSevered(pos, side, severed);
        BlockPos neighbourPos = pos.relative(side);
        if (NetworkBlock.connectsAny(level.getBlockState(neighbourPos), types)) {
            networks.setSevered(neighbourPos, side.getOpposite(), severed);
        }
        refresh(level, pos);
        if (level.getBlockState(neighbourPos).getBlock() instanceof ConduitBlock neighbourConduit) {
            neighbourConduit.refresh(level, neighbourPos);
        }
    }

    /**
     * Wrench click.
     * <ul>
     *   <li>Link side: severs it from its neighbour so the two networks no longer merge here.</li>
     *   <li>Previously severed side: reconnects it.</li>
     *   <li>Side without inventory: sends the no-inventory message.</li>
     *   <li>Otherwise: advances the role of the side and sends the role message.</li>
     * </ul>
     */
    public void onWrench(Level level, BlockPos pos, Player player, Direction side) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        Component sideName = Component.translatable("direction.vectrum." + side.getName());
        LevelNetworks networks = LevelNetworks.get(server);
        if (connection(state, side) == Connection.LINK) {
            setSevered(server, pos, side, true);
            player.displayClientMessage(Component.translatable("message.vectrum.link_severed", sideName), true);
            return;
        }
        if (networks.isSevered(pos, side)) {
            setSevered(server, pos, side, false);
            player.displayClientMessage(Component.translatable("message.vectrum.link_restored", sideName), true);
            return;
        }
        if (!hasInventory(level, pos, state, side)) {
            player.displayClientMessage(Component.translatable("message.vectrum.no_inventory"), true);
            return;
        }
        EndpointMode next = effectiveMode(networks, level, pos, side).next();
        setRole(server, pos, side, next);
        player.displayClientMessage(Component.translatable("message.vectrum.endpoint_mode",
                sideName, Component.translatable(roleKey(next))), true);
    }

    /**
     * Empty-hand click on the main hand: opens the endpoint screen.
     * <ul>
     *   <li>Blocks without screen and clicks with an item: no action, the item handles the click.</li>
     *   <li>Cables: the side is the inventory side closest to the hit point; without one a message is sent.</li>
     * </ul>
     */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (EndpointMenu.kindOf(state) == null || hand != InteractionHand.MAIN_HAND
                || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            Direction side = this instanceof CoderBlock ? hit.getDirection()
                    : guiSide(level, pos, state, hit.getLocation());
            if (side == null) {
                player.displayClientMessage(Component.translatable("message.vectrum.no_inventory"), true);
            } else {
                EndpointMenu.open(serverPlayer, server, pos, side);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Redstone output

    /** Signal blocks with at least one output side. */
    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState state) {
        return signal && maskOf(state, Connection.OUTPUT) != 0;
    }

    /**
     * Network signal on output sides of signal blocks, otherwise 0.
     *
     * @param direction from the reading block to this block
     */
    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!signal || !(level instanceof ServerLevel server) || connection(state, direction.getOpposite()) != Connection.OUTPUT) {
            return 0;
        }
        return LevelNetworks.get(server).signalOutput(pos);
    }

    // Shape

    /** Items that widen the placement/interaction shape ({@link #isExpandTrigger}); built once, lazily. */
    private static List<Item> expandTriggerItems;

    private static List<Item> expandTriggerItems() {
        List<Item> items = expandTriggerItems;
        if (items == null) {
            List<Item> built = new ArrayList<>(List.of(ModItems.WRENCH.get(), ModItems.ITEM_CABLE.get(),
                    ModItems.FLUID_CABLE.get(), ModItems.ENERGY_CABLE.get(), ModItems.REDSTONE_CABLE.get(),
                    ModItems.UNIVERSAL_CABLE.get(), ModItems.DIGITAL_CABLE.get(), ModItems.CODER.get(),
                    ModItems.WIRELESS_PORT.get()));
            if (ModItems.GAS_CABLE != null) {
                built.add(ModItems.GAS_CABLE.get());
            }
            items = List.copyOf(built);
            expandTriggerItems = items;
        }
        return items;
    }

    /** Whether the clicking entity holds the wrench, a cable, a coder or a wireless port. */
    private static boolean isExpandTrigger(CollisionContext context) {
        for (Item item : expandTriggerItems()) {
            if (context.isHoldingItem(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Placement/interaction shape: decides what the crosshair hits here, so it governs opening the port screen, the
     * wrench, and targeting through or against this block while placing or breaking something else.
     * <ul>
     *   <li>No {@link #expandedSelectionSpec} (universal cable, digital cable, item endpoint): always {@link #selectionSpec}.</li>
     *   <li>Otherwise: {@link #expandedSelectionSpec} while the wrench, a cable, a coder or a wireless port is held
     *       ({@link #isExpandTrigger}), so the network stays easy to extend; {@link #selectionSpec} (matching the
     *       rendered model) the rest of the time, so an empty hand or an unrelated item can click past the cable.</li>
     * </ul>
     */
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int key = linkMask(state) | (portMask(state) << 6);
        if (expandedSelectionSpec != null && isExpandTrigger(context)) {
            return expandedSelectionShapes.computeIfAbsent(key, expandedSelectionSpec::build);
        }
        return selectionShapes.computeIfAbsent(key, selectionSpec::build);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int key = linkMask(state) | (portMask(state) << 6);
        return collisionShapes.computeIfAbsent(key, collisionSpec::build);
    }
}
