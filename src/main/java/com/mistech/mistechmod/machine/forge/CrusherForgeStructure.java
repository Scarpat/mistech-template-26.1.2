package com.mistech.mistechmod.machine.forge;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.machine.arcane.ArcaneMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Forja Trituradora: cubo 2×2×2. Olhando para a frente do Núcleo (N), o cubo vai para a direita, para trás e para cima.
 * <pre>
 * Embaixo:  frente  N  F       Em cima:  frente  T  R
 *           trás    R  R                 trás    R  R
 * </pre>
 * N = Núcleo da Forja, F = Fornalha Arcana, T = Triturador Arcano, R = Bloco de Magia Impura.
 */
public final class CrusherForgeStructure {
    /** Montada: o núcleo desenha o modelo grande e as outras 7 peças ficam invisíveis (mas continuam lá). */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private CrusherForgeStructure() {}

    /** Liga/desliga o visual montado no núcleo e nas peças que estiverem no lugar. */
    public static void setFormed(Level level, BlockPos core, Direction front, boolean formed) {
        set(level, core, formed);
        for (Part part : parts(core, front)) {
            set(level, part.pos(), formed);
        }
    }

    private static void set(Level level, BlockPos pos, boolean formed) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(FORMED) && state.getValue(FORMED) != formed) {
            level.setBlock(pos, state.setValue(FORMED, formed), Block.UPDATE_ALL);
        }
    }

    /** O núcleo de uma Forja montada que usa esta peça (ou null). */
    public static @Nullable BlockPos findCore(Level level, BlockPos part) {
        for (BlockPos p : BlockPos.betweenClosed(part.offset(-1, -1, -1), part.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(p);
            if (!state.is(ForgeContent.CRUSHER_FORGE.get())) {
                continue;
            }
            Direction front = state.getValue(com.mistech.mistechmod.machine.MachineBlock.FACING);
            for (Part candidate : parts(p, front)) {
                if (candidate.pos().equals(part)) {
                    return p.immutable();
                }
            }
        }
        return null;
    }

    /** Clique numa peça montada: abre a tela do núcleo. */
    public static boolean openCore(Level level, BlockPos part, net.minecraft.world.entity.player.Player player) {
        BlockPos core = findCore(level, part);
        if (core == null) {
            return false;
        }
        if (!level.isClientSide() && level.getBlockEntity(core) instanceof CrusherForgeBlockEntity forge) {
            player.openMenu(forge);
        }
        return true;
    }

    public record Part(BlockPos pos, Block block) {}

    /** A direita de quem olha para a frente da máquina. */
    public static Direction right(Direction front) {
        return front.getCounterClockWise();
    }

    /** As 7 peças (sem o núcleo). */
    public static List<Part> parts(BlockPos core, Direction front) {
        Direction right = right(front), back = front.getOpposite();
        Block casing = com.mistech.mistechmod.Mistech.IMPURE_MAGIC_BLOCK.get();
        List<Part> list = new ArrayList<>(7);
        list.add(new Part(core.relative(right), ArcaneMachines.ARCANE_FURNACE.get()));
        list.add(new Part(core.relative(back), casing));
        list.add(new Part(core.relative(back).relative(right), casing));
        BlockPos up = core.above();
        list.add(new Part(up, ArcaneMachines.ARCANE_CRUSHER.get()));
        list.add(new Part(up.relative(right), casing));
        list.add(new Part(up.relative(back), casing));
        list.add(new Part(up.relative(back).relative(right), casing));
        return list;
    }

    /** Primeira peça errada (ou null se está completa). */
    public static @Nullable BlockPos firstError(Level level, BlockPos core, Direction front) {
        for (Part part : parts(core, front)) {
            if (!level.getBlockState(part.pos()).is(part.block())) {
                return part.pos();
            }
        }
        return null;
    }
}
