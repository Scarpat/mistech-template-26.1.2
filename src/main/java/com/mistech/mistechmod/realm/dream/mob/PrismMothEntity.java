package com.mistech.mistechmod.realm.dream.mob;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.DreamDeepContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Mariposa-Prisma (Sonho Profundo): asas iridescentes que brilham. É atraída pela luz — rodeia blocos que brilham (≥ 10) ou o jogador
 * que segura um item de luz — e, enquanto voa perto de uma luz, de tempos em tempos solta Pó de Sonho. Morrendo, também dá Pó de Sonho.
 */
public class PrismMothEntity extends DreamFlyerEntity {
    public static final int DUST_TICKS = 3 * 60 * 20;
    private int dustTimer;

    public PrismMothEntity(EntityType<? extends PrismMothEntity> type, Level level) {
        super(type, level);
        this.dustTimer = DUST_TICKS / 2 + this.random.nextInt(DUST_TICKS);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.45);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new SeekLightGoal(this));
        this.goalSelector.addGoal(5, new WanderGoal(this, 0.7, 2));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("DustTimer", this.dustTimer);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.dustTimer = in.getIntOr("DustTimer", DUST_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(6) == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.2, this.getZ(), 0, -0.02, 0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 64 * 64;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F;
    }

    static boolean isLightItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block && block.getBlock().defaultBlockState().getLightEmission() >= 10;
    }

    /** Procura luz por perto e fica rodeando; perto da luz, solta pó de vez em quando. */
    static class SeekLightGoal extends Goal {
        private final PrismMothEntity moth;
        private @Nullable Vec3 light;
        private @Nullable Player holder;
        private int scan;
        private double angle;

        SeekLightGoal(PrismMothEntity moth) {
            this.moth = moth;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean find() {
            this.holder = this.moth.level().getNearestPlayer(this.moth, 12.0);
            if (this.holder != null && (isLightItem(this.holder.getMainHandItem()) || isLightItem(this.holder.getOffhandItem()))) {
                return true;
            }
            this.holder = null;
            BlockPos o = this.moth.blockPosition();
            for (int i = 0; i < 48; i++) {
                BlockPos p = o.offset(this.moth.getRandom().nextInt(21) - 10, this.moth.getRandom().nextInt(11) - 5, this.moth.getRandom().nextInt(21) - 10);
                if (this.moth.level().getBlockState(p).getLightEmission() >= 10) {
                    this.light = Vec3.atCenterOf(p);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canUse() {
            if (--this.scan > 0) {
                return false;
            }
            this.scan = 40;
            return this.find();
        }

        @Override
        public boolean canContinueToUse() {
            if (this.holder != null) {
                return this.holder.isAlive() && this.holder.distanceToSqr(this.moth) < 16 * 16
                        && (isLightItem(this.holder.getMainHandItem()) || isLightItem(this.holder.getOffhandItem()));
            }
            return this.light != null && this.moth.level().getBlockState(BlockPos.containing(this.light)).getLightEmission() >= 10
                    && this.moth.getRandom().nextInt(600) != 0;
        }

        @Override
        public void stop() {
            this.light = null;
            this.holder = null;
        }

        @Override
        public void tick() {
            Vec3 c = this.holder != null ? this.holder.getEyePosition().add(0, 0.6, 0) : this.light;
            if (c == null) {
                return;
            }
            this.angle += 0.12;
            double r = 1.4 + 0.4 * Mth.sin((float) this.angle * 0.7F);
            this.moth.flyTo(c.add(Math.cos(this.angle) * r, 0.6 + 0.4 * Math.sin(this.angle * 1.3), Math.sin(this.angle) * r), 1.0);
            if (--this.moth.dustTimer <= 0 && this.moth.level() instanceof ServerLevel level) {
                this.moth.dustTimer = DUST_TICKS + this.moth.getRandom().nextInt(DUST_TICKS);
                this.moth.spawnAtLocation(level, new ItemStack(DreamDeepContent.DREAM_DUST.get()));
                level.sendParticles(ParticleTypes.END_ROD, this.moth.getX(), this.moth.getY(), this.moth.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }
}
