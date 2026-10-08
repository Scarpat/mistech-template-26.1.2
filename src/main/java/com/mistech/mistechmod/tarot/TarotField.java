package com.mistech.mistechmod.tarot;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.ritual.RitualPedestalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Círculo de Tarologia: base 7x7 de ametista (A), obsidiana (O) e obsidiana chorona (C). '0' = qualquer bloco.
 * A mesa fica em cima do bloco central (obsidiana chorona). Simétrico: vale em qualquer direção.
 * <p>
 * Os itens da invocação/forja vão em até 8 Pedestais Rituais em cima da base, na altura da mesa:
 * 4 nos pontos cardeais (3 blocos da mesa, sobre ametista) e 4 nas diagonais (2,2), sobre obsidiana chorona.
 */
public final class TarotField {
    public static final String[] PATTERN = {
            "00AAA00",
            "0COAOC0",
            "AOAAAOA",
            "AAACAAA",
            "AOAAAOA",
            "0COAOC0",
            "00AAA00",
    };
    public static final int RADIUS = 3;

    /** Posições dos pedestais em relação à mesa (mesma altura). */
    public static final int[][] PEDESTAL_OFFSETS = {
            {0, -3}, {3, 0}, {0, 3}, {-3, 0},
            {2, -2}, {2, 2}, {-2, 2}, {-2, -2},
    };
    public static final int PEDESTALS = PEDESTAL_OFFSETS.length;

    private TarotField() {}

    public static boolean isValid(Level level, BlockPos tablePos) {
        return firstError(level, tablePos) == null;
    }

    /** O bloco embaixo da mesa é o centro do círculo? (para escolher qual multibloco reclamar) */
    public static boolean looksLikeTarot(Level level, BlockPos tablePos) {
        return level.getBlockState(tablePos.below()).is(Blocks.CRYING_OBSIDIAN);
    }

    /** Primeira posição da base que está errada, ou null se o círculo estiver completo. */
    public static @Nullable BlockPos firstError(Level level, BlockPos tablePos) {
        BlockPos center = tablePos.below();
        for (int row = 0; row < PATTERN.length; row++) {
            for (int col = 0; col < PATTERN[row].length(); col++) {
                Block expected = switch (PATTERN[row].charAt(col)) {
                    case 'A' -> Blocks.AMETHYST_BLOCK;
                    case 'O' -> Blocks.OBSIDIAN;
                    case 'C' -> Blocks.CRYING_OBSIDIAN;
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

    public static BlockPos pedestalPos(BlockPos tablePos, int index) {
        int[] o = PEDESTAL_OFFSETS[index];
        return tablePos.offset(o[0], 0, o[1]);
    }

    /** Os pedestais que existem (na ordem de {@link #PEDESTAL_OFFSETS}; ausentes ficam de fora). Funciona no cliente também. */
    public static List<RitualPedestalBlockEntity> pedestals(Level level, BlockPos tablePos) {
        List<RitualPedestalBlockEntity> list = new ArrayList<>();
        for (int i = 0; i < PEDESTALS; i++) {
            if (level.getBlockEntity(pedestalPos(tablePos, i)) instanceof RitualPedestalBlockEntity pedestal) {
                list.add(pedestal);
            }
        }
        return list;
    }

    /** Itens que estão nos pedestais (sem os vazios). */
    public static List<ItemStack> pedestalItems(Level level, BlockPos tablePos) {
        List<ItemStack> items = new ArrayList<>();
        for (RitualPedestalBlockEntity pedestal : pedestals(level, tablePos)) {
            if (!pedestal.getStored().isEmpty()) {
                items.add(pedestal.getStored());
            }
        }
        return items;
    }
}
