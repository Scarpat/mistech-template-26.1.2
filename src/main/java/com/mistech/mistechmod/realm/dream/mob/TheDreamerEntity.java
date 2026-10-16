package com.mistech.mistechmod.realm.dream.mob;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.DreamDeepContent;
import com.mistech.mistechmod.registry.DreamMobs;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * O Sonhador (chefe do Sono REM, 400 de vida): um gigante dormindo de touca, enrolado num cobertor, abraçado ao travesseiro.
 * Vem com o Sino do Despertar. Flutua em volta do alvo.
 * <ul>
 * <li><b>Dormindo</b> (acima da metade): bolhas de sono (balas que dão Levitação), canção de ninar (Lentidão II e Fadiga num raio de 12)
 * e chama Ecos que repetem os passos de quem luta.</li>
 * <li><b>Acordado</b> (metade para baixo): abre os olhos, fica mais rápido, chama Sombras, solta Escuridão (24 blocos) e
 * aparece atrás do alvo.</li>
 * </ul>
 * Solta Essência do Sonhador.
 */
public class TheDreamerEntity extends Monster {
    private static final EntityDataAccessor<Boolean> AWAKE = SynchedEntityData.defineId(TheDreamerEntity.class, EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int attackCooldown = 80;
    private int attackIndex;
    private double orbit;

    public TheDreamerEntity(EntityType<? extends TheDreamerEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.xpReward = 300;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 400.0).add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AWAKE, false);
    }

    public boolean isAwake() {
        return this.entityData.get(AWAKE);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void travel(Vec3 input) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        LivingEntity target = this.getTarget();
        if (this.tickCount % 20 == 0 && (target == null || !target.isAlive() || target.distanceToSqr(this) > 64 * 64
                || target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            target = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 48.0,
                    e -> e instanceof Player p && !p.isCreative() && !p.isSpectator());
            this.setTarget(target);
        }
        boolean awake = this.isAwake();
        if (!awake && this.getHealth() <= this.getMaxHealth() / 2) {
            this.wakeUp(level);
            awake = true;
        }
        Vec3 goal;
        if (target != null) {
            this.orbit += awake ? 0.03 : 0.018;
            double r = 11.0;
            goal = target.position().add(Math.cos(this.orbit) * r, 4.0 + 1.5 * Math.sin(this.tickCount * 0.04), Math.sin(this.orbit) * r);
            this.getLookControl().setLookAt(target, 10.0F, 10.0F);
            Vec3 d = target.position().subtract(this.position());
            float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
            if (--this.attackCooldown <= 0) {
                this.attack(level, target, awake);
                this.attackCooldown = awake ? 50 : 70;
            }
        } else {
            goal = this.position().add(0, Math.sin(this.tickCount * 0.03) * 0.5, 0);
        }
        Vec3 delta = goal.subtract(this.position());
        double max = awake ? 0.45 : 0.3;
        Vec3 vel = delta.length() > max ? delta.normalize().scale(max) : delta;
        this.setDeltaMovement(vel);
        this.move(MoverType.SELF, vel);
    }

    private void wakeUp(ServerLevel level) {
        this.entityData.set(AWAKE, true);
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.7F);
        for (ServerPlayer p : this.bossEvent.getPlayers()) {
            p.sendSystemMessage(Component.translatable("message.mistech.the_dreamer.awake"));
        }
        this.darkness(level);
    }

    private void attack(ServerLevel level, LivingEntity target, boolean awake) {
        int kind = this.attackIndex++ % (awake ? 4 : 3);
        switch (kind) {
            case 0 -> {
                // bolhas de sono
                int n = awake ? 5 : 3;
                for (int i = 0; i < n; i++) {
                    level.addFreshEntity(new ShulkerBullet(level, this, target, Direction.Axis.values()[i % 3]));
                }
                level.playSound(null, this.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            case 1 -> {
                // canção de ninar
                level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.HOSTILE, 3.0F, 0.5F);
                level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 3.0, this.getZ(), 20, 4.0, 1.0, 4.0, 1.0);
                for (Player p : level.getEntitiesOfClass(Player.class, new AABB(target.blockPosition()).inflate(12.0))) {
                    p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 1));
                    p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, 1));
                }
            }
            case 2 -> {
                // filhos do sonho: Ecos (dormindo) ou Sombras (acordado)
                for (int i = 0; i < 2; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2;
                    double x = target.getX() + Math.cos(a) * 5, z = target.getZ() + Math.sin(a) * 5;
                    if (awake) {
                        ShadeEntity shade = DreamMobs.SHADE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                        if (shade != null) {
                            shade.snapTo(x, target.getY(), z, 0.0F, 0.0F);
                            shade.setTarget(target);
                            level.addFreshEntity(shade);
                        }
                    } else if (target instanceof Player p) {
                        EchoEntity echo = DreamDeepContent.ECHO.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                        if (echo != null) {
                            echo.snapTo(x, target.getY(), z, 0.0F, 0.0F);
                            echo.followPlayer(p);
                            level.addFreshEntity(echo);
                        }
                    }
                }
                level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            default -> {
                // pesadelo: escuridão e aparece atrás do alvo
                this.darkness(level);
                Vec3 back = target.getLookAngle().multiply(-1, 0, -1).normalize().scale(7.0);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 2, this.getZ(), 40, 1.5, 2, 1.5, 0.1);
                this.teleportTo(target.getX() + back.x, target.getY() + 3.0, target.getZ() + back.z);
                level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.5F);
            }
        }
    }

    private void darkness(ServerLevel level) {
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0))) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(3) == 0) {
            this.level().addParticle(this.isAwake() ? ParticleTypes.SOUL : ParticleTypes.END_ROD,
                    this.getX() + (this.random.nextDouble() - 0.5) * 3, this.getY() + this.random.nextDouble() * 4,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 3, 0, 0.02, 0);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel) {
            for (ServerPlayer p : this.bossEvent.getPlayers()) {
                p.sendSystemMessage(Component.translatable("message.mistech.the_dreamer.defeated"));
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isAwake() ? SoundEvents.WARDEN_AMBIENT : SoundEvents.FOX_SLEEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return this.isAwake() ? 0.8F : 0.5F;
    }
}
