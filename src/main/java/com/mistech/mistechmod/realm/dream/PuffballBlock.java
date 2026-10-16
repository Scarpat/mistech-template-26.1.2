package com.mistech.mistechmod.realm.dream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Dente-de-Leão Gigante: passar por ele solta as sementes e dá Queda Lenta (8 s). */
public class PuffballBlock extends DreamPlantBlock {
    public PuffballBlock(BlockBehaviour.Properties properties) {
        super(properties, 14.0, ParticleTypes.WHITE_ASH, 6);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living && !living.hasEffect(MobEffects.SLOW_FALLING)) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
            server.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 30, 0.4, 0.5, 0.4, 0.05);
            server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.3, 0.4, 0.3, 0.02);
            server.playSound(null, pos, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 0.6F, 1.6F);
        }
    }
}
