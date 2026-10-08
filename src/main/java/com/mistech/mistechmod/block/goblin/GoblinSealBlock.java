package com.mistech.mistechmod.block.goblin;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.GoblinContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Selo Corrompido: parede indestrutível que tampa passagens das catacumbas.
 * Some quando o puzzle perto dele é resolvido (Fechadura de Ossos, Totens, Painel de Runas ou a morte da Rainha).
 */
public class GoblinSealBlock extends Block {
    public GoblinSealBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            Direction side = Direction.getRandom(random);
            BlockPos out = pos.relative(side);
            if (!level.getBlockState(out).isSolidRender()) {
                level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5 + side.getStepX() * 0.6, pos.getY() + random.nextDouble(),
                        pos.getZ() + 0.5 + side.getStepZ() * 0.6, 0.0, 0.02, 0.0);
            }
        }
    }

    /**
     * Acha o selo mais perto de {@code center} (até {@code radius} blocos) e desfaz ele e todos os selos encostados.
     * @return quantos blocos sumiram
     */
    public static int dissolveNearest(ServerLevel level, BlockPos center, int radius) {
        BlockPos start = findNearest(level, center, radius);
        if (start == null) {
            return 0;
        }
        Block seal = GoblinContent.GOBLIN_SEAL.get();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        int count = 0;
        while (!queue.isEmpty() && count < 256) {
            BlockPos p = queue.poll();
            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
            count++;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n) && level.getBlockState(n).is(seal)) {
                    queue.add(n);
                }
            }
        }
        level.playSound(null, start, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.2F, 0.7F);
        level.playSound(null, start, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
        return count;
    }

    private static @Nullable BlockPos findNearest(ServerLevel level, BlockPos center, int radius) {
        Block seal = GoblinContent.GOBLIN_SEAL.get();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (level.getBlockState(p).is(seal)) {
                double d = p.distSqr(center);
                if (d < bestDist) {
                    bestDist = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }
}
