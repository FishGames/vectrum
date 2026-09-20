package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.core.network.Network;
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
 * (Anschlussseite mit Eingang) an die Ziele (Anschlussseiten mit Ausgang) im selben Netz.
 *
 * <p>Ziele werden der Reihe nach bedient (sortiert nach Position): erst wird Ziel A gefüllt, dann B.
 * Filter, Priorität und weitere Modi kommen in Etappe 5 dazu.
 */
public final class ItemTransport {
    private static final Predicate<ItemStack> ANY = stack -> true;

    private ItemTransport() {
    }

    /** Eine Uebergaberunde fuer einen Baustein: fuer jede Quellseite bis zum Durchsatzlimit des Bausteins Items verteilen. */
    public static void run(ServerLevel level, BlockPos pos, BlockState state, ConduitBlock block) {
        if (!block.hasSource(state)) {
            return;
        }

        LevelNetworks networks = LevelNetworks.get(level);
        Network network = networks.networkAt(block.transportType(), pos);
        if (network == null) {
            return;
        }
        // Limit des Quellbausteins: ein gespeicherter Wert, hier nur nachgeschlagen (K3).
        int budget = networks.budget(block.transportType(), pos);
        if (budget <= 0) {
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
            distribute(level, source, sourcePos, targets, budget);
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
