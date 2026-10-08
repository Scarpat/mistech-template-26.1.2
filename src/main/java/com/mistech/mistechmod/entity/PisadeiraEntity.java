package com.mistech.mistechmod.entity;

import com.mistech.mistechmod.dream.SleepParalysis;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Pisadeira: a velha magra de unhas compridas que senta no peito de quem dorme de barriga cheia (ou de chá de cogumelo).
 * Não anda, não apanha e some sozinha. Quem controla onde ela fica e quando vai embora é {@link SleepParalysis}.
 */
public class PisadeiraEntity extends Mob {
    /** Segurança: nunca fica mais que isso no mundo. */
    public static final int MAX_LIFE = 20 * 20;

    public PisadeiraEntity(EntityType<? extends PisadeiraEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel server) {
            if (this.tickCount > MAX_LIFE || !SleepParalysis.isPisadeira(this)) {
                this.discard();
                return;
            }
            // risadinha: "ri ri ri"
            if (this.tickCount % 30 == 5) {
                server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE,
                        0.7F, 1.6F + this.random.nextFloat() * 0.2F);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }
}
