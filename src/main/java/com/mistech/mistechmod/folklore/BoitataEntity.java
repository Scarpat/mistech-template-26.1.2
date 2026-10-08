package com.mistech.mistechmod.folklore;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Boitatá: a cobra de fogo que protege a mata, acordada no altar Tier III numa Lua de Sangue.
 * Voa atravessando tudo, com um corpo comprido de segmentos de fogo (o rastro das últimas posições, calculado igual
 * nos dois lados). Rodeia o alvo cuspindo bolas de fogo e de tempos em tempos dá um bote. Abaixo da metade da vida
 * cospe mais e faz chover fogo em volta do alvo. Quem encosta no corpo pega fogo.
 */
public class BoitataEntity extends Monster {
    public static final int SEGMENTS = 16;
    public static final double SPACING = 1.1;
    public static final int TRAIL_MAX = 240;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    /** Posições passadas da cabeça (mais nova primeiro). */
    private final List<Vec3> trail = new ArrayList<>();
    private @Nullable BlockPos home;
    private int diveTicks;
    private int diveCooldown = 120;
    private int fireballCooldown = 60;
    private int rainCooldown = 100;
    private int idleTicks;
    private double orbitAngle;

    public BoitataEntity(EntityType<? extends BoitataEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.xpReward = 150;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    public void setHome(BlockPos pos) {
        this.home = pos.immutable();
    }

    // ----- rastro / segmentos -----

    private void recordTrail() {
        Vec3 now = this.position();
        if (this.trail.isEmpty() || this.trail.get(0).distanceToSqr(now) > 0.04) {
            this.trail.add(0, now);
            if (this.trail.size() > TRAIL_MAX) {
                this.trail.remove(this.trail.size() - 1);
            }
        }
    }

    /** Centros dos segmentos do corpo, andando pelo rastro a partir de {@code head}. */
    public List<Vec3> segments(Vec3 head) {
        List<Vec3> out = new ArrayList<>(SEGMENTS);
        Vec3 prev = head;
        double need = SPACING;
        for (Vec3 p : this.trail) {
            double d = prev.distanceTo(p);
            while (d >= need && out.size() < SEGMENTS) {
                Vec3 at = prev.add(p.subtract(prev).scale(need / d));
                out.add(at);
                prev = at;
                d = prev.distanceTo(p);
                need = SPACING;
            }
            need -= d;
            prev = p;
            if (out.size() >= SEGMENTS) {
                break;
            }
        }
        // rastro curto (acabou de nascer): o resto do corpo fica enrolado atrás
        while (out.size() < SEGMENTS) {
            Vec3 last = out.isEmpty() ? head : out.get(out.size() - 1);
            Vec3 back = Vec3.directionFromRotation(0, this.getYRot()).scale(-SPACING * 0.5);
            out.add(last.add(back).add(0, -0.15, 0));
        }
        return out;
    }

    // ----- movimento e ataque -----

    @Override
    public void travel(Vec3 input) {
        // movimento próprio (customServerAiStep); no cliente a posição vem do servidor
    }

    @Override
    public void tick() {
        super.tick();
        this.recordTrail();
        if (this.level().isClientSide()) {
            RandomSource r = this.getRandom();
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(ParticleTypes.FLAME, this.getX() + (r.nextDouble() - 0.5) * 1.5, this.getY() + r.nextDouble() * 1.4,
                        this.getZ() + (r.nextDouble() - 0.5) * 1.5, 0, 0.03, 0);
            }
            List<Vec3> segs = this.segments(this.position());
            Vec3 s = segs.get(r.nextInt(segs.size()));
            this.level().addParticle(r.nextBoolean() ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, s.x + (r.nextDouble() - 0.5),
                    s.y + 0.6 + (r.nextDouble() - 0.5), s.z + (r.nextDouble() - 0.5), 0, 0.02, 0);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.home == null) {
            this.home = this.blockPosition();
        }
        LivingEntity target = this.getTarget();
        if (this.tickCount % 20 == 0 && (target == null || !target.isAlive() || target.distanceToSqr(this) > 64 * 64
                || target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            target = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 48.0,
                    e -> e instanceof Player p && !p.isCreative() && !p.isSpectator());
            this.setTarget(target);
        }
        boolean enraged = this.getHealth() < this.getMaxHealth() / 2;
        Vec3 goal;
        double maxSpeed;
        if (target != null) {
            this.idleTicks = 0;
            if (this.diveTicks > 0) {
                this.diveTicks--;
                goal = target.position().add(0, target.getBbHeight() * 0.5, 0);
                maxSpeed = enraged ? 1.25 : 1.05;
            } else {
                this.orbitAngle += enraged ? 0.07 : 0.05;
                double r = 9.0 + 2.0 * Math.sin(this.tickCount * 0.03);
                goal = target.position().add(Math.cos(this.orbitAngle) * r, 5.0 + 2.5 * Math.sin(this.tickCount * 0.05), Math.sin(this.orbitAngle) * r);
                maxSpeed = 0.65;
                if (--this.diveCooldown <= 0) {
                    this.diveCooldown = enraged ? 80 : 120;
                    this.diveTicks = 40;
                    this.playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.4F);
                }
                if (--this.fireballCooldown <= 0) {
                    this.fireballCooldown = enraged ? 30 : 55;
                    this.spit(level, target, enraged ? 4 : 3);
                }
            }
            if (enraged && --this.rainCooldown <= 0) {
                this.rainCooldown = 100;
                this.fireRain(level, target);
            }
        } else {
            this.idleTicks++;
            this.orbitAngle += 0.03;
            Vec3 center = Vec3.atCenterOf(this.home);
            goal = center.add(Math.cos(this.orbitAngle) * 12, 8 + 3 * Math.sin(this.tickCount * 0.02), Math.sin(this.orbitAngle) * 12);
            maxSpeed = 0.45;
            // sem ninguém por muito tempo, de dia: volta para a mata
            if (this.idleTicks > 2400 && level.isBrightOutside()) {
                level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1, this.getZ(), 80, 1.5, 1.5, 1.5, 0.1);
                this.discard();
                return;
            }
        }
        // direção suave até o objetivo
        Vec3 desired = goal.subtract(this.position());
        double len = desired.length();
        if (len > 0.001) {
            desired = desired.scale(Math.min(maxSpeed, len) / len);
        }
        Vec3 vel = this.getDeltaMovement().add(desired.subtract(this.getDeltaMovement()).scale(0.12));
        this.setDeltaMovement(vel);
        this.move(MoverType.SELF, vel);
        if (vel.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(vel.z, vel.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        }
        this.setXRot((float) (-Mth.atan2(vel.y, vel.horizontalDistance()) * Mth.RAD_TO_DEG));

        this.contactDamage(level);
    }

    private void spit(ServerLevel level, LivingEntity target, int count) {
        Vec3 mouth = this.position().add(Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(1.5)).add(0, 0.8, 0);
        for (int i = 0; i < count; i++) {
            Vec3 dir = target.getEyePosition().subtract(mouth).normalize()
                    .add(this.random.triangle(0, 0.12), this.random.triangle(0, 0.08), this.random.triangle(0, 0.12));
            SmallFireball ball = new SmallFireball(level, this, dir.normalize());
            ball.setPos(mouth.x, mouth.y, mouth.z);
            level.addFreshEntity(ball);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    /** Chuva de fogo: acende o chão em volta do alvo. */
    private void fireRain(ServerLevel level, LivingEntity target) {
        RandomSource r = this.random;
        for (int i = 0; i < 8; i++) {
            BlockPos p = target.blockPosition().offset(r.nextInt(11) - 5, 3, r.nextInt(11) - 5);
            for (int dy = 0; dy < 8 && level.getBlockState(p).isAir(); dy++) {
                p = p.below();
            }
            BlockPos fire = p.above();
            if (level.getBlockState(fire).isAir() && BaseFireBlock.canBePlacedAt(level, fire, net.minecraft.core.Direction.UP)) {
                level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
            }
            level.sendParticles(ParticleTypes.LAVA, fire.getX() + 0.5, fire.getY() + 0.5, fire.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5F, 0.5F);
    }

    private void contactDamage(ServerLevel level) {
        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.4), this::canBurn)) {
            if (e.hurtServer(level, source, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                e.igniteForSeconds(6.0F);
                this.diveTicks = 0;
            }
        }
        if (this.tickCount % 10 == 0) {
            List<Vec3> segs = this.segments(this.position());
            for (int i = 1; i < segs.size(); i += 2) {
                Vec3 s = segs.get(i);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(s, s).inflate(1.0), this::canBurn)) {
                    if (e.hurtServer(level, level.damageSources().inFire(), 4.0F)) {
                        e.igniteForSeconds(4.0F);
                    }
                }
            }
        }
    }

    private boolean canBurn(LivingEntity e) {
        return e != this && e.isAlive() && !(e instanceof BoitataEntity) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    // ----- chefe -----

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
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, net.minecraft.world.level.block.state.BlockState onState, BlockPos pos) {
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_BURN;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.4F;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.home != null) {
            output.store("Home", BlockPos.CODEC, this.home);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.home = input.read("Home", BlockPos.CODEC).orElse(null);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(net.minecraft.network.chat.@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    /** Desenhado mesmo longe (o corpo é comprido). */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
