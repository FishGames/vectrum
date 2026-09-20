package io.github.fishgames.vectrum.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.fishgames.vectrum.block.NetworkBlock;
import io.github.fishgames.vectrum.core.util.SaturatedMath;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Verwaltungsbefehle ({@code /vectrum ...}, nur fuer Operatoren). Sie sind loaderunabhaengig; der jeweilige Loader
 * ruft {@link #register} beim Registrieren der Befehle auf.
 *
 * <ul>
 *   <li>{@code /vectrum throughput <pos>} zeigt das Durchsatzlimit eines Kabels oder Endpunkts (Ergebnis des Befehls
 *       ist der Wert, auf {@code int} geklemmt, z. B. fuer {@code /execute store}).</li>
 *   <li>{@code /vectrum throughput <pos> <wert>} setzt es (Einheiten pro Uebergabe und Quellseite).</li>
 *   <li>{@code /vectrum throughput <pos> reset} setzt es auf das Grundlimit zurueck.</li>
 *   <li>{@code /vectrum port <pos> <seite> ...} Prioritaet, Verteilmodus und Filter einer Anschlussseite
 *       (siehe {@link PortCommands}).</li>
 * </ul>
 * Die Durchsatz-Upgrades (Etappe 6) setzen denselben Wert; der Befehl bleibt fuer Tests und Verwaltung.
 */
public final class VectrumCommands {
    private VectrumCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vectrum")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("throughput")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(VectrumCommands::showThroughput)
                                .then(Commands.literal("reset")
                                        .executes(VectrumCommands::resetThroughput))
                                .then(Commands.argument("value", LongArgumentType.longArg(0))
                                        .executes(VectrumCommands::setThroughput))))
                .then(PortCommands.node()));
    }

    private static int showThroughput(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return withNetworkBlock(context, (networks, block, pos) -> {
            long limit = networks.throughput(block.transportType(), pos);
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.vectrum.throughput.show", pos.toShortString(), limit), false);
            return SaturatedMath.clampToNonNegativeInt(limit); // Ergebnis fuer /execute store
        });
    }

    private static int setThroughput(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        long value = LongArgumentType.getLong(context, "value");
        return withNetworkBlock(context, (networks, block, pos) -> {
            networks.setThroughput(block.transportType(), pos, value);
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.vectrum.throughput.set", pos.toShortString(), value), true);
            return 1;
        });
    }

    private static int resetThroughput(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return withNetworkBlock(context, (networks, block, pos) -> {
            networks.clearThroughput(block.transportType(), pos);
            long limit = networks.throughput(block.transportType(), pos);
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.vectrum.throughput.reset", pos.toShortString(), limit), true);
            return 1;
        });
    }

    private interface Action {
        int run(LevelNetworks networks, NetworkBlock block, BlockPos pos);
    }

    /** Liest die Position, prueft, dass dort ein Netzbaustein steht, und fuehrt die Aktion aus. */
    private static int withNetworkBlock(CommandContext<CommandSourceStack> context, Action action)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkBlock block)) {
            source.sendFailure(Component.translatable("command.vectrum.not_a_network_block", pos.toShortString()));
            return 0;
        }
        return action.run(LevelNetworks.get(level), block, pos);
    }
}
