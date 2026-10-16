package com.mistech.mistechmod.realm.dream.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraft.world.phys.Vec3;

/**
 * Bicho-Papão (Pesadelo): figura alta e magra de braços compridos que vive no escuro (sai de baixo das camas da Casa Errada).
 * <ul>
 * <li>O golpe agarra: puxa o jogador para perto e dá Cegueira (3 s).</li>
 * <li>Machucado no escuro, às vezes some e reaparece atrás do jogador.</li>
 * <li><b>Luz forte (nível 11+)</b>: fica lento, bate fraco e queima devagar — leve tochas.</li>
 * </ul>
 */
public class BogeymanEntity extends Monster {
    private static final net.minecraft.resources.Identifier LIGHT_SLOW = net.minecraft.resources.Identifier.fromNamespaceAndPath("mistech", "bogeyman_light");
    public static final int BRIGHT = 11;

    public BogeymanEntity(EntityType<? extends BogeymanEntity> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean inBrightLight() {
        return this.level().getMaxLocalRawBrightness(BlockPos.containing(this.getEyePosition())) >= BRIGHT;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel server) || this.tickCount % 10 != 0) {
            return;
        }
        boolean bright = this.inBrightLight();
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            boolean has = speed.getModifier(LIGHT_SLOW) != null;
            if (bright && !has) {
                speed.addTransientModifier(new AttributeModifier(LIGHT_SLOW, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!bright && has) {
                speed.removeModifier(LIGHT_SLOW);
            }
        }
        if (bright && this.tickCount % 40 == 0) {
            this.hurtServer(server, this.damageSources().onFire(), 1.0F);
            server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.6, this.getZ(), 8, 0.3, 0.6, 0.3, 0.01);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (this.inBrightLight()) {
            // na luz o golpe sai fraco e não agarra
            return target.hurtServer(level, this.damageSources().mobAttack(this), 2.0F);
        }
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), this);
            Vec3 pull = this.position().subtract(living.position()).normalize().scale(0.6);
            living.push(pull.x, 0.1, pull.z);
            living.hurtMarked = true;
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isAlive() && !this.inBrightLight() && this.random.nextFloat() < 0.25F && this.getTarget() != null) {
            teleportBehind(level, this.getTarget());
        }
        return hurt;
    }

    private void teleportBehind(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        for (int i = 0; i < 8; i++) {
            double d = 2.5 + this.random.nextDouble() * 2.0;
            Vec3 p = target.position().subtract(look.x * d, 0, look.z * d).add((this.random.nextDouble() - 0.5) * 2, 0, (this.random.nextDouble() - 0.5) * 2);
            if (this.randomTeleport(p.x, p.y, p.z, false)) {
                level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.2, this.getZ(), 16, 0.3, 0.7, 0.3, 0.02);
                level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 0.5F);
                return;
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}
