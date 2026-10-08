package com.mistech.mistechmod.artifact;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.item.mithril.MithrilGear;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Vassoura das Nuvens montada. Quem monta controla (o cliente mexe a vassoura, como num barco):
 * frente/trás voam para onde o jogador olha, lados de lado, pulo sobe; agachar desce da vassoura (vanilla).
 * Gasta {@link #FE_PER_SECOND} FE por segundo montada; sem energia ela desce devagar.
 * Runas engastadas: Ar = +velocidade, Energia = gasta menos, Nuvens = rastro de nuvem e queda lenta ao descer,
 * Luz = brilha, Fogo/Gelo = rastro.
 * Sem ninguém: fica parada flutuando. Botão direito = montar; agachar + botão direito (ou bater nela) = recolher.
 */
public class BroomEntity extends Entity {
    public static final double BASE_SPEED = 0.7;
    public static final double CLIMB_SPEED = 0.45;
    public static final double AIR_BONUS = 0.12;
    public static final int FE_PER_SECOND = 200;
    public static final double ENERGY_SAVING = 0.15;

    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(BroomEntity.class, EntityDataSerializers.ITEM_STACK);

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    public BroomEntity(EntityType<? extends BroomEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ITEM, ItemStack.EMPTY);
    }

    public ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(ITEM, stack.copy());
    }

    private Map<RuneElement, Double> runes() {
        return GearRunes.tally(this.getItem());
    }

    public boolean hasEnergy() {
        return MithrilGear.isCharged(this.getItem());
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, dimensions.height() * 0.5, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        this.interpolation.interpolate();
        LivingEntity rider = this.getControllingPassenger();
        if (this.isLocalInstanceAuthoritative()) {
            Vec3 motion;
            if (rider instanceof Player player && this.level().isClientSide()) {
                this.setYRot(player.getYRot());
                motion = this.steer(player);
            } else if (rider == null) {
                motion = this.getDeltaMovement().scale(0.6); // parada, flutuando
            } else {
                motion = this.getDeltaMovement();
            }
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, motion);
        }
        if (this.level() instanceof ServerLevel server) {
            this.serverTick(server, rider);
        }
    }

    /** Movimento a partir das teclas de quem monta (só no cliente dele). */
    private Vec3 steer(Player player) {
        float[] input = com.mistech.mistechmod.artifact.client.BroomInput.read();
        float forward = input[0], strafe = input[1];
        boolean up = input[2] > 0.0F;
        if (!this.hasEnergy()) {
            // sem energia: plana para baixo
            Vec3 look = player.getLookAngle();
            return new Vec3(look.x * forward * 0.2, -0.12, look.z * forward * 0.2);
        }
        double speed = BASE_SPEED * (1.0 + AIR_BONUS * GearRunes.count(this.runes(), RuneElement.AIR));
        Vec3 look = player.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize();
        Vec3 wanted = look.scale(forward * speed).add(side.scale(-strafe * speed * 0.6));
        if (up) {
            wanted = wanted.add(0.0, CLIMB_SPEED, 0.0);
        }
        // um pouco de inércia
        return this.getDeltaMovement().scale(0.55).add(wanted.scale(0.45));
    }

    private void serverTick(ServerLevel server, @Nullable LivingEntity rider) {
        Map<RuneElement, Double> runes = this.runes();
        this.setGlowingTag(GearRunes.count(runes, RuneElement.LIGHT) > 0.0);
        if (rider == null) {
            if (this.tickCount % 10 == 0) {
                server.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 1, 0.3, 0.05, 0.3, 0.0);
            }
            return;
        }
        if (this.tickCount % 20 == 0 && !(rider instanceof Player p && p.hasInfiniteMaterials())) {
            ItemStack stack = this.getItem().copy();
            double saving = Math.min(0.75, ENERGY_SAVING * GearRunes.count(runes, RuneElement.ENERGY));
            MithrilGear.setEnergy(stack, MithrilGear.getEnergy(stack) - (int) Math.ceil(FE_PER_SECOND * (1.0 - saving)));
            this.setItem(stack);
        }
        if (this.getDeltaMovement().lengthSqr() > 0.01 && this.tickCount % 2 == 0) {
            ParticleOptions trail = GearRunes.count(runes, RuneElement.FIRE) > 0.0 ? ParticleTypes.FLAME
                    : GearRunes.count(runes, RuneElement.ICE) > 0.0 ? ParticleTypes.SNOWFLAKE
                    : GearRunes.count(runes, RuneElement.CLOUD) > 0.0 ? ParticleTypes.CLOUD : ParticleTypes.END_ROD;
            Vec3 back = this.position().subtract(this.getDeltaMovement().normalize().scale(0.8));
            server.sendParticles(trail, back.x, back.y + 0.1, back.z, 2, 0.1, 0.05, 0.1, 0.0);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide() && passenger instanceof LivingEntity living && !living.onGround()) {
            int ticks = GearRunes.count(this.runes(), RuneElement.CLOUD) > 0.0 ? 200 : 60;
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0, false, false, true));
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                this.pickUp(player);
            }
            return InteractionResult.SUCCESS;
        }
        if (this.getPassengers().isEmpty() && !player.isPassenger()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Volta a ser item no inventário de quem recolheu. */
    public void pickUp(Player player) {
        if (this.isRemoved()) {
            return;
        }
        this.ejectPassengers();
        ItemStack stack = this.getItem();
        if (!stack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(stack.copy());
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.0F);
        this.discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && !this.hasPassenger(player)) {
            this.pickUp(player);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return false;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public ItemStack getPickResult() {
        return this.getItem().copy();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setItem(input.read("Item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (!this.getItem().isEmpty()) {
            output.store("Item", ItemStack.OPTIONAL_CODEC, this.getItem());
        }
    }
}
