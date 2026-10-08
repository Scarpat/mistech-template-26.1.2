package com.mistech.mistechmod.folklore;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.Mule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * Mula sem Cabeça: montaria de fogo, chamada no altar Tier III numa sexta-feira à noite. Já nasce mansa e selada para
 * quem fez o ritual. Não pega fogo, <b>anda sobre lava</b>, é rápida e salta alto; quem monta fica resistente ao fogo e
 * o que ela atropela pega fogo. Do pescoço sai fogo.
 *
 * <p>Fora do ritual (ovo, /summon) ela nasce selvagem: amanse montando, como uma mula comum. Mansa, aceita sela e
 * baú do mesmo jeito (a tag minecraft:can_equip_saddle inclui a mula).
 */
public class HeadlessMuleEntity extends Mule {
    public HeadlessMuleEntity(EntityType<? extends HeadlessMuleEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseChestedHorseAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.JUMP_STRENGTH, 1.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    public void bindTo(Player player) {
        this.tameWithName(player);
        this.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        this.setPersistenceRequired();
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidTags.LAVA);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        Level level = this.level();
        RandomSource r = this.getRandom();
        if (level.isClientSide()) {
            // o modelo já tem as labaredas; as partículas só completam: faíscas, fumaça e brasas caindo
            Vec3 forward = Vec3.directionFromRotation(0, this.yBodyRot).scale(0.85);
            double x = this.getX() + forward.x;
            double y = this.getY() + 1.45;
            double z = this.getZ() + forward.z;
            if (r.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 0.25, y + 0.3 + r.nextDouble() * 0.4, z + (r.nextDouble() - 0.5) * 0.25,
                        0, 0.04, 0);
            }
            if (r.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.SMOKE, x, y + 0.9, z, 0, 0.05, 0);
            }
            if (r.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.LAVA, x, y + 0.4, z, 0, 0, 0);
            }
            if (r.nextInt(6) == 0 && this.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                // brasas dos cascos
                level.addParticle(ParticleTypes.SMALL_FLAME, this.getX() + (r.nextDouble() - 0.5) * 0.9, this.getY() + 0.05,
                        this.getZ() + (r.nextDouble() - 0.5) * 0.9, 0, 0.02, 0);
            }
            return;
        }
        if (!(level instanceof ServerLevel server) || this.tickCount % 10 != 0) {
            return;
        }
        LivingEntity rider = this.getControllingPassenger();
        if (rider != null) {
            rider.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.02) {
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.3),
                        e -> e != this && e != rider && e.isAlive())) {
                    e.igniteForSeconds(4.0F);
                    e.hurtServer(server, server.damageSources().inFire(), 3.0F);
                }
                server.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.1, this.getZ(), 4, 0.4, 0.05, 0.4, 0.01);
            }
        }
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_HORSE_DEATH;
    }
}
