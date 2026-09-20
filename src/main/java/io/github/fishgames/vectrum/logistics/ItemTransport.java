package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.block.EndpointBlock;
import io.github.fishgames.vectrum.block.entity.EndpointBlockEntity;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.ItemPort;
import io.github.fishgames.vectrum.transfer.ItemPorts;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/**
 * Item-Transport, erste Ausbaustufe: Die Ware reist nicht, es gibt nur eine direkte Übergabe von der Quelle
 * (Endpunkt-Seite mit Eingang) an die Ziele (Endpunkt-Seiten mit Ausgang) im selben Netz.
 *
 * <p>Ziele werden der Reihe nach bedient (sortiert nach Position): erst wird Ziel A gefüllt, dann B.
 * Filter, Priorität und weitere Modi kommen in Etappe 5 dazu.
 */
public final class ItemTransport {
    private static final Predicate<ItemStack> ANY = stack -> true;

    private ItemTransport() {
    }

    /** Eine Übergaberunde für einen Endpunkt: für jede Quellseite bis zu {@code throughput()} Items verteilen. */
    public static void run(ServerLevel level, EndpointBlockEntity endpoint) {
        BlockPos pos = endpoint.getBlockPos();
        BlockState state = endpoint.getBlockState();
        if (!(state.getBlock() instanceof EndpointBlock block)) {
            return;
        }

        // Schneller Ausstieg: Hat dieser Endpunkt gar keine Quellseite, kostet der Takt fast nichts.
        boolean hasSource = false;
        for (Direction side : Sides.ALL) {
            if (block.connection(state, side) == Connection.INPUT) {
                hasSource = true;
                break;
            }
        }
        if (!hasSource) {
            return;
        }

        LevelNetworks networks = LevelNetworks.get(level);
        Network network = networks.networkAt(TransportType.ITEM, pos);
        if (network == null) {
            return;
        }
        List<ItemTarget> targets = networks.itemTargets(level, network);
        if (targets.isEmpty()) {
            return;
        }

        for (Direction side : Sides.ALL) {
            if (block.connection(state, side) != Connection.INPUT) {
                continue;
            }
            BlockPos sourcePos = pos.relative(side);
            if (!level.hasChunkAt(sourcePos)) {
                continue;
            }
            ItemPort source = ItemPorts.find(level, sourcePos, side.getOpposite());
            if (source == null) {
                continue;
            }
            distribute(level, source, sourcePos, targets, endpoint.throughput());
        }
    }

    private static void distribute(ServerLevel level, ItemPort source, BlockPos sourcePos,
                                   List<ItemTarget> targets, int budget) {
        for (ItemTarget target : targets) {
            if (budget <= 0) {
                return;
            }
            // Ein Inventar, das gleichzeitig Quelle und Ziel ist, überspringen.
            if (target.inventory().equals(sourcePos) || !level.hasChunkAt(target.inventory())) {
                continue;
            }
            ItemPort destination = ItemPorts.find(level, target.inventory(), target.side().getOpposite());
            if (destination == null) {
                continue;
            }
            budget -= source.moveTo(destination, ANY, budget);
        }
    }
}
