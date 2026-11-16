package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.Modules;
import io.github.fishgames.vectrum.core.config.VectrumConfig;
import io.github.fishgames.vectrum.core.network.NodeKind;
import io.github.fishgames.vectrum.gui.EndpointMenu;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.wireless.LinkMode;
import io.github.fishgames.vectrum.logistics.WirelessTransport;
import io.github.fishgames.vectrum.logistics.WorkBudget;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import io.github.fishgames.vectrum.world.WirelessRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;

import java.util.List;

/**
 * Wireless port block: no block entity; frequency and modes live in the {@link WirelessRegistry}.
 * <ul>
 *   <li>Full block; connects to cables of all quantity types and to coders as an endpoint of their networks.</li>
 *   <li>A sending port passes the goods of its cable network to all receivers of the frequency; a receiving port
 *       offers the targets of its cable network to all senders of the frequency.</li>
 *   <li>Blocks with the same frequency form one group, across dimensions.</li>
 *   <li>Mode per type: send, receive, both or off.</li>
 *   <li>Filter, priority and distribution mode per side, as on cables.</li>
 *   <li>Links across dimensions need the dimension upgrade at both ends.</li>
 *   <li>Throughput per transfer and source side: {@link #UNLIMITED}.</li>
 * </ul>
 */
public class WirelessBlock extends Block implements NetworkBlock {
    /** Transport types carried by wireless ports. */
    public static final List<TransportType> TYPES = Modules.quantityTypes();

    /** Throughput per transfer and source side. */
    public static final long UNLIMITED = Integer.MAX_VALUE;

    public WirelessBlock(Properties properties) {
        super(properties);
    }

    @Override
    public List<TransportType> transportTypes() {
        return TYPES;
    }

    // Network nodes

    /**
     * Registers the block as an endpoint in the layer of each type.
     * <ul>
     *   <li>Link sides: neighbours that are conduit blocks carrying the type.</li>
     *   <li>Side with an unloaded neighbour chunk keeps its registered value.</li>
     * </ul>
     *
     * @param always {@code false}: register only when the link sides differ from the network
     */
    public void syncGraph(ServerLevel level, BlockPos pos, boolean always) {
        LevelNetworks networks = LevelNetworks.get(level);
        for (TransportType type : TYPES) {
            int registered = networks.sideMask(type, pos);
            int links = 0;
            for (int i = 0; i < Sides.ALL.length; i++) {
                BlockPos neighbour = pos.relative(Sides.ALL[i]);
                if (!level.hasChunkAt(neighbour)) {
                    links |= registered & (1 << i);
                } else if (level.getBlockState(neighbour).getBlock() instanceof ConduitBlock conduit
                        && conduit.carries(type)) {
                    links |= 1 << i;
                }
            }
            if (always || !networks.matches(type, pos, NodeKind.ENDPOINT, links)) {
                networks.put(type, pos, NodeKind.ENDPOINT, links);
            }
        }
    }

    /** Message suffix ", Gas: <mode>, Source: <mode>, ..." for the modules that are on; empty without modules. */
    public static Component moduleSuffix(WirelessRegistry registry, ServerLevel level, BlockPos pos) {
        MutableComponent suffix = Component.empty();
        for (TransportType type : Modules.moduleTypes()) {
            String name = type.id().substring(type.id().indexOf(':') + 1);
            suffix.append(Component.literal(", ")).append(Component.translatable("type.vectrum." + name))
                    .append(": ").append(Component.translatable("link.vectrum." + registry.mode(level, pos, type).id()));
        }
        return suffix;
    }

