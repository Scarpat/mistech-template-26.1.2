package com.mistech.mistechmod.lore;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/**
 * Mapinguari (chefe, 500 de vida): gigante peludo de um olho só e boca na barriga, pés virados para trás.
 * Garras pesadas; <b>pancada no chão</b> a cada 8 s (dano e arremesso em 6 blocos); <b>urro fedorento</b> a cada 12 s
 * (tontura, fraqueza, fome, lentidão em 14 blocos). Flechas e projéteis fazem só 25%. Derruba folhas por onde passa.
 * Nasce raro à noite na Mata Ancestral (ou por ritual). Solta Garras do Mapinguari.
 */
public class MapinguariEntity extends Monster {
    private static final EntityDataAccessor<Integer> ROARING = SynchedEntityData.defineId(MapinguariEntity.class, EntityDataSerializers.INT);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    private int slamCooldown = 100;
    private int roarCooldown = 200;

    public MapinguariEntity(EntityType<? extends MapinguariEntity> type, Level level) {
        super(type, level);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    public static boolean checkSpawn(EntityType<MapinguariEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getLevel().isDarkOutside() && random.nextInt(4) == 0
                && level.getBlockState(pos.below()).isSolid()
                && level.getEntitiesOfClass(MapinguariEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(128)).isEmpty();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROARING, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isRoaring() {
        return this.entityData.get(ROARING) > 0;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.entityData.get(ROARING) > 0) {
            this.entityData.set(ROARING, this.entityData.get(ROARING) - 1);
        }
        // abre caminho pela mata: derruba folhas
        if (this.tickCount % 10 == 0 && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            BlockPos.betweenClosedStream(this.getBoundingBox().inflate(0.5, 0.2, 0.5)).forEach(p -> {
                if (level.getBlockState(p).is(BlockTags.LEAVES)) {
                    level.destroyBlock(p, true, this);
                }
            });
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (--this.slamCooldown <= 0 && this.onGround() && this.distanceToSqr(target) < 7 * 7) {
            this.slamCooldown = 160;
            this.slam(level);
        }
        if (--this.roarCooldown <= 0 && this.distanceToSqr(target) < 14 * 14) {
            this.roarCooldown = 240;
            this.roar(level);
        }
    }

    private void slam(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.5F);
        level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), this.getX(), this.getY() + 0.2,
                this.getZ(), 120, 3.0, 0.2, 3.0, 0.2);
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 3, 1.5, 0.2, 1.5, 0.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(6.0, 2.0, 6.0), e -> e != this && e.isAlive())) {
            double d = e.distanceTo(this);
            if (d > 6.5) {
                continue;
            }
            e.hurtServer(level, this.damageSources().mobAttack(this), (float) (12.0 - d));
            Vec3 away = e.position().subtract(this.position()).multiply(1, 0, 1).normalize().scale(1.2);
            e.push(away.x, 0.9, away.z);
            e.hurtMarked = true;
        }
    }

    private void roar(ServerLevel level) {
        this.entityData.set(ROARING, 30);
        level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.55F);
        level.sendParticles(ParticleTypes.ITEM_SLIME, this.getX(), this.getY() + 1.5, this.getZ(), 60, 2.5, 1.0, 2.5, 0.1);
        level.sendParticles(ParticleTypes.SNEEZE, this.getX(), this.getY() + 1.5, this.getZ(), 40, 2.5, 1.0, 2.5, 0.05);
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(14))) {
            if (p.isCreative() || p.isSpectator()) {
                continue;
            }
            p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0), this);
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0), this);
            p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1), this);
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
        }
    }

    /** Couro grosso: flechas e outros projéteis fazem só 25%. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            amount *= 0.25F;
        }
        return super.hurtServer(level, source, amount);
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
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }
}
