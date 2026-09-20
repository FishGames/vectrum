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
import net.minecraft.core.BlockPos;
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
        return Commands.literal("upgrade")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(UpgradeCommands::show)
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
                        .then(Commands.literal("clear").executes(UpgradeCommands::clear)));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> typeArgument() {
        return Commands.argument("type", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(UpgradeType.VALUES).map(UpgradeType::id), builder));
    }

    private interface Action {
        int run(LevelNetworks networks, List<TransportType> types, boolean wireless, BlockPos pos);
    }

    private static int withBlock(CommandContext<CommandSourceStack> context, Action action)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WirelessBlock) {
            return action.run(LevelNetworks.get(level), WirelessBlock.TYPES, true, pos);
        }
        if (!(state.getBlock() instanceof NetworkBlock block)) {
            source.sendFailure(Component.translatable("command.vectrum.not_a_network_block", pos.toShortString()));
            return 0;
        }
        if (block instanceof ConduitBlock conduit && !conduit.acceptsUpgrades()) {
            source.sendFailure(Component.translatable("command.vectrum.signal_unsupported", pos.toShortString()));
            return 0;
        }
        return action.run(LevelNetworks.get(level), block.transportTypes(), false, pos);
    }

    private static Component describe(Upgrades upgrades) {
        return Component.translatable("command.vectrum.upgrade.list",
                upgrades.count(UpgradeType.THROUGHPUT), upgrades.count(UpgradeType.SPEED),
                upgrades.count(UpgradeType.TYPES), upgrades.count(UpgradeType.FILTER),
                upgrades.count(UpgradeType.PRIORITY), upgrades.count(UpgradeType.DIMENSION));
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return withBlock(context, (networks, types, wireless, pos) -> {
            Upgrades upgrades = networks.upgrades(pos);
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
        return withBlock(context, (networks, types, wireless, pos) -> {
            if (type.wireless() != wireless) {
                context.getSource().sendFailure(Component.translatable(wireless
                        ? "command.vectrum.upgrade.not_for_wireless" : "command.vectrum.upgrade.wireless_only",
                        pos.toShortString()));
                return 0;
            }
            Upgrades before = networks.upgrades(pos);
            Upgrades after = before.with(type, before.count(type) + delta);
            networks.setUpgrades(types, pos, after);
            context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.upgrade.changed",
                    pos.toShortString(), Component.translatable(ModItems.upgrade(type).getDescriptionId()),
                    after.count(type), type.maxCount()), true);
            return after.count(type);
        });
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return withBlock(context, (networks, types, wireless, pos) -> {
            networks.setUpgrades(types, pos, Upgrades.EMPTY);
            context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.upgrade.cleared",
                    pos.toShortString()), true);
            return 1;
        });
    }
}
