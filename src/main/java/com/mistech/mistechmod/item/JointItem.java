package com.mistech.mistechmod.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Baseado: cada tragada deixa a visão turva (náusea) e dá fome. */
public class JointItem extends SmokableItem {
    public static final int BLUR_TICKS = 10 * 20;
    public static final int HUNGER_TICKS = 15 * 20;

    public JointItem(Properties properties) {
        super(properties, 120, 5);
    }

    @Override
    protected void applyPuffEffects(ServerLevel level, LivingEntity entity) {
        applyHemp(entity);
    }

    /** Efeitos de uma tragada de cânhamo (também usado pelo Cachimbo). */
    public static void applyHemp(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, BLUR_TICKS, 0, false, true, true));
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, HUNGER_TICKS, 0, false, true, true));
    }
}
