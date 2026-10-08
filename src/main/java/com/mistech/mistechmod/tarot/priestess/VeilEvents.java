package com.mistech.mistechmod.tarot.priestess;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessories;
import com.mistech.mistechmod.accessory.Accessory;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * O Véu da Sacerdotisa (pingente, espaço de amuleto ou colar do Curios): o jogador fica invisível <b>de verdade</b> —
 * armadura, item na mão e nome também somem para os outros (o cliente não desenha nada dele, ver
 * {@code client.PriestessClient}) e os mobs quase não o percebem.
 * <p>
 * Atacar rasga o véu por {@link #BROKEN_TICKS} ticks (volta sozinho), para não ficar forte demais.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class VeilEvents {
    public static final int BROKEN_TICKS = 60;
    private static final String BROKEN_TAG = "mistech_veil_broken_until";

    private VeilEvents() {}

    public static boolean wearsVeil(Player player) {
        return Accessories.has(player, Accessory.PRIESTESS_VEIL);
    }

    public static boolean isVeiled(Player player) {
        return wearsVeil(player) && player.level().getGameTime() >= player.getPersistentData().getLongOr(BROKEN_TAG, 0L)
                && !player.isSpectator();
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        boolean veiled = isVeiled(player);
        boolean was = player.getData(PriestessContent.VEILED);
        if (veiled != was) {
            player.setData(PriestessContent.VEILED, veiled);
            level.sendParticles(veiled ? ParticleTypes.SQUID_INK : ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                    12, 0.3, 0.6, 0.3, 0.02);
            level.playSound(null, player.blockPosition(), veiled ? SoundEvents.ILLUSIONER_PREPARE_MIRROR : SoundEvents.ILLUSIONER_MIRROR_MOVE,
                    SoundSource.PLAYERS, 0.6F, 1.3F);
        }
        if (veiled && player.tickCount % 10 == 0) {
            MobEffectInstance current = player.getEffect(MobEffects.INVISIBILITY);
            if (current == null || current.getDuration() < 30) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 50, 0, true, false, true));
            }
        }
    }

    /** Mobs enxergam quem está com o véu como se estivesse invisível e sem armadura nenhuma. */
    @SubscribeEvent
    static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player && player.getData(PriestessContent.VEILED)) {
            event.modifyVisibility(0.07);
        }
    }

    /** Atacar alguém rasga o véu por alguns segundos. */
    @SubscribeEvent
    static void onAttack(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker && attacker.level() instanceof ServerLevel
                && wearsVeil(attacker) && event.getEntity() != attacker) {
            attacker.getPersistentData().putLong(BROKEN_TAG, attacker.level().getGameTime() + BROKEN_TICKS);
            MobEffectInstance invisibility = attacker.getEffect(MobEffects.INVISIBILITY);
            if (invisibility != null && invisibility.isAmbient() && invisibility.getDuration() <= 50) {
                attacker.removeEffect(MobEffects.INVISIBILITY); // só a do véu (poção continua)
            }
        }
    }
}
