package io.github.fishgames.vectrum.item;

import io.github.fishgames.vectrum.block.ConduitBlock;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.core.upgrade.Upgrades;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Das Wrench-Werkzeug zum Einstellen. Rechtsklick auf ein Kabel oder einen Endpunkt neben einem Inventar schaltet die
 * Rolle dieser Seite um (Ausgang, Eingang, Aus); mit Schleichen (Shift) nimmt er alle Upgrades des Bausteins heraus. Weitere Funktionen (Verbindungen abschalten, Einstellungen kopieren)
 * folgen in Etappe 12. Zum Ansehen von Netzen dient das {@link DiagnosticItem}.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (state.getBlock() instanceof ConduitBlock conduit) {
            if (!level.isClientSide && player != null && player.isSecondaryUseActive()) {
                returnUpgrades(level, pos, conduit, player);
            } else if (!level.isClientSide && player != null) {
                conduit.onWrench(level, pos, player,
                        conduit.pickSide(level, pos, state, context.getClickLocation(), context.getClickedFace()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** Schleichen + Rechtsklick: alle Upgrades dieses Bausteins herausnehmen und dem Spieler geben. */
    private static void returnUpgrades(Level level, BlockPos pos, ConduitBlock conduit, Player player) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        LevelNetworks networks = LevelNetworks.get(server);
        Upgrades installed = networks.upgrades(pos);
        if (installed.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.vectrum.upgrades_none"), true);
            return;
        }
        networks.setUpgrades(conduit.transportTypes(), pos, Upgrades.EMPTY);
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
