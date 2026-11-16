package io.github.fishgames.vectrum.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.NetworkBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

/**
 * {@code /vectrum upgrade <pos> ...}: shows and changes the upgrades of a block.
 *
 * <ul>
 *   <li>{@code ... <pos>}: show the upgrades.</li>
 *   <li>{@code ... add <type> [<count>]} / {@code remove <type> [<count>]}</li>
 *   <li>{@code ... clear}</li>
 * </ul>
 * Types: throughput, speed, types, filter, priority, dimension (wireless port only).
 */
final class UpgradeCommands {
    private UpgradeCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        // A cable has upgrades per side: /vectrum upgrade <pos> <side> ...; a wireless port: /vectrum upgrade <pos> ...
        return Commands.literal("upgrade")
                .then(actions(Commands.argument("pos", BlockPosArgument.blockPos()))
                        .then(actions(Commands.argument("side", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Sides.ALL).map(Direction::getName), builder)))));
    }

    private static <B extends com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, B>> B actions(B node) {
        return node.executes(UpgradeCommands::show)
                .then(Commands.literal("add")
                        .then(typeArgument()
                                .executes(context -> change(context, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                        .executes(context -> change(context,
                                                IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("remove")
                        .then(typeArgument()
                                .executes(context -> change(context, -1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                        .executes(context -> change(context,
                                                -IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("clear").executes(UpgradeCommands::clear));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> typeArgument() {
        return Commands.argument("type", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(UpgradeType.VALUES).map(UpgradeType::id), builder));
    }

    private interface Action {
        /** {@code side}: the port side of a cable; {@code null} for a wireless port. */
        int run(LevelNetworks networks, List<TransportType> types, boolean wireless, BlockPos pos, Direction side);
    }

    private static int withBlock(CommandContext<CommandSourceStack> context, Action action)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WirelessBlock) {
            return action.run(LevelNetworks.get(level), WirelessBlock.TYPES, true, pos, null);
        }
        if (!(state.getBlock() instanceof NetworkBlock block)) {
            source.sendFailure(Component.translatable("command.vectrum.not_a_network_block", pos.toShortString()));
            return 0;
        }
        if (block instanceof ConduitBlock conduit && !conduit.acceptsUpgrades()) {
            source.sendFailure(Component.translatable("command.vectrum.signal_unsupported", pos.toShortString()));
            return 0;
        }
        Direction side = Direction.byName(sideName(context));
        if (side == null) {
            source.sendFailure(Component.translatable("command.vectrum.upgrade.side_needed", pos.toShortString()));
            return 0;
        }
        return action.run(LevelNetworks.get(level), block.transportTypes(), false, pos, side);
    }

    private static String sideName(CommandContext<CommandSourceStack> context) {
        try {
            return StringArgumentType.getString(context, "side");
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static Component describe(Upgrades upgrades) {
        return Component.translatable("command.vectrum.upgrade.list",
                upgrades.count(UpgradeType.THROUGHPUT), upgrades.count(UpgradeType.SPEED),
                upgrades.count(UpgradeType.TYPES), upgrades.count(UpgradeType.FILTER),
                upgrades.count(UpgradeType.PRIORITY), upgrades.count(UpgradeType.DIMENSION));
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return withBlock(context, (networks, types, wireless, pos, side) -> {
            Upgrades upgrades = side == null ? networks.blockUpgrades(pos) : networks.upgrades(pos, side);
            context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.upgrade.show",
                    pos.toShortString(), describe(upgrades)), false);
            return upgrades.total();
        });
    }

    private static int change(CommandContext<CommandSourceStack> context, int delta) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "type");
        UpgradeType type = UpgradeType.byId(name);
        if (type == null) {
            context.getSource().sendFailure(Component.translatable("command.vectrum.upgrade.unknown_type", name));
            return 0;
        }
        return withBlock(context, (networks, types, wireless, pos, side) -> {
            if (type.wireless() != wireless) {
                context.getSource().sendFailure(Component.translatable(wireless
                        ? "command.vectrum.upgrade.not_for_wireless" : "command.vectrum.upgrade.wireless_only",
                        pos.toShortString()));
                return 0;
            }
            Upgrades before = side == null ? networks.blockUpgrades(pos) : networks.upgrades(pos, side);
            Upgrades after = before.with(type, before.count(type) + delta);
            if (side == null) {
                networks.setBlockUpgrades(pos, after);
            } else {
                networks.setUpgrades(pos, side, after);
            }
            context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.upgrade.changed",
                    pos.toShortString(), Component.translatable(ModItems.upgrade(type).getDescriptionId()),
                    after.count(type), type.maxCount()), true);
            return after.count(type);
        });
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return withBlock(context, (networks, types, wireless, pos, side) -> {
            if (side == null) {
                networks.setBlockUpgrades(pos, Upgrades.EMPTY);
            } else {
                networks.setUpgrades(pos, side, Upgrades.EMPTY);
            }
            context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.upgrade.cleared",
                    pos.toShortString()), true);
            return 1;
        });
    }
}
