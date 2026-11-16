package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Voxel shapes of cables and endpoint blocks; all measures in pixels (16 = one block). */
final class CableShapes {
    private CableShapes() {
    }

    /**
     * Core cube plus one arm per side in {@code armMask}.
     *
     * @param armMask side bits with an arm (cable/endpoint link or attached inventory)
     */
    static VoxelShape build(int armMask,
                            double coreLo, double coreHi,
                            double armLo, double armHi) {
        VoxelShape shape = Block.box(coreLo, coreLo, coreLo, coreHi, coreHi, coreHi);
        for (int i = 0; i < Sides.ALL.length; i++) {
            int bit = 1 << i;
            if ((armMask & bit) != 0) {
                shape = Shapes.or(shape, side(Sides.ALL[i], armLo, armHi, coreLo, coreHi));
            }
        }
        return shape.optimize();
    }

    /** Box from the core face to the block edge in {@code direction}, {@code lo..hi} wide. */
    private static VoxelShape side(Direction direction, double lo, double hi, double coreLo, double coreHi) {
        return switch (direction) {
            case DOWN -> Block.box(lo, 0, lo, hi, coreLo, hi);
            case UP -> Block.box(lo, coreHi, lo, hi, 16, hi);
            case NORTH -> Block.box(lo, lo, 0, hi, hi, coreLo);
            case SOUTH -> Block.box(lo, lo, coreHi, hi, hi, 16);
            case WEST -> Block.box(0, lo, lo, coreLo, hi, hi);
            case EAST -> Block.box(coreHi, lo, lo, 16, hi, hi);
        };
    }
}
