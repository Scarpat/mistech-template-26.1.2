package com.mistech.mistechmod.entity;

import java.util.EnumSet;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.GolemContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Golem de Barro: leva itens de um container (origem) para outro (destino), até {@link #CARRY} itens por viagem.
 * A rota vem da Vara de Comando. Botão direito com um item define o filtro; agachado com a mão vazia limpa.
 * Não luta. Pega itens pela face de baixo da origem (saídas das máquinas) e coloca pela face de cima do destino.
 */
public class ClayGolemEntity extends PathfinderMob implements com.mistech.mistechmod.golem.RoutedGolem {
    public static final int CARRY = 16;
    public static final int MAX_ROUTE = 24;

    private static final EntityDataAccessor<ItemStack> CARRIED = SynchedEntityData.defineId(ClayGolemEntity.class, EntityDataSerializers.ITEM_STACK);

    private @Nullable BlockPos source;
    private @Nullable BlockPos target;
    private ItemStack filter = ItemStack.EMPTY;
    /** 0 = normal, 1 = esperando (destino cheio / origem vazia), 2 = perdido (não chega). Só para partículas. */
    private int mood;

    public ClayGolemEntity(EntityType<? extends ClayGolemEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CARRIED, ItemStack.EMPTY);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TransportGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public ItemStack getCarried() {
        return this.entityData.get(CARRIED);
    }

    private void setCarried(ItemStack stack) {
        this.entityData.set(CARRIED, stack);
    }

    @Override
    public boolean hasRoute() {
        return this.source != null && this.target != null;
    }

    @Override
    public void setRoute(BlockPos source, BlockPos target) {
        this.source = source.immutable();
        this.target = target.immutable();
        this.mood = 0;
    }

    // ----- interação -----

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(GolemContent.GOLEM_WAND.get()) || held.is(GolemContent.RUNIC_GOLEM_WAND.get())) {
            return InteractionResult.PASS; // a vara cuida (interactLivingEntity)
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) {
                this.filter = ItemStack.EMPTY;
                player.sendOverlayMessage(Component.translatable("message.mistech.clay_golem.filter_cleared"));
            } else {
                player.sendOverlayMessage(this.describe());
            }
            return InteractionResult.SUCCESS;
        }
        this.filter = held.copyWithCount(1);
        player.sendOverlayMessage(Component.translatable("message.mistech.clay_golem.filter", held.getHoverName()));
        this.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.6F, 1.2F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component describe() {
        Component filterName = this.filter.isEmpty() ? Component.translatable("message.mistech.clay_golem.any") : this.filter.getHoverName();
        if (!this.hasRoute()) {
            return Component.translatable("message.mistech.clay_golem.no_route", filterName);
        }
        return Component.translatable("message.mistech.clay_golem.route", this.source.toShortString(), this.target.toShortString(), filterName);
    }

    /** Mostra a rota com partículas (Vara de Comando agachado). */
    @Override
    public void showRoute(ServerLevel level) {
        if (this.source != null) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.source.getX() + 0.5, this.source.getY() + 1.1, this.source.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.0);
        }
        if (this.target != null) {
            level.sendParticles(ParticleTypes.WAX_ON, this.target.getX() + 0.5, this.target.getY() + 1.1, this.target.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.0);
        }
    }

    private boolean accepts(ItemResource resource) {
        return this.filter.isEmpty() || resource.matches(this.filter.copyWithCount(1)) || resource.toStack().is(this.filter.getItem());
    }

    // ----- trabalho -----

    private @Nullable ResourceHandler<ItemResource> handlerAt(BlockPos pos, Direction side) {
        return this.level().getCapability(Capabilities.Item.BLOCK, pos, side);
    }

    /** Quantos desse item o destino aceitaria agora (simulação: a transação é desfeita). */
    private int destinationRoom(ItemResource resource, int amount) {
        ResourceHandler<ItemResource> target = this.target == null ? null : this.handlerAt(this.target, Direction.UP);
        if (target == null) {
            return 0;
        }
        try (Transaction sim = Transaction.openRoot()) {
            return ResourceHandlerUtil.insertStacking(target, resource, amount, sim); // sem commit = desfeito
        }
    }

    /**
     * Pega até CARRY itens da origem — só itens que passam no filtro E que o destino aceita agora
     * (ex.: da Colheitadeira para a Plantadeira leva só sementes). @return true se pegou algo.
     */
    boolean takeFromSource() {
        ResourceHandler<ItemResource> handler = this.source == null ? null : this.handlerAt(this.source, Direction.DOWN);
        if (handler == null) {
            return false;
        }
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            if (resource.isEmpty() || handler.getAmountAsInt(slot) <= 0 || !this.accepts(resource)) {
                continue;
            }
            int room = this.destinationRoom(resource, Math.min(CARRY, resource.toStack().getMaxStackSize()));
            if (room <= 0) {
                continue;
            }
            int taken;
            try (Transaction tx = Transaction.openRoot()) {
                taken = handler.extract(slot, resource, room, tx);
                if (taken > 0) {
                    tx.commit();
                }
            }
            if (taken > 0) {
                this.setCarried(resource.toStack(taken));
                this.playSound(SoundEvents.ITEM_PICKUP, 0.5F, 0.8F);
                return true;
            }
        }
        return false;
    }

    /** Coloca o que carrega num container. @return quantos entraram. */
    private int insertInto(@Nullable BlockPos pos, Direction side) {
        ItemStack carried = this.getCarried();
        ResourceHandler<ItemResource> handler = pos == null ? null : this.handlerAt(pos, side);
        if (handler == null || carried.isEmpty()) {
            return 0;
        }
        int inserted;
        try (Transaction tx = Transaction.openRoot()) {
            inserted = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(carried), carried.getCount(), tx);
            tx.commit();
        }
        if (inserted > 0) {
            ItemStack rest = carried.copy();
            rest.shrink(inserted);
            this.setCarried(rest);
            this.playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 0.5F, 1.0F);
        }
        return inserted;
    }

    /** Coloca o que carrega no destino. @return true se ficou de mãos vazias. */
    boolean depositToTarget() {
        this.insertInto(this.target, Direction.UP);
        return this.getCarried().isEmpty();
    }

    /** Devolve a carga para a origem; se não couber, larga no chão ao lado dela. */
    void returnToSource() {
        this.insertInto(this.source, Direction.UP);
        if (!this.getCarried().isEmpty() && this.level() instanceof ServerLevel server) {
            this.spawnAtLocation(server, this.getCarried());
            this.setCarried(ItemStack.EMPTY);
        }
    }

    @Override
    public void tick() {
        super.tick();
        Level level = this.level();
        if (level.isClientSide() && this.tickCount % 20 == 0) {
            if (!this.getCarried().isEmpty()) {
                level.addParticle(ParticleTypes.WAX_ON, this.getX(), this.getY() + 0.9, this.getZ(), 0.0, 0.02, 0.0);
            }
        }
        if (!level.isClientSide() && this.tickCount % 30 == 0 && level instanceof ServerLevel server) {
            if (this.mood == 1) {
                server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.1, 0.05, 0.1, 0.0);
            } else if (this.mood == 2) {
                server.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
            }
        }
    }

    // ----- salvar -----

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.source != null) {
            output.store("Source", BlockPos.CODEC, this.source);
        }
        if (this.target != null) {
            output.store("Target", BlockPos.CODEC, this.target);
        }
        if (!this.filter.isEmpty()) {
            output.store("Filter", ItemStack.OPTIONAL_CODEC, this.filter);
        }
        if (!this.getCarried().isEmpty()) {
            output.store("Carried", ItemStack.OPTIONAL_CODEC, this.getCarried());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.source = input.read("Source", BlockPos.CODEC).orElse(null);
        this.target = input.read("Target", BlockPos.CODEC).orElse(null);
        this.filter = input.read("Filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.setCarried(input.read("Carried", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (!this.getCarried().isEmpty()) {
            this.spawnAtLocation(level, this.getCarried());
            this.setCarried(ItemStack.EMPTY);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // sons de barro
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.MUD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.MUD_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.MUD_STEP, 0.3F, 1.3F);
    }

    /**
     * Anda entre origem e destino carregando itens. Perto do alvo tenta a cada 1 s (não todo tick).
     * Se o destino não recebe a carga por 10 tentativas seguidas, leva de volta para a origem e recomeça.
     */
    static class TransportGoal extends Goal {
        private static final double REACH = 3.0;
        private static final int GIVE_UP = 10;

        private final ClayGolemEntity golem;
        private int repath;
        private int stuck;
        private int cooldown;
        private int failures;
        private boolean returning;

        TransportGoal(ClayGolemEntity golem) {
            this.golem = golem;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.golem.hasRoute();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ClayGolemEntity g = this.golem;
            boolean carrying = !g.getCarried().isEmpty();
            if (!carrying) {
                this.returning = false;
            }
            BlockPos goal = carrying && !this.returning ? g.target : g.source;
            if (goal == null) {
                return;
            }
            g.getLookControl().setLookAt(goal.getX() + 0.5, goal.getY() + 0.5, goal.getZ() + 0.5);
            double dist = g.distanceToSqr(goal.getX() + 0.5, goal.getY() + 0.5, goal.getZ() + 0.5);
            if (dist <= REACH * REACH) {
                g.getNavigation().stop();
                this.stuck = 0;
                if (--this.cooldown > 0) {
                    return;
                }
                this.cooldown = 20;
                if (!carrying) {
                    g.mood = g.takeFromSource() ? 0 : 1; // nada que o destino aceite: espera soltando fumaça
                } else if (this.returning) {
                    g.returnToSource();
                    this.returning = false;
                    g.mood = 0;
                } else if (g.depositToTarget()) {
                    this.failures = 0;
                    g.mood = 0;
                } else if (++this.failures >= GIVE_UP) {
                    this.failures = 0;
                    this.returning = true; // destino não recebe: devolve na origem
                    g.mood = 1;
                } else {
                    g.mood = 1;
                }
                return;
            }
            this.cooldown = 0;
            if (--this.repath <= 0) {
                this.repath = 20;
                boolean found = g.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 1, 1.0);
                this.stuck = found ? 0 : this.stuck + 1;
                g.mood = this.stuck >= 3 ? 2 : g.mood == 2 ? 0 : g.mood;
            }
        }
    }

    // usado pela vara
    public Optional<BlockPos> source() {
        return Optional.ofNullable(this.source);
    }
}
