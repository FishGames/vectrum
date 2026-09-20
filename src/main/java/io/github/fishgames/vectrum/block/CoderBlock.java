package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.Modules;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.world.LevelNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Coder: bridge between transport networks and the digital network.
 * <ul>
 *   <li>Full block; connects to transport cables (all quantity types) and digital cables.</li>
 *   <li>Coders in one digital network with the same frequency share their transport networks.</li>
 *   <li>Wrench: frequency up; wrench + sneak: frequency down (minimum 0, default 0).</li>
 * </ul>
 */
public class CoderBlock extends ConduitBlock {
    public CoderBlock(Properties properties) {
        super(types(), properties,
                new ShapeSpec(0, 16, 0, 16, 0, 16, 0),
                new ShapeSpec(0, 16, 0, 16, 0, 16, 0));
    }

    /** All quantity types plus the digital layer. */
    private static List<TransportType> types() {
        List<TransportType> types = new ArrayList<>(Modules.quantityTypes());
        types.add(TransportType.DIGITAL);
        return List.copyOf(types);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected boolean alwaysActive() {
        return true;
    }

    @Override
    protected boolean portless() {
        return true;
    }

    @Override
    public void onWrench(Level level, BlockPos pos, Player player, Direction side) {
        changeFrequency(level, pos, player, 1);
    }

    @Override
    public void onWrenchSneak(Level level, BlockPos pos, Player player) {
        changeFrequency(level, pos, player, -1);
    }

    private static void changeFrequency(Level level, BlockPos pos, Player player, int delta) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        LevelNetworks networks = LevelNetworks.get(server);
        int frequency = Math.max(0, networks.frequency(pos) + delta);
        networks.setFrequency(pos, frequency);
        player.displayClientMessage(Component.translatable("message.vectrum.coder_frequency", frequency), true);
    }
}
