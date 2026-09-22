package io.github.fishgames.vectrum.gui;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.CoderBlock;
import io.github.fishgames.vectrum.block.EndpointMode;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.diagnosis.FlowReason;
import io.github.fishgames.vectrum.core.routing.DistributionMode;
import io.github.fishgames.vectrum.core.routing.PortSettings;
import io.github.fishgames.vectrum.core.routing.ResourceFilter;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.upgrade.UpgradeEffects;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.core.wireless.LinkMode;
import io.github.fishgames.vectrum.registry.ModMenus;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.PortDiagnosis;
import io.github.fishgames.vectrum.world.Sides;
import io.github.fishgames.vectrum.world.WirelessRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Menu of the endpoint screen (cable side, redstone cable side, wireless port, coder).
 * <ul>
 *   <li>The server computes a view of integer fields and sends them as data slots (two 16-bit slots per field).</li>
 *   <li>Buttons arrive as menu button ids ({@code BUTTON_*}).</li>
 *   <li>Nine ghost slots show the filter entries; clicking one with a stack sets or removes an entry.</li>
 *   <li>The client instance knows only the view.</li>
 * </ul>
 */
public final class EndpointMenu extends AbstractContainerMenu {
    public static final int GHOSTS = 9;
    /** Types shown in the screen, in field order. */
    public static final List<TransportType> TYPE_ORDER = List.of(TransportType.ITEM, TransportType.FLUID,
            TransportType.ENERGY, TransportType.GAS);

    public static final int BUTTON_ROLE = 0;
    public static final int BUTTON_DISTRIBUTION = 1;
    public static final int BUTTON_FILTER_TYPE = 2;
    public static final int BUTTON_PRIORITY_DOWN = 3;
    public static final int BUTTON_PRIORITY_UP = 4;
    public static final int BUTTON_PRIORITY_DOWN_10 = 5;
    public static final int BUTTON_PRIORITY_UP_10 = 6;
    public static final int BUTTON_CLEAR_FILTER = 7;
    /** Frequency buttons: -1, +1, -10, +10. */
    public static final int BUTTON_FREQUENCY = 8;
    /** Link mode buttons, one per entry of {@link #TYPE_ORDER}. */
    public static final int BUTTON_LINK = 12;
    public static final int BUTTON_SIDE_PREVIOUS = 16;
    public static final int BUTTON_SIDE_NEXT = 17;
    public static final int BUTTON_COPY = 18;
    public static final int BUTTON_PASTE = 19;

    private static final int FIELD_KIND = 0;
    private static final int FIELD_SIDE = 1;
    private static final int FIELD_ROLE = 2;
    private static final int FIELD_DISTRIBUTION = 3;
    private static final int FIELD_BLACKLIST = 4;
    private static final int FIELD_PRIORITY = 5;
    private static final int FIELD_FREQUENCY = 6;
    private static final int FIELD_FLAGS = 7;
    private static final int FIELD_SIGNAL = 8;
    private static final int FIELD_FILTER_SIZE = 9;
    private static final int FIELD_TYPE_MASK = 10;
    private static final int FIELD_SIDE_MASK = 11;
    private static final int FIELD_INTERVAL = 12;
    private static final int FIELD_MAX_TYPES = 13;
    private static final int FIELD_UPGRADES = 14;
    private static final int FIELD_TYPES = FIELD_UPGRADES + UpgradeType.VALUES.length;
    private static final int PER_TYPE = 4;
    private static final int FIELDS = FIELD_TYPES + PER_TYPE * TYPE_ORDER.size();

    private static final int FLAG_FILTER = 1;
    private static final int FLAG_PRIORITY = 2;
    private static final int FLAG_CLIPBOARD = 4;

    /** Ticks between two refreshes of the server view. */
    private static final int REFRESH_INTERVAL = 5;
    private static final int MAX_PRIORITY = 9999;

    /** Copied settings per player. */
    private record Clipboard(EndpointKind kind, PortSettings settings, EndpointMode role, int frequency,
                             Map<TransportType, LinkMode> links) {
    }

    private static final Map<UUID, Clipboard> CLIPBOARDS = new HashMap<>();

