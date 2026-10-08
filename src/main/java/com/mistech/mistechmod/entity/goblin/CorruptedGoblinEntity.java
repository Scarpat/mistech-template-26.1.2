package com.mistech.mistechmod.entity.goblin;

import com.mistech.mistechmod.entity.GnomeEntity;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.EnumSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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

/**
 * Duende Corrompido: pequeno, rápido e agressivo. Ataca jogadores e gnomos; o golpe da adaga às vezes
 * passa a corrupção (Murchamento). Quando um apanha, os outros por perto vêm ajudar.
 * <p>
 * <b>Invocado</b> pelo Chamado da Rainha (Coroa Corrompida/Purificada): tem um dono ({@link #summoner}), não ataca jogadores
 * nem gnomos, defende o dono, ataca monstros perto dele, segue o dono e some depois de {@link #summonTicks}.
 * Os invocados pela Coroa Purificada são "limpos" ({@link #PURE}: textura dourada, golpe sem Murchamento).
 */
public class CorruptedGoblinEntity extends Monster implements GoblinKin {
    private static final EntityDataAccessor<Boolean> PURE = SynchedEntityData.defineId(CorruptedGoblinEntity.class, EntityDataSerializers.BOOLEAN);

    private @Nullable UUID summoner;
    private int summonTicks;

    public CorruptedGoblinEntity(EntityType<? extends CorruptedGoblinEntity> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PURE, false);
    }

    /** Transforma este duende num aliado de {@code owner} por {@code ticks}. */
    public void setSummoner(Player owner, boolean pure, int ticks) {
        this.summoner = owner.getUUID();
        this.summonTicks = ticks;
        this.entityData.set(PURE, pure);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public boolean isSummoned() {
        return this.summoner != null;
    }

    public boolean isPure() {
        return this.entityData.get(PURE);
    }

    public @Nullable Player summonerPlayer() {
        return this.summoner == null ? null : this.level().getPlayerByUUID(this.summoner);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.summoner == null || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        Player owner = this.summonerPlayer();
        if (--this.summonTicks <= 0 || owner == null || !owner.isAlive() || owner.level() != this.level()) {
            level.sendParticles(this.isPure() ? ParticleTypes.END_ROD : ParticleTypes.WITCH, this.getX(), this.getY() + 0.6, this.getZ(),
                    15, 0.3, 0.4, 0.3, 0.02);
            this.playSound(SoundEvents.EVOKER_CAST_SPELL, 0.6F, 1.6F);
            this.discard();
            return;
        }
        // longe demais: aparece do lado do dono
        if (this.distanceToSqr(owner) > 24.0 * 24.0) {
            this.snapTo(owner.getX() + (this.random.nextDouble() - 0.5) * 2.0, owner.getY(), owner.getZ() + (this.random.nextDouble() - 0.5) * 2.0,
                    this.getYRot(), this.getXRot());
            this.getNavigation().stop();
        }
        // nunca mira no dono nem em outro jogador
        if (this.getTarget() instanceof Player) {
            this.setTarget(null);
        }
        if (this.tickCount % 10 == 0 && this.isPure()) {
            level.sendParticles(ParticleTypes.WAX_OFF, this.getX(), this.getY() + 0.8, this.getZ(), 1, 0.2, 0.3, 0.2, 0.0);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.summoner != null) {
            output.store("Summoner", UUIDUtil.CODEC, this.summoner);
            output.putInt("SummonTicks", this.summonTicks);
            output.putBoolean("Pure", this.isPure());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.summoner = input.read("Summoner", UUIDUtil.CODEC).orElse(null);
        this.summonTicks = input.getIntOr("SummonTicks", 0);
        this.entityData.set(PURE, input.getBooleanOr("Pure", false));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.summoner == null && super.removeWhenFarAway(distance);
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return this.summoner == null && super.shouldDropLoot(level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
        this.goalSelector.addGoal(4, new FollowSummonerGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new DefendSummonerGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (t, l) -> !this.isSummoned()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, GnomeEntity.class, true, (t, l) -> !this.isSummoned()));
        // invocado: monstros perto do dono (menos os da vila)
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 10, true, false, (t, l) -> {
            Player owner = this.summonerPlayer();
            return owner != null && t instanceof Enemy && !(t instanceof GoblinKin) && t.distanceToSqr(owner) < 16.0 * 16.0;
        }));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && !this.isPure() && target instanceof LivingEntity living && this.random.nextFloat() < 0.2F) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), this);
        }
        return hit;
    }

    @Override
    protected boolean considersEntityAsAlly(Entity other) {
        if (this.summoner != null) {
            return this.summoner.equals(other.getUUID())
                    || other instanceof CorruptedGoblinEntity goblin && this.summoner.equals(goblin.summoner)
                    || super.considersEntityAsAlly(other);
        }
        return GoblinKin.allied(this, other) || super.considersEntityAsAlly(other);
    }

    /** Invocado: mira em quem bateu no dono ou em quem o dono bateu. */
    private static final class DefendSummonerGoal extends Goal {
        private final CorruptedGoblinEntity goblin;
        private @Nullable LivingEntity chosen;

        DefendSummonerGoal(CorruptedGoblinEntity goblin) {
            this.goblin = goblin;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = this.goblin.summonerPlayer();
            if (owner == null) {
                return false;
            }
            LivingEntity attacker = owner.getLastHurtByMob();
            LivingEntity victim = owner.getLastHurtMob();
            this.chosen = valid(attacker, owner) && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200 ? attacker
                    : valid(victim, owner) && owner.tickCount - owner.getLastHurtMobTimestamp() < 200 ? victim : null;
            return this.chosen != null && this.chosen != this.goblin.getTarget();
        }

        private boolean valid(@Nullable LivingEntity e, Player owner) {
            return e != null && e.isAlive() && e != this.goblin && !(e instanceof Player)
                    && !(e instanceof CorruptedGoblinEntity g && owner.getUUID().equals(g.summoner))
                    && e.distanceToSqr(owner) < 24.0 * 24.0;
        }

        @Override
        public void start() {
            this.goblin.setTarget(this.chosen);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    /** Invocado: anda atrás do dono quando ele se afasta. */
    private static final class FollowSummonerGoal extends Goal {
        private final CorruptedGoblinEntity goblin;

        FollowSummonerGoal(CorruptedGoblinEntity goblin) {
            this.goblin = goblin;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player owner = this.goblin.summonerPlayer();
            return owner != null && this.goblin.getTarget() == null && this.goblin.distanceToSqr(owner) > 6.0 * 6.0;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = this.goblin.summonerPlayer();
            return owner != null && this.goblin.getTarget() == null && this.goblin.distanceToSqr(owner) > 3.0 * 3.0
                    && !this.goblin.getNavigation().isDone();
        }

        @Override
        public void start() {
            Player owner = this.goblin.summonerPlayer();
            if (owner != null) {
                this.goblin.getNavigation().moveTo(owner, 1.3);
            }
        }

        @Override
        public void tick() {
            Player owner = this.goblin.summonerPlayer();
            if (owner != null && this.goblin.tickCount % 10 == 0) {
                this.goblin.getNavigation().moveTo(owner, 1.3);
            }
        }
    }

    @Override
    public float getVoicePitch() {
        return 1.6F + (this.random.nextFloat() - 0.5F) * 0.2F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_ANGRY;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_DEATH;
    }
}