    // Tick

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel server) {
            WirelessRegistry.get(server).register(server, pos);
            syncGraph(server, pos, true);
            server.scheduleTick(pos, this, startDelay(pos));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            WirelessRegistry.get(server).unregister(server, pos);
            LevelNetworks networks = LevelNetworks.get(server);
            for (TransportType type : TYPES) {
                networks.remove(type, pos);
            }
            networks.clearModes(pos);
            networks.clearSettings(pos);
            int[] installed = networks.takeUpgrades(pos);
            for (UpgradeType upgrade : UpgradeType.VALUES) {
                if (installed[upgrade.ordinal()] > 0) {
                    Block.popResource(level, pos, new ItemStack(ModItems.upgrade(upgrade), installed[upgrade.ordinal()]));
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** Updates the link sides; the caches are cleared only when the link sides or the sides with a storage changed. */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        if (level instanceof ServerLevel server) {
            syncGraph(server, pos, false);
            LevelNetworks networks = LevelNetworks.get(server);
            networks.notePorts(pos, portMask(server, pos));
            networks.wakeIdle(pos);
        }
    }

    /** One bit per type and side that has a storage. */
    private static int portMask(ServerLevel level, BlockPos pos) {
        int mask = 0;
        int bit = 0;
        for (TransportType type : TYPES) {
            for (net.minecraft.core.Direction side : Sides.ALL) {
                BlockPos neighbour = pos.relative(side);
                if (level.hasChunkAt(neighbour) && Ports.find(type, level, neighbour, side.getOpposite()) != null) {
                    mask |= 1 << bit;
                }
                bit++;
            }
        }
        return mask;
    }

    /**
     * Tick.
     * <ul>
     *   <li>Registers missing network nodes.</li>
     *   <li>Waits for the next tick when the work budget of the server tick is spent.</li>
     *   <li>Runs the wireless transfer.</li>
     *   <li>Schedules the next tick when the block sends: regular interval, longer while nothing is moved.</li>
     * </ul>
     */
    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        WorkBudget.begin(level.getServer());
        if (WorkBudget.exhausted()) {
            level.scheduleTick(pos, this, 1, TickPriority.HIGH);
            return;
        }
        LevelNetworks networks = LevelNetworks.get(level);
        if (networks.refreshDue(pos, level.getGameTime())) {
            syncGraph(level, pos, false);
        }
        long moved = WirelessTransport.run(level, networks, pos);
        if (moved == WirelessTransport.NOT_SENDING) {
            return;
        }
        if (moved == 0 && WorkBudget.exhausted()) {
            level.scheduleTick(pos, this, 1, TickPriority.HIGH);
            return;
        }
        int interval = VectrumConfig.get().wirelessInterval();
        level.scheduleTick(pos, this, networks.nextDelay(pos, moved > 0, interval));
    }

    /** Schedules a tick unless one is already scheduled. */
    public void wake(ServerLevel level, BlockPos pos) {
        LevelNetworks.get(level).wakeIdle(pos);
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, startDelay(pos));
        }
    }

    private static int startDelay(BlockPos pos) {
        int interval = VectrumConfig.get().wirelessInterval();
        return interval + WorkBudget.phase(pos, interval);
    }

    /** Empty-hand click on the main hand: opens the wireless screen with the clicked face as the side. */
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            EndpointMenu.open(serverPlayer, server, pos, hit.getDirection());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Wrench

    /** Wrench: advances the mode of all types together. */
    public void onWrench(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        WirelessRegistry registry = WirelessRegistry.get(server);
        LinkMode next = registry.mode(server, pos, TYPES.get(0)).next();
        for (TransportType type : TYPES) {
            registry.setMode(server, pos, type, next);
        }
        wake(server, pos);
        player.displayClientMessage(Component.translatable("message.vectrum.wireless_mode",
                Component.translatable("link.vectrum." + next.id())), true);
    }

    /** Wrench + sneak: frequency up by one. */
    public void onWrenchSneak(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        WirelessRegistry registry = WirelessRegistry.get(server);
        int frequency = registry.frequency(server, pos) + 1;
        registry.setFrequency(server, pos, frequency);
        player.displayClientMessage(Component.translatable("message.vectrum.coder_frequency", frequency), true);
    }
}
