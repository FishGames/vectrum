package io.github.fishgames.vectrum.block;

import io.github.fishgames.vectrum.world.Sides;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Baut die Umrisse von Kabeln und Endpunkten. Alle Maße sind in Pixeln (16 = ein Block).
 * Kabel: Kollision 6 px (5..11), Auswahl und Modell 10 px (3..13), so wie in {@code docs/implementierungs-prompt.md}.
 */
final class CableShapes {
    private CableShapes() {
    }

    /**
     * @param linkMask Seiten mit Verbindung zu einem Kabel/Endpunkt (Arme)
     * @param portMask Seiten mit angeschlossenem Inventar (flache Platten, {@code plateDepth} dick)
     */
    static VoxelShape build(int linkMask, int portMask,
                            double coreLo, double coreHi,
                            double armLo, double armHi,
                            double plateLo, double plateHi, double plateDepth) {
        VoxelShape shape = Block.box(coreLo, coreLo, coreLo, coreHi, coreHi, coreHi);
        for (int i = 0; i < Sides.ALL.length; i++) {
            int bit = 1 << i;
            if ((linkMask & bit) != 0) {
                shape = Shapes.or(shape, side(Sides.ALL[i], armLo, armHi, coreLo, coreHi));
            } else if ((portMask & bit) != 0) {
                shape = Shapes.or(shape, side(Sides.ALL[i], plateLo, plateHi, plateDepth, 16 - plateDepth));
            }
        }
        return shape.optimize();
    }

    /** Quader von der Kernfläche bis zum Blockrand in Richtung {@code direction}, {@code lo..hi} breit. */
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
