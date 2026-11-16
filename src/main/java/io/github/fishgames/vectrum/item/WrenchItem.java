package io.github.fishgames.vectrum.item;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
//? if fabric {

//?} else if neoforge {
/*import net.minecraftforge.common.ToolAction;
*///?} else {
/*import net.minecraftforge.common.ToolAction;
*///?}

/**
 * Wrench. Right click on a cable, endpoint or wireless port cycles the side role (output, input, off); sneaking
 * removes all upgrades of the block.
 *
 * <p>On Forge and NeoForge this also declares the common {@code "wrench"} tool action ({@link #canPerformAction}),
 * the de-facto cross-mod convention other wrenches (AE2, Create, EnderIO, Mekanism, ...) check for as well; that
 * makes this item work as a normal wrench on their blocks too. The reverse direction - letting their wrenches
 * operate Vectrum's own cables and ports - is handled by a Forge-side event listener
 * ({@code platform/forge/ForgeWrenchInterop}) that calls {@link #apply} for any held item declaring the same
 * action, since only that block's own item class receives {@link #useOn}.
 */
public class WrenchItem extends Item {
    //? if fabric {

    //?} else if neoforge {
    /*private static final ToolAction WRENCH_ACTION = ToolAction.get("wrench");
    *///?} else {
    /*private static final ToolAction WRENCH_ACTION = ToolAction.get("wrench");
    *///?}

    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return apply(context.getLevel(), context.getClickedPos(), player, context.getClickLocation(),
                context.getClickedFace());
    }

    //? if fabric {

    //?} else if neoforge {
    /*@Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return toolAction == WRENCH_ACTION;
    }
    *///?} else {
    /*@Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return toolAction == WRENCH_ACTION;
    }
    *///?}

    /**
     * Applies a wrench click to the block at {@code pos}: for a cable, endpoint or coder side this cycles its role
     * or severs/reconnects a link (sneaking with an upgrade-capable block removes its upgrades instead); for a
     * wireless port it advances the mode (sneaking raises the frequency). Shared between {@link #useOn} and, on
     * Forge/NeoForge, third-party wrenches handled by {@code ForgeWrenchInterop}.
     */
    public static InteractionResult apply(Level level, BlockPos pos, Player player, Vec3 clickLocation,
                                          Direction clickedFace) {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof ConduitBlock conduit) {
            if (!level.isClientSide) {
                if (player.isSecondaryUseActive()) {
                    if (conduit.acceptsUpgrades()) {
                        returnUpgrades(level, pos, conduit, player,
                                conduit.pickSide(level, pos, state, clickLocation, clickedFace));
                    } else {
                        conduit.onWrenchSneak(level, pos, player);
                    }
                } else {
                    conduit.onWrench(level, pos, player,
                            conduit.pickSide(level, pos, state, clickLocation, clickedFace));
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getBlock() instanceof WirelessBlock wireless) {
            if (!level.isClientSide) {
                if (player.isSecondaryUseActive()) {
                    wireless.onWrenchSneak(level, pos, player);
                } else {
                    wireless.onWrench(level, pos, player);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** Gives all upgrades of the clicked port side to the player. */
    private static void returnUpgrades(Level level, BlockPos pos, ConduitBlock conduit, Player player,
                                       Direction side) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        LevelNetworks networks = LevelNetworks.get(server);
        Upgrades installed = networks.upgrades(pos, side);
        if (installed.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.vectrum.upgrades_none"), true);
            return;
        }
        networks.setUpgrades(pos, side, Upgrades.EMPTY);
        for (UpgradeType type : UpgradeType.VALUES) {
            int count = installed.count(type);
            if (count > 0) {
                ItemStack stack = new ItemStack(ModItems.upgrade(type), count);
                if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                    player.drop(stack, false);
                }
            }
        }
        player.displayClientMessage(Component.translatable("message.vectrum.upgrades_removed", installed.total()), true);
    }
}
