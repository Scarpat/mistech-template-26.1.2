package com.mistech.mistechmod.lore;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.accessory.Accessories;
import com.mistech.mistechmod.accessory.Accessory;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Iara: mãe-d'água dos rios. À noite canta (melodia de harpa e sinos): quem estiver a até {@link #CHARM_RANGE} blocos, fora
 * d'água, é puxado devagar para o rio; quem cai na água vira alvo — ela agarra, puxa para o fundo e rouba o fôlego.
 * Fora d'água seca e se machuca. O Anel da Iara protege do canto. Solta Escama da Iara.
 */
public class IaraEntity extends Monster {
    public static final double CHARM_RANGE = 14.0;
    private static final EntityDataAccessor<Boolean> SINGING = SynchedEntityData.defineId(IaraEntity.class, EntityDataSerializers.BOOLEAN);

    private int songTimer = 200;
    private int songTicks;

    public IaraEntity(EntityType<? extends IaraEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.03F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 1.1)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public static boolean checkSpawn(EntityType<IaraEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getFluidState(pos).is(FluidTags.WATER) && level.getLevel().isDarkOutside() && random.nextInt(3) == 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SINGING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 40));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target, level) -> target.isInWater() && !protectedFromSong(target)));
    }

    public boolean isSinging() {
        return this.entityData.get(SINGING);
    }

    private static boolean protectedFromSong(LivingEntity entity) {
        return entity instanceof Player player && Accessories.has(player, Accessory.IARA_RING);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public int getMaxAirSupply() {
        return 4800;
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(this.getSpeed() * 0.6F, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            if (this.isSinging() && this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.NOTE, this.getX(), this.getY() + 2.2, this.getZ(), this.random.nextDouble(), 0, 0);
            }
            return;
        }
        // fora d'água ela seca
        if (!this.isInWaterOrRain() && this.tickCount % 40 == 0) {
            this.hurtServer(level, this.damageSources().dryOut(), 1.0F);
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && target.isInWater() && this.distanceToSqr(target) < 2.8 * 2.8 && this.tickCount % 20 == 0) {
            // agarra e puxa para o fundo
            Vec3 dm = target.getDeltaMovement();
            target.setDeltaMovement(dm.x * 0.3, -0.35, dm.z * 0.3);
            target.hurtMarked = true;
            target.setAirSupply(Math.max(-20, target.getAirSupply() - 60));
            level.sendParticles(ParticleTypes.BUBBLE, target.getX(), target.getY() + 1, target.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
        }
        this.songTick(level, target);
    }

    private void songTick(ServerLevel level, @Nullable LivingEntity target) {
        boolean canSing = this.isInWater() && level.isDarkOutside() && (target == null || !target.isInWater());
        if (this.songTicks > 0) {
            if (!canSing) {
                this.songTicks = 0;
            } else {
                this.songTicks--;
                if (this.songTicks % 10 == 0) {
                    float[] melody = {0.7F, 0.84F, 0.94F, 1.06F, 1.26F, 1.06F, 0.94F, 0.84F};
                    float pitch = melody[(this.songTicks / 10) % melody.length];
                    level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.HOSTILE, 2.5F, pitch);
                    if (this.songTicks % 20 == 0) {
                        level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.HOSTILE, 1.5F, pitch * 1.5F);
                    }
                }
                for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(CHARM_RANGE))) {
                    if (player.isInWater() || player.isSpectator() || player.isCreative() || protectedFromSong(player)) {
                        continue;
                    }
                    Vec3 pull = this.position().subtract(player.position()).multiply(1, 0, 1);
                    if (pull.lengthSqr() < 1.0) {
                        continue;
                    }
                    pull = pull.normalize().scale(0.09);
                    player.push(pull.x, 0.0, pull.z);
                    player.hurtMarked = true;
                    if (this.songTicks % 60 == 0) {
                        player.sendOverlayMessage(Component.translatable("message.mistech.iara.song").withStyle(ChatFormatting.AQUA));
                    }
                }
            }
            this.entityData.set(SINGING, this.songTicks > 0);
            return;
        }
        this.entityData.set(SINGING, false);
        if (canSing && --this.songTimer <= 0) {
            this.songTimer = 300 + this.random.nextInt(300);
            this.songTicks = 200;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        this.songTicks = 0;
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.DOLPHIN_AMBIENT_WATER : SoundEvents.DOLPHIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DROWNED_HURT_WATER;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DROWNED_DEATH_WATER;
    }

    @Override
    public float getVoicePitch() {
        return 1.5F;
    }
}
