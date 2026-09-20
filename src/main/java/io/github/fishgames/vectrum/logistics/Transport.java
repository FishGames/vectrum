package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Transport fuer alle Mengen-Typen (Items, Fluide, Energie): Die Ware reist nicht, es gibt nur eine direkte Uebergabe
 * von der Quelle (Anschlussseite mit Eingang) an die Ziele (Anschlussseiten mit Ausgang) im selben Netz. Der Typ ergibt
 * sich aus dem Block; nur der Zugang zum Speicher ({@link Ports}) unterscheidet sich.
 *
 * <p>Ziele werden der Reihe nach bedient (sortiert nach Position): erst wird Ziel A gefuellt, dann B.
 * Filter, Prioritaet und weitere Modi kommen in Etappe 5 dazu.
 */
public final class Transport {
    private Transport() {
    }

    /** Eine Uebergaberunde fuer einen Baustein: fuer jede Quellseite bis zum Durchsatzlimit des Bausteins verteilen. */
    public static void run(ServerLevel level, BlockPos pos, BlockState state, ConduitBlock block) {
        if (!block.hasSource(state)) {
            return;
        }

        TransportType type = block.transportType();
        LevelNetworks networks = LevelNetworks.get(level);
        Network network = networks.networkAt(type, pos);
        if (network == null) {
            return;
        }
        // Limit des Quellbausteins: ein gespeicherter Wert, hier nur nachgeschlagen (K3).
        long budget = networks.throughput(type, pos);
        if (budget <= 0) {
            return;
        }
        List<Target> targets = networks.targets(level, type, network);
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
            Port source = Ports.find(type, level, sourcePos, side.getOpposite());
            if (source == null) {
                continue;
            }
            distribute(level, type, source, sourcePos, targets, budget);
        }
    }

    private static void distribute(ServerLevel level, TransportType type, Port source, BlockPos sourcePos,
                                   List<Target> targets, long budget) {
        long remaining = budget;
        for (Target target : targets) {
            if (remaining <= 0) {
                return;
            }
            // Ein Speicher, der gleichzeitig Quelle und Ziel ist, überspringen.
            if (target.inventory().equals(sourcePos) || !level.hasChunkAt(target.inventory())) {
                continue;
            }
            Port destination = Ports.find(type, level, target.inventory(), target.side().getOpposite());
            if (destination == null) {
                continue;
            }
            remaining -= source.moveTo(destination, remaining);
        }
    }
}
