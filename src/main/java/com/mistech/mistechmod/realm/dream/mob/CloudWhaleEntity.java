package com.mistech.mistechmod.realm.dream.mob;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.realm.DreamLayout;
import com.mistech.mistechmod.realm.GnomeRealm;
import com.mistech.mistechmod.registry.DreamDeepContent;
import com.mistech.mistechmod.registry.DreamRealmContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Baleia-Nuvem (Sonho Profundo): gigante e mansa, nada no ar bem acima das ilhas ({@link #CRUISE} ± 8), atravessando tudo.
 * <ul>
 * <li>Mão vazia: sobe nas costas dela (até 2). Montado, ela vai devagar para onde você olha. Ao descer, Queda Lenta.</li>
 * <li>Algodão-de-Nuvem: ela canta e, a cada {@link #AMBER_COOLDOWN} ticks, solta Âmbar Onírico.</li>
 * <li>Nunca sai do Sonho Profundo sozinha: se passar da borda do anel, faz a curva.</li>
 * <li><b>Sela</b> (a do jogo): quem vai na frente pilota. Olhar + andar para frente = voa para onde olha (sobe e desce junto),
 * A/D = vira, pular = sobe. Selada e pilotada ela bate nos blocos (não atravessa) e pode ir a qualquer camada; solta, volta
 * nadando para o Sonho Profundo. Agachado + mão vazia (sem ninguém montado) tira a sela.</li>
 * <li>Solta pelo Apanha-Sonho Espacial (no Sonho ou fora): fica rodando em volta de onde foi solta, 24 blocos acima. Se alguém
 * pilotar e descer, o ponto passa a ser onde desceu (na mesma altura).</li>
 * </ul>
 */
public class CloudWhaleEntity extends PathfinderMob {
    public static final double CRUISE = 172.0;
    public static final int AMBER_COOLDOWN = 5 * 60 * 20;
    /** Velocidade pilotada (aceleração por tick; com o atrito de 0,91 dá ≈ 9 blocos/s). */
    private static final float RIDDEN_ACCEL = 0.04F;
    private float heading;
    private int amberReady;
    /** Ponto em volta do qual ela nada (solta pelo Apanha-Sonho Espacial, ou fora do Sonho). Nulo = baleia selvagem do Sonho. */
    private @Nullable BlockPos home;

    public CloudWhaleEntity(EntityType<? extends CloudWhaleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.heading = this.random.nextFloat() * 360.0F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 120.0).add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putFloat("Heading", this.heading);
        out.putInt("AmberReady", this.amberReady);
        if (this.home != null) {
            out.putLong("Home", this.home.asLong());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.heading = in.getFloatOr("Heading", 0.0F);
        this.amberReady = in.getIntOr("AmberReady", 0);
        long h = in.getLongOr("Home", Long.MIN_VALUE);
        this.home = h == Long.MIN_VALUE ? null : BlockPos.of(h);
    }

    public void setHome(BlockPos pos) {
        this.home = pos.immutable();
    }

    @Override
    public void tick() {
        // pilotada ela é sólida (não leva o jogador para dentro dos blocos); solta, atravessa tudo como sempre
        this.noPhysics = !(this.getControllingPassenger() instanceof Player);
        super.tick();
    }

    @Override
    public void travel(Vec3 input) {
        // solta: movimento próprio em customServerAiStep. Pilotada: voo com inércia, como o ghast feliz
        if (this.getControllingPassenger() instanceof Player) {
            this.moveRelative(RIDDEN_ACCEL, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.91F));
        }
    }

    // ----- sela

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.SADDLE ? this.isAlive() : super.canUseSlot(slot);
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(slot);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.isSaddled() && this.getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float strafe = controller.xxa * 0.3F;
        float forward = 0.0F;
        float up = 0.0F;
        if (controller.zza != 0.0F) {
            float pitch = controller.getXRot() * Mth.DEG_TO_RAD;
            forward = Mth.cos(pitch);
            up = -Mth.sin(pitch);
            if (controller.zza < 0.0F) {
                forward *= -0.4F;
                up *= -0.4F;
            }
        }
        if (controller.isJumping()) {
            up += 0.6F;
        }
        return new Vec3(strafe, up, forward);
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        // vira devagar (é uma baleia): A/D também viram
        float want = controller.getYRot() - controller.xxa * 25.0F;
        float yRot = this.getYRot() + Mth.clamp(Mth.wrapDegrees(want - this.getYRot()) * 0.06F, -3.0F, 3.0F);
        this.setRot(yRot, 0.0F);
        this.yRotO = this.yBodyRot = this.yHeadRot = yRot;
        this.heading = yRot;
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return RIDDEN_ACCEL;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.amberReady > 0) {
            this.amberReady--;
        }
        if (this.getControllingPassenger() instanceof Player) {
            return; // quem move é o piloto (lado do cliente)
        }
        double speed;
        Entity rider = this.getFirstPassenger();
        if (rider instanceof Player player) {
            // montado: vira devagar para onde o jogador olha
            float diff = Mth.wrapDegrees(player.getYRot() - this.heading);
            this.heading += Mth.clamp(diff, -2.0F, 2.0F);
            speed = 0.32;
        } else {
            this.heading += Mth.sin(this.tickCount * 0.004F + this.getId()) * 0.35F;
            speed = 0.14;
        }
        double cruise = CRUISE;
        if (this.home == null && GnomeRealm.isRealm(level)) {
            // selvagem: não sai do Sonho Profundo: perto das bordas do anel, vira para o meio dele
            double d = DreamLayout.distance(this.getX(), this.getZ());
            if (d < DreamLayout.NIGHTMARE + 150 || d > DreamLayout.DEEP - 150) {
                float out = (float) (Mth.atan2(this.getZ(), this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
                float want = d < DreamLayout.NIGHTMARE + 150 ? out : out + 180.0F;
                this.heading += Mth.clamp(Mth.wrapDegrees(want - this.heading), -1.5F, 1.5F);
            }
        } else {
            // solta pelo Apanha-Sonho (ou fora do Sonho): roda em volta da casa, 24 blocos acima
            if (this.home == null) {
                this.home = this.blockPosition();
            }
            double dx = this.home.getX() - this.getX(), dz = this.home.getZ() - this.getZ();
            if (dx * dx + dz * dz > 40.0 * 40.0) {
                float back = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
                this.heading += Mth.clamp(Mth.wrapDegrees(back - this.heading), -1.5F, 1.5F);
            } else {
                this.heading += 0.25F;
            }
            cruise = Math.min(this.home.getY() + 24.0, level.getMaxY() - 8.0);
            speed = Math.min(speed, 0.12);
        }
        double targetY = cruise + 8.0 * Math.sin(this.tickCount * 0.01 + this.getId());
        double vy = Mth.clamp((targetY - this.getY()) * 0.02, -0.08, 0.08);
        float rad = this.heading * Mth.DEG_TO_RAD;
        Vec3 vel = new Vec3(-Mth.sin(rad) * speed, vy, Mth.cos(rad) * speed);
        this.setDeltaMovement(vel);
        this.move(MoverType.SELF, vel);
        this.setYRot(this.heading);
        this.yBodyRot = this.heading;
        this.yHeadRot = this.heading;
        if (this.tickCount % 400 == 0 && this.random.nextInt(3) == 0) {
            this.playSound(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 3.0F, 0.5F);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(DreamRealmContent.CLOUD_COTTON.get().asItem())) {
            if (this.level() instanceof ServerLevel level) {
                stack.consume(1, player);
                level.playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.NEUTRAL, 3.0F, 0.6F);
                level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 3.0, this.getZ(), 6, 1.5, 0.5, 1.5, 0.0);
                if (this.amberReady <= 0) {
                    this.amberReady = AMBER_COOLDOWN;
                    this.spawnAtLocation(level, new ItemStack(DreamDeepContent.DREAM_AMBER.get()), 2.0F);
                } else {
                    player.sendOverlayMessage(Component.translatable("message.mistech.cloud_whale.full"));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (this.isEquippableInSlot(stack, EquipmentSlot.SADDLE) && !this.isSaddled()) {
            return stack.interactLivingEntity(player, this, hand);
        }
        if (stack.isEmpty() && player.isSecondaryUseActive() && this.isSaddled() && !this.isVehicle()) {
            // agachado + mão vazia: tira a sela
            if (!this.level().isClientSide()) {
                ItemStack saddle = this.getItemBySlot(EquipmentSlot.SADDLE).copy();
                this.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
                if (!player.getInventory().add(saddle)) {
                    player.drop(saddle, false);
                }
                this.playSound(SoundEvents.SADDLE_UNEQUIP.value(), 1.0F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.isEmpty() && !player.isSecondaryUseActive() && this.getPassengers().size() < 2) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 2;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int index = Math.max(0, this.getPassengers().indexOf(passenger));
        if (this.isSaddled()) {
            // selada: o piloto vai na sela, à frente das nuvens; o carona, em cima das nuvens
            return new Vec3(0.0, index == 0 ? 3.35 : 3.5, index == 0 ? 0.9 : -0.9).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
        }
        return new Vec3(0.0, 3.25, index == 0 ? -0.6 : 1.4).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (this.home != null && passenger instanceof Player && !this.level().isClientSide()) {
            // tem casa: a nova casa é onde o piloto desceu (na altura em que ela está)
            this.home = this.blockPosition().below(24);
        }
        if (passenger instanceof LivingEntity living && !this.level().isClientSide()) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 20, 0, false, true));
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();
    }

    @Override
    public float getVoicePitch() {
        return 0.4F;
    }
}
