package com.mistech.mistechmod.boitata;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Boitatá montaria, chocado do Ovo do Boitatá. Cresce com o tempo (e mais rápido comendo):
 * <b>filhote</b> (segue o dono) → <b>jovem</b> (1 dia: montaria de chão, anda sobre lava) → <b>adulto</b> (+2 dias: voa montado,
 * como a vassoura; segurando a tecla R solta um jato de fogo contínuo, com fôlego). Pode vestir armadura (escamas ou oricalco)
 * e obedece ao Apito do Boitatá (seguir / ficar / vagar / chamar). Morte é permanente.
 */
public class BoitataMountEntity extends TamableAnimal {
    public static final int BABY = 0, YOUNG = 1, ADULT = 2;
    public static final int YOUNG_AT = 24000, ADULT_AT = 72000;
    public static final float[] SCALE = {0.4F, 0.8F, 1.25F};
    public static final double[] HEALTH = {20.0, 50.0, 120.0};
    public static final double FLY_SPEED = 0.9;
    public static final double CLIMB = 0.45;
    /** Fôlego do sopro contínuo: 10 s de fogo; recarrega 1 por tick parado. */
    public static final int MAX_STAMINA = 200;
    public static final int COMMAND_FOLLOW = 0, COMMAND_STAY = 1, COMMAND_WANDER = 2;
    public static final Identifier ARMOR_MODIFIER = Identifier.fromNamespaceAndPath(Mistech.MODID, "boitata_mount_armor");
    public static final Identifier TOUGHNESS_MODIFIER = Identifier.fromNamespaceAndPath(Mistech.MODID, "boitata_mount_toughness");

