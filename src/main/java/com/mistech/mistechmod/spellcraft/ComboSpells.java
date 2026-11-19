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
        com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.LAVA_RIVER, 0, null, 100.0F, start, flat);
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
        com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.ERUPTION, null, (float) r, c);
        com.mistech.mistechmod.spellcraft.fx.Vfx.shake(level, c, 30, 0.6F, 14);
        com.mistech.mistechmod.spellcraft.fx.Vfx.flash(level, c, 24, 0xFF7A20, 8);
        level.playSound(null, BlockPos.containing(c), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.6F);
        return true;
    }

    /** Armadura de Magma (Magma T4): resistência ao fogo, resistência e força por 60 s. */
    static boolean magmaArmor(ServerLevel level, ServerPlayer player) {
        int d = Spells.dur(player, RuneElement.MAGMA, 1200);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, d, 0));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, d, 0));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, d, 0));
        com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.MAGMA_ARMOR, player, 0.0F, player.position());
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    /** Névoa (Vapor T2): você some numa nuvem de vapor; as criaturas em volta ficam cegas. */
    static boolean mist(ServerLevel level, ServerPlayer player) {
        int d = Spells.dur(player, RuneElement.VAPOR, 160);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, d, 0));
        Spells.areaEffects(level, player, player.position(), Spells.rad(player, RuneElement.VAPOR, 6.0),
                t -> t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, d, 0)));
        com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.MIST, player, 80.0F, player.position());
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
        com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.STEAM_JET, 0, player, 8.0F, eye.subtract(0, 0.2, 0), look);
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
        com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.SHEEP_PLATFORM, null, 2.0F, Vec3.atCenterOf(center).add(0, 0.5, 0));
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
        com.mistech.mistechmod.spellcraft.fx.DreamFx.send(level, com.mistech.mistechmod.spellcraft.fx.DreamFx.SHEEP_BRIDGE, 0, null, length, Vec3.atCenterOf(start).add(0, 0.5, 0), new Vec3(dir.getStepX(), 0, dir.getStepZ()));
        level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }
}
