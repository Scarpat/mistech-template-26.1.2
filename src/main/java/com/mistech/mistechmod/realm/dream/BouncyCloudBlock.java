package com.mistech.mistechmod.realm.dream;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Nuvem Saltitante: pisar nela joga lá para cima (~85 blocos) com Queda Lenta — o jeito de subir do chão até as ilhas. Agachado não pula. */
public class BouncyCloudBlock extends Block {
    public static final MapCodec<BouncyCloudBlock> CODEC = simpleCodec(BouncyCloudBlock::new);
    public static final double LAUNCH = 4.8;

    public BouncyCloudBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.stepOn(level, pos, state, entity);
            return;
        }
        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x * 1.2, LAUNCH, v.z * 1.2);
        entity.resetFallDistance();
        entity.hurtMarked = true;
        if (entity instanceof net.minecraft.world.entity.LivingEntity living && !level.isClientSide()) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 120, 0, false, false));
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY(), entity.getZ(), 20, 0.5, 0.1, 0.5, 0.08);
            server.playSound(null, pos, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
        }
    }
}
