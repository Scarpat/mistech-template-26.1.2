package com.mistech.mistechmod.ranged;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.life.LifeRuneItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Efeitos das flechas mágicas ({@link ArrowMagic}) e da Adaga de Prata.
 * <p>Força p: Flecha Rúnica = 3; cada runa engastada no arco = 1 (selada 1,5); afinidade (Arco Turvo) = 0,5.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class RangedEvents {
    /** Recarga do raio das flechas de Energia, por atirador. */
    public static final int LIGHTNING_COOLDOWN = 40;
    private static final Map<UUID, Long> LAST_LIGHTNING = new HashMap<>();
    private static final String LAUNCHED = "mistech_arrow_launched";

    private RangedEvents() {}

    // ================================================================== Adaga de Prata

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Player player) || !player.getMainHandItem().is(RangedContent.SILVER_DAGGER.get())) {
            return;
        }
        LivingEntity target = event.getEntity();
        float mult = 1.0F;
        if (target.is(EntityTypeTags.UNDEAD)) {
            mult *= SilverDaggerItem.UNDEAD_MULTIPLIER;
        }
        // pelas costas: o alvo está olhando para longe do atacante
        Vec3 toAttacker = player.position().subtract(target.position()).normalize();
        Vec3 facing = Vec3.directionFromRotation(0.0F, target.getYHeadRot());
        if (facing.x * toAttacker.x + facing.z * toAttacker.z < -0.3) {
            mult *= SilverDaggerItem.BACKSTAB_MULTIPLIER;
            if (target.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
        }
        if (mult > 1.0F) {
            event.setAmount(event.getAmount() * mult);
        }
    }

    // ================================================================== flechas

    private static ArrowMagic magic(Entity entity) {
        return entity instanceof AbstractArrow arrow && arrow.hasData(RangedContent.ARROW_MAGIC) ? arrow.getData(RangedContent.ARROW_MAGIC) : ArrowMagic.EMPTY;
    }

    /** Ar: mais rápida e sem cair; Eco: teleguiada; rastro na cor do elemento. */
    @SubscribeEvent
    static void onArrowTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.level() instanceof ServerLevel level)) {
            return;
        }
        ArrowMagic magic = magic(arrow);
        if (magic.isEmpty()) {
            return;
        }
        boolean flying = arrow.getDeltaMovement().lengthSqr() > 0.04;
        double air = magic.power(RuneElement.AIR);
        if (!arrow.getPersistentData().getBooleanOr(LAUNCHED, false) && flying) {
            arrow.getPersistentData().putBoolean(LAUNCHED, true);
            if (air > 0) {
                arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.0 + 0.1 * Math.min(air, 4.0)));
                arrow.setNoGravity(true);
            }
        }
        if (!flying) {
            if (arrow.isNoGravity() && arrow.tickCount > 5) {
                arrow.setNoGravity(false);
            }
            return;
        }
        if (air > 0 && arrow.tickCount > 100) {
            arrow.setNoGravity(false); // não voa para sempre
        }
        double echo = magic.power(RuneElement.ECHO);
        if (echo > 0 && arrow.tickCount > 2) {
            LivingEntity target = null;
            double best = 10.0 * 10.0;
            Vec3 vel = arrow.getDeltaMovement();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, arrow.getBoundingBox().inflate(10.0),
                    e -> e instanceof Enemy && e.isAlive() && e != arrow.getOwner())) {
                Vec3 to = e.getBoundingBox().getCenter().subtract(arrow.position());
                double d = to.lengthSqr();
                if (d < best && to.normalize().dot(vel.normalize()) > 0.5) {
                    best = d;
                    target = e;
                }
            }
            if (target != null) {
                Vec3 want = target.getBoundingBox().getCenter().subtract(arrow.position()).normalize().scale(vel.length());
                double k = Math.min(0.5, 0.08 * echo);
                arrow.setDeltaMovement(vel.scale(1.0 - k).add(want.scale(k)));
            }
        }
        if (arrow.tickCount % 2 == 0) {
            RuneElement main = magic.strongest();
            if (main != null) {
                level.sendParticles(new DustParticleOptions(main.color(), 1.0F), arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    @SubscribeEvent
    static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(arrow.level() instanceof ServerLevel level)) {
            return;
        }
        ArrowMagic magic = magic(arrow);
        if (magic.isEmpty()) {
            return;
        }
        Player shooter = arrow.getOwner() instanceof Player p ? p : null;
        HitResult hit = event.getRayTraceResult();
        // Vida: flecha que atinge um aliado cura em vez de ferir
        double life = magic.power(RuneElement.LIFE);
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity ally && life > 0 && shooter != null
                && ally != shooter && LifeRuneItem.isAlly(shooter, ally)) {
            event.setCanceled(true);
            LifeRuneItem.mend(level, ally, (float) (2.0 * life), 60);
            arrow.discard();
            return;
        }
        // só uma vez por flecha (a flecha cravada não repete)
        arrow.removeData(RangedContent.ARROW_MAGIC);
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity target) {
            hitEntity(level, arrow, magic, target, shooter);
        } else if (hit instanceof BlockHitResult bh) {
            hitBlock(level, arrow, magic, bh, shooter);
        }
        // Espaço: o atirador vai até onde a flecha caiu (agachado ao atirar, ou Flecha Rúnica)
        if (magic.power(RuneElement.SPACE) > 0 && magic.sneaking() && shooter != null && shooter.level() == level) {
            Vec3 to = hit.getLocation();
            if (hit instanceof BlockHitResult bh) {
                to = Vec3.atBottomCenterOf(bh.getBlockPos().relative(bh.getDirection()));
            }
            shooter.teleportTo(to.x, to.y, to.z);
            shooter.resetFallDistance();
            level.playSound(null, shooter.blockPosition(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
            level.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1, to.z, 30, 0.4, 0.8, 0.4, 0.2);
        }
    }

    /** Dano extra que não some na imunidade do golpe da flecha. */
    private static void extraDamage(ServerLevel level, LivingEntity target, AbstractArrow arrow, Player shooter, float amount, boolean magic) {
        if (amount <= 0) {
            return;
        }
        target.hurtServer(level, magic ? level.damageSources().indirectMagic(arrow, shooter) : level.damageSources().arrow(arrow, shooter), amount);
        target.invulnerableTime = 0;
    }

    private static void hitEntity(ServerLevel level, AbstractArrow arrow, ArrowMagic m, LivingEntity t, Player shooter) {
        double p;
        if ((p = m.power(RuneElement.FIRE)) > 0) t.igniteForSeconds((float) (2 + 2 * p));
        if ((p = m.power(RuneElement.MAGMA)) > 0) t.igniteForSeconds((float) (3 + 2 * p));
        if ((p = m.power(RuneElement.ICE)) > 0) {
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, (int) Math.min(3, Math.floor(p))), shooter);
            t.setTicksFrozen(Math.max(t.getTicksFrozen(), (int) (60 * p)));
        }
        if ((p = m.power(RuneElement.DARKNESS)) > 0) t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int) (40 + 20 * p), 0), shooter);
        if ((p = m.power(RuneElement.LIGHT)) > 0) {
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), shooter);
            if (t.is(EntityTypeTags.UNDEAD)) extraDamage(level, t, arrow, shooter, (float) (2 * p), true);
        }
        if ((p = m.power(RuneElement.TIME)) > 0) {
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) (30 * p), 1), shooter);
            t.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, (int) (30 * p), 1), shooter);
        }
        if ((p = m.power(RuneElement.SOUL)) > 0) t.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) (40 * p), 0), shooter);
        if ((p = m.power(RuneElement.VIGIL)) > 0) {
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0), shooter);
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int) (40 * p), 0), shooter);
        }
        if ((p = m.power(RuneElement.WATER)) > 0) {
            t.clearFire();
            knock(t, arrow, 0.4 * p);
        }
        if ((p = m.power(RuneElement.EARTH)) > 0) knock(t, arrow, 0.5 * p);
        if ((p = m.power(RuneElement.VOID)) > 0) extraDamage(level, t, arrow, shooter, (float) (1.5 * p), true);
        if ((p = m.power(RuneElement.ARCANE)) > 0) extraDamage(level, t, arrow, shooter, (float) p, true);
        if ((p = m.power(RuneElement.LUCK)) > 0 && level.getRandom().nextDouble() < 0.15 * p) {
            extraDamage(level, t, arrow, shooter, 4.0F, false);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, t.getX(), t.getY() + 1, t.getZ(), 8, 0.3, 0.4, 0.3, 0.0);
        }
        if ((p = m.power(RuneElement.VAPOR)) > 0) vaporCloud(level, t.position(), shooter, p);
        if ((p = m.power(RuneElement.ENERGY)) > 0) lightning(level, t.position(), shooter, p, t);
        if ((p = m.power(RuneElement.CLOUD)) > 0) t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, (int) (10 * p), 0), shooter);
    }

    private static void hitBlock(ServerLevel level, AbstractArrow arrow, ArrowMagic m, BlockHitResult hit, Player shooter) {
        BlockPos front = hit.getBlockPos().relative(hit.getDirection());
        double p;
        if (((p = m.power(RuneElement.FIRE)) >= 1 || m.power(RuneElement.MAGMA) >= 1) && level.getBlockState(front).isAir()
                && BaseFireBlock.canBePlacedAt(level, front, hit.getDirection())) {
            level.setBlockAndUpdate(front, BaseFireBlock.getState(level, front));
        }
        if ((p = m.power(RuneElement.CLOUD)) > 0 && shooter != null) {
            BlockPos at = BlockPos.containing(arrow.position()).relative(hit.getDirection());
            if (p >= 2) {
                com.mistech.mistechmod.sky.Clouds.platform(level, at, 1, shooter);
            } else {
                com.mistech.mistechmod.sky.Clouds.placeTemporary(level, at, shooter);
            }
        }
        if ((p = m.power(RuneElement.VAPOR)) > 0) vaporCloud(level, hit.getLocation(), shooter, p);
        if ((p = m.power(RuneElement.ENERGY)) >= 2) lightning(level, hit.getLocation(), shooter, p, null);
    }

    private static void knock(LivingEntity t, AbstractArrow arrow, double strength) {
        Vec3 dir = arrow.getDeltaMovement().multiply(1, 0, 1).normalize();
        t.push(dir.x * strength, 0.1 * strength, dir.z * strength);
        t.hurtMarked = true;
    }

    private static void vaporCloud(ServerLevel level, Vec3 at, Player shooter, double power) {
        net.minecraft.world.entity.AreaEffectCloud cloud = new net.minecraft.world.entity.AreaEffectCloud(level, at.x, at.y, at.z);
        cloud.setOwner(shooter);
        cloud.setRadius((float) Math.min(4.0, 1.5 + 0.5 * power));
        cloud.setDuration((int) (60 + 20 * power));
        cloud.setCustomParticle(ParticleTypes.CLOUD);
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        level.addFreshEntity(cloud);
    }

    /** Raio de verdade só visual + dano, com recarga por atirador (a Flecha Rúnica sempre cai, o arco tem chance). */
    private static void lightning(ServerLevel level, Vec3 at, Player shooter, double power, LivingEntity target) {
        if (power < ArrowMagic.RUNE_ARROW_POWER && level.getRandom().nextDouble() > 0.3 * power) {
            return;
        }
        if (shooter != null) {
            long now = level.getGameTime();
            Long last = LAST_LIGHTNING.get(shooter.getUUID());
            if (last != null && now - last < LIGHTNING_COOLDOWN) {
                return;
            }
            LAST_LIGHTNING.put(shooter.getUUID(), now);
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) {
            return;
        }
        bolt.snapTo(at.x, at.y, at.z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
        if (target != null) {
            target.hurtServer(level, level.damageSources().lightningBolt(), 5.0F);
            target.invulnerableTime = 0;
        }
    }
}
