package com.mistech.mistechmod.beyond;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Strider Rúnico (familiar do Nether, ritual Tier I no Nether). Já vem selado; qualquer um monta, mas <b>só se controla sem a
 * vara de fungo</b> (para a frente/trás com as teclas de andar) e é 40% mais rápido. Segue o dono quando ninguém está montado.
 * Se o dono cair na lava a até {@link #RESCUE_RANGE} blocos, ele corre até lá e o põe nas costas (recarga 10 s). Não procria.
 */
public class RunicStriderEntity extends Strider {
    public static final double RESCUE_RANGE = 16.0;

    private @Nullable UUID owner;
    private int rescueCooldown;

    public RunicStriderEntity(EntityType<? extends RunicStriderEntity> type, Level level) {
        super(type, level);
    }

    public void bindTo(Player player) {
        this.owner = player.getUUID();
        this.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        this.setPersistenceRequired();
    }

    public @Nullable UUID owner() {
        return this.owner;
    }

    public @Nullable Player ownerPlayer() {
        return this.owner == null ? null : this.level().getPlayerByUUID(this.owner);
    }

    public static int countOwned(ServerLevel level, UUID owner) {
        return level.getEntities(PaleNetherContent.RUNIC_STRIDER.get(), s -> owner.equals(s.owner())).size();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.isSaddled() && this.getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float forward = controller.zza;
        return new Vec3(0.0, 0.0, forward > 0 ? 1.0 : forward < 0 ? -0.35 : 0.0);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return super.getRiddenSpeed(controller) * 1.4F;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable Strider getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).is(Items.WARPED_FUNGUS) && this.getHealth() < this.getMaxHealth()) {
            if (!this.level().isClientSide()) {
                this.heal(6.0F);
                player.getItemInHand(hand).consume(1, player);
                this.playSound(SoundEvents.STRIDER_EAT, 1.0F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel server) || this.tickCount % 20 != 0) {
            return;
        }
        if (this.rescueCooldown > 0) {
            this.rescueCooldown -= 20;
        }
        Player owner = this.ownerPlayer();
        if (owner == null || this.rescueCooldown > 0 || this.isVehicle() || owner.isPassenger()) {
            return;
        }
        if (owner.isInLava() && owner.distanceToSqr(this) <= RESCUE_RANGE * RESCUE_RANGE) {
            this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            owner.startRiding(this);
            owner.clearFire();
            this.rescueCooldown = 200;
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.4, 0.4, 0.4, 0.02);
            this.playSound(SoundEvents.STRIDER_HAPPY, 1.0F, 1.0F);
            owner.sendSystemMessage(Component.translatable("message.mistech.runic_strider.rescue"));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.owner != null) {
            output.putString("Owner", this.owner.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String owner = input.getStringOr("Owner", "");
        this.owner = owner.isEmpty() ? null : UUID.fromString(owner);
    }

    /** Anda até o dono quando ele se afasta mais de 8 blocos (e ninguém está montado). */
    private static class FollowOwnerGoal extends Goal {
        private final RunicStriderEntity strider;
        private int recalc;

        FollowOwnerGoal(RunicStriderEntity strider) {
            this.strider = strider;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player owner = this.strider.ownerPlayer();
            return owner != null && !this.strider.isVehicle() && !owner.isSpectator() && this.strider.distanceToSqr(owner) > 8.0 * 8.0;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = this.strider.ownerPlayer();
            return owner != null && !this.strider.isVehicle() && this.strider.distanceToSqr(owner) > 3.0 * 3.0;
        }

        @Override
        public void tick() {
            Player owner = this.strider.ownerPlayer();
            if (owner == null) {
                return;
            }
            this.strider.getLookControl().setLookAt(owner, 10.0F, this.strider.getMaxHeadXRot());
            if (--this.recalc <= 0) {
                this.recalc = 10;
                if (this.strider.distanceToSqr(owner) > 48.0 * 48.0) {
                    this.strider.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                } else {
                    this.strider.getNavigation().moveTo(owner, 1.2);
                }
            }
        }

        @Override
        public void stop() {
            this.strider.getNavigation().stop();
        }
    }
}
