package com.mistech.mistechmod.event;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Efeitos dos charutos/baseados mágicos que reagem a golpes. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class MagicSmokeEvents {
    private static final TeleportRandomlyConsumeEffect TELEPORT = new TeleportRandomlyConsumeEffect();
    private static final float DODGE_CHANCE = 0.25F;

    private MagicSmokeEvents() {}

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();

        // Guarda Gélida: não sofre congelamento
        if (target.hasEffect(MistechEffects.FROST_WARD) && source.is(DamageTypeTags.IS_FREEZING)) {
            event.setCanceled(true);
            target.setTicksFrozen(0);
            return;
        }

        // Desvio Espacial: chance de sumir antes do golpe (não vale para dano de queda, fome, void...)
        if (target.hasEffect(MistechEffects.SPACE_DODGE) && source.getEntity() != null
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && target.getRandom().nextFloat() < DODGE_CHANCE
                && TELEPORT.apply(level, ItemStack.EMPTY, target)) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.3F);
            event.setCanceled(true);
            return;
        }

        // golpe corpo a corpo: quem bateu é a própria fonte direta
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
            if (attacker.hasEffect(MistechEffects.FLAME_TOUCH)) {
                target.igniteForSeconds(4.0F);
            }
            if (attacker.hasEffect(MistechEffects.FROST_TOUCH)) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), attacker);
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + 80));
            }
            if (attacker.hasEffect(MistechEffects.SHADOW_TOUCH)) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), attacker);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), attacker);
            }
            if (target.hasEffect(MistechEffects.FROST_WARD)) {
                attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), target);
            }
        }
    }
}
