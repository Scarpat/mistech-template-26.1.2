package com.mistech.mistechmod.beyond;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Sentinela Rangente: nasce quando o Coração Rangente é plantado num Tronco de Carvalho Pálido. Guarda a área (16 blocos em volta
 * do tronco), ataca <b>hostis</b> e nunca jogadores — mas, como o Rangedor, <b>congela enquanto algum jogador olha para ela</b>.
 * Se o tronco for quebrado, ela se desfaz. Usa o modelo vanilla do Rangedor.
 */
public class PaleSentinelEntity extends AbstractGolem {
    private static final EntityDataAccessor<Boolean> CAN_MOVE = SynchedEntityData.defineId(PaleSentinelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(PaleSentinelEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int HOME_RADIUS = 16;
    public static final int MAX_PER_PLAYER = 2;

    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState invulnerabilityAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
    private int attackAnimationRemainingTicks;

    private @Nullable BlockPos heartLog;
    private @Nullable UUID owner;

    public PaleSentinelEntity(EntityType<? extends PaleSentinelEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0625);
    }

    public void bind(BlockPos log, Player player) {
        this.heartLog = log.immutable();
        this.owner = player.getUUID();
        this.setHomeTo(log, HOME_RADIUS);
        this.setPersistenceRequired();
    }

    public @Nullable UUID owner() {
        return this.owner;
    }

    public static int countOwned(ServerLevel level, UUID owner) {
        return level.getEntities(PaleNetherContent.PALE_SENTINEL.get(), s -> owner.equals(s.owner())).size();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CAN_MOVE, true);
        builder.define(ACTIVE, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Player.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                (target, level) -> target instanceof Enemy && !(target instanceof Creeper) && !(target instanceof PaleSentinelEntity)));
    }

    public boolean canMove() {
        return this.entityData.get(CAN_MOVE);
    }

    public boolean isActive() {
        return this.entityData.get(ACTIVE);
    }

    /** Algum jogador perto está olhando para ela? */
    private boolean isWatched(ServerLevel level) {
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0))) {
            if (!player.isSpectator() && this.isLookingAtMe(player, 0.5, false, true,
                    this.getEyeY(), this.getY() + 0.5 * this.getScale(), (this.getEyeY() + this.getY()) / 2.0)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || !this.canMove();
    }

    @Override
    public void aiStep() {
        if (this.attackAnimationRemainingTicks > 0) {
            this.attackAnimationRemainingTicks--;
        }
        if (this.level() instanceof ServerLevel server) {
            boolean couldMove = this.canMove();
            boolean nowCanMove = !this.isWatched(server);
            if (couldMove != nowCanMove) {
                this.playSound(nowCanMove ? SoundEvents.CREAKING_UNFREEZE : SoundEvents.CREAKING_FREEZE, 1.0F, 1.0F);
                if (!nowCanMove) {
                    this.getNavigation().stop();
                    this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
                }
            }
            this.entityData.set(CAN_MOVE, nowCanMove);
            this.entityData.set(ACTIVE, this.getTarget() != null);
            if (this.tickCount % 20 == 0 && this.heartLog != null) {
                BlockState log = server.getBlockState(this.heartLog);
                if (!log.is(BlockTags.PALE_OAK_LOGS)) {
                    server.sendParticles(ParticleTypes.WHITE_ASH, this.getX(), this.getY() + 1.4, this.getZ(), 40, 0.4, 1.0, 0.4, 0.02);
                    this.playSound(SoundEvents.CREAKING_DEATH, 1.0F, 1.0F);
                    this.discard();
                    return;
                }
            }
        }
        super.aiStep();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!this.canMove() || target instanceof Player) {
            return false;
        }
        this.attackAnimationRemainingTicks = 15;
        level.broadcastEntityEvent(this, (byte) 4);
        this.playSound(SoundEvents.CREAKING_ATTACK, 1.0F, 1.0F);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.attackAnimationRemainingTicks = 15;
            this.attackAnimationState.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && this.canMove();
    }

    @Override
    public void push(double xa, double ya, double za) {
        if (this.canMove()) {
            super.push(xa, ya, za);
        }
    }

    @Override
    public void knockback(double power, double xd, double zd) {
        if (this.canMove()) {
            super.knockback(power, xd, zd);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.isDeadOrDying()) {
            this.deathAnimationState.startIfStopped(this.tickCount);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isActive() ? null : SoundEvents.CREAKING_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CREAKING_SWAY;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREAKING_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.heartLog != null) {
            output.store("HeartLog", BlockPos.CODEC, this.heartLog);
        }
        if (this.owner != null) {
            output.putString("Owner", this.owner.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.heartLog = input.read("HeartLog", BlockPos.CODEC).orElse(null);
        String owner = input.getStringOr("Owner", "");
        this.owner = owner.isEmpty() ? null : UUID.fromString(owner);
        if (this.heartLog != null) {
            this.setHomeTo(this.heartLog, HOME_RADIUS);
        }
    }
}
