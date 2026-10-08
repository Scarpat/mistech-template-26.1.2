package com.mistech.mistechmod.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Bloco líquido da Essência Mágica Impura: quem encosta ganha Levitação. */
public class ImpureEssenceBlock extends LiquidBlock {
    public static final int LEVITATION_TICKS = 3 * 20;

    public ImpureEssenceBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            // renova só quando está acabando (evita reenviar o efeito todo tick)
            MobEffectInstance current = living.getEffect(MobEffects.LEVITATION);
            if (current == null || current.getDuration() < 10) {
                living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITATION_TICKS, 0, false, true, true));
            }
            // Mácula: 0,5 por segundo encostado
            if (living instanceof net.minecraft.world.entity.player.Player player) {
                com.mistech.mistechmod.purity.Taint.add(player, 0.5F / 20.0F);
            }
        }
    }
}
