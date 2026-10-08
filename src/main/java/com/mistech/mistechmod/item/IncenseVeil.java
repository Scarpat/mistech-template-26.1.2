package com.mistech.mistechmod.item;

import com.mistech.mistechmod.registry.MistechEffects;
import com.mistech.mistechmod.registry.MistechTags;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Aplica o efeito Véu de Incenso (usado pelo incenso na mão e pelo Porta-Incenso). */
public final class IncenseVeil {
    /** Duração aplicada a cada renovação (1,5 s); renovada a cada segundo enquanto o incenso queima. */
    public static final int REFRESH_TICKS = 30;
    /** Raio em que mobs que já estavam perseguindo o jogador perdem o alvo. */
    public static final double CALM_RADIUS = 32.0;

    private IncenseVeil() {}

    public static void apply(ServerLevel level, LivingEntity target) {
        apply(level, target, CALM_RADIUS);
    }

    /** Igual, com raio de perda de alvo próprio (o Incenso Mágico usa 4×). */
    public static void apply(ServerLevel level, LivingEntity target, double calmRadius) {
        target.addEffect(new MobEffectInstance(MistechEffects.INCENSE_VEIL, REFRESH_TICKS, 0, true, true, true));

        if (target instanceof Player player) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(calmRadius),
                    m -> m.getTarget() == player && m.is(MistechTags.INCENSE_IGNORES))) {
                mob.setTarget(null);
            }
        }
    }
}
