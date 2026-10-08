package com.mistech.mistechmod.sky;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Colocar nuvens no mundo (runa, cajado, cachimbo, botas). */
public final class Clouds {
    /** Lã branca convertida em Nuvem Estável por uso da runa. */
    public static final int WOOL_PER_USE = 8;

    private Clouds() {}

    /** Dá para pôr uma nuvem aqui? (ar ou planta substituível, sem líquido, e o jogador pode mexer no lugar). */
    public static boolean canPlace(ServerLevel level, BlockPos pos, @Nullable Player player) {
        if (!level.isInWorldBounds(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.canBeReplaced() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return player == null || level.mayInteract(player, pos);
    }

    /** Põe uma nuvem temporária (30 s). @return true se colocou. */
    public static boolean placeTemporary(ServerLevel level, BlockPos pos, @Nullable Player player) {
        if (!canPlace(level, pos, player)) {
            return false;
        }
        level.setBlock(pos, SkyContent.CLOUD_BLOCK.get().defaultBlockState(), 3);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.01);
        return true;
    }

    /** Plataforma quadrada de nuvens temporárias centrada em {@code center} (raio 1 = 3×3). @return quantas colocou. */
    public static int platform(ServerLevel level, BlockPos center, int radius, @Nullable Player player) {
        int placed = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (placeTemporary(level, center.offset(dx, 0, dz), player)) {
                    placed++;
                }
            }
        }
        if (placed > 0) {
            level.playSound(null, center, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        return placed;
    }

    /** Ponte de nuvens: {@code length} blocos na direção {@code dir}, largura 3, a partir de {@code start} (exclusive). */
    public static int bridge(ServerLevel level, BlockPos start, Direction dir, int length, @Nullable Player player) {
        Direction side = dir.getClockWise();
        int placed = 0;
        for (int i = 1; i <= length; i++) {
            BlockPos center = start.relative(dir, i);
            for (int w = -1; w <= 1; w++) {
                if (placeTemporary(level, center.relative(side, w), player)) {
                    placed++;
                }
            }
        }
        if (placed > 0) {
            level.playSound(null, start, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.6F, 1.3F);
        }
        return placed;
    }

    /** Lã branca ligada a {@code start} (até {@link #WOOL_PER_USE} blocos) vira Nuvem Estável. @return quantos converteu. */
    public static int condenseWool(ServerLevel level, BlockPos start, @Nullable Player player) {
        if (!level.getBlockState(start).is(Blocks.WHITE_WOOL)) {
            return 0;
        }
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        int converted = 0;
        while (!queue.isEmpty() && converted < WOOL_PER_USE) {
            BlockPos pos = queue.poll();
            if (player != null && !level.mayInteract(player, pos)) {
                continue;
            }
            level.setBlock(pos, SkyContent.STABLE_CLOUD.get().defaultBlockState(), 3);
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.35, 0.35, 0.35, 0.02);
            converted++;
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (seen.add(next) && level.getBlockState(next).is(Blocks.WHITE_WOOL)) {
                    queue.add(next);
                }
            }
        }
        if (converted > 0) {
            level.playSound(null, start, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        return converted;
    }
}
