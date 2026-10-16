package com.mistech.mistechmod.realm.dream.mob;

import java.util.ArrayDeque;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Eco (Sono REM): uma silhueta que <b>repete os seus passos</b> — percorre exatamente o caminho que você fez {@link #DELAY} ticks atrás
 * (atravessando tudo, pulos incluídos). Se você para, ele te alcança: ao tocar, causa dano mágico e Escuridão e se desfaz.
 * Frágil: um ou dois golpes o estilhaçam. Sem alvo por muito tempo, some.
 */
public class EchoEntity extends Monster {
    public static final int DELAY = 70;
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private final ArrayDeque<Float> yaws = new ArrayDeque<>();
    private @Nullable UUID follow;
    private int lonely;

    public EchoEntity(EntityType<? extends EchoEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Passa a seguir este jogador (o rastro começa de onde o eco está). */
    public void followPlayer(Player player) {
        this.follow = player.getUUID();
        this.trail.clear();
        this.yaws.clear();
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
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        Player target = this.follow == null ? null : level.getPlayerByUUID(this.follow);
        if (target == null || !target.isAlive() || target.isSpectator() || target.isCreative() || target.distanceToSqr(this) > 40 * 40) {
            this.follow = null;
            Player near = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 20.0,
                    e -> e instanceof Player p && !p.isCreative() && !p.isSpectator());
            if (near != null) {
                this.followPlayer(near);
            } else if (++this.lonely > 60 * 20) {
                this.fade(level);
            }
            return;
        }
        this.lonely = 0;
        this.trail.addLast(target.position());
        this.yaws.addLast(target.getYRot());
        if (this.trail.size() > DELAY) {
            Vec3 next = this.trail.pollFirst();
            float yaw = this.yaws.pollFirst();
            this.setPos(next.x, next.y, next.z);
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        } else {
            // ainda "gravando": vai devagar para o começo do rastro
            Vec3 first = this.trail.peekFirst();
            Vec3 d = first.subtract(this.position());
            if (d.lengthSqr() > 0.01) {
                Vec3 step = d.normalize().scale(Math.min(0.2, d.length()));
                this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);
                this.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.FEET, first);
            }
        }
        if (this.getBoundingBox().inflate(0.2).intersects(target.getBoundingBox())) {
            target.hurtServer(level, level.damageSources().indirectMagic(this, this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            target.sendOverlayMessage(Component.translatable("message.mistech.echo.caught"));
            this.fade(level);
        }
    }

    private void fade(ServerLevel level) {
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX() + (this.random.nextDouble() - 0.5) * 0.5, this.getY() + 0.05,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.5, 0, 0.01, 0);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.SCULK_CLICKING;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_BLOCK_BREAK;
    }
}
