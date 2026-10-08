package com.mistech.mistechmod.entity;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.RitualContent;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Coruja (familiar básico, nasce do ritual "Chamado da Coruja"). Voa seguindo o dono; botão direito = senta/segue.
 * Alimentada (coelho ou frango cru = 10 min cada, até 16):
 * <ul>
 * <li>à noite, mobs hostis a até {@link #SIGHT_RANGE} blocos aparecem com contorno <b>só para o dono</b>;</li>
 * <li>pia e avisa quando um hostil mira no dono;</li>
 * <li>perto do dono em lugar escuro dá Visão Noturna.</li>
 * </ul>
 */
public class OwlEntity extends Parrot {
    public static final int FOOD_TICKS = 10 * 60 * 20;
    public static final int MAX_FOOD_TICKS = 16 * FOOD_TICKS;
    public static final double SIGHT_RANGE = 24.0;

    private int fedTicks;
    private int alertCooldown;
    private boolean glowing;

    public OwlEntity(EntityType<? extends OwlEntity> type, Level level) {
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

    public boolean isFed() {
        return this.fedTicks > 0;
    }

    public static @Nullable UUID ownerId(TamableAnimal animal) {
        EntityReference<LivingEntity> ref = animal.getOwnerReference();
        return ref == null ? null : ref.getUUID();
    }

    public static int countOwned(ServerLevel level, UUID owner) {
        return level.getEntities(RitualContent.OWL.get(), owl -> owner.equals(ownerId(owl))).size();
    }

    public static boolean isOwlFood(ItemStack stack) {
        return stack.is(Items.RABBIT) || stack.is(Items.CHICKEN);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.COOKIE)) {
            return InteractionResult.PASS;
        }
        if (this.isTame() && this.isOwnedBy(player) && isOwlFood(held)) {
            if (!this.level().isClientSide()) {
                if (this.fedTicks + FOOD_TICKS > MAX_FOOD_TICKS) {
                    player.sendOverlayMessage(Component.translatable("message.mistech.familiar.full"));
                    return InteractionResult.FAIL;
                }
                this.fedTicks += FOOD_TICKS;
                held.consume(1, player);
                this.heal(2.0F);
                this.playSound(SoundEvents.PARROT_EAT, 1.0F, 0.6F);
                player.sendOverlayMessage(Component.translatable("message.mistech.familiar.fed", (this.fedTicks + 1199) / 1200));
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel server) || !this.isTame()) {
            return;
        }
        if (this.alertCooldown > 0) {
            this.alertCooldown--;
        }
        if (this.tickCount % 20 != 0) {
            return;
        }
        LivingEntity owner = this.getOwner();
        boolean active = owner instanceof ServerPlayer && this.isFed() && owner.distanceToSqr(this) <= SIGHT_RANGE * SIGHT_RANGE;
        if (!active) {
            if (this.glowing && owner instanceof ServerPlayer player) {
                this.clearGlow(server, player);
            }
            return;
        }
        ServerPlayer player = (ServerPlayer) owner;
        this.fedTicks -= 20;

        // Visão Noturna no escuro
        if (owner.distanceToSqr(this) <= 16 * 16 && server.getMaxLocalRawBrightness(owner.blockPosition()) < 7) {
            owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 15 * 20, 0, true, false, true));
        }

        // Olhos da Noite: contorno só para o dono
        if (server.isDarkOutside() || server.getMaxLocalRawBrightness(owner.blockPosition()) < 7) {
            for (Mob mob : this.nearbyHostiles(server, owner)) {
                byte flags = mob.getEntityData().get(DATA_SHARED_FLAGS_ID);
                player.connection.send(new ClientboundSetEntityDataPacket(mob.getId(),
                        List.of(SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID, (byte) (flags | (1 << 6))))));
            }
            this.glowing = true;
        } else if (this.glowing) {
            this.clearGlow(server, player);
        }
    }

    private List<Mob> nearbyHostiles(ServerLevel level, LivingEntity owner) {
        return level.getEntitiesOfClass(Mob.class, new AABB(owner.blockPosition()).inflate(SIGHT_RANGE),
                mob -> mob instanceof Enemy && mob.isAlive());
    }

    /** Devolve as flags verdadeiras dos mobs em volta (tira o contorno). */
    private void clearGlow(ServerLevel level, ServerPlayer player) {
        this.glowing = false;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(SIGHT_RANGE + 8),
                mob -> mob instanceof Enemy)) {
            player.connection.send(new ClientboundSetEntityDataPacket(mob.getId(),
                    List.of(SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID, mob.getEntityData().get(DATA_SHARED_FLAGS_ID)))));
        }
    }

    /** Um mob hostil mirou no dono: a coruja pia e avisa (chamado pelo evento de alvo). */
    public void alert(LivingEntity attacker) {
        if (this.alertCooldown > 0 || !this.isFed()) {
            return;
        }
        this.alertCooldown = 100;
        this.playSound(SoundEvents.PARROT_AMBIENT, 1.2F, 0.5F);
        if (this.getOwner() instanceof Player player) {
            player.sendOverlayMessage(Component.translatable("message.mistech.owl.alert", attacker.getDisplayName()));
        }
    }

    @Override
    public @Nullable SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? super.getAmbientSound() : SoundEvents.PARROT_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
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
