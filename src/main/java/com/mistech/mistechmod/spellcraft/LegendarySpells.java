package com.mistech.mistechmod.spellcraft;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Magias lendárias (tier 6, exigem afinidade nível 6; só achadas no mundo). Registradas em {@link Spells}. */
final class LegendarySpells {
    private LegendarySpells() {}

    private static Vec3 target(ServerLevel level, ServerPlayer player, double range) {
        Spells.Aim aim = Spells.aim(level, player, range);
        return aim != null ? aim.point() : player.position();
    }

    private static void hurt(ServerLevel level, ServerPlayer player, LivingEntity t, double amount) {
        t.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) amount);
    }

    private static List<Player> allies(ServerLevel level, ServerPlayer player, double radius) {
        List<Player> list = new java.util.ArrayList<>(level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && e.isAlliedTo(player)));
        list.add(player);
        return list;
    }

    /** Inferno (Fogo): o ponto mirado explode em chamas num raio de 10. */
    static boolean inferno(ServerLevel level, ServerPlayer player) {
        Vec3 c = target(level, player, 32);
        double w = Spells.pw(player, RuneElement.FIRE), r = Spells.rad(player, RuneElement.FIRE, 10.0);
        SpellFx.sigil(level, c, RuneElement.FIRE, 4.0);
        for (int k = 0; k < 3; k++) {
            final int wave = k;
            SpellFx.later(level, k * 5, () -> {
                SpellFx.expandingRing(level, c, RuneElement.FIRE, r * (0.5 + 0.25 * wave), 8, ParticleTypes.FLAME);
                level.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.5, c.z, 30, r * 0.4, 0.5, r * 0.4, 0.0);
                Spells.areaEffects(level, player, c, r, t -> {
                    hurt(level, player, t, 6.0 * w);
                    t.igniteForSeconds(10.0F);
                });
                level.playSound(null, BlockPos.containing(c), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.7F + 0.1F * wave);
            });
        }
        for (int i = 0; i < 24; i++) {
            BlockPos pos = BlockPos.containing(c).offset(level.getRandom().nextInt(13) - 6, 0, level.getRandom().nextInt(13) - 6);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos p = pos.above(dy);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolidRender()) {
                    level.setBlockAndUpdate(p, Blocks.FIRE.defaultBlockState());
                    break;
                }
            }
        }
        return true;
    }

    /** Era Glacial (Gelo): tudo num raio de 12 congela; a água vira gelo e cai neve. */
    static boolean iceAge(ServerLevel level, ServerPlayer player) {
        double w = Spells.pw(player, RuneElement.ICE), r = Spells.rad(player, RuneElement.ICE, 12.0);
        Vec3 c = player.position();
        Spells.areaEffects(level, player, c, r, t -> {
            hurt(level, player, t, 10.0 * w);
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Spells.dur(player, RuneElement.ICE, 200), 4));
            t.setTicksFrozen(t.getTicksRequiredToFreeze() + 200);
        });
        BlockPos center = player.blockPosition();
        int ri = (int) r;
        for (int x = -ri; x <= ri; x++) {
            for (int z = -ri; z <= ri; z++) {
                if (x * x + z * z > ri * ri) {
                    continue;
                }
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, center.offset(x, 0, z));
                if (Math.abs(top.getY() - center.getY()) > 6) {
                    continue;
                }
                BlockPos below = top.below();
                if (level.getFluidState(below).is(net.minecraft.tags.FluidTags.WATER) && level.getFluidState(below).isSource()) {
                    level.setBlockAndUpdate(below, Blocks.ICE.defaultBlockState());
                } else if (level.getBlockState(top).isAir() && level.getBlockState(below).isSolidRender() && level.getRandom().nextInt(3) == 0) {
                    level.setBlockAndUpdate(top, Blocks.SNOW.defaultBlockState());
                }
            }
        }
        SpellFx.expandingRing(level, c, RuneElement.ICE, r, 12, ParticleTypes.SNOWFLAKE);
        level.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 3, c.z, 300, r * 0.6, 2.0, r * 0.6, 0.02);
        level.playSound(null, center, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.4F);
        return true;
    }

    /** Maremoto (Água, carregada): uma onda de 16 blocos à frente arrasta e fere tudo. */
    static boolean tidalWave(ServerLevel level, ServerPlayer player) {
        double w = Spells.pw(player, RuneElement.WATER);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        Set<LivingEntity> hit = new HashSet<>();
        for (int step = 1; step <= 16; step++) {
            final int s = step;
            SpellFx.later(level, step, () -> {
                Vec3 front = player.position().add(flat.scale(s));
                for (int k = -3; k <= 3; k++) {
                    Vec3 p = front.add(side.scale(k));
                    level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 1.0, p.z, 6, 0.3, 0.8, 0.3, 0.1);
                    level.sendParticles(ParticleTypes.FALLING_WATER, p.x, p.y + 2.2, p.z, 2, 0.3, 0.2, 0.3, 0.0);
                }
                for (LivingEntity t : Spells.around(level, player, front, 3.5)) {
                    if (t instanceof Player || !hit.add(t)) {
                        continue;
                    }
                    hurt(level, player, t, 12.0 * w);
                    t.push(flat.x * 2.2, 0.6, flat.z * 2.2);
                    t.hurtMarked = true;
                    t.clearFire();
                }
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.5F, 0.6F);
        return true;
    }

    /** Cataclismo (Terra): o chão se abre num raio de 10 e arremessa tudo para o alto. */
    static boolean cataclysm(ServerLevel level, ServerPlayer player) {
        double w = Spells.pw(player, RuneElement.EARTH), r = Spells.rad(player, RuneElement.EARTH, 10.0);
        Vec3 c = player.position();
        BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
        BlockParticleOption stone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        Spells.areaEffects(level, player, c, r, t -> {
            hurt(level, player, t, 16.0 * w);
            t.push(0, 1.3, 0);
            t.hurtMarked = true;
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        });
        for (int k = 0; k < 4; k++) {
            final double rr = r * (k + 1) / 4.0;
            SpellFx.later(level, k * 3, () -> {
                SpellFx.ring(level, c.add(0, 0.2, 0), rr, stone, 60);
                level.sendParticles(dirt, c.x, c.y + 0.5, c.z, 80, rr * 0.5, 0.3, rr * 0.5, 0.3);
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5F, 0.4F);
        return true;
    }

    /** Furacão (Ar, contínua): um redemoinho no ponto mirado suga, gira e fere as criaturas. */
    static boolean hurricane(ServerLevel level, ServerPlayer player) {
        Vec3 c = target(level, player, 24);
        double w = Spells.pw(player, RuneElement.AIR), r = Spells.rad(player, RuneElement.AIR, 10.0);
        for (LivingEntity t : Spells.around(level, player, c, r)) {
            if (t instanceof Player) {
                continue;
            }
            Vec3 d = c.subtract(t.position());
            Vec3 in = new Vec3(d.x, 0, d.z).normalize();
            Vec3 spin = new Vec3(-in.z, 0, in.x);
            t.setDeltaMovement(in.scale(0.35).add(spin.scale(0.45)).add(0, 0.25, 0));
            t.hurtMarked = true;
            hurt(level, player, t, 2.0 * w);
        }
        for (int h = 0; h < 8; h++) {
            SpellFx.ring(level, c.add(0, h * 0.8, 0), 0.6 + h * 0.45, ParticleTypes.CLOUD, 10 + h * 2);
        }
        level.playSound(null, BlockPos.containing(c), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.8F, 0.6F);
        return true;
    }

    /** Julgamento (Energia, carregada): raios de verdade caem em até 6 criaturas (mais com carga). */
    static boolean judgement(ServerLevel level, ServerPlayer player) {
        double w = Spells.pw(player, RuneElement.ENERGY);
        int max = 2 + (int) Math.round(4 * Spells.CHARGE.get());
        List<LivingEntity> targets = Spells.around(level, player, player.position(), Spells.rad(player, RuneElement.ENERGY, 20.0));
        targets.removeIf(t -> t instanceof Player);
        targets.sort(java.util.Comparator.comparingDouble(t -> t.distanceToSqr(player)));
        int i = 0;
        for (LivingEntity t : targets) {
            if (i >= max) {
                break;
            }
            final int delay = i * 4;
            SpellFx.later(level, delay, () -> {
                if (!t.isAlive()) {
                    return;
                }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (bolt != null) {
                    bolt.snapTo(t.getX(), t.getY(), t.getZ());
                    bolt.setCause(player);
                    level.addFreshEntity(bolt);
                }
                hurt(level, player, t, 10.0 * w);
                SpellFx.lightning(level, t.position().add(0, 18, 0), t.position().add(0, t.getBbHeight() * 0.5, 0), RuneElement.ENERGY);
                com.mistech.mistechmod.spellcraft.fx.Vfx.beam(level, t.position().add(0, 24, 0), t.position(), Blocks.SEA_LANTERN.defaultBlockState(), 0.6F, 8);
                com.mistech.mistechmod.spellcraft.fx.Vfx.flash(level, t.position(), 30, 0xFFF6A0, 4);
                com.mistech.mistechmod.spellcraft.fx.Vfx.shake(level, t.position(), 20, 0.3F, 6);
            });
            i++;
        }
        SpellFx.burst(level, player.position().add(0, 1.0, 0), RuneElement.ENERGY, 0.4, 50, ParticleTypes.ELECTRIC_SPARK);
        return true;
    }

    /** Alvorada (Luz): cura total sua e dos aliados, limpa efeitos ruins e queima mortos-vivos num raio de 14. */
    static boolean dawn(ServerLevel level, ServerPlayer player) {
        double w = Spells.pw(player, RuneElement.LIGHT), r = Spells.rad(player, RuneElement.LIGHT, 14.0);
        for (Player p : allies(level, player, r)) {
            p.heal((float) (20.0 * w));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
            for (MobEffectInstance inst : new java.util.ArrayList<>(p.getActiveEffects())) {
                if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    p.removeEffect(inst.getEffect());
                }
            }
        }
        Spells.areaEffects(level, player, player.position(), r, t -> {
            if (t.isInvertedHealAndHarm()) {
                hurt(level, player, t, 30.0 * w);
                t.igniteForSeconds(6.0F);
            }
        });
        SpellFx.column(level, player.position(), RuneElement.LIGHT, 12.0, 14, ParticleTypes.END_ROD);
        SpellFx.expandingRing(level, player.position(), RuneElement.LIGHT, r, 12, ParticleTypes.END_ROD);
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 2, player.getZ(), 200, r * 0.5, 2.0, r * 0.5, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 1.2F);
        return true;
    }

    /** Eclipse Total (Trevas): escuridão, cegueira e definhar num raio de 14; você drena vida de cada um. */
    static boolean totalEclipse(ServerLevel level, ServerPlayer player) {
        double w = Spells.pw(player, RuneElement.DARKNESS), r = Spells.rad(player, RuneElement.DARKNESS, 14.0);
        int hit = Spells.areaEffects(level, player, player.position(), r, t -> {
            hurt(level, player, t, 6.0 * w);
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
            t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
            t.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1));
        });
        player.heal((float) (Math.min(10, hit) * 1.5 * w));
        SpellFx.contractingRing(level, player.position(), RuneElement.DARKNESS, r, 12, ParticleTypes.LARGE_SMOKE);
        level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.5, player.getZ(), 200, r * 0.5, 1.5, r * 0.5, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6F, 1.4F);
        return true;
    }

    /** Instante Eterno (Tempo): zera as recargas das outras magias e acelera você por 10 s. */
    static boolean eternalInstant(ServerLevel level, ServerPlayer player) {
        player.getData(SpellcraftContent.COOLDOWNS).keySet().removeIf(id -> !id.equals("eternal_instant"));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, Spells.dur(player, RuneElement.TIME, 200), 2));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, Spells.dur(player, RuneElement.TIME, 200), 2));
        SpellFx.helix(level, player, RuneElement.TIME, 20, 2.5, ParticleTypes.REVERSE_PORTAL);
        SpellFx.contractingRing(level, player.position(), RuneElement.TIME, 6.0, 10, ParticleTypes.END_ROD);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 0.5F);
        return true;
    }

    /** Buraco Negro (Espaço): uma singularidade no ponto mirado suga e esmaga tudo por 6 s. */
    static boolean blackHole(ServerLevel level, ServerPlayer player) {
        Vec3 c = target(level, player, 32).add(0, 1.5, 0);
        double w = Spells.pw(player, RuneElement.SPACE), r = Spells.rad(player, RuneElement.SPACE, 12.0);
        for (int t = 0; t < 120; t += 2) {
            final int tick = t;
            SpellFx.later(level, t, () -> {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 30, r * 0.4, r * 0.3, r * 0.4, -0.4);
                level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 6, 0.2, 0.2, 0.2, 0.0);
                for (LivingEntity e : Spells.around(level, player, c, r)) {
                    if (e instanceof Player) {
                        continue;
                    }
                    Vec3 d = c.subtract(e.position().add(0, e.getBbHeight() * 0.5, 0));
                    double dist = Math.max(0.5, d.length());
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(d.normalize().scale(Math.min(0.8, 3.0 / dist))));
                    e.hurtMarked = true;
                    if (dist < 2.5 && tick % 10 == 0) {
                        hurt(level, player, e, 4.0 * w);
                    }
                }
                if (tick % 20 == 0) {
                    level.playSound(null, BlockPos.containing(c), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.5F);
                }
            });
        }
        SpellFx.sigil(level, c.subtract(0, 1.5, 0), RuneElement.SPACE, 3.0);
        return true;
    }

    /** Árvore da Vida (Vida): por 20 s, uma zona de raio 8 cura você e os aliados e faz tudo brotar. */
    static boolean treeOfLife(ServerLevel level, ServerPlayer player) {
        Vec3 c = player.position();
        double w = Spells.pw(player, RuneElement.LIFE), r = Spells.rad(player, RuneElement.LIFE, 8.0);
        ItemStack meal = new ItemStack(Items.BONE_MEAL);
        for (int s = 0; s < 20; s++) {
            SpellFx.later(level, s * 20, () -> {
                for (Player p : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(r, 4, r),
                        e -> e.isAlive() && (e == player || e.isAlliedTo(player)))) {
                    p.heal((float) (2.0 * w));
                }
                for (int i = 0; i < 4; i++) {
                    BlockPos pos = BlockPos.containing(c).offset(level.getRandom().nextInt(13) - 6, level.getRandom().nextInt(3) - 1, level.getRandom().nextInt(13) - 6);
                    net.minecraft.world.item.BoneMealItem.growCrop(meal, level, pos);
                }
                SpellFx.ring(level, c.add(0, 0.2, 0), r, SpellFx.dust(RuneElement.LIFE, 1.4F), 40);
                SpellFx.column(level, c, RuneElement.LIFE, 6.0, 6, ParticleTypes.HAPPY_VILLAGER);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState()),
                        c.x, c.y + 5, c.z, 20, 2.0, 1.0, 2.0, 0.05);
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.5F, 0.5F);
        return true;
    }
}
