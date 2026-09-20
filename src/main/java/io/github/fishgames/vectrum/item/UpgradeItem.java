package io.github.fishgames.vectrum.item;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Upgrade item. Right click on a cable, endpoint or wireless port installs one upgrade (stored in {@link LevelNetworks}).
 */
public class UpgradeItem extends Item {
    private final UpgradeType type;

    public UpgradeItem(UpgradeType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public UpgradeType upgradeType() {
        return type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        boolean wirelessPort = state.getBlock() instanceof WirelessBlock;
        if (!wirelessPort && !(state.getBlock() instanceof ConduitBlock)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide || !(level instanceof ServerLevel server)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (wirelessPort) {
            return useOnWirelessPort(context, server, pos);
        }
        ConduitBlock conduit = (ConduitBlock) state.getBlock();

        Component name = Component.translatable(getDescriptionId());
        if (type.wireless()) {
            tell(player, Component.translatable("message.vectrum.upgrade_wireless_only", name));
            return InteractionResult.CONSUME;
        }
        if (!conduit.acceptsUpgrades()) {
            tell(player, Component.translatable("message.vectrum.upgrade_not_supported"));
            return InteractionResult.CONSUME;
        }
        if (!conduit.hasPort(state)) {
            tell(player, Component.translatable("message.vectrum.upgrade_needs_port"));
            return InteractionResult.CONSUME;
        }
        LevelNetworks networks = LevelNetworks.get(server);
        Upgrades installed = networks.upgrades(pos);
        if (installed.freeSlots(type) <= 0) {
            tell(player, Component.translatable("message.vectrum.upgrade_full", name, type.maxCount()));
            return InteractionResult.CONSUME;
        }
        Upgrades updated = installed.with(type, installed.count(type) + 1);
        networks.setUpgrades(conduit.transportTypes(), pos, updated);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        tell(player, Component.translatable("message.vectrum.upgrade_added", name, updated.count(type),
                type.maxCount()));
        return InteractionResult.CONSUME;
    }

    /** Installs, or with sneaking removes, a wireless upgrade. */
    private InteractionResult useOnWirelessPort(UseOnContext context, ServerLevel server, BlockPos pos) {
        Player player = context.getPlayer();
        Component name = Component.translatable(getDescriptionId());
        if (!type.wireless()) {
            tell(player, Component.translatable("message.vectrum.upgrade_not_supported"));
            return InteractionResult.CONSUME;
        }
        LevelNetworks networks = LevelNetworks.get(server);
        Upgrades installed = networks.upgrades(pos);
        if (player != null && player.isSecondaryUseActive()) {
            if (installed.count(type) == 0) {
                tell(player, Component.translatable("message.vectrum.upgrades_none"));
                return InteractionResult.CONSUME;
            }
            networks.setUpgrades(WirelessBlock.TYPES, pos, installed.with(type, installed.count(type) - 1));
            ItemStack returned = new ItemStack(this);
            if (!player.getInventory().add(returned) && !returned.isEmpty()) {
                player.drop(returned, false);
            }
            tell(player, Component.translatable("message.vectrum.upgrades_removed", 1));
            return InteractionResult.CONSUME;
        }
        if (installed.freeSlots(type) <= 0) {
            tell(player, Component.translatable("message.vectrum.upgrade_full", name, type.maxCount()));
            return InteractionResult.CONSUME;
        }
        Upgrades updated = installed.with(type, installed.count(type) + 1);
        networks.setUpgrades(WirelessBlock.TYPES, pos, updated);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        tell(player, Component.translatable("message.vectrum.upgrade_added", name, updated.count(type),
                type.maxCount()));
        return InteractionResult.CONSUME;
    }

    private static void tell(@Nullable Player player, Component message) {
        if (player != null) {
            player.displayClientMessage(message, true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.vectrum.upgrade.max", type.maxCount())
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
