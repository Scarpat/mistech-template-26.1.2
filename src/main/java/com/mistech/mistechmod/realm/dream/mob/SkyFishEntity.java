package com.mistech.mistechmod.realm.dream.mob;

import java.util.EnumSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Peixe-do-Ar (Sonho Profundo): peixinhos que nadam no ar em cardume. O cardume segue o primeiro peixe (o "guia"),
 * cada um com uma posição própria em volta dele; fogem de quem chega perto. Três cores. Dá o Peixe-do-Ar (comida que dá Queda Lenta).
 */
public class SkyFishEntity extends DreamFlyerEntity {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(SkyFishEntity.class, EntityDataSerializers.INT);
    private @Nullable SkyFishEntity leader;
    private final Vec3 slot;

    public SkyFishEntity(EntityType<? extends SkyFishEntity> type, Level level) {
        super(type, level);
        this.slot = new Vec3(this.random.nextDouble() * 4 - 2, this.random.nextDouble() * 2 - 1, this.random.nextDouble() * 4 - 2);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        // o cardume nasce junto e da mesma cor
        if (data instanceof SchoolData school) {
            this.entityData.set(VARIANT, school.variant);
        } else {
            int v = this.random.nextInt(3);
            this.entityData.set(VARIANT, v);
            data = new SchoolData(v);
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    record SchoolData(int variant) implements SpawnGroupData {}

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Variant", this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.entityData.set(VARIANT, in.getIntOr("Variant", 0));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.4, 1.8));
        this.goalSelector.addGoal(3, new SchoolGoal(this));
        this.goalSelector.addGoal(5, new WanderGoal(this, 0.8, 3));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 64 * 64;
    }

    /** Segue o guia do cardume, cada um no seu lugar em volta dele. */
    static class SchoolGoal extends Goal {
        private final SkyFishEntity fish;

        SchoolGoal(SkyFishEntity fish) {
            this.fish = fish;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        private @Nullable SkyFishEntity findLeader() {
            List<SkyFishEntity> near = this.fish.level().getEntitiesOfClass(SkyFishEntity.class, this.fish.getBoundingBox().inflate(12),
                    f -> f != this.fish && f.leader == null);
            SkyFishEntity best = null;
            for (SkyFishEntity f : near) {
                if (best == null || f.getId() < best.getId()) {
                    best = f;
                }
            }
            return best != null && best.getId() < this.fish.getId() ? best : null;
        }

        @Override
        public boolean canUse() {
            if (this.fish.tickCount % 20 == 0 && this.fish.level() instanceof ServerLevel) {
                this.fish.leader = this.findLeader();
            }
            return this.fish.leader != null && this.fish.leader.isAlive();
        }

        @Override
        public void tick() {
            SkyFishEntity l = this.fish.leader;
            if (l != null) {
                this.fish.flyTo(l.position().add(this.fish.slot), 1.0);
            }
        }
    }
}
