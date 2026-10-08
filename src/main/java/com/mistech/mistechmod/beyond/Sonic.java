package com.mistech.mistechmod.beyond;

import java.util.Comparator;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * O grito do Warden (Sonic Boom) saindo do jogador: raio reto na direção do olhar que acerta todas as criaturas na linha.
 * Dano do tipo {@code minecraft:sonic_boom} (ignora armadura, encantamentos e resistência) e a mesma repulsão do Warden.
 */
public final class Sonic {
    private Sonic() {}

    /**
     * @param range     alcance em blocos
     * @param damage    dano em cada criatura atingida
     * @param throughWalls  true = o grito atravessa blocos (como o do Warden); false = para no primeiro bloco sólido
     * @return quantas criaturas foram atingidas
     */
    public static int boom(ServerLevel level, Player player, double range, float damage, boolean throughWalls) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        if (!throughWalls) {
            BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (block.getType() != HitResult.Type.MISS) {
                end = block.getLocation();
            }
        }
        Vec3 from = eye.add(look.scale(0.6)).add(0.0, -0.15, 0.0);
        double length = end.distanceTo(from);
        for (int i = 1; i < length; i++) {
            Vec3 p = from.add(look.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 1.0F);

        final Vec3 start = eye;
        final Vec3 stop = end;
        List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, new AABB(start, stop).inflate(1.5),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.getBoundingBox().inflate(0.6).clip(start, stop).isPresent());
        hits.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        int struck = 0;
        for (LivingEntity target : hits) {
            if (target.hurtServer(level, level.damageSources().sonicBoom(player), damage)) {
                double resist = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                target.push(look.x * 2.5 * resist, look.y * 0.5 * resist + 0.1, look.z * 2.5 * resist);
                target.hurtMarked = true;
                struck++;
            }
        }
        // o grito é uma vibração forte: sensores, shriekers e o Warden escutam
        level.gameEvent(GameEvent.RESONATE_15, player.position(), new GameEvent.Context(null, null));
        return struck;
    }
}
