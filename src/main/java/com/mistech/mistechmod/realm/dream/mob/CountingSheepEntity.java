package com.mistech.mistechmod.realm.dream.mob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mistech.mistechmod.registry.DreamRealmContent;

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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Ovelha-de-Contar (Sonho Comum): ovelhinha de lã de nuvem que vive pulando.
 * <ul>
 * <li>Tesoura: solta 1-2 Lã-de-Nuvem; a lã volta em ~3 min.</li>
 * <li>Mão vazia: você "conta" a ovelha — ela dá um pulão e toca uma nota (cada vez mais aguda).
 * Contou <b>3 ovelhas diferentes</b> em 30 s: Regeneração I por 20 s e Saturação ("descansou").</li>
 * </ul>
 */
public class CountingSheepEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> SHEARED = SynchedEntityData.defineId(CountingSheepEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int REGROW = 3 * 60 * 20;
    public static final int COUNT_WINDOW = 30 * 20;

    /** Contagem por jogador: ovelhas já contadas e quando começou. */
    private static final Map<UUID, Set<UUID>> COUNTED = new HashMap<>();
    private static final Map<UUID, Long> COUNT_START = new HashMap<>();

    private int regrow;
    private int hopCooldown = 40;

    public CountingSheepEntity(EntityType<? extends CountingSheepEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.SAFE_FALL_DISTANCE, 10.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHEARED, false);
    }

    public boolean isSheared() {
        return this.entityData.get(SHEARED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
        this.goalSelector.addGoal(3, new HopGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** De vez em quando dá um pulinho no lugar (pulando a "cerca" imaginária). */
    static class HopGoal extends Goal {
        private final CountingSheepEntity sheep;

        HopGoal(CountingSheepEntity sheep) {
            this.sheep = sheep;
        }

        @Override
        public boolean canUse() {
            return this.sheep.onGround() && --this.sheep.hopCooldown <= 0;
        }

        @Override
        public void start() {
            this.sheep.hopCooldown = 60 + this.sheep.getRandom().nextInt(160);
            this.sheep.hop(0.55);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    void hop(double strength) {
        this.setDeltaMovement(this.getDeltaMovement().x, strength, this.getDeltaMovement().z);
        this.hurtMarked = true;
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.WHITE_ASH, this.getX(), this.getY() + 0.3, this.getZ(), 8, 0.3, 0.1, 0.3, 0.02);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.isSheared() && --this.regrow <= 0) {
            this.entityData.set(SHEARED, false);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.SHEARS)) {
            if (this.isSheared()) {
                return InteractionResult.PASS;
            }
            if (this.level() instanceof ServerLevel server) {
                this.entityData.set(SHEARED, true);
                this.regrow = REGROW;
                int n = 1 + this.random.nextInt(2);
                for (int i = 0; i < n; i++) {
                    this.spawnAtLocation(server, new ItemStack(DreamRealmContent.CLOUD_WOOL.get()));
                }
                server.playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.2F);
                held.hurtAndBreak(1, player, hand.asEquipmentSlot());
            }
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            if (this.level() instanceof ServerLevel server) {
                count(server, player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void count(ServerLevel server, Player player) {
        long now = server.getGameTime();
        UUID id = player.getUUID();
        if (now - COUNT_START.getOrDefault(id, -COUNT_WINDOW * 2L) > COUNT_WINDOW) {
            COUNTED.put(id, new HashSet<>());
            COUNT_START.put(id, now);
        }
        Set<UUID> set = COUNTED.computeIfAbsent(id, k -> new HashSet<>());
        if (!set.add(this.getUUID())) {
            return; // essa já foi contada
        }
        this.hop(0.75);
        int n = set.size();
        server.playSound(null, this, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.0F, 0.8F + 0.25F * n);
        player.sendOverlayMessage(Component.translatable("message.mistech.counting_sheep.count", n));
        if (n >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20, 0));
            player.sendOverlayMessage(Component.translatable("message.mistech.counting_sheep.rested"));
            server.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.8, player.getZ(), 5, 0.4, 0.2, 0.4, 0.0);
            COUNTED.remove(id);
            COUNT_START.remove(id);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Sheared", this.isSheared());
        output.putInt("Regrow", this.regrow);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(SHEARED, input.getBooleanOr("Sheared", false));
        this.regrow = input.getIntOr("Regrow", 0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.3F + (this.random.nextFloat() - 0.5F) * 0.2F;
    }
}
