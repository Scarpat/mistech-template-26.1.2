package com.mistech.mistechmod.tarot.magus;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Pedra Rúnica do modo Terra: três orbitam o Mago e o protegem (projéteis não passam, e cada pedra inteira
 * reduz o dano que ele leva). Quebra com {@link #HITS} golpes (picareta conta {@link #PICKAXE_HITS}).
 */
public class MagusRunestoneEntity extends Entity {
    public static final int HITS = 10;
    public static final int PICKAXE_HITS = 4;
    private static final EntityDataAccessor<Integer> DATA_CRACKS = SynchedEntityData.defineId(MagusRunestoneEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID owner;
    private int index;
    private int hits;
    private int hurtCooldown;

    public MagusRunestoneEntity(EntityType<? extends MagusRunestoneEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static MagusRunestoneEntity create(ServerLevel level, MagusEntity owner, int index) {
        MagusRunestoneEntity stone = new MagusRunestoneEntity(MagusContent.RUNESTONE.get(), level);
        stone.owner = owner.getUUID();
        stone.index = index;
        stone.setPos(orbit(owner, index, 0));
        return stone;
    }

    static Vec3 orbit(Entity owner, int index, int tick) {
        double a = tick * 0.045 + index * Math.PI * 2 / 3;
        double r = owner.getBbWidth() * 0.5 + 2.2;
        return owner.position().add(Math.cos(a) * r, owner.getBbHeight() * 0.55 + Math.sin(tick * 0.08 + index) * 0.4, Math.sin(a) * r);
    }

    public int cracks() {
        return this.entityData.get(DATA_CRACKS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CRACKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.WAX_ON, this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0, 0, 0);
            }
            return;
        }
        if (this.hurtCooldown > 0) {
            this.hurtCooldown--;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.owner == null || !(level.getEntity(this.owner) instanceof MagusEntity magus) || !magus.isAlive()) {
            this.discard();
            return;
        }
        Vec3 next = orbit(magus, this.index, magus.tickCount);
        this.setDeltaMovement(next.subtract(this.position()));
        this.setPos(next);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isRemoved() || this.hurtCooldown > 0 || !(source.getEntity() instanceof Player player)) {
            return false;
        }
        this.hurtCooldown = 6;
        boolean pick = player.getMainHandItem().is(ItemTags.PICKAXES);
        this.hits += pick ? HITS / PICKAXE_HITS + 1 : 1;
        this.entityData.set(DATA_CRACKS, Math.min(3, this.hits * 4 / HITS));
        level.playSound(null, this.blockPosition(), SoundEvents.STONE_HIT, SoundSource.HOSTILE, 1.5F, 0.6F + this.hits * 0.05F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                this.getX(), this.getY() + 0.3, this.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
        if (this.hits >= HITS) {
            level.playSound(null, this.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                    this.getX(), this.getY() + 0.3, this.getZ(), 40, 0.4, 0.4, 0.4, 0.2);
            level.sendParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + 0.3, this.getZ(), 20, 0.4, 0.4, 0.4, 0.5);
            if (this.owner != null && level.getEntity(this.owner) instanceof MagusEntity magus) {
                magus.onRunestoneBroken();
            }
            this.discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
