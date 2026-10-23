package com.mistech.mistechmod.spellcraft;

import java.util.HashSet;
import java.util.Set;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.sky.Clouds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Magias de combinação: Magma (Fogo + Terra), Vapor (Fogo + Água) e Nuvem (Ar + Vapor). Registradas em {@link Spells}. */
final class ComboSpells {
    private ComboSpells() {}

    private static void hurt(ServerLevel level, ServerPlayer player, LivingEntity t, double amount) {
        t.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) amount);
    }

    /** Rio de Lava (Magma T3): um rastro incandescente de 12 blocos à frente queima quem pisar nele por 5 s. */
    static boolean lavaRiver(ServerLevel level, ServerPlayer player) {
        double w = Spells.pw(player, RuneElement.MAGMA);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 start = player.position();
        for (int t = 0; t < 100; t += 5) {
            final int tick = t;
            SpellFx.later(level, t, () -> {
                for (int i = 1; i <= 12; i++) {
                    Vec3 p = start.add(flat.scale(i));
                    level.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.1, p.z, tick % 10 == 0 ? 1 : 0, 0.3, 0.0, 0.3, 0.0);
                    level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.15, p.z, 2, 0.35, 0.02, 0.35, 0.005);
                    level.sendParticles(SpellFx.dust(RuneElement.MAGMA, 1.4F), p.x, p.y + 0.1, p.z, 2, 0.35, 0.0, 0.35, 0.0);
                }
                Set<LivingEntity> hit = new HashSet<>();
                for (int i = 1; i <= 12; i += 2) {
                    for (LivingEntity e : Spells.around(level, player, start.add(flat.scale(i)), 1.6)) {
                        if (!(e instanceof Player) && hit.add(e)) {
                            hurt(level, player, e, 1.5 * w);
                            e.igniteForSeconds(4.0F);
                        }
                    }
                }
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.5F, 0.6F);
        return true;
    }

    /** Erupção (Magma T4, ritual): o chão no ponto mirado explode em magma, arremessando e queimando num raio de 5. */
    static boolean eruption(ServerLevel level, ServerPlayer player) {
        Spells.Aim aim = Spells.aim(level, player, 28);
        Vec3 c = aim != null ? aim.point() : player.position();
        double w = Spells.pw(player, RuneElement.MAGMA), r = Spells.rad(player, RuneElement.MAGMA, 5.0);
        Spells.areaEffects(level, player, c, r, t -> {
            hurt(level, player, t, 14.0 * w);
            t.igniteForSeconds(8.0F);
            t.push(0, 1.1, 0);
            t.hurtMarked = true;
        });
        for (int k = 0; k < 10; k++) {
            final int h = k;
            SpellFx.later(level, k, () -> {
                level.sendParticles(ParticleTypes.LAVA, c.x, c.y + h * 0.6, c.z, 6, 0.5, 0.2, 0.5, 0.0);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + h * 0.7, c.z, 4, 0.6, 0.2, 0.6, 0.02);
            });
        }
        SpellFx.expandingRing(level, c, RuneElement.MAGMA, r, 8, ParticleTypes.FLAME);
        level.playSound(null, BlockPos.containing(c), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.6F);
        return true;
    }

    /** Armadura de Magma (Magma T4): resistência ao fogo, resistência e força por 60 s. */
    static boolean magmaArmor(ServerLevel level, ServerPlayer player) {
        int d = Spells.dur(player, RuneElement.MAGMA, 1200);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, d, 0));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, d, 0));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, d, 0));
        SpellFx.helix(level, player, RuneElement.MAGMA, 14, 2.0, ParticleTypes.FLAME);
        level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.4, 0.6, 0.4, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    /** Névoa (Vapor T2): você some numa nuvem de vapor; as criaturas em volta ficam cegas. */
    static boolean mist(ServerLevel level, ServerPlayer player) {
        int d = Spells.dur(player, RuneElement.VAPOR, 160);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, d, 0));
        Spells.areaEffects(level, player, player.position(), Spells.rad(player, RuneElement.VAPOR, 6.0),
                t -> t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, d, 0)));
        for (int k = 0; k < 8; k++) {
            SpellFx.later(level, k * 10, () -> level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 2.5, 1.0, 2.5, 0.01));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.2F, 0.8F);
        return true;
    }

    /** Jato de Vapor (Vapor T3): um cone de vapor escaldante de 8 blocos fere e empurra. */
    static boolean steamJet(ServerLevel level, ServerPlayer player) {
        double w = Spells.pw(player, RuneElement.VAPOR);
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        for (LivingEntity t : Spells.around(level, player, player.position(), 8.0)) {
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(eye);
            if (t instanceof Player || to.normalize().dot(look) < 0.75) {
                continue;
            }
            hurt(level, player, t, 7.0 * w);
            t.push(look.x * 1.4, 0.35, look.z * 1.4);
            t.hurtMarked = true;
            t.clearFire();
        }
        for (int i = 1; i <= 8; i++) {
            Vec3 p = eye.add(look.scale(i));
            double spread = 0.15 * i;
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 8, spread, spread, spread, 0.02);
            level.sendParticles(ParticleTypes.SPLASH, p.x, p.y, p.z, 4, spread, spread, spread, 0.05);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.2F, 1.4F);
        return true;
    }

    /** Plataforma de Nuvem (Nuvem T3): uma plataforma de nuvem temporária sob os seus pés (ou onde você mira). */
    static boolean cloudPlatform(ServerLevel level, ServerPlayer player) {
        BlockPos center;
        if (player.onGround()) {
            Spells.Aim aim = Spells.aim(level, player, 16);
            center = aim != null ? BlockPos.containing(aim.point()) : player.blockPosition().below();
        } else {
            center = player.blockPosition().below();
        }
        int placed = Clouds.platform(level, center, 2, player);
        SpellFx.burst(level, Vec3.atCenterOf(center), RuneElement.CLOUD, 0.2, 30, ParticleTypes.CLOUD);
        level.playSound(null, center, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 1.2F);
        if (placed > 0) {
            player.resetFallDistance();
        }
        return true;
    }

    /** Ponte de Nuvem (Nuvem T4): uma ponte de nuvem de 24 blocos na direção em que você olha. */
    static boolean cloudBridge(ServerLevel level, ServerPlayer player) {
        Direction dir = player.getDirection();
        BlockPos start = player.blockPosition().below().relative(dir);
        int length = (int) Math.round(24 * Math.min(1.5, Spells.pw(player, RuneElement.CLOUD)));
        Clouds.bridge(level, start, dir, length, player);
        SpellFx.burst(level, Vec3.atCenterOf(start), RuneElement.CLOUD, 0.2, 30, ParticleTypes.CLOUD);
        level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }
}
