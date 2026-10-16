package com.mistech.mistechmod.realm.dream.mob;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.DreamDeepContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Memória (Sono REM): fantasminha manso que flutua devagar segurando uma lembrança que brilha. Clique com a mão vazia: ela conta a
 * lembrança (uma de {@link #LINES} frases), se desfaz e deixa um Fragmento de Memória.
 */
public class MemoryEntity extends DreamFlyerEntity {
    public static final int LINES = 14;

    public MemoryEntity(EntityType<? extends MemoryEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FLYING_SPEED, 0.2);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new WanderGoal(this, 0.4, 1));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return super.mobInteract(player, hand);
        }
        if (this.level() instanceof ServerLevel level) {
            int line = this.random.nextInt(LINES);
            player.sendSystemMessage(Component.translatable("message.mistech.memory." + line).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.6, this.getZ(), 20, 0.3, 0.4, 0.3, 0.04);
            level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.2F);
            this.spawnAtLocation(level, new ItemStack(DreamDeepContent.MEMORY_SHARD.get()));
            this.discard();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(8) == 0) {
            this.level().addParticle(ParticleTypes.WHITE_ASH, this.getX(), this.getY() + 0.3, this.getZ(), 0, 0.01, 0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 64 * 64;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }
}
