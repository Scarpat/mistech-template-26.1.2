package com.mistech.mistechmod.entity;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Mal Olhado: ser alto e corcunda que aparece no Ermo Turvo e fica te vigiando de longe, por trás das árvores.
 * Não anda e não apanha. Se você chega perto ou encara ele por um tempo, ele some.
 * Quem cria ele é {@link com.mistech.mistechmod.realm.EvilEyeWatcher}.
 */
public class EvilEyeEntity extends Mob {
    public static final int MAX_LIFE = 90 * 20;
    public static final double VANISH_DISTANCE = 14.0;
    public static final int STARE_TICKS = 25;

    private @Nullable UUID watched;
    private int stared;
    private boolean noticed;
    /** Atraído pela Mácula (fora do Ermo): quando é notado, lança o mau-olhado no jogador. */
    private boolean cursing;

    public EvilEyeEntity(EntityType<? extends EvilEyeEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0);
    }

    public void setWatched(UUID player) {
        this.watched = player;
    }

    public void setCursing(boolean cursing) {
        this.cursing = cursing;
    }

    public @Nullable UUID watched() {
        return this.watched;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer player = this.watched == null ? null : level.getServer().getPlayerList().getPlayer(this.watched);
        if (player == null || player.level() != level || this.tickCount > MAX_LIFE) {
            this.discard();
            return;
        }
        // sempre virado para o jogador
        double dx = player.getX() - this.getX(), dz = player.getZ() - this.getZ();
        double dy = player.getEyeY() - this.getEyeY();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI));
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.setXRot(pitch);

        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < VANISH_DISTANCE) {
            if (this.cursing && !this.noticed) {
                com.mistech.mistechmod.purity.TaintOmens.curse(level, player);
            }
            this.vanish(level);
            return;
        }
        // o jogador está olhando para ele?
        Vec3 look = player.getViewVector(1.0F);
        Vec3 toMe = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ()).normalize();
        boolean looking = look.dot(toMe) > 0.985 && player.hasLineOfSight(this);
        if (looking) {
            if (!this.noticed) {
                this.noticed = true;
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 0.7F);
                level.playSound(null, this.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.35F, 0.5F);
                if (this.cursing) {
                    com.mistech.mistechmod.purity.TaintOmens.curse(level, player);
                }
            }
            if (++this.stared >= STARE_TICKS) {
                this.vanish(level);
            }
        } else if (this.tickCount % 60 == 0) {
            // sussurros de longe
            level.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.6F, 0.6F);
        }
    }

    private void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.6, this.getZ(), 25, 0.3, 0.9, 0.3, 0.01);
        level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 0.4F);
        this.discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!this.isRemoved()) {
            this.vanish(level);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