    private static final EntityDataAccessor<Integer> GROWTH = SynchedEntityData.defineId(BoitataMountEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BREATHING = SynchedEntityData.defineId(BoitataMountEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> ARMOR = SynchedEntityData.defineId(BoitataMountEntity.class, EntityDataSerializers.ITEM_STACK);

    private int appliedStage = -1;
    private int stamina = MAX_STAMINA;
    private int command = COMMAND_FOLLOW;

    public BoitataMountEntity(EntityType<? extends BoitataMountEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.LAVA, 0.0F);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0F);
        this.setPathfindingMalus(PathType.FIRE, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH[0])
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROWTH, 0);
        builder.define(BREATHING, false);
        builder.define(ARMOR, ItemStack.EMPTY);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1, 8.0F, 3.0F) {
            @Override
            public boolean canUse() {
                return BoitataMountEntity.this.command == COMMAND_FOLLOW && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return BoitataMountEntity.this.command == COMMAND_FOLLOW && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    public void bindTo(Player player) {
        this.tame(player);
        this.setPersistenceRequired();
    }

    // ----- crescimento -----

    public int growth() {
        return this.entityData.get(GROWTH);
    }

    public void setGrowth(int growth) {
        this.entityData.set(GROWTH, Math.max(0, Math.min(ADULT_AT, growth)));
    }

    public int stage() {
        int g = this.growth();
        return g >= ADULT_AT ? ADULT : g >= YOUNG_AT ? YOUNG : BABY;
    }

    public boolean isAdultStage() {
        return this.stage() == ADULT;
    }

    public float size() {
        return SCALE[this.stage()];
    }

    /** Voando agora (adulto montado fora do chão). */
    public boolean isFlyingNow() {
        return this.isAdultStage() && this.isVehicle() && !this.onGround();
    }

    public boolean isBreathing() {
        return this.entityData.get(BREATHING);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (GROWTH.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(this.size());
    }

    private void applyStage() {
        int stage = this.stage();
        if (stage == this.appliedStage) {
            return;
        }
        boolean grew = this.appliedStage >= 0 && stage > this.appliedStage;
        this.appliedStage = stage;
        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(HEALTH[stage]);
        }
        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(4.0 + 4.0 * stage);
        }
        if (grew) {
            this.setHealth(this.getMaxHealth());
            if (this.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.5, this.getZ(), 40, 0.6, 0.6, 0.6, 0.05);
                this.playSound(SoundEvents.BLAZE_AMBIENT, 1.2F, 0.6F);
                if (this.getOwner() instanceof Player owner) {
                    owner.sendSystemMessage(Component.translatable("message.mistech.boitata_mount.grew." + stage).withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }

    /** Quanto a comida adianta o crescimento (0 = não come). */
    public static int growthFrom(ItemStack stack) {
        if (stack.is(Items.BLAZE_ROD)) {
            return 3600;
        }
        if (stack.is(Items.BLAZE_POWDER) || stack.is(Items.MAGMA_CREAM)) {
            return 1200;
        }
        if (stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_MUTTON) || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.COOKED_RABBIT)) {
            return 600;
        }
        return 0;
    }

    // ----- armadura -----

    public ItemStack armor() {
        return this.entityData.get(ARMOR);
    }

    /** 0 = sem armadura, 1 = escamas, 2 = oricalco. */
    public int armorKind() {
        return this.armor().getItem() instanceof BoitataMountArmorItem item ? item.kind().ordinal() + 1 : 0;
    }

    public void setArmor(ItemStack stack) {
        this.entityData.set(ARMOR, stack.copyWithCount(stack.isEmpty() ? 0 : 1));
        this.applyArmor();
    }

    private void applyArmor() {
        AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
        AttributeInstance toughness = this.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armor != null) {
            armor.removeModifier(ARMOR_MODIFIER);
        }
        if (toughness != null) {
            toughness.removeModifier(TOUGHNESS_MODIFIER);
        }
        if (this.armor().getItem() instanceof BoitataMountArmorItem item) {
            if (armor != null) {
                armor.addPermanentModifier(new AttributeModifier(ARMOR_MODIFIER, item.kind().armor, AttributeModifier.Operation.ADD_VALUE));
            }
            if (toughness != null) {
                toughness.addPermanentModifier(new AttributeModifier(TOUGHNESS_MODIFIER, item.kind().toughness, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    // ----- comandos (Apito) -----

    public int command() {
        return this.command;
    }

    public void setCommand(int command) {
        this.command = command;
        this.setOrderedToSit(command == COMMAND_STAY);
        this.navigation.stop();
        this.setTarget(null);
    }

    // ----- vida -----

    @Override
    public void aiStep() {
        super.aiStep();
        boolean flying = this.isVehicle() && this.isAdultStage();
        if (this.isNoGravity() != flying) {
            this.setNoGravity(flying);
        }
        if (this.level() instanceof ServerLevel server) {
            this.applyStage();
            if (this.isTame() && this.tickCount % 20 == 0 && this.growth() < ADULT_AT) {
                this.setGrowth(this.growth() + 20);
            }
            LivingEntity rider = this.getControllingPassenger();
            if (rider != null && this.tickCount % 20 == 0) {
                rider.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
            }
            this.breathTick(server, rider);
            if (this.tickCount % 4 == 0) {
                server.sendParticles(ParticleTypes.SMALL_FLAME, this.getX(), this.getY() + this.getBbHeight() * 0.8, this.getZ(), 1,
                        0.3 * this.size(), 0.1, 0.3 * this.size(), 0.0);
            }
        } else {
            this.appliedStage = this.stage();
            if (this.isBreathing()) {
                this.breathParticles();
            }
        }
    }

    // ----- sopro contínuo -----

    /** Direção do sopro: para onde quem monta olha (ou o próprio Boitatá). */
    private Vec3 breathDirection() {
        LivingEntity rider = this.getControllingPassenger();
        return rider != null ? rider.getLookAngle() : Vec3.directionFromRotation(this.getXRot(), this.getYHeadRot());
    }

    /** Ponta do focinho (aproximada pelo tamanho). */
    public Vec3 mouth() {
        Vec3 flat = Vec3.directionFromRotation(0.0F, this.getYRot());
        float s = this.size();
        return this.position().add(flat.scale(1.9 * s)).add(0.0, 0.85 * s, 0.0);
    }

    public void setBreathing(Player rider, boolean on) {
        if (on && !this.isAdultStage()) {
            rider.sendOverlayMessage(Component.translatable("message.mistech.boitata_mount.too_young").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (on && this.stamina < 20) {
            rider.sendOverlayMessage(Component.translatable("message.mistech.boitata_mount.out_of_breath").withStyle(ChatFormatting.GRAY));
            return;
        }
        this.entityData.set(BREATHING, on);
    }

    private void breathTick(ServerLevel server, @Nullable LivingEntity rider) {
        if (!this.isBreathing()) {
            if (this.stamina < MAX_STAMINA) {
                this.stamina++;
            }
            return;
        }
        if (rider == null || !this.isAdultStage() || --this.stamina <= 0) {
            this.entityData.set(BREATHING, false);
            if (rider instanceof Player player && this.stamina <= 0) {
                player.sendOverlayMessage(Component.translatable("message.mistech.boitata_mount.out_of_breath").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        Vec3 dir = this.breathDirection();
        Vec3 from = this.mouth();
        float dmg = 4.0F * (this.armorKind() == 2 ? 1.25F : 1.0F);
        if (this.tickCount % 4 == 0) {
            FireBreath.cone(server, this, from, dir, 14.0, dmg, this.tickCount % 12 == 0);
        }
        // queima o que o jato acerta (com mobGriefing)
        if (this.tickCount % 8 == 0 && server.getGameRules().get(GameRules.MOB_GRIEFING)) {
            BlockHitResult hit = server.clip(new ClipContext(from, from.add(dir.scale(14.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos firePos = hit.getBlockPos().relative(hit.getDirection());
                if (server.getBlockState(firePos).isAir() && BaseFireBlock.canBePlacedAt(server, firePos, hit.getDirection())) {
                    server.setBlock(firePos, BaseFireBlock.getState(server, firePos), 11);
                }
            }
        }
    }

    private void breathParticles() {
        Vec3 dir = this.breathDirection();
        Vec3 from = this.mouth();
        var random = this.getRandom();
        for (int i = 0; i < 8; i++) {
            double speed = 0.7 + random.nextDouble() * 0.5;
            Vec3 v = dir.scale(speed).add((random.nextDouble() - 0.5) * 0.18, (random.nextDouble() - 0.5) * 0.18, (random.nextDouble() - 0.5) * 0.18);
            this.level().addParticle(i % 5 == 0 ? ParticleTypes.LAVA : i % 3 == 0 ? ParticleTypes.SMOKE : ParticleTypes.FLAME, from.x, from.y, from.z,
                    v.x, v.y, v.z);
        }
        if (this.tickCount % 6 == 0) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.NEUTRAL, 1.5F, 0.7F, false);
        }
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidTags.LAVA);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    // ----- interação -----

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!this.isTame() || !this.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (held.getItem() instanceof BoitataMountArmorItem) {
            if (!this.level().isClientSide()) {
                ItemStack old = this.armor();
                this.setArmor(held);
                held.consume(1, player);
                if (!old.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(old);
                }
                this.playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.0F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
        if (held.is(Items.SHEARS) && !this.armor().isEmpty()) {
            if (!this.level().isClientSide()) {
                player.getInventory().placeItemBackInInventory(this.armor().copy());
                this.setArmor(ItemStack.EMPTY);
                this.playSound(SoundEvents.ARMOR_UNEQUIP_WOLF, 1.0F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
        int food = growthFrom(held);
        if (food > 0) {
            if (!this.level().isClientSide()) {
                boolean useful = this.growth() < ADULT_AT || this.getHealth() < this.getMaxHealth();
                if (!useful) {
                    return InteractionResult.FAIL;
                }
                this.setGrowth(this.growth() + food);
                this.heal(food / 300.0F);
                held.consume(1, player);
                this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 0.7F);
                player.sendOverlayMessage(Component.translatable("message.mistech.boitata_mount.fed",
                        Component.translatable("message.mistech.boitata_mount.stage." + this.stage()), this.growthPercent()));
            }
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof BoitataWhistleItem) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive() || (this.stage() == BABY && held.isEmpty())) {
            if (!this.level().isClientSide()) {
                this.setCommand(this.command == COMMAND_STAY ? COMMAND_FOLLOW : COMMAND_STAY);
                player.sendOverlayMessage(Component.translatable("message.mistech.boitata_mount.command." + this.command));
            }
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && this.stage() >= YOUNG && !this.isVehicle() && !player.isPassenger()) {
            if (!this.level().isClientSide()) {
                this.setOrderedToSit(false);
                this.navigation.stop();
                this.setTarget(null);
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private int growthPercent() {
        int stage = this.stage();
        if (stage == ADULT) {
            return 100;
        }
        int from = stage == BABY ? 0 : YOUNG_AT, to = stage == BABY ? YOUNG_AT : ADULT_AT;
        return (this.growth() - from) * 100 / (to - from);
    }

    // ----- montaria -----

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player && this.isOwnedBy(player) ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && this.stage() >= YOUNG;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // sentado no dorso, logo atrás do pescoço
        return new Vec3(0.0, 0.5 * this.size(), 0.1 * this.size()).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        this.setYRot(player.getYRot());
        this.yRotO = this.getYRot();
        this.setXRot(this.isAdultStage() && !this.onGround() ? player.getXRot() * 0.6F : 0.0F);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        if (!this.level().isClientSide()) {
            return;
        }
        float[] keys = com.mistech.mistechmod.artifact.client.BroomInput.read();
        if (this.isAdultStage()) {
            float forward = keys[0], strafe = keys[1];
            Vec3 look = player.getLookAngle();
            Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize();
            Vec3 wanted = look.scale(forward * FLY_SPEED).add(side.scale(-strafe * FLY_SPEED * 0.6));
            if (keys[2] > 0.0F) {
                wanted = wanted.add(0.0, CLIMB, 0.0);
            }
            this.setDeltaMovement(this.getDeltaMovement().scale(0.6).add(wanted.scale(0.4)));
        } else if (keys[2] > 0.0F && this.onGround()) {
            Vec3 dm = this.getDeltaMovement();
            this.setDeltaMovement(dm.x, 0.6, dm.z);
            this.needsSync = true;
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        if (this.isAdultStage()) {
            return Vec3.ZERO;
        }
        float strafe = player.xxa * 0.5F;
        float forward = player.zza < 0 ? player.zza * 0.3F : player.zza;
        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (this.isInLava() ? 1.5F : 1.1F);
    }

    // ----- sons e salvar -----

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.getRandom().nextInt(3) == 0 ? SoundEvents.BLAZE_BURN : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + 0.3F * (2 - this.stage());
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        if (!this.armor().isEmpty()) {
            this.spawnAtLocation(level, this.armor().copy());
            this.setArmor(ItemStack.EMPTY);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Growth", this.growth());
        output.putInt("Command", this.command);
        if (!this.armor().isEmpty()) {
            output.store("BodyArmor", ItemStack.CODEC, this.armor());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setGrowth(input.getIntOr("Growth", 0));
        this.command = input.getIntOr("Command", this.isOrderedToSit() ? COMMAND_STAY : COMMAND_FOLLOW);
        this.setArmor(input.read("BodyArmor", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        this.appliedStage = -1;
    }
}
