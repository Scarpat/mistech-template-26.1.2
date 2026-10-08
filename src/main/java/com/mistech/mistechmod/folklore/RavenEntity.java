package com.mistech.mistechmod.folklore;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.entity.OwlEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Corvo (familiar impuro, altar Tier II). Voa seguindo o dono; botão direito senta/segue.
 * Alimentado (carne crua ou podre, 10 min cada, até 16): <b>recolhe os itens caídos</b> num raio de
 * {@link #COLLECT_RANGE} blocos em volta do dono e larga aos pés dele (o que acabou de ser jogado fica).
 */
public class RavenEntity extends Parrot {
    public static final int FOOD_TICKS = 10 * 60 * 20;
    public static final int MAX_FOOD_TICKS = 16 * FOOD_TICKS;
    public static final double COLLECT_RANGE = 12.0;
    public static final int PER_SECOND = 4;

    private int fedTicks;

    public RavenEntity(EntityType<? extends RavenEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0, 5.0F, 1.0F));
    }

    public void bindTo(Player player) {
        this.tame(player);
        this.setPersistenceRequired();
        this.fedTicks = FOOD_TICKS;
    }

    public static int countOwned(ServerLevel level, java.util.UUID owner) {
        return level.getEntities(FolkloreContent.RAVEN.get(), e -> owner.equals(OwlEntity.ownerId(e))).size();
    }

    public static boolean isRavenFood(ItemStack stack) {
        return stack.is(Items.ROTTEN_FLESH) || stack.is(Items.BEEF) || stack.is(Items.MUTTON) || stack.is(Items.PORKCHOP);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.COOKIE)) {
            return InteractionResult.PASS;
        }
        if (this.isTame() && this.isOwnedBy(player) && isRavenFood(held)) {
            if (!this.level().isClientSide()) {
                if (this.fedTicks + FOOD_TICKS > MAX_FOOD_TICKS) {
                    player.sendOverlayMessage(Component.translatable("message.mistech.familiar.full"));
                    return InteractionResult.FAIL;
                }
                this.fedTicks += FOOD_TICKS;
                held.consume(1, player);
                this.heal(2.0F);
                this.playSound(SoundEvents.PARROT_EAT, 1.0F, 0.5F);
                player.sendOverlayMessage(Component.translatable("message.mistech.familiar.fed", (this.fedTicks + 1199) / 1200));
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel server) || !this.isTame() || this.tickCount % 20 != 0 || this.fedTicks <= 0) {
            return;
        }
        LivingEntity owner = this.getOwner();
        if (!(owner instanceof Player player) || owner.distanceToSqr(this) > 32 * 32) {
            return;
        }
        this.fedTicks -= 20;
        int moved = 0;
        for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(COLLECT_RANGE),
                e -> e.isAlive() && e.getAge() > 60 && e.distanceToSqr(player) > 2.5 * 2.5)) {
            server.sendParticles(ParticleTypes.SMOKE, item.getX(), item.getY() + 0.2, item.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
            item.teleportTo(player.getX(), player.getY() + 0.3, player.getZ());
            item.setDeltaMovement(0, 0.1, 0);
            item.setNoPickUpDelay();
            if (++moved >= PER_SECOND) {
                break;
            }
        }
        if (moved > 0 && this.getRandom().nextInt(4) == 0) {
            this.playSound(SoundEvents.PARROT_AMBIENT, 1.0F, 0.4F);
        }
    }

    @Override
    public @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.35F; // grasnado grave
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("FedTicks", this.fedTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.fedTicks = input.getIntOr("FedTicks", 0);
    }
}
