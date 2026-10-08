package com.mistech.mistechmod.ritual;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.AstralPillarBlock;
import com.mistech.mistechmod.block.IncenseHolderBlock;
import com.mistech.mistechmod.block.ritual.RitualPedestalBlockEntity;
import com.mistech.mistechmod.registry.RitualContent;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Círculos do Altar Impuro (tudo na altura do altar). A = altar, G = Marca de Giz, X = Marca de Giz Impuro,
 * P = Pedestal Ritual, I = Porta-Incenso com Incenso Mágico aceso, S = Pilar de Conexão Astral,
 * O = Obelisco de Mitril, B = Braseiro de Oricalco aceso.
 * <pre>
 * Tier I (5×5)      Tier II (9×9)            Tier III (11×11) = Tier II + pilares + braseiros
 * I . P . I         O . . . . . . . O        . . . . . B . . . . .
 * . G G G .         . I . . P . . I .        . O . . . . . . . O .
 * P G A G P         . . P . X . P . .        . . I . . S . . I . .
 * . G G G .         . . . G G G . . .        . . . P . P . P . . .
 * I . P . I         . P X G A G X P .        . . S . . X . . S . .   (etc.: pilares cardeais a 4,
 *                   . . . G G G . . .        B . P X G A G X P . B    braseiros cardeais a 5)
 *                   . . P . X . P . .
 *                   . I . . P . . I .
 *                   O . . . . . . . O
 * </pre>
 * Os Obeliscos (diagonais a 4) e os Braseiros (cardeais a 5) precisam estar prontos ANTES da Ascensão
 * correspondente — o ritual confere ({@link #checkAscension}).
 */
public final class AltarStructure {
    public enum Problem { NONE, CHALK, IMPURE_CHALK, PEDESTAL, INCENSE_HOLDER, INCENSE_UNLIT, PILLAR, OBELISK, BRAZIER, BRAZIER_UNLIT }

    public record Result(Problem problem, @Nullable BlockPos pos) {
        public boolean ok() {
            return this.problem == Problem.NONE;
        }
    }

    private static final int[][] CARDINAL = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private static final int[][] DIAGONAL = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};

    private AltarStructure() {}

    private static List<BlockPos> ring(BlockPos altar, int[][] dirs, int distance) {
        List<BlockPos> list = new ArrayList<>(4);
        for (int[] d : dirs) {
            list.add(altar.offset(d[0] * distance, 0, d[1] * distance));
        }
        return list;
    }

    /** Pedestais: Tier I = 4 cardeais a 2; Tier II/III = 4 cardeais a 3 + 4 diagonais a (2,2). */
    public static List<BlockPos> pedestals(BlockPos altar, int tier) {
        if (tier <= 1) {
            return ring(altar, CARDINAL, 2);
        }
        List<BlockPos> list = ring(altar, CARDINAL, 3);
        list.addAll(ring(altar, DIAGONAL, 2));
        return list;
    }

    /** Porta-incensos: Tier I nos cantos (2,2); Tier II/III nos cantos (3,3). */
    public static List<BlockPos> holders(BlockPos altar, int tier) {
        return ring(altar, DIAGONAL, tier <= 1 ? 2 : 3);
    }

    public static List<BlockPos> chalk(BlockPos altar) {
        List<BlockPos> list = new ArrayList<>(8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    list.add(altar.offset(dx, 0, dz));
                }
            }
        }
        return list;
    }

    public static List<BlockPos> impureChalk(BlockPos altar) {
        return ring(altar, CARDINAL, 2);
    }

    public static List<BlockPos> pillars(BlockPos altar) {
        return ring(altar, CARDINAL, 4);
    }

    /** Obeliscos de Mitril: Tier II/III, diagonais a (4,4). */
    public static List<BlockPos> obelisks(BlockPos altar) {
        return ring(altar, DIAGONAL, 4);
    }

    /** Braseiros de Oricalco: Tier III, cardeais a 5 (atrás dos pilares). */
    public static List<BlockPos> braziers(BlockPos altar) {
        return ring(altar, CARDINAL, 5);
    }

    private static Result checkObelisks(Level level, BlockPos altar) {
        for (BlockPos pos : obelisks(altar)) {
            if (!level.getBlockState(pos).is(com.mistech.mistechmod.registry.AltarContent.MITHRIL_OBELISK.get())) {
                return new Result(Problem.OBELISK, pos);
            }
        }
        return new Result(Problem.NONE, null);
    }

    private static Result checkPillars(Level level, BlockPos altar) {
        for (BlockPos pos : pillars(altar)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(Mistech.ASTRAL_PILLAR.get()) || state.getValue(AstralPillarBlock.PART) != AstralPillarBlock.Part.BOTTOM) {
                return new Result(Problem.PILLAR, pos);
            }
        }
        return new Result(Problem.NONE, null);
    }

    private static Result checkBraziers(Level level, BlockPos altar) {
        for (BlockPos pos : braziers(altar)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(com.mistech.mistechmod.registry.AltarContent.ORICHALCUM_BRAZIER.get())) {
                return new Result(Problem.BRAZIER, pos);
            }
            if (!state.getValue(com.mistech.mistechmod.block.ritual.OrichalcumBrazierBlock.LIT)) {
                return new Result(Problem.BRAZIER_UNLIT, pos);
            }
        }
        return new Result(Problem.NONE, null);
    }

    /**
     * Peças novas do próximo tier, que precisam estar no lugar antes da Ascensão:
     * Tier II = 4 Obeliscos de Mitril; Tier III = 4 Pilares Astrais + 4 Braseiros de Oricalco acesos.
     */
    public static Result checkAscension(Level level, BlockPos altar, int targetTier) {
        if (targetTier == 2) {
            return checkObelisks(level, altar);
        }
        Result r = checkPillars(level, altar);
        return r.ok() ? checkBraziers(level, altar) : r;
    }

    /** Qualquer altar cujo círculo (de qualquer tier) usa este pedestal. */
    public static boolean usesPedestal(BlockPos altar, int tier, BlockPos pedestal) {
        return pedestals(altar, tier).contains(pedestal);
    }

    public static Result check(Level level, BlockPos altar, int tier, boolean needLit) {
        for (BlockPos pos : chalk(altar)) {
            if (!level.getBlockState(pos).is(RitualContent.CHALK_MARK.get())) {
                return new Result(Problem.CHALK, pos);
            }
        }
        if (tier >= 2) {
            for (BlockPos pos : impureChalk(altar)) {
                if (!level.getBlockState(pos).is(RitualContent.IMPURE_CHALK_MARK.get())) {
                    return new Result(Problem.IMPURE_CHALK, pos);
                }
            }
        }
        for (BlockPos pos : pedestals(altar, tier)) {
            if (!(level.getBlockEntity(pos) instanceof RitualPedestalBlockEntity)) {
                return new Result(Problem.PEDESTAL, pos);
            }
        }
        for (BlockPos pos : holders(altar, tier)) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof IncenseHolderBlock)) {
                return new Result(Problem.INCENSE_HOLDER, pos);
            }
            if (needLit && !(state.getValue(IncenseHolderBlock.LIT) && state.getValue(IncenseHolderBlock.MAGIC))) {
                return new Result(Problem.INCENSE_UNLIT, pos);
            }
        }
        if (tier >= 2) {
            Result r = checkObelisks(level, altar);
            if (!r.ok()) {
                return r;
            }
        }
        if (tier >= 3) {
            Result r = checkPillars(level, altar);
            if (!r.ok()) {
                return r;
            }
            r = checkBraziers(level, altar);
            if (!r.ok()) {
                return r;
            }
        }
        return new Result(Problem.NONE, null);
    }
}
