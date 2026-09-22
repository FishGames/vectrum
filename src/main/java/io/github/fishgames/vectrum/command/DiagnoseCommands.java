package io.github.fishgames.vectrum.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.world.PortDiagnosis;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * {@code /vectrum diagnose <pos> [<side>]}: flow diagnosis of a wireless port (no side) or a port side. The result
 * is the number of entries with a problem.
 */
final class DiagnoseCommands {
    private DiagnoseCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> node() {
        return Commands.literal("diagnose")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> run(context, null))
                        .then(Commands.argument("side", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        List.of("down", "up", "north", "south", "west", "east"), builder))
                                .executes(context -> run(context, "side"))));
    }

    private static int run(CommandContext<CommandSourceStack> context, String sideArgument)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ServerLevel level = source.getLevel();
        List<PortDiagnosis.Entry> entries;
        if (level.getBlockState(pos).getBlock() instanceof WirelessBlock) {
            entries = PortDiagnosis.ofWireless(level, pos);
        } else {
            if (sideArgument == null) {
                source.sendFailure(Component.translatable("command.vectrum.port.bad_side", ""));
                return 0;
            }
            String name = StringArgumentType.getString(context, sideArgument);
            Direction side = Direction.byName(name);
            if (side == null) {
                source.sendFailure(Component.translatable("command.vectrum.port.bad_side", name));
                return 0;
            }
            entries = PortDiagnosis.ofSide(level, pos, side);
        }
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.vectrum.diagnosis_none"), false);
            return 0;
        }
        int problems = 0;
        for (PortDiagnosis.Entry entry : entries) {
            source.sendSuccess(() -> PortDiagnosis.text(entry), false);
            if (entry.hasProblem()) {
                problems++;
            }
        }
        return problems;
    }
}
