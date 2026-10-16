package com.mistech.mistechmod.realm.dream.mob;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sombra (Pesadelo): silhueta que <b>só se mexe quando ninguém está olhando</b> — encarada, congela no lugar e não ataca.
 * Perto dela (6 blocos) as pernas pesam: Lentidão I.
 */
public class ShadeEntity extends Monster {
    private static final EntityDataAccessor<Boolean> WATCHED = SynchedEntityData.defineId(ShadeEntity.class, EntityDataSerializers.BOOLEAN);
    public static final double HEAVY_LEGS = 6.0;

    public ShadeEntity(EntityType<? extends ShadeEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WATCHED, false);
    }

    public boolean isWatched() {
        return this.entityData.get(WATCHED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true) {
            @Override
            public boolean canUse() {
                return !ShadeEntity.this.isWatched() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !ShadeEntity.this.isWatched() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7) {
            @Override
            public boolean canUse() {
                return !ShadeEntity.this.isWatched() && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Algum jogador está olhando para ela (com linha de visão)? */
    private boolean observed(ServerLevel level) {
        for (Player player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(this) > 64 * 64 || !player.hasLineOfSight(this)) {
                continue;
            }
            Vec3 look = player.getViewVector(1.0F).normalize();
            Vec3 to = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
            double len = to.length();
            if (len < 1.0E-3 || look.dot(to.scale(1.0 / len)) > 0.6) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void aiStep() {
        if (this.level() instanceof ServerLevel server) {
            boolean watched = this.observed(server);
            if (watched != this.isWatched()) {
                this.entityData.set(WATCHED, watched);
            }
            if (watched) {
                this.getNavigation().stop();
                this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
                this.setYRot(this.yRotO);
                this.yHeadRot = this.yHeadRotO;
            }
            if (this.tickCount % 20 == 0) {
                for (Player p : server.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(HEAVY_LEGS))) {
                    if (!p.isCreative() && !p.isSpectator()) {
                        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false), this);
                    }
                }
            }
        }
        super.aiStep();
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isWatched()) {
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(input);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isWatched() ? null : SoundEvents.SCULK_CLICKING;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SCULK_CLICKING_STOP;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }
}
