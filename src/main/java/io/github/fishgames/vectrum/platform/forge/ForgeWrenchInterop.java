package io.github.fishgames.vectrum.platform.forge;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.block.WirelessBlock;
import io.github.fishgames.vectrum.item.WrenchItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Lets third-party wrenches (AE2, Create, EnderIO, Mekanism, ...) operate Vectrum's cables, endpoints and wireless
 * ports, the same way Vectrum's own wrench works on their blocks via {@link WrenchItem#canPerformAction}. Only the
 * clicked item's own class normally receives the interaction, so a foreign wrench never learns about
 * {@link ConduitBlock}/{@link WirelessBlock} on its own; this listens for the common {@code "wrench"} tool action
 * on any right-clicked item instead and, for Vectrum's own blocks, runs the same {@link WrenchItem#apply} logic.
 * Vectrum's own wrench is excluded here since {@link WrenchItem#useOn} already handles it.
 */
final class ForgeWrenchInterop {
    private static final ToolAction WRENCH_ACTION = ToolAction.get("wrench");

    private ForgeWrenchInterop() {
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener(ForgeWrenchInterop::onRightClickBlock);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof WrenchItem || !stack.canPerformAction(WRENCH_ACTION)) {
            return;
        }
        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof ConduitBlock) && !(state.getBlock() instanceof WirelessBlock)) {
            return;
        }
        Player player = event.getEntity();
        InteractionResult result = WrenchItem.apply(level, event.getPos(), player,
                event.getHitVec().getLocation(), event.getFace());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
