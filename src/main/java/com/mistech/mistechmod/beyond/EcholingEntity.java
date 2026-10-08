package com.mistech.mistechmod.beyond;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.entity.OwlEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Ecólito (familiar do Deep Dark, ritual Tier II). Bolinha de sculk que flutua seguindo o dono; botão direito senta/segue.
 * Alimentado (Bloco de Sculk ou Garrafa de Encantamento, 10 min cada, até 16):
 * <ul>
 *   <li>"ouve" pelo dono: criaturas que fazem barulho a até 24 blocos do dono ganham contorno por 3 s, mesmo atrás de paredes,
 *       e só o dono vê;</li>
 *   <li>avisa quando há um Shrieker de Sculk a até {@link #SHRIEKER_RANGE} blocos.</li>
 * </ul>
 */
public class EcholingEntity extends Parrot {
    public static final int FOOD_TICKS = 10 * 60 * 20;
    public static final int MAX_FOOD_TICKS = 16 * FOOD_TICKS;
    public static final int SHRIEKER_RANGE = 16;

    private int fedTicks;
    private int warnCooldown;

    public EcholingEntity(EntityType<? extends EcholingEntity> type, Level level) {
        super(type, level);
    }

    /** Liga/desliga o contorno de {@code target} só para {@code player} (manda as flags com o bit de brilho). */
    public static void sendOutline(ServerPlayer player, Entity target, boolean on) {
        byte flags = target.getEntityData().get(DATA_SHARED_FLAGS_ID);
        if (on) {
            flags = (byte) (flags | (1 << 6));
        }
        player.connection.send(new ClientboundSetEntityDataPacket(target.getId(),
                List.of(SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID, flags))));
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

    public boolean isFed() {
        return this.fedTicks > 0;
    }

    public static int countOwned(ServerLevel level, UUID owner) {
        return level.getEntities(BeyondContent.ECHOLING.get(), e -> owner.equals(OwlEntity.ownerId(e))).size();
    }

    public static boolean isEcholingFood(ItemStack stack) {
        return stack.is(Items.SCULK) || stack.is(Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.COOKIE)) {
            return InteractionResult.PASS;
        }
        if (this.isTame() && this.isOwnedBy(player) && isEcholingFood(held)) {
            if (!this.level().isClientSide()) {
                if (this.fedTicks + FOOD_TICKS > MAX_FOOD_TICKS) {
                    player.sendOverlayMessage(Component.translatable("message.mistech.familiar.full"));
                    return InteractionResult.FAIL;
                }
                this.fedTicks += FOOD_TICKS;
                held.consume(1, player);
                this.heal(2.0F);
                this.playSound(SoundEvents.SCULK_CATALYST_BLOOM, 1.0F, 1.4F);
                player.sendOverlayMessage(Component.translatable("message.mistech.familiar.fed", (this.fedTicks + 1199) / 1200));
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.random.nextInt(6) == 0) {
                this.level().addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(0.4), this.getY() + 0.5, this.getRandomZ(0.4), 0.0, 0.02, 0.0);
            }
            return;
        }
        if (!(this.level() instanceof ServerLevel server) || !this.isTame() || this.tickCount % 20 != 0) {
            return;
        }
        if (this.warnCooldown > 0) {
            this.warnCooldown -= 20;
        }
        LivingEntity owner = this.getOwner();
        if (!(owner instanceof ServerPlayer player) || !this.isFed() || owner.distanceToSqr(this) > 32 * 32) {
            return;
        }
        this.fedTicks -= 20;
        EchoEvents.listenFor(player, server.getGameTime() + 30);
        if (this.warnCooldown <= 0 && this.tickCount % 60 == 0) {
            BlockPos shrieker = this.nearestShrieker(server, player.blockPosition());
            if (shrieker != null) {
                int distance = (int) Math.sqrt(shrieker.distSqr(player.blockPosition()));
                player.sendOverlayMessage(Component.translatable("message.mistech.echoling.shrieker", distance).withColor(0x29DFEB));
                this.playSound(SoundEvents.SCULK_CLICKING, 1.0F, 1.6F);
                server.sendParticles(ParticleTypes.SCULK_SOUL, shrieker.getX() + 0.5, shrieker.getY() + 1.0, shrieker.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
                this.warnCooldown = 200;
            }
        }
    }

    private @Nullable BlockPos nearestShrieker(ServerLevel level, BlockPos center) {
        int r = SHRIEKER_RANGE;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int cx = (center.getX() - r) >> 4; cx <= (center.getX() + r) >> 4; cx++) {
            for (int cz = (center.getZ() - r) >> 4; cz <= (center.getZ() + r) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be.getType() != BlockEntityType.SCULK_SHRIEKER) {
                        continue;
                    }
                    double d = be.getBlockPos().distSqr(center);
                    if (d <= r * r && d < bestDist) {
                        bestDist = d;
                        best = be.getBlockPos();
                    }
                }
            }
        }
        return best;
    }

    @Override
    public @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.SCULK_CLICKING;
    }

    @Override
    public float getVoicePitch() {
        return 1.5F;
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
