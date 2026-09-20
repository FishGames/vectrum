package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.Modules;
import io.github.fishgames.vectrum.core.network.NodeKind;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.core.wireless.LinkMode;
import io.github.fishgames.vectrum.logistics.WirelessTransport;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import io.github.fishgames.vectrum.world.WirelessRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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

    /** Message suffix ", Gas: <mode>" when the gas module is active, otherwise empty. */
    public static Component gasSuffix(WirelessRegistry registry, ServerLevel level, BlockPos pos) {
        if (!Modules.gas()) {
            return Component.empty();
        }
        return Component.literal(", ").append(Component.translatable("type.vectrum.gas")).append(": ")
                .append(Component.translatable("link.vectrum." + registry.mode(level, pos, TransportType.GAS).id()));
    }

    // Tick

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel server) {
            WirelessRegistry.get(server).register(server, pos);
            syncGraph(server, pos, true);
            LevelNetworks.invalidateCaches();
            server.scheduleTick(pos, this, ConduitBlock.INTERVAL);
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
            networks.clearSettings(pos);
            Upgrades installed = networks.takeUpgrades(pos);
            for (UpgradeType upgrade : UpgradeType.VALUES) {
                if (installed.count(upgrade) > 0) {
                    Block.popResource(level, pos, new ItemStack(ModItems.upgrade(upgrade), installed.count(upgrade)));
                }
            }
            LevelNetworks.invalidateCaches();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** Updates the link sides and invalidates the network caches. */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        if (level instanceof ServerLevel server) {
            syncGraph(server, pos, false);
            LevelNetworks.invalidateCaches();
        }
    }

    /**
     * Tick.
     * <ul>
     *   <li>Registers missing network nodes.</li>
     *   <li>Runs the wireless transfer.</li>
     *   <li>Schedules the next tick when the block sends.</li>
     * </ul>
     */
    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        syncGraph(level, pos, false);
        if (WirelessTransport.run(level, pos)) {
            level.scheduleTick(pos, this, ConduitBlock.INTERVAL);
        }
    }

    /** Schedules a tick unless one is already scheduled. */
    public void wake(ServerLevel level, BlockPos pos) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, ConduitBlock.INTERVAL);
        }
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