    private final int[] view = new int[FIELDS];
    private final Container filterSlots = new SimpleContainer(GHOSTS);
    private final ServerLevel level;
    private final BlockPos pos;
    private final EndpointKind serverKind;
    private final UUID owner;
    private Direction side;
    private long lastRefresh = Long.MIN_VALUE;

    /** Client instance. */
    public EndpointMenu(int id, Inventory inventory) {
        this(id, inventory, null, BlockPos.ZERO, EndpointKind.CABLE, Direction.DOWN);
    }

    /** Server instance. */
    public EndpointMenu(int id, Inventory inventory, ServerLevel level, BlockPos pos, EndpointKind kind,
                        Direction side) {
        super(ModMenus.ENDPOINT.get(), id);
        this.level = level;
        this.pos = pos;
        this.serverKind = kind;
        this.side = side;
        this.owner = inventory.player.getUUID();
        addDataSlots();
        for (int i = 0; i < GHOSTS; i++) {
            addSlot(new GhostSlot(filterSlots, i, 9 + 18 * i, 121));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, 79 + 18 * column, 143 + 18 * row));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 79 + 18 * column, 201));
        }
        refresh();
    }

    // Opening

    /** Kind of the screen of a block, or {@code null} for blocks without screen. */
    public static EndpointKind kindOf(BlockState state) {
        if (state.getBlock() instanceof WirelessBlock) {
            return EndpointKind.WIRELESS;
        }
        if (state.getBlock() instanceof CoderBlock) {
            return EndpointKind.CODER;
        }
        if (state.getBlock() instanceof ConduitBlock conduit) {
            if (conduit.isSignalBlock()) {
                return EndpointKind.REDSTONE;
            }
            return conduit.acceptsUpgrades() ? EndpointKind.CABLE : null;
        }
        return null;
    }

    /** Opens the screen of the block for the player. */
    public static void open(ServerPlayer player, ServerLevel level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        EndpointKind kind = kindOf(state);
        if (kind == null) {
            return;
        }
        Component title = state.getBlock().getName();
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new EndpointMenu(id, inventory, level, pos, kind, side), title));
    }

    private void addDataSlots() {
        for (int field = 0; field < FIELDS; field++) {
            int index = field;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return (short) view[index];
                }

                @Override
                public void set(int value) {
                    view[index] = view[index] & 0xFFFF0000 | value & 0xFFFF;
                }
            });
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return (short) (view[index] >>> 16);
                }

                @Override
                public void set(int value) {
                    view[index] = view[index] & 0xFFFF | (value & 0xFFFF) << 16;
                }
            });
        }
    }

    // View accessors (client and server)

    public EndpointKind kind() {
        return EndpointKind.byOrdinal(view[FIELD_KIND]);
    }

    public Direction side() {
        return Direction.from3DDataValue(view[FIELD_SIDE]);
    }

    public EndpointMode role() {
        return EndpointMode.byOrdinal(view[FIELD_ROLE]);
    }

    public DistributionMode distribution() {
        DistributionMode[] modes = DistributionMode.values();
        int index = view[FIELD_DISTRIBUTION];
        return index >= 0 && index < modes.length ? modes[index] : DistributionMode.DEFAULT;
    }

    public boolean blacklist() {
        return view[FIELD_BLACKLIST] != 0;
    }

    public int priority() {
        return view[FIELD_PRIORITY];
    }

    public int frequency() {
        return view[FIELD_FREQUENCY];
    }

    public boolean filterUnlocked() {
        return (view[FIELD_FLAGS] & FLAG_FILTER) != 0;
    }

    public boolean priorityUnlocked() {
        return (view[FIELD_FLAGS] & FLAG_PRIORITY) != 0;
    }

    public boolean hasClipboard() {
        return (view[FIELD_FLAGS] & FLAG_CLIPBOARD) != 0;
    }

    public int signal() {
        return view[FIELD_SIGNAL];
    }

    /** Number of filter entries (may exceed the number of ghost slots). */
    public int filterSize() {
        return view[FIELD_FILTER_SIZE];
    }

    /** Whether the side can be changed to another one in the screen. */
    public boolean canSwitchSide() {
        return Integer.bitCount(view[FIELD_SIDE_MASK]) > 1;
    }

    /** Ticks between two transfers. */
    public int interval() {
        return view[FIELD_INTERVAL];
    }

    /** Maximum item types per transfer. */
    public int maxTypes() {
        return view[FIELD_MAX_TYPES];
    }

    public int upgradeCount(UpgradeType type) {
        return view[FIELD_UPGRADES + type.ordinal()];
    }

    /** Whether the screen shows the type. */
    public boolean hasType(TransportType type) {
        int index = TYPE_ORDER.indexOf(type);
        return index >= 0 && (view[FIELD_TYPE_MASK] & 1 << index) != 0;
    }

    public List<FlowReason> reasons(TransportType type) {
        int mask = view[typeField(type, 0)];
        List<FlowReason> reasons = new ArrayList<>();
        for (FlowReason reason : FlowReason.values()) {
            if ((mask & 1 << reason.ordinal()) != 0) {
                reasons.add(reason);
            }
        }
        return reasons;
    }

    /** Units per second, in tenths. */
    public int rateTenths(TransportType type) {
        return view[typeField(type, 1)];
    }

    /** Units per transfer; {@link Integer#MAX_VALUE} means unlimited. */
    public int limit(TransportType type) {
        return view[typeField(type, 2)];
    }

    public LinkMode linkMode(TransportType type) {
        LinkMode[] modes = LinkMode.values();
        int index = view[typeField(type, 3)];
        return index >= 0 && index < modes.length ? modes[index] : LinkMode.OFF;
    }

    private static int typeField(TransportType type, int offset) {
        return FIELD_TYPES + PER_TYPE * Math.max(0, TYPE_ORDER.indexOf(type)) + offset;
    }

    // Server view

    @Override
    public void broadcastChanges() {
        if (level != null && level.getGameTime() - lastRefresh >= REFRESH_INTERVAL) {
            refresh();
        }
        super.broadcastChanges();
    }

    private boolean free() {
        return serverKind == EndpointKind.WIRELESS;
    }

    /** Selectable sides of the block. */
    private List<Direction> sides() {
        BlockState state = level.getBlockState(pos);
        if (serverKind == EndpointKind.WIRELESS) {
            return List.of(Sides.ALL);
        }
        if (state.getBlock() instanceof ConduitBlock conduit) {
            return conduit.portSides(level, pos, state);
        }
        return List.of();
    }

    /** Recomputes the view fields and the filter slots. */
    private void refresh() {
        if (level == null) {
            return;
        }
        lastRefresh = level.getGameTime();
        BlockState state = level.getBlockState(pos);
        LevelNetworks networks = LevelNetworks.get(level);
        Upgrades upgrades = networks.upgrades(pos);
        int[] next = new int[FIELDS];
        next[FIELD_KIND] = serverKind.ordinal();
        next[FIELD_SIDE] = side.get3DDataValue();
        for (UpgradeType type : UpgradeType.VALUES) {
            next[FIELD_UPGRADES + type.ordinal()] = upgrades.count(type);
        }
        int flags = 0;
        if (serverKind.hasFilter()) {
            if (free() || UpgradeEffects.filterUnlocked(upgrades)) {
                flags |= FLAG_FILTER;
            }
            if (free() || UpgradeEffects.priorityUnlocked(upgrades)) {
                flags |= FLAG_PRIORITY;
            }
        }
        Clipboard clipboard = CLIPBOARDS.get(owner);
        if (clipboard != null && clipboard.kind() == serverKind) {
            flags |= FLAG_CLIPBOARD;
        }
        next[FIELD_FLAGS] = flags;
        next[FIELD_INTERVAL] = networks.interval(pos);
        next[FIELD_MAX_TYPES] = networks.maxTypes(pos);
        int sideMask = 0;
        for (Direction direction : sides()) {
            sideMask |= Sides.bit(direction);
        }
        next[FIELD_SIDE_MASK] = sideMask;

        ResourceFilter filter = ResourceFilter.NONE;
        switch (serverKind) {
            case CABLE -> {
                ConduitBlock conduit = (ConduitBlock) state.getBlock();
                EndpointMode mode = conduit.effectiveMode(networks, level, pos, side);
                next[FIELD_ROLE] = mode.ordinal();
                PortSettings settings = networks.settings(pos, side);
                filter = settings.filter();
                next[FIELD_DISTRIBUTION] = settings.mode().ordinal();
                next[FIELD_BLACKLIST] = filter.blacklist() ? 1 : 0;
                next[FIELD_PRIORITY] = clampPriority(settings.priority());
                long now = level.getGameTime();
                for (PortDiagnosis.Entry entry : PortDiagnosis.ofSide(level, pos, side)) {
                    int index = TYPE_ORDER.indexOf(entry.type());
                    if (index < 0) {
                        continue;
                    }
                    next[FIELD_TYPE_MASK] |= 1 << index;
                    fillType(next, entry, index, mode == EndpointMode.IN
                            ? networks.rate(pos, side, entry.type(), now) : 0,
                            networks.throughput(entry.type(), pos), null);
                }
            }
            case REDSTONE -> {
                ConduitBlock conduit = (ConduitBlock) state.getBlock();
                next[FIELD_ROLE] = conduit.effectiveMode(networks, level, pos, side).ordinal();
                next[FIELD_SIGNAL] = networks.signalOutput(pos);
            }
            case WIRELESS -> {
                WirelessRegistry registry = WirelessRegistry.get(level);
                next[FIELD_FREQUENCY] = registry.frequency(level, pos);
                PortSettings settings = networks.settings(pos, side);
                filter = settings.filter();
                next[FIELD_BLACKLIST] = filter.blacklist() ? 1 : 0;
                next[FIELD_PRIORITY] = clampPriority(settings.priority());
                long now = level.getGameTime();
                for (PortDiagnosis.Entry entry : PortDiagnosis.ofWireless(level, pos)) {
                    int index = TYPE_ORDER.indexOf(entry.type());
                    if (index < 0) {
                        continue;
                    }
                    double rate = 0;
                    for (Direction direction : Sides.ALL) {
                        rate += networks.rate(pos, direction, entry.type(), now);
                    }
                    fillType(next, entry, index, rate, WirelessBlock.UNLIMITED,
                            registry.mode(level, pos, entry.type()));
                }
                for (TransportType type : WirelessBlock.TYPES) {
                    int index = TYPE_ORDER.indexOf(type);
                    if (index >= 0) {
                        next[FIELD_TYPE_MASK] |= 1 << index;
                        next[FIELD_TYPES + PER_TYPE * index + 3] = registry.mode(level, pos, type).ordinal();
                    }
                }
            }
            case CODER -> next[FIELD_FREQUENCY] = networks.frequency(pos);
        }
        next[FIELD_FILTER_SIZE] = filter.ids().size();
        System.arraycopy(next, 0, view, 0, FIELDS);

        List<String> ids = new ArrayList<>(filter.ids());
        for (int i = 0; i < GHOSTS; i++) {
            ItemStack shown = i < ids.size() && serverKind.hasFilter() ? FilterIds.stackOf(ids.get(i)) : ItemStack.EMPTY;
            if (!ItemStack.matches(filterSlots.getItem(i), shown)) {
                filterSlots.setItem(i, shown);
            }
        }
    }

    private static void fillType(int[] next, PortDiagnosis.Entry entry, int index, double rate, long limit,
                                 LinkMode link) {
        int base = FIELD_TYPES + PER_TYPE * index;
        int mask = 0;
        for (FlowReason reason : entry.reasons()) {
            mask |= 1 << reason.ordinal();
        }
        next[base] = mask;
        next[base + 1] = (int) Math.min(Integer.MAX_VALUE, Math.round(rate * 10));
        next[base + 2] = (int) Math.min(Integer.MAX_VALUE, limit);
        next[base + 3] = link == null ? 0 : link.ordinal();
    }

    private static int clampPriority(int priority) {
        return Math.max(-MAX_PRIORITY, Math.min(MAX_PRIORITY, priority));
    }

    // Buttons

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level == null || !stillValid(player)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        LevelNetworks networks = LevelNetworks.get(level);
        switch (id) {
            case BUTTON_ROLE -> {
                if (state.getBlock() instanceof ConduitBlock conduit && serverKind != EndpointKind.WIRELESS) {
                    conduit.setRole(level, pos, side, conduit.effectiveMode(networks, level, pos, side).next());
                }
            }
            case BUTTON_DISTRIBUTION -> updateSettings(settings -> {
                DistributionMode[] modes = DistributionMode.values();
                return settings.withMode(modes[(settings.mode().ordinal() + 1) % modes.length]);
            }, false, false);
            case BUTTON_FILTER_TYPE -> updateSettings(settings ->
                    settings.withFilter(settings.filter().withBlacklist(!settings.filter().blacklist())), true, false);
            case BUTTON_CLEAR_FILTER -> updateSettings(settings ->
                    settings.withFilter(settings.filter().cleared()), true, false);
            case BUTTON_PRIORITY_DOWN -> changePriority(-1);
            case BUTTON_PRIORITY_UP -> changePriority(1);
            case BUTTON_PRIORITY_DOWN_10 -> changePriority(-10);
            case BUTTON_PRIORITY_UP_10 -> changePriority(10);
            case BUTTON_FREQUENCY, BUTTON_FREQUENCY + 1, BUTTON_FREQUENCY + 2, BUTTON_FREQUENCY + 3 ->
                    changeFrequency(new int[]{-1, 1, -10, 10}[id - BUTTON_FREQUENCY]);
            case BUTTON_LINK, BUTTON_LINK + 1, BUTTON_LINK + 2, BUTTON_LINK + 3 -> cycleLink(id - BUTTON_LINK);
            case BUTTON_SIDE_PREVIOUS -> switchSide(-1);
            case BUTTON_SIDE_NEXT -> switchSide(1);
            case BUTTON_COPY -> copy(networks);
            case BUTTON_PASTE -> paste(networks);
            default -> {
                return false;
            }
        }
        refresh();
        return true;
    }

    /**
     * Changes the stored settings of the current side.
     *
     * @param needsFilter   the change needs the filter to be unlocked
     * @param needsPriority the change needs priority to be unlocked
     */
    private void updateSettings(UnaryOperator<PortSettings> change, boolean needsFilter, boolean needsPriority) {
        if (!serverKind.hasFilter() || needsFilter && !filterUnlocked() || needsPriority && !priorityUnlocked()) {
            return;
        }
        LevelNetworks networks = LevelNetworks.get(level);
        networks.setSettings(pos, side, change.apply(networks.settings(pos, side)));
    }

    private void changePriority(int delta) {
        updateSettings(settings -> settings.withPriority(clampPriority(settings.priority() + delta)), false, true);
    }

    private void changeFrequency(int delta) {
        if (serverKind == EndpointKind.WIRELESS) {
            WirelessRegistry registry = WirelessRegistry.get(level);
            registry.setFrequency(level, pos, Math.max(0, registry.frequency(level, pos) + delta));
        } else if (serverKind == EndpointKind.CODER) {
            LevelNetworks networks = LevelNetworks.get(level);
            networks.setFrequency(pos, Math.max(0, networks.frequency(pos) + delta));
        }
    }

    private void cycleLink(int index) {
        if (serverKind != EndpointKind.WIRELESS || index >= TYPE_ORDER.size()
                || !(level.getBlockState(pos).getBlock() instanceof WirelessBlock wireless)) {
            return;
        }
        TransportType type = TYPE_ORDER.get(index);
        if (!WirelessBlock.TYPES.contains(type)) {
            return;
        }
        WirelessRegistry registry = WirelessRegistry.get(level);
        registry.setMode(level, pos, type, registry.mode(level, pos, type).next());
        wireless.wake(level, pos);
    }

    private void switchSide(int step) {
        List<Direction> sides = sides();
        if (sides.size() < 2) {
            return;
        }
        int index = sides.indexOf(side);
        side = sides.get(Math.floorMod(index + step, sides.size()));
    }

    private void copy(LevelNetworks networks) {
        BlockState state = level.getBlockState(pos);
        EndpointMode role = state.getBlock() instanceof ConduitBlock conduit && serverKind != EndpointKind.WIRELESS
                ? conduit.effectiveMode(networks, level, pos, side) : null;
        Map<TransportType, LinkMode> links = new HashMap<>();
        int frequency = 0;
        if (serverKind == EndpointKind.WIRELESS) {
            WirelessRegistry registry = WirelessRegistry.get(level);
            frequency = registry.frequency(level, pos);
            for (TransportType type : WirelessBlock.TYPES) {
                links.put(type, registry.mode(level, pos, type));
            }
        } else if (serverKind == EndpointKind.CODER) {
            frequency = networks.frequency(pos);
        }
        CLIPBOARDS.put(owner, new Clipboard(serverKind,
                serverKind.hasFilter() ? networks.settings(pos, side) : PortSettings.DEFAULT, role, frequency, links));
    }

    private void paste(LevelNetworks networks) {
        Clipboard clipboard = CLIPBOARDS.get(owner);
        if (clipboard == null || clipboard.kind() != serverKind) {
            return;
        }
        if (serverKind.hasFilter()) {
            PortSettings current = networks.settings(pos, side);
            PortSettings copied = clipboard.settings();
            networks.setSettings(pos, side, new PortSettings(
                    priorityUnlocked() ? copied.priority() : current.priority(), copied.mode(),
                    filterUnlocked() ? copied.filter() : current.filter()));
        }
        if (clipboard.role() != null && level.getBlockState(pos).getBlock() instanceof ConduitBlock conduit) {
            conduit.setRole(level, pos, side, clipboard.role());
        }
        if (serverKind == EndpointKind.WIRELESS) {
            WirelessRegistry registry = WirelessRegistry.get(level);
            registry.setFrequency(level, pos, clipboard.frequency());
            clipboard.links().forEach((type, mode) -> registry.setMode(level, pos, type, mode));
            if (level.getBlockState(pos).getBlock() instanceof WirelessBlock wireless) {
                wireless.wake(level, pos);
            }
        } else if (serverKind == EndpointKind.CODER) {
            networks.setFrequency(pos, clipboard.frequency());
        }
    }

    // Filter slots

    /** Adds a filter entry (drop from a recipe viewer or a stack in a filter slot). */
    public void addFilterId(String id) {
        if (level != null && id != null) {
            updateSettings(settings -> settings.withFilter(settings.filter().with(id)), true, false);
            refresh();
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < GHOSTS) {
            if (level != null && clickType == ClickType.PICKUP && filterUnlocked()) {
                ghostClick(slotId, getCarried(), button);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /** Stack in a ghost slot: sets the entry; empty hand: removes the entry shown in the slot. */
    private void ghostClick(int index, ItemStack carried, int button) {
        LevelNetworks networks = LevelNetworks.get(level);
        ResourceFilter filter = networks.settings(pos, side).filter();
        if (carried.isEmpty()) {
            List<String> ids = new ArrayList<>(filter.ids());
            if (index < ids.size()) {
                String id = ids.get(index);
                updateSettings(settings -> settings.withFilter(settings.filter().without(id)), true, false);
            }
        } else {
            addFilterId(FilterIds.idOf(carried, button == 1 || fluidOnly()));
        }
        refresh();
    }

    /** Whether the current side has a fluid storage but no item storage (a bucket then means its fluid). */
    private boolean fluidOnly() {
        BlockPos neighbour = pos.relative(side);
        if (!level.hasChunkAt(neighbour)) {
            return false;
        }
        Direction facing = side.getOpposite();
        return Ports.find(TransportType.ITEM, level, neighbour, facing) == null
                && Ports.find(TransportType.FLUID, level, neighbour, facing) != null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null) {
            return true;
        }
        return kindOf(level.getBlockState(pos)) == serverKind
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    /** Filter slot: shows an entry, takes and gives nothing. */
    private final class GhostSlot extends Slot {
        GhostSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean isActive() {
            return filterUnlocked();
        }
    }
}
