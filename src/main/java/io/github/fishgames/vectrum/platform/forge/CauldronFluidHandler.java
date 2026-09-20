package io.github.fishgames.vectrum.platform.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * {@link IFluidHandler} for vanilla cauldrons: one tank of one bucket (1000 mB) holding water or lava; partially
 * filled water cauldrons count as empty.
 */
final class CauldronFluidHandler implements IFluidHandler {
    private static final int BUCKET = 1000;

    private final Level level;
    private final BlockPos pos;

    private CauldronFluidHandler(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    /** Handler for the cauldron at this position, or {@code null} if there is none. */
    static CauldronFluidHandler find(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean cauldron = state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON) || state.is(Blocks.LAVA_CAULDRON);
        return cauldron ? new CauldronFluidHandler(level, pos) : null;
    }

    /** Fluid of a full cauldron; {@code EMPTY} if empty or partially filled. */
    private Fluid content() {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LAVA_CAULDRON)) {
            return Fluids.LAVA;
        }
        if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL) {
            return Fluids.WATER;
        }
        return Fluids.EMPTY;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        Fluid content = content();
        return content == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(content, BUCKET);
    }

    @Override
    public int getTankCapacity(int tank) {
        return BUCKET;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return stack.getFluid().isSame(Fluids.WATER) || stack.getFluid().isSame(Fluids.LAVA);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || resource.getAmount() < BUCKET || !level.getBlockState(pos).is(Blocks.CAULDRON)) {
            return 0;
        }
        BlockState filled;
        if (resource.getFluid().isSame(Fluids.WATER)) {
            filled = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL);
        } else if (resource.getFluid().isSame(Fluids.LAVA)) {
            filled = Blocks.LAVA_CAULDRON.defaultBlockState();
        } else {
            return 0;
        }
        if (action.execute()) {
            level.setBlockAndUpdate(pos, filled);
        }
        return BUCKET;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        Fluid content = content();
        if (resource.isEmpty() || content == Fluids.EMPTY || !resource.getFluid().isSame(content)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        Fluid content = content();
        if (content == Fluids.EMPTY || maxDrain < BUCKET) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
        }
        return new FluidStack(content, BUCKET);
    }
}
