package com.mistech.mistechmod.tarot.priestess;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Reflexo da Sacerdotisa (modo Jachin): por fora é igual a ela, mas não tem sombra. Quem cuida dele é a Sacerdotisa
 * (posição, para onde olha, quando ataca). Um golpe de jogador o estoura — com um contra-ataque rápido.
 * Some se a dona não existir mais. Não é salvo no mundo.
 */
public class PriestessReflectionEntity extends Monster implements PriestessFigure {
    private static final EntityDataAccessor<Integer> DATA_ACTION =
            SynchedEntityData.defineId(PriestessReflectionEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID owner;
    private int actionTicks;
    private int clientActionStart;

    public PriestessReflectionEntity(EntityType<? extends PriestessReflectionEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        // mesma vida máxima da Sacerdotisa para a barra/tooltip não denunciar; morre com qualquer golpe de jogador
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, PriestessEntity.MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    void setOwner(PriestessEntity owner) {
        this.owner = owner.getUUID();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, IDLE);
    }

    void setAction(int action) {
        this.entityData.set(DATA_ACTION, action);
        this.actionTicks = 0;
    }

    @Override
    public int figureAction() {
        return this.entityData.get(DATA_ACTION);
    }

    @Override
    public float figureActionTime(float partialTick) {
        return this.tickCount - this.clientActionStart + partialTick;
    }

    @Override
    public boolean seated() {
        return false;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_ACTION.equals(accessor)) {
            this.clientActionStart = this.tickCount;
        }
    }

    private @Nullable PriestessEntity owner(ServerLevel level) {
        return this.owner != null && level.getEntity(this.owner) instanceof PriestessEntity p && p.isAlive() ? p : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level) {
            if (++this.actionTicks > 22 && this.figureAction() != IDLE) {
                this.setAction(IDLE);
            }
            if (this.tickCount % 10 == 0 && (this.owner(level) == null || this.owner(level).mode() != PriestessEntity.JACHIN)) {
                this.discard();
            }
        } else if (this.random.nextInt(3) == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, this.getRandomX(0.9),
                    this.getY() + 0.5 + this.random.nextDouble() * 2.5, this.getRandomZ(0.9), 0.0, -0.01, 0.0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }
        if (!(source.getEntity() instanceof ServerPlayer attacker)) {
            return false;
        }
        PriestessEntity owner = this.owner(level);
        if (owner != null) {
            owner.onReflectionShattered(level, this, attacker);
        }
        this.discard();
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
    }
}
