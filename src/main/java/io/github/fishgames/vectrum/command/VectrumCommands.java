package io.github.fishgames.vectrum.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.fishgames.vectrum.block.NetworkBlock;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Verwaltungsbefehle ({@code /vectrum ...}, nur fuer Operatoren). Sie sind loaderunabhaengig; der jeweilige Loader
 * ruft {@link #register} beim Registrieren der Befehle auf.
 *
 * <ul>
 *   <li>{@code /vectrum throughput <pos>} zeigt das Durchsatzlimit eines Kabels oder Endpunkts (Ergebnis des Befehls
 *       ist der Wert, auf {@code int} geklemmt, z. B. fuer {@code /execute store}).</li>
 *   <li>{@code /vectrum throughput <pos> <wert>} setzt es (Einheiten pro Uebergabe und Quellseite).</li>
 *   <li>{@code /vectrum throughput <pos> reset} setzt es auf das Grundlimit zurueck.</li>
 *   <li>Ein Universalkabel hat je Typ ein eigenes Limit: {@code /vectrum throughput <pos> type <item|fluid|energy> ...}
 *       waehlt den Typ; ohne Angabe gilt der erste Typ des Bausteins (Items).</li>
 *   <li>{@code /vectrum port <pos> <seite> ...} Prioritaet, Verteilmodus und Filter einer Anschlussseite
 *       (siehe {@link PortCommands}).</li>
 *   <li>{@code /vectrum upgrade <pos> ...} zeigt, fuegt hinzu und entfernt Upgrades (siehe {@link UpgradeCommands}).</li>
 * </ul>
 * Die Durchsatz-Upgrades setzen denselben Wert; der Befehl bleibt fuer Tests und Verwaltung.
 */
public final class VectrumCommands {
    /** Kurznamen der Typen fuer den Befehl: der Teil der Kennung nach dem Doppelpunkt. */
    private static final List<TransportType> KNOWN_TYPES = List.of(TransportType.ITEM, TransportType.FLUID,
            TransportType.ENERGY, TransportType.REDSTONE, TransportType.GAS);

    private VectrumCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vectrum")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("throughput")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> showThroughput(context, null))
                                .then(resetNode(null))
                                .then(valueNode(null))
                                .then(Commands.literal("type")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        KNOWN_TYPES.stream().map(VectrumCommands::shortName), builder))
                                                .executes(context -> showThroughput(context, "type"))
                                                .then(resetNode("type"))
                                                .then(valueNode("type"))))))
                .then(PortCommands.node())
                .then(UpgradeCommands.node()));
    }

    /** {@code reset}; {@code typeArgument} ist der Name des Typ-Arguments oder {@code null}. */
    private static ArgumentBuilder<CommandSourceStack, ?> resetNode(String typeArgument) {
        return Commands.literal("reset").executes(context -> resetThroughput(context, typeArgument));
    }

    /** {@code <wert>}; {@code typeArgument} ist der Name des Typ-Arguments oder {@code null}. */
    private static ArgumentBuilder<CommandSourceStack, ?> valueNode(String typeArgument) {
        return Commands.argument("value", LongArgumentType.longArg(0))
                .executes(context -> setThroughput(context, typeArgument));
    }

    private static String shortName(TransportType type) {
        return type.id().substring(type.id().indexOf(':') + 1);
    }

    private static int showThroughput(CommandContext<CommandSourceStack> context, String typeArgument)
            throws CommandSyntaxException {
        return withNetworkBlock(context, typeArgument, (networks, type, pos) -> {
            long limit = networks.throughput(type, pos);
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.vectrum.throughput.show", pos.toShortString(), limit), false);
            return SaturatedMath.clampToNonNegativeInt(limit); // Ergebnis fuer /execute store
        });
    }

    private static int setThroughput(CommandContext<CommandSourceStack> context, String typeArgument)
            throws CommandSyntaxException {
        long value = LongArgumentType.getLong(context, "value");
        return withNetworkBlock(context, typeArgument, (networks, type, pos) -> {
            networks.setThroughput(type, pos, value);
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.vectrum.throughput.set", pos.toShortString(), value), true);
            return 1;
        });
    }

    private static int resetThroughput(CommandContext<CommandSourceStack> context, String typeArgument)
            throws CommandSyntaxException {
        return withNetworkBlock(context, typeArgument, (networks, type, pos) -> {
            networks.clearThroughput(type, pos);
            long limit = networks.throughput(type, pos);
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.vectrum.throughput.reset", pos.toShortString(), limit), true);
            return 1;
        });
    }

    private interface Action {
        int run(LevelNetworks networks, TransportType type, BlockPos pos);
    }

    /**
     * Liest die Position, prueft, dass dort ein Netzbaustein steht, und waehlt den Typ (Angabe im Befehl oder der
     * erste Typ des Bausteins). Fuehrt dann die Aktion aus.
     */
    private static int withNetworkBlock(CommandContext<CommandSourceStack> context, String typeArgument, Action action)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkBlock block)) {
            source.sendFailure(Component.translatable("command.vectrum.not_a_network_block", pos.toShortString()));
            return 0;
        }
        if (block.transportType().behavior() == io.github.fishgames.vectrum.core.transport.TransportType.Behavior.SIGNAL) {
            source.sendFailure(Component.translatable("command.vectrum.signal_unsupported", pos.toShortString()));
            return 0;
        }
        TransportType type = block.transportType();
        if (typeArgument != null) {
            String name = StringArgumentType.getString(context, typeArgument);
            type = block.transportTypes().stream().filter(t -> shortName(t).equalsIgnoreCase(name)).findFirst()
                    .orElse(null);
            if (type == null) {
                source.sendFailure(Component.translatable("command.vectrum.throughput.bad_type", name,
                        pos.toShortString()));
                return 0;
            }
        }
        return action.run(LevelNetworks.get(level), type, pos);
    }
}
