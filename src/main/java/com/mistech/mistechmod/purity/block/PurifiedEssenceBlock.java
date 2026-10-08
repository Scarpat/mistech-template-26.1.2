package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.purity.Taint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Essência Purificada (fluido): quem encosta cai devagar (Queda Lenta) e perde Mácula (0,5 por segundo, até zero).
 * Para quem está Profano, a pureza queima: 1 de dano mágico por segundo.
 */
public class PurifiedEssenceBlock extends LiquidBlock {
    public static final float CLEANSE_PER_TICK = 0.5F / 20.0F;

    public PurifiedEssenceBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
        if (!(level instanceof ServerLevel server) || !(entity instanceof LivingEntity living)) {
            return;
        }
        MobEffectInstance current = living.getEffect(MobEffects.SLOW_FALLING);
        if (current == null || current.getDuration() < 10) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, true, true));
        }
        if (living instanceof Player player) {
            if (Taint.stage(player) == Taint.Stage.PROFANE && player.tickCount % 20 == 0) {
                player.hurtServer(server, server.damageSources().magic(), 1.0F);
            }
            Taint.purify(player, CLEANSE_PER_TICK);
        }
    }
}
