package com.mistech.mistechmod.astrology;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Campo de Astrologia: base 7x7 de blocos de lápis-lazúli (L) e ouro (O). '0' = qualquer bloco.
 * A mesa fica em cima do bloco central (que é de ouro). O desenho é simétrico, então vale em qualquer direção.
 */
public final class AstrologyField {
    public static final String[] PATTERN = {
            "00LLL00",
            "0LLOLL0",
            "LLLLLLL",
            "LOLOLOL",
            "LLLLLLL",
            "0LLOLL0",
            "00LLL00",
    };
    public static final int RADIUS = 3;

    private AstrologyField() {}

    /** @param tablePos posição da mesa (a base fica logo abaixo). */
    public static boolean isValid(Level level, BlockPos tablePos) {
        return firstError(level, tablePos) == null;
    }

    /** Primeira posição da base que está errada, ou null se o campo estiver completo. */
    public static BlockPos firstError(Level level, BlockPos tablePos) {
        BlockPos center = tablePos.below();
        for (int row = 0; row < PATTERN.length; row++) {
            for (int col = 0; col < PATTERN[row].length(); col++) {
                Block expected = switch (PATTERN[row].charAt(col)) {
                    case 'L' -> Blocks.LAPIS_BLOCK;
                    case 'O' -> Blocks.GOLD_BLOCK;
                    default -> null;
                };
                if (expected == null) {
                    continue;
                }
                BlockPos pos = center.offset(col - RADIUS, 0, row - RADIUS);
                if (!level.getBlockState(pos).is(expected)) {
                    return pos;
                }
            }
        }
        return null;
    }
}
