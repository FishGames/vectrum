package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.core.network.NodeKind;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ein Transportkabel. Es hat keinen Blockentity: welche Seiten verbunden sind, steht im BlockState, und die
 * Zugehörigkeit zu einem Netz verwaltet {@link LevelNetworks}.
 */
public class CableBlock extends Block implements NetworkBlock {
    /** Reihenfolge wie {@link Sides#ALL}: unten, oben, Norden, Süden, Westen, Osten. */
    public static final BooleanProperty[] CONNECTIONS = {
            BlockStateProperties.DOWN, BlockStateProperties.UP,
            BlockStateProperties.NORTH, BlockStateProperties.SOUTH,
            BlockStateProperties.WEST, BlockStateProperties.EAST
    };

    private static final VoxelShape[] SELECTION = new VoxelShape[64]; // 10 px: Zielen und Platzieren
    private static final VoxelShape[] COLLISION = new VoxelShape[64]; // 6 px: Anstoßen

    static {
        for (int mask = 0; mask < 64; mask++) {
            SELECTION[mask] = CableShapes.build(mask, 0, 3, 13, 3, 13, 0, 0, 0);
            COLLISION[mask] = CableShapes.build(mask, 0, 5, 11, 5, 11, 0, 0, 0);
        }
    }

    private final TransportType type;

    public CableBlock(TransportType type, Properties properties) {
        super(properties);
        this.type = type;
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    public TransportType transportType() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTIONS);
    }

    /** Bitmaske der verbundenen Seiten, wie sie der Netzwerk-Kern erwartet. */
    public static int sideMask(BlockState state) {
        int mask = 0;
        for (int i = 0; i < CONNECTIONS.length; i++) {
            if (state.getValue(CONNECTIONS[i])) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    // ------------------------------------------------------------------ Zustand

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    /**
     * Verbindet sich mit jedem Nachbarn, der zum selben Netz-Typ gehört. Bei Nachbarn in nicht geladenen Chunks
     * bleibt der bisherige Wert, damit am Chunkrand keine Verbindung verloren geht (und kein Chunk geladen wird).
     */
    private BlockState computeState(Level level, BlockPos pos, BlockState state) {
        for (int i = 0; i < CONNECTIONS.length; i++) {
            BlockPos neighbourPos = pos.relative(Sides.ALL[i]);
            if (level.hasChunkAt(neighbourPos)) {
                boolean connected = NetworkBlock.connects(level.getBlockState(neighbourPos), type);
                state = state.setValue(CONNECTIONS[i], connected);
            }
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        BlockState updated = computeState(level, pos, state);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ Netz

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel server) {
            LevelNetworks.get(server).put(type, pos, NodeKind.CABLE, sideMask(state));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            LevelNetworks.get(server).remove(type, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ------------------------------------------------------------------ Form

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SELECTION[sideMask(state)];
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION[sideMask(state)];
    }
}
