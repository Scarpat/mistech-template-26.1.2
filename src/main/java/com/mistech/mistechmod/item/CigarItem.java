package com.mistech.mistechmod.item;

import java.util.List;

import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Charuto: cada tragada remove os efeitos negativos e dá "Foco do Tabaco" (FOV menor, visão mais "fechada").
 */
public class CigarItem extends SmokableItem {
    public static final int FOCUS_TICKS = 20 * 20;

    public CigarItem(Properties properties) {
        super(properties, 180, 5);
    }

    @Override
    protected void applyPuffEffects(ServerLevel level, LivingEntity entity) {
        applyTobacco(entity);
    }

    /** Efeitos de uma tragada de tabaco (também usado pelo Cachimbo). */
    public static void applyTobacco(LivingEntity entity) {
        List<Holder<MobEffect>> harmful = entity.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> effect.value().getCategory() == MobEffectCategory.HARMFUL)
                .toList();
        harmful.forEach(entity::removeEffect);
        entity.addEffect(new MobEffectInstance(MistechEffects.TOBACCO_FOCUS, FOCUS_TICKS, 0, false, true, true));
    }
}
