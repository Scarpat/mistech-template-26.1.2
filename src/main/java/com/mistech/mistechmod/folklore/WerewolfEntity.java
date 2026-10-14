package com.mistech.mistechmod.folklore;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Lobisomem: aparece nas noites de lua cheia (e muito na Lua de Sangue). Rápido, pula no alvo, regenera —
 * menos depois de levar golpe de <b>prata</b> (armas de mitril). A mordida pode passar a <b>Licantropia</b>.
 * Foge ao amanhecer.
 */
public class WerewolfEntity extends Monster {
    public static final float INFECT_CHANCE = 0.25F;
    /** Por quanto tempo a prata impede a regeneração. */
    public static final int SILVER_TICKS = 100;

    private int silverUntil;
    /** Até quando (tickCount) foge da fumaça do Incenso de Prata, e de onde ela vem. */
    private int scaredUntil;
    private @Nullable Vec3 scaredFrom;

    public WerewolfEntity(EntityType<? extends WerewolfEntity> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeSilverSmokeGoal());
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
    }

    /** A fumaça do Incenso de Prata: larga o alvo e foge de {@code source} por uns segundos. */
    public void scareFrom(Vec3 source) {
        boolean wasCalm = !this.isScared();
        this.scaredFrom = source;
        this.scaredUntil = this.tickCount + 60;
        this.setTarget(null);
        if (wasCalm && this.level() instanceof ServerLevel level) {
            level.playSound(null, this.blockPosition(), sounds().whineSound().value(), this.getSoundSource(), 1.2F, 0.6F);
        }
    }

    public boolean isScared() {
        return this.scaredFrom != null && this.tickCount < this.scaredUntil;
    }

    @Override
    public void setTarget(net.minecraft.world.entity.@Nullable LivingEntity target) {
        // assustado não escolhe alvo novo (mas revida se apanhar: o HurtByTarget passa por aqui também, então deixa)
        if (target != null && this.isScared() && target != this.getLastHurtByMob()) {
            return;
        }
        super.setTarget(target);
    }

    /** Corre para longe da fumaça de prata. */
    private final class FleeSilverSmokeGoal extends net.minecraft.world.entity.ai.goal.Goal {
        FleeSilverSmokeGoal() {
            this.setFlags(java.util.EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return WerewolfEntity.this.isScared() && WerewolfEntity.this.getLastHurtByMob() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return WerewolfEntity.this.isScared() && !WerewolfEntity.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.runAway();
        }

        @Override
        public void tick() {
            if (WerewolfEntity.this.getNavigation().isDone()) {
                this.runAway();
            }
        }

        private void runAway() {
            Vec3 from = WerewolfEntity.this.scaredFrom;
            if (from == null) {
                return;
            }
            Vec3 away = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(WerewolfEntity.this, 20, 7, from);
            if (away != null) {
                WerewolfEntity.this.getNavigation().moveTo(away.x, away.y, away.z, 1.4);
            }
        }
    }

    public void markSilver() {
        this.silverUntil = this.tickCount + SILVER_TICKS;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof Player player && this.getRandom().nextFloat() < INFECT_CHANCE) {
            Lycanthropy.infect(player);
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level) || this.tickCount % 20 != 0) {
            return;
        }
        if (this.tickCount >= this.silverUntil && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }
        // ao amanhecer, a maldição passa: o lobisomem volta a ser gente (um aldeão sem profissão)
        if (level.isBrightOutside() && !this.isPersistenceRequired() && this.getRandom().nextInt(10) == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.2, this.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
            this.turnBackIntoVillager(level);
            this.discard();
        }
    }

    /** Cria um aldeão desempregado (tipo do bioma) onde o lobisomem estava. */
    private void turnBackIntoVillager(ServerLevel level) {
        net.minecraft.world.entity.npc.villager.Villager villager =
                net.minecraft.world.entity.EntityType.VILLAGER.create(level, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
        if (villager == null) {
            return;
        }
        villager.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(villager.blockPosition()),
                net.minecraft.world.entity.EntitySpawnReason.CONVERSION, null);
        villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(),
                net.minecraft.world.entity.npc.villager.VillagerProfession.NONE));
        if (this.hasCustomName()) {
            villager.setCustomName(this.getCustomName());
            villager.setCustomNameVisible(this.isCustomNameVisible());
        }
        // acorda zonzo da noite de caça
        villager.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 200, 1));
        level.addFreshEntity(villager);
        net.neoforged.neoforge.event.EventHooks.onLivingConvert(this, villager);
        level.playSound(null, villager.blockPosition(), net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT, this.getSoundSource(), 1.0F, 0.8F);
    }

    private static WolfSoundVariant.WolfSoundSet sounds() {
        return net.minecraft.sounds.SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.SoundSet.BIG).adultSounds();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return sounds().growlSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sounds().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sounds().deathSound().value();
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
    }

    @Override
    protected float getSoundVolume() {
        return 1.4F;
    }
}
