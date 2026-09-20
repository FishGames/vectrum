package io.github.fishgames.vectrum.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.EndpointMode;
import io.github.fishgames.vectrum.block.NetworkBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.routing.DistributionMode;
import io.github.fishgames.vectrum.core.routing.PortSettings;
import io.github.fishgames.vectrum.core.routing.ResourceFilter;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.transfer.ResourceIds;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.function.UnaryOperator;

/**
 * {@code /vectrum port <pos> <side> ...}: port side settings (priority, distribution mode, filter, role).
 *
 * <ul>
 *   <li>{@code ... <side>}: show the settings.</li>
 *   <li>{@code ... priority <value>}</li>
 *   <li>{@code ... mode sequential|round_robin|balanced}</li>
 *   <li>{@code ... filter add|remove <id>}, {@code filter clear}, {@code filter type whitelist|blacklist}</li>
 *   <li>{@code ... role in|out|off}</li>
 *   <li>{@code ... reset}</li>
 * </ul>
 */
final class PortCommands {
    private PortCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("port")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("side", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Direction.values()).map(Direction::getName), builder))
                                .executes(PortCommands::show)
                                .then(Commands.literal("priority")
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(PortCommands::setPriority)))
                                .then(Commands.literal("mode")
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(DistributionMode.values())
                                                                .map(DistributionMode::id), builder))
                                                .executes(PortCommands::setMode)))
                                .then(Commands.literal("filter")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                                        .executes(context -> editFilter(context, true))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                                        .executes(context -> editFilter(context, false))))
                                        .then(Commands.literal("clear")
                                                .executes(PortCommands::clearFilter))
                                        .then(Commands.literal("type")
                                                .then(Commands.literal("whitelist")
                                                        .executes(context -> setFilterType(context, false)))
                                                .then(Commands.literal("blacklist")
                                                        .executes(context -> setFilterType(context, true)))))
                                .then(Commands.literal("role")
                                        .then(Commands.argument("role", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        java.util.List.of("in", "out", "off"), builder))
                                                .executes(PortCommands::setRole)))
                                .then(Commands.literal("reset")
                                        .executes(PortCommands::reset))));
    }

    private interface Action {
        int run(LevelNetworks networks, BlockPos pos, Direction side) throws CommandSyntaxException;
    }

    /** Reads position and side, checks for a network block there, runs the action. */
    private static int withPort(CommandContext<CommandSourceStack> context, Action action)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        String sideName = StringArgumentType.getString(context, "side");
        Direction side = Direction.byName(sideName);
        if (side == null) {
            source.sendFailure(Component.translatable("command.vectrum.port.bad_side", sideName));
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkBlock) && !(state.getBlock() instanceof WirelessBlock)) {
            source.sendFailure(Component.translatable("command.vectrum.not_a_network_block", pos.toShortString()));
            return 0;
        }
        return action.run(LevelNetworks.get(level), pos, side);
    }

    private static Component sideName(Direction side) {
        return Component.translatable("direction.vectrum." + side.getName());
    }

    private static Component describeFilter(ResourceFilter filter) {
        if (filter.isEmpty()) {
            return Component.translatable(filter.blacklist() ? "filter.vectrum.empty_blacklist_hint"
                    : "filter.vectrum.empty_whitelist_hint");
        }
        String list = String.join(", ", filter.ids());
        return Component.translatable(filter.blacklist() ? "filter.vectrum.blacklist" : "filter.vectrum.whitelist", list);
    }

    private static int show(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return withPort(context, (networks, pos, side) -> {
            PortSettings settings = networks.settings(pos, side);
            context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.port.show",
                    pos.toShortString(), sideName(side), settings.priority(),
                    Component.translatable("distribution.vectrum." + settings.mode().id()),
                    describeFilter(settings.filter())), false);
            boolean free = context.getSource().getLevel().getBlockState(pos).getBlock() instanceof WirelessBlock;
            if (!free && (!networks.upgrades(pos).has(UpgradeType.PRIORITY) && settings.priority() != 0
                    || !networks.upgrades(pos).has(UpgradeType.FILTER) && !settings.filter().isEmpty())) {
                context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.port.inactive"), false);
            }
            return settings.priority();
        });
    }

    private static int update(CommandContext<CommandSourceStack> context, UnaryOperator<PortSettings> change,
                              String messageKey, java.util.function.Function<PortSettings, Object> argument,
                              UpgradeType required) throws CommandSyntaxException {
        return withPort(context, (networks, pos, side) -> {
            if (required != null && !networks.upgrades(pos).has(required)
                    && !(context.getSource().getLevel().getBlockState(pos).getBlock() instanceof WirelessBlock)) {
                context.getSource().sendFailure(Component.translatable("command.vectrum.port.locked",
                        pos.toShortString(), Component.translatable(ModItems.upgrade(required).getDescriptionId())));
                return 0;
            }
            PortSettings updated = change.apply(networks.settings(pos, side));
            networks.setSettings(pos, side, updated);
            context.getSource().sendSuccess(() -> Component.translatable(messageKey, pos.toShortString(),
                    sideName(side), argument.apply(updated)), true);
            return 1;
        });
    }

    private static int setPriority(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int value = IntegerArgumentType.getInteger(context, "value");
        return update(context, settings -> settings.withPriority(value), "command.vectrum.port.priority",
                settings -> settings.priority(), UpgradeType.PRIORITY);
    }

    private static int setMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "mode");
        DistributionMode mode = DistributionMode.byId(name);
        if (mode == null) {
            context.getSource().sendFailure(Component.literal(name + "?"));
            return 0;
        }
        return update(context, settings -> settings.withMode(mode), "command.vectrum.port.mode",
                settings -> Component.translatable("distribution.vectrum." + settings.mode().id()), null);
    }

    private static int editFilter(CommandContext<CommandSourceStack> context, boolean add)
            throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        if (add && !BuiltInRegistries.ITEM.containsKey(id) && !BuiltInRegistries.FLUID.containsKey(id)
                && !ResourceIds.belongsTo(TransportType.GAS, id.toString())) {
            context.getSource().sendFailure(Component.translatable("command.vectrum.port.unknown_resource", id));
            return 0;
        }
        String key = id.toString();
        return update(context,
                settings -> settings.withFilter(add ? settings.filter().with(key) : settings.filter().without(key)),
                "command.vectrum.port.filter", settings -> describeFilter(settings.filter()), UpgradeType.FILTER);
    }

    private static int clearFilter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return update(context, settings -> settings.withFilter(settings.filter().cleared()),
                "command.vectrum.port.filter", settings -> describeFilter(settings.filter()), UpgradeType.FILTER);
    }

    private static int setFilterType(CommandContext<CommandSourceStack> context, boolean blacklist)
            throws CommandSyntaxException {
        return update(context, settings -> settings.withFilter(settings.filter().withBlacklist(blacklist)),
                "command.vectrum.port.filter", settings -> describeFilter(settings.filter()), UpgradeType.FILTER);
    }

    /** Sets the role of a side (in, out, off). */
    private static int setRole(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "role");
        EndpointMode mode = switch (name) {
            case "in" -> EndpointMode.IN;
            case "out" -> EndpointMode.OUT;
            case "off" -> EndpointMode.OFF;
            default -> null;
        };
        if (mode == null) {
            context.getSource().sendFailure(Component.literal(name + "?"));
            return 0;
        }
        return withPort(context, (networks, pos, side) -> {
            if (!(context.getSource().getLevel().getBlockState(pos).getBlock() instanceof ConduitBlock conduit)) {
                context.getSource().sendFailure(Component.translatable("command.vectrum.not_a_network_block",
                        pos.toShortString()));
                return 0;
            }
            conduit.setRole(context.getSource().getLevel(), pos, side, mode);
            context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.port.role",
                    pos.toShortString(), sideName(side), Component.translatable(conduit.roleKey(mode))), true);
            return 1;
        });
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return withPort(context, (networks, pos, side) -> {
            networks.setSettings(pos, side, PortSettings.DEFAULT);
            context.getSource().sendSuccess(() -> Component.translatable("command.vectrum.port.reset",
                    pos.toShortString(), sideName(side)), true);
            return 1;
        });
    }
}
