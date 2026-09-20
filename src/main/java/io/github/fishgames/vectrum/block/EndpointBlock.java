package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.block.entity.EndpointBlockEntity;
import io.github.fishgames.vectrum.core.network.NodeKind;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.registry.ModBlockEntities;
import io.github.fishgames.vectrum.transfer.ItemPorts;
import io.github.fishgames.vectrum.world.LevelNetworks;
import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Ein Endpunkt: pipe-artiger Block, der Ware zwischen angrenzenden Inventaren und dem Netz vermittelt.
 * Er leitet wie ein Kabel (verbindet sich mit Kabeln und Endpunkten desselben Typs) und kann zusätzlich an
 * jeder freien Seite ein Inventar bedienen: als Quelle (entnimmt) oder als Ziel (liefert).
 *
 * <p>Der BlockState zeigt pro Seite, was dort ist ({@link Connection}). Die gewählte Rolle je Seite steht im
 * {@link EndpointBlockEntity}.
 */
public class EndpointBlock extends BaseEntityBlock implements NetworkBlock {
    public static final EnumProperty<Connection> DOWN = EnumProperty.create("down", Connection.class);
    public static final EnumProperty<Connection> UP = EnumProperty.create("up", Connection.class);
    public static final EnumProperty<Connection> NORTH = EnumProperty.create("north", Connection.class);
    public static final EnumProperty<Connection> SOUTH = EnumProperty.create("south", Connection.class);
    public static final EnumProperty<Connection> WEST = EnumProperty.create("west", Connection.class);
    public static final EnumProperty<Connection> EAST = EnumProperty.create("east", Connection.class);
    /** Reihenfolge wie {@link Sides#ALL}. */
    public static final List<EnumProperty<Connection>> SIDES = List.of(DOWN, UP, NORTH, SOUTH, WEST, EAST);

    private static final ConcurrentMap<Integer, VoxelShape> SELECTION = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, VoxelShape> COLLISION = new ConcurrentHashMap<>();

    private final TransportType type;

    public EndpointBlock(TransportType type, Properties properties) {
        super(properties);
        this.type = type;
        BlockState state = stateDefinition.any();
        for (EnumProperty<Connection> property : SIDES) {
            state = state.setValue(property, Connection.NONE);
        }
        registerDefaultState(state);
    }

    @Override
    public TransportType transportType() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    public Connection connection(BlockState state, Direction side) {
        return state.getValue(SIDES.get(side.get3DDataValue()));
    }

    /** Bitmaske der Seiten, die mit dem Netz verbunden sind (Kabel oder Endpunkt), für den Netzwerk-Kern. */
    public int linkMask(BlockState state) {
        return maskOf(state, Connection.LINK);
    }

    private int portMask(BlockState state) {
        return maskOf(state, Connection.INPUT) | maskOf(state, Connection.OUTPUT);
    }

