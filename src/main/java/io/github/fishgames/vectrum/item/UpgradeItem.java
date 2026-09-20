package io.github.fishgames.vectrum.item;

import io.github.fishgames.vectrum.block.ConduitBlock;
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
 * Ein Upgrade. Rechtsklick auf ein Kabel oder einen Endpunkt mit angeschlossenem Inventar steckt eines hinein; die
 * Upgrades bleiben im Block ({@link LevelNetworks}, nicht im Item) und kommen beim Abbauen vollstaendig zurueck.
 * Herausnehmen: Schluessel mit Schleichen (Shift) und Rechtsklick. Eine Oberflaeche dafuer folgt in Etappe 12.
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
        if (!(state.getBlock() instanceof ConduitBlock conduit)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide || !(level instanceof ServerLevel server)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        Component name = Component.translatable(getDescriptionId());
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
