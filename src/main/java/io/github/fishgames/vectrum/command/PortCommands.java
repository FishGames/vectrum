package io.github.fishgames.vectrum.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.fishgames.vectrum.block.NetworkBlock;
import io.github.fishgames.vectrum.core.routing.DistributionMode;
import io.github.fishgames.vectrum.core.routing.PortSettings;
import io.github.fishgames.vectrum.core.routing.ResourceFilter;
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
 * {@code /vectrum port <pos> <seite> ...}: Einstellungen einer Anschlussseite (Prioritaet, Verteilmodus, Filter).
 * Bis die Upgrades (Etappe 6) und die Oberflaeche (Etappe 12) da sind, ist das der Weg, sie zu setzen.
 *
 * <ul>
 *   <li>{@code ... <seite>} zeigt die Einstellungen (Ergebnis des Befehls ist die Prioritaet).</li>
 *   <li>{@code ... priority <zahl>}: hoehere Zahl wird als Ziel zuerst beliefert (Standard 0, auch negativ).</li>
 *   <li>{@code ... mode sequential|round_robin|balanced}: wie die Seite als Quelle verteilt.</li>
 *   <li>{@code ... filter add|remove <id>}, {@code filter clear}, {@code filter type whitelist|blacklist}.</li>
 *   <li>{@code ... reset}: alles auf Standard.</li>
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
                                .then(Commands.literal("reset")
                                        .executes(PortCommands::reset))));
    }

    private interface Action {
        int run(LevelNetworks networks, BlockPos pos, Direction side) throws CommandSyntaxException;
    }

    /** Liest Position und Seite, prueft, dass dort ein Netzbaustein steht, und fuehrt die Aktion aus. */
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
        if (!(state.getBlock() instanceof NetworkBlock)) {
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
            return settings.priority(); // Ergebnis fuer /execute store
        });
    }

    private static int update(CommandContext<CommandSourceStack> context, UnaryOperator<PortSettings> change,
                              String messageKey, java.util.function.Function<PortSettings, Object> argument)
            throws CommandSyntaxException {
        return withPort(context, (networks, pos, side) -> {
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
                settings -> settings.priority());
    }

    private static int setMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "mode");
        DistributionMode mode = DistributionMode.byId(name);
        if (mode == null) {
            context.getSource().sendFailure(Component.literal(name + "?"));
            return 0;
        }
        return update(context, settings -> settings.withMode(mode), "command.vectrum.port.mode",
                settings -> Component.translatable("distribution.vectrum." + settings.mode().id()));
    }

    private static int editFilter(CommandContext<CommandSourceStack> context, boolean add)
            throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        if (add && !BuiltInRegistries.ITEM.containsKey(id) && !BuiltInRegistries.FLUID.containsKey(id)) {
            context.getSource().sendFailure(Component.translatable("command.vectrum.port.unknown_resource", id));
            return 0;
        }
        String key = id.toString();
        return update(context,
                settings -> settings.withFilter(add ? settings.filter().with(key) : settings.filter().without(key)),
                "command.vectrum.port.filter", settings -> describeFilter(settings.filter()));
    }

    private static int clearFilter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return update(context, settings -> settings.withFilter(settings.filter().cleared()),
                "command.vectrum.port.filter", settings -> describeFilter(settings.filter()));
    }

    private static int setFilterType(CommandContext<CommandSourceStack> context, boolean blacklist)
            throws CommandSyntaxException {
        return update(context, settings -> settings.withFilter(settings.filter().withBlacklist(blacklist)),
                "command.vectrum.port.filter", settings -> describeFilter(settings.filter()));
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
