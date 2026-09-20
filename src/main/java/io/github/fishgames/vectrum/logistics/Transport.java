package io.github.fishgames.vectrum.logistics;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.Connection;
import io.github.fishgames.vectrum.core.network.Network;
import io.github.fishgames.vectrum.core.routing.PortSettings;
import io.github.fishgames.vectrum.core.routing.ResourceFilter;
import io.github.fishgames.vectrum.core.routing.Router;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.transfer.Port;
import io.github.fishgames.vectrum.transfer.Ports;
import io.github.fishgames.vectrum.transfer.ResourceIds;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Transport fuer alle Mengen-Typen (Items, Fluide, Energie): Die Ware reist nicht, es gibt nur eine direkte Uebergabe
 * von der Quelle (Anschlussseite mit Eingang) an die Ziele (Anschlussseiten mit Ausgang) im selben Netz. Der Typ ergibt
 * sich aus dem Block; nur der Zugang zum Speicher ({@link Ports}) unterscheidet sich.
 *
 * <p>Die Routing-Kaskade ist <b>Filter -> Prioritaet -> Modus</b> ({@link Router}). Der Filter der Quelle und der des
 * Ziels muessen die Ware beide durchlassen; Prioritaet steht am Ziel, der Verteilmodus an der Quelle.
 *
 * <p>Ein Ziel, das trotz Angebot nichts annimmt, waehrend die Quelle an andere liefert (also voll ist), wird eine
 * Weile nicht mehr gefragt ({@link LevelNetworks#noteMiss}). Jede Aenderung am Netz oder an Einstellungen hebt das auf.
 */
public final class Transport {
    private Transport() {
    }

    /**
     * Eine Uebergaberunde fuer einen Baustein: fuer jeden Typ, den er fuehrt, und jede Quellseite bis zum
     * Durchsatzlimit des Bausteins verteilen. Einzelkabel fuehren einen Typ, das Universalkabel mehrere; jeder Typ hat
     * sein eigenes Netz, sein eigenes Limit und seine eigene Zielliste.
     */
    public static void run(ServerLevel level, BlockPos pos, BlockState state, ConduitBlock block) {
        if (!block.hasSource(state)) {
            return;
        }
        LevelNetworks networks = LevelNetworks.get(level);
        for (TransportType type : block.transportTypes()) {
            if (type.behavior() != TransportType.Behavior.QUANTITY) {
                continue; // Signale (Redstone) laufen ueber Signals, nicht ueber Mengenuebergaben
            }
            run(level, networks, type, pos, state, block);
        }
    }

    private static void run(ServerLevel level, LevelNetworks networks, TransportType type, BlockPos pos,
                            BlockState state, ConduitBlock block) {
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

        // Ziel-Speicher werden je Uebergaberunde nur einmal gesucht (auch fuer den Fuellstand).
        Map<Target, Port> destinations = new HashMap<>();
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
            distribute(level, networks, type, pos, side, source, sourcePos, targets, budget, destinations);
        }
    }

    private static void distribute(ServerLevel level, LevelNetworks networks, TransportType type, BlockPos pos,
                                   Direction side, Port source, BlockPos sourcePos, List<Target> targets,
                                   long budget, Map<Target, Port> destinations) {
        PortSettings own = networks.effectiveSettings(pos, side);
        int maxTypes = networks.maxTypes(pos);
        ResourceFilter sourceFilter = forType(type, own.filter());
        long now = level.getGameTime();

        Set<Target> asked = new HashSet<>();
        Set<Target> accepted = new HashSet<>();

        Router.Pointer pointer = new Router.Pointer() {
            @Override
            public int get() {
                return networks.pointer(pos, side);
            }

            @Override
            public void set(int value) {
                networks.setPointer(pos, side, value);
            }
        };

        long moved = Router.distribute(budget, targets,
                target -> target.settings().priority(),
                target -> fillLevel(level, type, target, destinations),
                own.mode(), pointer,
                (target, max) -> {
                    // Ein Speicher, der gleichzeitig Quelle und Ziel ist, ueberspringen; ebenso Ziele in nicht
                    // geladenen Chunks und Ziele, die gerade Wartezeit haben.
                    if (target.inventory().equals(sourcePos) || !level.hasChunkAt(target.inventory())
                            || networks.isSleeping(pos, side, target, now)) {
                        return 0;
                    }
                    Port destination = destination(level, type, target, destinations);
                    if (destination == null) {
                        return 0;
                    }
                    asked.add(target);
                    long result = source.moveTo(destination, max, combine(sourceFilter, forType(type, target.settings().filter())), maxTypes);
                    if (result > 0) {
                        accepted.add(target);
                    }
                    return result;
                });

        // Nur wenn die Quelle etwas hergegeben hat, sagt ein leeres Ziel etwas ueber dieses Ziel aus (voll oder
        // lehnt die Ware ab). Ist die Quelle leer, geht sonst jedes Ziel zu Unrecht schlafen.
        if (moved > 0) {
            for (Target target : asked) {
                if (accepted.contains(target)) {
                    networks.noteHit(pos, side, target);
                } else {
                    networks.noteMiss(pos, side, target, now);
                }
            }
        }
    }

    /** Nur die Eintraege des Filters, die diesen Typ betreffen (siehe {@link ResourceFilter#restrictedTo}). */
    private static ResourceFilter forType(TransportType type, ResourceFilter filter) {
        return filter.isEmpty() ? filter : filter.restrictedTo(id -> ResourceIds.belongsTo(type, id));
    }

    /** Beide Filter muessen passen. Sind beide leer, gibt es den schnellen Weg ohne Nachfrage je Ware. */
    private static Predicate<String> combine(ResourceFilter source, ResourceFilter target) {
        if (source.isEmpty() && target.isEmpty()) {
            return Port.ALL;
        }
        return id -> source.matches(id) && target.matches(id);
    }

    private static Port destination(ServerLevel level, TransportType type, Target target,
                                    Map<Target, Port> destinations) {
        if (destinations.containsKey(target)) {
            return destinations.get(target);
        }
        Port port = level.hasChunkAt(target.inventory())
                ? Ports.find(type, level, target.inventory(), target.side().getOpposite())
                : null;
        destinations.put(target, port);
        return port;
    }

    private static double fillLevel(ServerLevel level, TransportType type, Target target,
                                    Map<Target, Port> destinations) {
        Port port = destination(level, type, target, destinations);
        return port == null ? 1.0 : port.fillLevel();
    }
}
