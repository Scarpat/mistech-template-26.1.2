package com.mistech.mistechmod.sky;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Nuvem da Runa das Nuvens: igual à {@link CloudBlock}, mas se desfaz sozinha depois de {@link #LIFETIME} ticks (30 s).
 * Não tem item nem drop.
 */
public class TemporaryCloudBlock extends CloudBlock {
    public static final MapCodec<TemporaryCloudBlock> CODEC = simpleCodec(TemporaryCloudBlock::new);
    public static final int LIFETIME = 30 * 20;

    public TemporaryCloudBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends TemporaryCloudBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            // um pouco de variação para a ponte não sumir toda de uma vez
            level.scheduleTick(pos, this, LIFETIME + level.getRandom().nextInt(20));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.35, 0.35, 0.35, 0.01);
        level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.4F, 1.6F);
    }
}
