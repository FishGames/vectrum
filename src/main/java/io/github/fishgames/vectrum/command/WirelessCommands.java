package io.github.fishgames.vectrum.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.wireless.LinkMode;
import io.github.fishgames.vectrum.world.WirelessRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** {@code /vectrum wireless <pos> [mode <type|all> <off|receive|send|both>]}: shows or sets the link mode per type. */
final class WirelessCommands {
    private WirelessCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("wireless")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(WirelessCommands::show)
                        .then(Commands.literal("mode")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(typeNames(), builder))
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(LinkMode.values()).map(LinkMode::id), builder))
                                                .executes(WirelessCommands::setMode)))));
    }

    private static List<String> typeNames() {
        List<String> names = new ArrayList<>();
        names.add("all");
        for (TransportType type : WirelessBlock.TYPES) {
            names.add(shortName(type));
        }
        return names;
    }

    private static String shortName(TransportType type) {
        return type.id().substring(type.id().indexOf(':') + 1);
    }

    private static WirelessBlock blockAt(CommandContext<CommandSourceStack> context, BlockPos pos) {
        if (context.getSource().getLevel().getBlockState(pos).getBlock() instanceof WirelessBlock block) {
            return block;
        }
        context.getSource().sendFailure(Component.translatable("command.vectrum.wireless.not_wireless", pos.toShortString()));
        return null;
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (blockAt(context, pos) == null) {
            return 0;
        }
        ServerLevel level = context.getSource().getLevel();
        WirelessRegistry registry = WirelessRegistry.get(level);
        int frequency = registry.frequency(level, pos);
        context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.wireless.show", pos.toShortString(),
                frequency, registry.memberCount(frequency),
                Component.translatable("link.vectrum." + registry.mode(level, pos, TransportType.ITEM).id()),
                Component.translatable("link.vectrum." + registry.mode(level, pos, TransportType.FLUID).id()),
                Component.translatable("link.vectrum." + registry.mode(level, pos, TransportType.ENERGY).id()),
                WirelessBlock.gasSuffix(registry, level, pos)), false);
        return frequency;
    }

    private static int setMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        WirelessBlock block = blockAt(context, pos);
        if (block == null) {
            return 0;
        }
        String typeName = StringArgumentType.getString(context, "type");
        LinkMode mode = LinkMode.byId(StringArgumentType.getString(context, "mode"));
        List<TransportType> types = new ArrayList<>();
        for (TransportType type : WirelessBlock.TYPES) {
            if (typeName.equals("all") || typeName.equals(shortName(type))) {
                types.add(type);
            }
        }
        if (mode == null || types.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.vectrum.wireless.bad_argument"));
            return 0;
        }
        ServerLevel level = context.getSource().getLevel();
        WirelessRegistry registry = WirelessRegistry.get(level);
        for (TransportType type : types) {
            registry.setMode(level, pos, type, mode);
        }
        block.wake(level, pos);
        context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.wireless.set", pos.toShortString(),
                typeName, Component.translatable("link.vectrum." + mode.id())), true);
        return 1;
    }
}
