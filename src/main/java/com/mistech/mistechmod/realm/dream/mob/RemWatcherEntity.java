package com.mistech.mistechmod.realm.dream.mob;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.DreamDeepContent;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Vigia REM (Sono REM): um olho que flutua devagar, com nervos pendurados. Não ataca — <b>dá o alarme</b>: se te enxerga por
 * {@link #STARE} ticks (até 20 blocos), arregala o olho, você fica Brilhando, os monstros em volta (32) vêm atrás de você e um Eco
 * nasce perto. Depois descansa {@link #COOLDOWN} ticks. Solta o Olho Vigia.
 */
public class RemWatcherEntity extends Monster {
    public static final int STARE = 40;
    public static final int COOLDOWN = 30 * 20;
    private static final EntityDataAccessor<Boolean> ALERT = SynchedEntityData.defineId(RemWatcherEntity.class, EntityDataSerializers.BOOLEAN);
    private int stare;
    private int rest;
    private int alertTicks;

    public RemWatcherEntity(EntityType<? extends RemWatcherEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ALERT, false);
    }

    public boolean isAlert() {
        return this.entityData.get(ALERT);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new DriftGoal(this));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.alertTicks > 0 && --this.alertTicks == 0) {
            this.entityData.set(ALERT, false);
        }
        if (this.rest > 0) {
            this.rest--;
            return;
        }
        Player p = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 20.0,
                e -> e instanceof Player pl && !pl.isCreative() && !pl.isSpectator() && !pl.isInvisible());
        if (p != null && this.hasLineOfSight(p)) {
            this.getLookControl().setLookAt(p, 30.0F, 30.0F);
            if (++this.stare >= STARE) {
                this.alarm(level, p);
            }
        } else if (this.stare > 0) {
            this.stare--;
        }
    }

    private void alarm(ServerLevel level, Player p) {
        this.stare = 0;
        this.rest = COOLDOWN;
        this.alertTicks = 60;
        this.entityData.set(ALERT, true);
        level.playSound(null, this.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.6F);
        p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        p.sendOverlayMessage(Component.translatable("message.mistech.rem_watcher.alarm"));
        for (Monster m : level.getEntitiesOfClass(Monster.class, new AABB(this.blockPosition()).inflate(32.0), m -> m != this)) {
            if (m.getTarget() == null) {
                m.setTarget(p);
            }
        }
        EchoEntity echo = DreamDeepContent.ECHO.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (echo != null) {
            Vec3 back = p.getLookAngle().multiply(-1, 0, -1).normalize().scale(6.0);
            echo.snapTo(p.getX() + back.x, p.getY(), p.getZ() + back.z, p.getYRot(), 0.0F);
            echo.followPlayer(p);
            level.addFreshEntity(echo);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.CHAIN_HIT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CHAIN_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLASS_BREAK;
    }

    /** Flutua devagar, mudando de lugar de vez em quando, a 3-6 blocos do chão. */
    static class DriftGoal extends Goal {
        private final RemWatcherEntity mob;

        DriftGoal(RemWatcherEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.mob.getRandom().nextInt(80) == 0;
        }

        @Override
        public void start() {
            Vec3 at = DreamFlyerEntity.airSpot(this.mob, this.mob.position(), 6, 2);
            if (at != null) {
                BlockPos g = BlockPos.containing(at);
                int below = 0;
                while (below < 8 && this.mob.level().getBlockState(g.below(below + 1)).isAir()) {
                    below++;
                }
                at = at.add(0, Math.max(0, 3 - below), 0);
                this.mob.getMoveControl().setWantedPosition(at.x, at.y, at.z, 0.6);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
}
