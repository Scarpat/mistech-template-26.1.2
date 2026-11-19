package com.mistech.mistechmod.refining;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.AstralPillarBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Prisma Astral: o Núcleo fica no centro de uma base 5×5 de blocos de lápis-lazúli (a camada logo abaixo dele), com
 * um Pilar de Conexão Astral em cada canto da base (em cima dela) e um bloco de vidro em cima do Núcleo.
 * Visto de cima (camada do Núcleo):
 * <pre>
 *   P . . . P
 *   . . . . .
 *   . . N . .     embaixo: 25 blocos de lápis   em cima do N: vidro
 *   . . . . .
 *   P . . . P
 * </pre>
 */
public final class AstralPrismStructure {
    private AstralPrismStructure() {}

    public enum Kind { LAPIS, PILLAR, GLASS }

    public record Piece(BlockPos pos, Kind kind) {}

    public static List<Piece> pieces(BlockPos core) {
        List<Piece> list = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                list.add(new Piece(core.offset(dx, -1, dz), Kind.LAPIS));
            }
        }
        for (int dx : new int[] {-2, 2}) {
            for (int dz : new int[] {-2, 2}) {
                list.add(new Piece(core.offset(dx, 0, dz), Kind.PILLAR));
            }
        }
        list.add(new Piece(core.above(), Kind.GLASS));
        return list;
    }

    public static boolean matches(Level level, Piece piece) {
        BlockState s = level.getBlockState(piece.pos());
        return switch (piece.kind()) {
            case LAPIS -> s.is(Blocks.LAPIS_BLOCK);
            case PILLAR -> s.is(Mistech.ASTRAL_PILLAR.get()) && s.getValue(AstralPillarBlock.PART) == AstralPillarBlock.Part.BOTTOM;
            case GLASS -> s.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS);
        };
    }

    /** Primeira peça errada (ou null se está completo). */
    public static @Nullable Piece firstError(Level level, BlockPos core) {
        for (Piece p : pieces(core)) {
            if (!matches(level, p)) {
                return p;
            }
        }
        return null;
    }
}
