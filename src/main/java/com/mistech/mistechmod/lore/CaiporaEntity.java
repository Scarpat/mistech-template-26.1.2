package com.mistech.mistechmod.lore;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Caipora, guardiã dos bichos, montada no queixada. Mansa na mata; brava quando vem atrás de quem caçou demais
 * (ver {@link LoreEvents}): carrega, e a cada 8 s assobia — tontura, cegueira e às vezes o caçador se perde na mata.
 * Oferenda (tabaco seco ou cachaça): acalma, zera a caça e dá a Bênção da Caça (10 min); depois ela vai embora.
 * Quem "mata" a Caipora não a mata: ela some rindo e deixa a Maldição da Caipora (20 min).
 */
public class CaiporaEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> ANGRY = SynchedEntityData.defineId(CaiporaEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int BLESSING_TICKS = 20 * 60 * 10;
    public static final int CURSE_TICKS = 20 * 60 * 20;

    private int whistle = 160;
    private int calmTicks;
    private int leavingTicks = -1;
    private final Set<UUID> blessed = new HashSet<>();

    public CaiporaEntity(EntityType<? extends CaiporaEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANGRY, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true) {
            @Override
            public boolean canUse() {
                return CaiporaEntity.this.isAngry() && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public void start() {
                super.start();
                CaiporaEntity.this.entityData.set(ANGRY, true);
            }
        });
    }

    public boolean isAngry() {
        return this.entityData.get(ANGRY);
    }

    /** Vem brava atrás deste caçador. */
    public void hunt(Player player) {
        this.entityData.set(ANGRY, true);
        this.setTarget(player);
        this.setPersistenceRequired();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            if (this.isAngry() && this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.ANGRY_VILLAGER, this.getRandomX(0.5), this.getY() + 2.1, this.getRandomZ(0.5), 0, 0, 0);
            }
            return;
        }
        if (this.leavingTicks >= 0) {
            this.getNavigation().stop();
            if (--this.leavingTicks <= 0) {
                this.vanish(level);
            }
            return;
        }
        if (!this.isAngry()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            if (++this.calmTicks > 20 * 60 * 3) {
                this.vanish(level);
            }
            return;
        }
        this.calmTicks = 0;
        if (--this.whistle <= 0 && this.distanceToSqr(target) < 24 * 24) {
            this.whistle = 160;
            this.whistleAt(level, target);
        }
    }

    /** O assobio da Caipora: tontura, cegueira, e às vezes o caçador se perde (é levado para outro canto da mata). */
    private void whistleAt(ServerLevel level, LivingEntity target) {
        level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.HOSTILE, 3.0F, 1.8F);
        level.playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.HOSTILE, 2.0F, 2.0F);
        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 140, 0), this);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0), this);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0), this);
        if (this.random.nextFloat() < 0.4F) {
            for (int i = 0; i < 10; i++) {
                double a = this.random.nextDouble() * Math.PI * 2;
                double d = 6 + this.random.nextDouble() * 6;
                double x = target.getX() + Math.cos(a) * d, z = target.getZ() + Math.sin(a) * d;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
                if (Math.abs(y - target.getY()) < 5 && target.randomTeleport(x, y, z, true)) {
                    if (target instanceof Player player) {
                        player.sendOverlayMessage(Component.translatable("message.mistech.caipora.lost").withStyle(ChatFormatting.DARK_GREEN));
                    }
                    break;
                }
            }
        }
    }

    private void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 1, this.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1, this.getZ(), 10, 0.5, 0.8, 0.5, 0.0);
        level.playSound(null, this.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.6F);
        this.discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean offering = held.is(Mistech.DRIED_TOBACCO_LEAF.get()) || held.getItem() instanceof com.mistech.mistechmod.drink.CachacaItem;
        if (!offering || this.leavingTicks >= 0) {
            return super.mobInteract(player, hand);
        }
        if (this.level() instanceof ServerLevel level) {
            if (!this.isAngry() && this.blessed.contains(player.getUUID())) {
                player.sendOverlayMessage(Component.translatable("message.mistech.caipora.already"));
                return InteractionResult.FAIL;
            }
            if (held.getItem() instanceof com.mistech.mistechmod.drink.CachacaItem) {
                player.getInventory().placeItemBackInInventory(new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
            }
            held.consume(1, player);
            boolean wasAngry = this.isAngry();
            this.entityData.set(ANGRY, false);
            this.setTarget(null);
            this.blessed.add(player.getUUID());
            player.setData(LoreContent.HUNT_SIN, 0);
            player.removeEffect(LoreContent.CAIPORA_CURSE);
            player.addEffect(new MobEffectInstance(LoreContent.HUNT_BLESSING, BLESSING_TICKS, 0));
            player.sendSystemMessage(Component.translatable(wasAngry ? "message.mistech.caipora.calmed" : "message.mistech.caipora.blessing")
                    .withStyle(ChatFormatting.GREEN));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.5, this.getZ(), 15, 0.4, 0.4, 0.4, 0.0);
            level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.NEUTRAL, 1.0F, 1.2F);
            if (wasAngry) {
                this.leavingTicks = 60;
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Ninguém mata a Caipora: ela some rindo e amaldiçoa quem tentou. */
    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel level) {
            if (source.getEntity() instanceof Player player) {
                player.addEffect(new MobEffectInstance(LoreContent.CAIPORA_CURSE, CURSE_TICKS, 0));
                player.sendSystemMessage(Component.translatable("message.mistech.caipora.curse").withStyle(ChatFormatting.DARK_RED));
            }
            this.vanish(level);
            return;
        }
        super.die(source);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.HOGLIN_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 1.4F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOGLIN_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !this.isAngry();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Angry", this.isAngry());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(ANGRY, input.getBooleanOr("Angry", false));
    }
}