    private int maskOf(BlockState state, Connection wanted) {
        int mask = 0;
        for (int i = 0; i < SIDES.size(); i++) {
            if (state.getValue(SIDES.get(i)) == wanted) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    // ------------------------------------------------------------------ Zustand

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Der Blockentity existiert noch nicht; Inventare werden in setPlacedBy erkannt.
        return computeState(context.getLevel(), context.getClickedPos(), defaultBlockState(), null);
    }

    /**
     * Berechnet für jede Seite, was dort ist: Kabel/Endpunkt (LINK), ein Inventar mit passender Rolle (INPUT/OUTPUT)
     * oder nichts. Nachbarn in nicht geladenen Chunks werden nicht angefasst; dort bleibt der alte Wert.
     */
    private BlockState computeState(Level level, BlockPos pos, BlockState state, EndpointBlockEntity blockEntity) {
        for (int i = 0; i < SIDES.size(); i++) {
            Direction side = Sides.ALL[i];
            BlockPos neighbourPos = pos.relative(side);
            if (!level.hasChunkAt(neighbourPos)) {
                continue;
            }
            Connection connection = Connection.NONE;
            if (NetworkBlock.connects(level.getBlockState(neighbourPos), type)) {
                connection = Connection.LINK;
            } else if (blockEntity != null) {
                EndpointMode mode = blockEntity.mode(side);
                if (mode != EndpointMode.OFF && ItemPorts.find(level, neighbourPos, side.getOpposite()) != null) {
                    connection = mode == EndpointMode.IN ? Connection.INPUT : Connection.OUTPUT;
                }
            }
            state = state.setValue(SIDES.get(i), connection);
        }
        return state;
    }

    /** Prüft die Umgebung neu und passt den BlockState an, falls sich etwas geändert hat. */
    public void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof EndpointBlock)) {
            return;
        }
        EndpointBlockEntity blockEntity = level.getBlockEntity(pos) instanceof EndpointBlockEntity e ? e : null;
        BlockState updated = computeState(level, pos, state, blockEntity);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            refresh(level, pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        if (!level.isClientSide) {
            refresh(level, pos);
        }
    }

    // ------------------------------------------------------------------ Wrench

    /**
     * Schaltet die Rolle der angeklickten Seite um (Ziel, Quelle, Aus) und meldet das Ergebnis in der Aktionsleiste.
     */
    public void onWrench(Level level, BlockPos pos, Player player, Direction side) {
        BlockState state = level.getBlockState(pos);
        Component sideName = Component.translatable("direction.vectrum." + side.getName());
        if (connection(state, side) == Connection.LINK) {
            player.displayClientMessage(Component.translatable("message.vectrum.endpoint_link", sideName), true);
            return;
        }
        if (level.getBlockEntity(pos) instanceof EndpointBlockEntity blockEntity) {
            EndpointMode next = blockEntity.cycleMode(side);
            refresh(level, pos);
            player.displayClientMessage(Component.translatable("message.vectrum.endpoint_mode",
                    sideName, Component.translatable(next.translationKey())), true);
        }
    }

    /**
     * Welche Seite meint ein Klick mit dem Wrench? Die Seite zum Inventar liegt direkt an der Kiste oder Maschine und
     * laesst sich nicht anklicken. Deshalb gilt: Von allen Seiten mit angeschlossenem Inventar (auch abgeschaltete)
     * wird die genommen, die der Klickstelle am naechsten liegt. Gibt es keine, zaehlt die angeklickte Flaeche.
     */
    public Direction pickSide(Level level, BlockPos pos, BlockState state, Vec3 hit, Direction clickedFace) {
        double x = hit.x - pos.getX() - 0.5;
        double y = hit.y - pos.getY() - 0.5;
        double z = hit.z - pos.getZ() - 0.5;

        Direction best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Direction side : Sides.ALL) {
            Connection connection = connection(state, side);
            if (connection == Connection.LINK) {
                continue;
            }
            boolean hasInventory = connection != Connection.NONE;
            if (!hasInventory) {
                BlockPos neighbour = pos.relative(side);
                hasInventory = level.hasChunkAt(neighbour) && ItemPorts.find(level, neighbour, side.getOpposite()) != null;
            }
            if (!hasInventory) {
                continue;
            }
            double score = x * side.getStepX() + y * side.getStepY() + z * side.getStepZ();
            if (score > bestScore) {
                bestScore = score;
                best = side;
            }
        }
        return best != null ? best : clickedFace;
    }

    // ------------------------------------------------------------------ Netz

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel server) {
            LevelNetworks.get(server).put(type, pos, NodeKind.ENDPOINT, linkMask(state));
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

    // ------------------------------------------------------------------ Blockentity

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EndpointBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.ENDPOINT.get(), EndpointBlockEntity::serverTick);
    }

    /** BaseEntityBlock würde den Block unsichtbar machen; wir wollen das normale Blockmodell. */
    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ------------------------------------------------------------------ Form

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int key = linkMask(state) | (portMask(state) << 6);
        return SELECTION.computeIfAbsent(key,
                k -> CableShapes.build(k & 63, k >> 6, 2, 14, 3, 13, 4, 12, 2));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int key = linkMask(state) | (portMask(state) << 6);
        return COLLISION.computeIfAbsent(key,
                k -> CableShapes.build(k & 63, k >> 6, 4, 12, 5, 11, 4, 12, 2));
    }
}
