package com.mistech.mistechmod.purity;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.ritual.RitualPedestalBlockEntity;
import com.mistech.mistechmod.registry.AltarContent;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

/**
 * Santuário (5×5, tudo na altura da Fonte). F = Fonte do Santuário, P = Pedestal Ritual, W = fonte de água, O = Obelisco de Mitril.
 * <pre>
 * O . P . O
 * . W . W .
 * P . F . P
 * . W . W .
 * O . P . O
 * </pre>
 * Além disso: céu aberto em cima da Fonte e nada impuro (tag {@code mistech:impure_blocks}) a até
 * {@link #PURITY_RADIUS} blocos na horizontal e {@link #PURITY_HEIGHT} na vertical.
 */
public final class SanctuaryStructure {
    public enum Problem { NONE, PEDESTAL, WATER, OBELISK, SKY, IMPURITY }

    public record Result(Problem problem, @Nullable BlockPos pos) {
        public boolean ok() {
            return this.problem == Problem.NONE;
        }
    }

    public static final int PURITY_RADIUS = 8;
    public static final int PURITY_HEIGHT = 4;

    private static final int[][] CARDINAL = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private static final int[][] DIAGONAL = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};

    private SanctuaryStructure() {}

    private static List<BlockPos> ring(BlockPos font, int[][] dirs, int distance) {
        List<BlockPos> list = new ArrayList<>(4);
        for (int[] d : dirs) {
            list.add(font.offset(d[0] * distance, 0, d[1] * distance));
        }
        return list;
    }

    public static List<BlockPos> pedestals(BlockPos font) {
        return ring(font, CARDINAL, 2);
    }

    public static List<BlockPos> waters(BlockPos font) {
        return ring(font, DIAGONAL, 1);
    }

    public static List<BlockPos> obelisks(BlockPos font) {
        return ring(font, DIAGONAL, 2);
    }

    /** Peças do círculo (rápido; conferido a cada 10 ticks durante o ritual). */
    public static Result checkShape(Level level, BlockPos font) {
        for (BlockPos pos : pedestals(font)) {
            if (!(level.getBlockEntity(pos) instanceof RitualPedestalBlockEntity)) {
                return new Result(Problem.PEDESTAL, pos);
            }
        }
        for (BlockPos pos : waters(font)) {
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) {
                return new Result(Problem.WATER, pos);
            }
        }
        for (BlockPos pos : obelisks(font)) {
            if (!level.getBlockState(pos).is(AltarContent.MITHRIL_OBELISK.get())) {
                return new Result(Problem.OBELISK, pos);
            }
        }
        if (!Taint.openSky(level, font.above())) {
            return new Result(Problem.SKY, font.above());
        }
        return new Result(Problem.NONE, null);
    }

    /** Procura algo impuro em volta (mais pesado; conferido ao começar e a cada 5 s). */
    public static Result checkPurity(Level level, BlockPos font) {
        for (BlockPos pos : BlockPos.betweenClosed(font.offset(-PURITY_RADIUS, -PURITY_HEIGHT, -PURITY_RADIUS),
                font.offset(PURITY_RADIUS, PURITY_HEIGHT, PURITY_RADIUS))) {
            if (level.getBlockState(pos).is(PurityContent.IMPURE_BLOCKS)) {
                return new Result(Problem.IMPURITY, pos.immutable());
            }
        }
        return new Result(Problem.NONE, null);
    }

    public static Result check(Level level, BlockPos font) {
        Result shape = checkShape(level, font);
        return shape.ok() ? checkPurity(level, font) : shape;
    }
}
