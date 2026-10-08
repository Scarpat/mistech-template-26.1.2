package com.mistech.mistechmod.folklore;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.entity.OwlEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Base dos familiares do folclore (Saci, Curupira): seguem o dono, botão direito senta/segue, comem Folha de Tabaco Seca
 * (10 min cada, até 16) e só trabalham alimentados. Não se reproduzem.
 */
public abstract class FolkFamiliar extends TamableAnimal {
    public static final int FOOD_TICKS = 10 * 60 * 20;
    public static final int MAX_FOOD_TICKS = 16 * FOOD_TICKS;

    protected int fedTicks;

    protected FolkFamiliar(EntityType<? extends FolkFamiliar> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.1, 6.0F, 2.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public void bindTo(Player player) {
        this.tame(player);
        this.setPersistenceRequired();
        this.fedTicks = Math.max(this.fedTicks, FOOD_TICKS);
    }

    public boolean isFed() {
        return this.fedTicks > 0;
    }

    public int fedTicks() {
        return this.fedTicks;
    }

    public void setFedTicks(int ticks) {
        this.fedTicks = Math.max(0, Math.min(MAX_FOOD_TICKS, ticks));
    }

    public static int countOwned(ServerLevel level, EntityType<? extends FolkFamiliar> type, UUID owner) {
        return level.getEntities(type, e -> owner.equals(OwlEntity.ownerId(e))).size();
    }

    /** Comida do familiar (padrão: Folha de Tabaco Seca). */
    public boolean isTreat(ItemStack stack) {
        return stack.is(com.mistech.mistechmod.Mistech.DRIED_TOBACCO_LEAF.get());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false; // não procria
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!this.isTame() || !this.isOwnedBy(player)) {
            return super.mobInteract(player, hand);
        }
        if (this.isTreat(held)) {
            if (!this.level().isClientSide()) {
                if (this.fedTicks + FOOD_TICKS > MAX_FOOD_TICKS) {
                    player.sendOverlayMessage(Component.translatable("message.mistech.familiar.full"));
                    return InteractionResult.FAIL;
                }
                this.fedTicks += FOOD_TICKS;
                held.consume(1, player);
                this.heal(4.0F);
                this.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 1.3F);
                player.sendOverlayMessage(Component.translatable("message.mistech.familiar.fed", (this.fedTicks + 1199) / 1200));
                this.onTreat(player);
            }
            return InteractionResult.SUCCESS;
        }
        InteractionResult special = this.interactSpecial(player, hand, held);
        if (special != null) {
            return special;
        }
        if (held.isEmpty() || !held.has(net.minecraft.core.component.DataComponents.FOOD)) {
            if (!this.level().isClientSide()) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.jumping = false;
                this.navigation.stop();
                this.setTarget(null);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Interação extra do familiar (antes do sentar/levantar). null = nada. */
    protected @Nullable InteractionResult interactSpecial(Player player, InteractionHand hand, ItemStack held) {
        return null;
    }

    protected void onTreat(Player player) {}

    /** Chamado a cada segundo no servidor, com o dono por perto. */
    protected abstract void workSecond(ServerLevel level, Player owner);

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel server) || !this.isTame() || this.tickCount % 20 != 0) {
            return;
        }
        LivingEntity owner = this.getOwner();
        if (owner instanceof Player player && owner.distanceToSqr(this) <= 32 * 32) {
            if (this.fedTicks > 0) {
                this.fedTicks = Math.max(0, this.fedTicks - 20);
            }
            this.workSecond(server, player);
        }
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
