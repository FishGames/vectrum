package io.github.fishgames.vectrum.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.fishgames.vectrum.block.CoderBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.world.WirelessRegistry;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** {@code /vectrum frequency <pos> [<value>]}: shows or sets the frequency of a coder or wireless block. */
final class FrequencyCommands {
    private FrequencyCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("frequency")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(FrequencyCommands::show)
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .executes(FrequencyCommands::set)));
    }

    private static boolean isCoder(CommandContext<CommandSourceStack> context, BlockPos pos) {
        Object block = context.getSource().getLevel().getBlockState(pos).getBlock();
        if (block instanceof CoderBlock || block instanceof WirelessBlock) {
            return true;
        }
        context.getSource().sendFailure(Component.translatable("command.vectrum.frequency.not_a_coder", pos.toShortString()));
        return false;
    }

    private static int frequencyOf(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof WirelessBlock
                ? WirelessRegistry.get(level).frequency(level, pos) : LevelNetworks.get(level).frequency(pos);
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!isCoder(context, pos)) {
            return 0;
        }
        int frequency = frequencyOf(context.getSource().getLevel(), pos);
        context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.frequency.show",
                pos.toShortString(), frequency), false);
        return frequency;
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!isCoder(context, pos)) {
            return 0;
        }
        int value = IntegerArgumentType.getInteger(context, "value");
        ServerLevel level = context.getSource().getLevel();
        if (level.getBlockState(pos).getBlock() instanceof WirelessBlock) {
            WirelessRegistry.get(level).setFrequency(level, pos, value);
        } else {
            LevelNetworks.get(level).setFrequency(pos, value);
        }
        context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.frequency.set",
                pos.toShortString(), value), true);
        return 1;
    }
}
