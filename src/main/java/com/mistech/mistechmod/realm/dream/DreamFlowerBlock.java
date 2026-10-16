package com.mistech.mistechmod.realm.dream;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Flor do Sonho (vai no ensopado suspeito como as do jogo), com partícula opcional. */
public class DreamFlowerBlock extends FlowerBlock {
    public static final MapCodec<DreamFlowerBlock> CODEC = simpleCodec(p -> new DreamFlowerBlock(MobEffects.NIGHT_VISION, 5.0F, p, null, 0));
    private final @Nullable ParticleOptions particle;
    private final int particleChance;

    public DreamFlowerBlock(Holder<MobEffect> stewEffect, float seconds, BlockBehaviour.Properties properties, @Nullable ParticleOptions particle,
            int particleChance) {
        super(stewEffect, seconds, properties);
        this.particle = particle;
        this.particleChance = particleChance;
    }

    @Override
    public MapCodec<? extends FlowerBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (this.particle != null && random.nextInt(this.particleChance) == 0) {
            level.addParticle(this.particle, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.5 + random.nextDouble() * 0.3,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.01, 0.0);
        }
    }
}
