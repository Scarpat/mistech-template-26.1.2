package com.mistech.mistechmod.golem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.archive.ArchiveCoreBlockEntity;
import com.mistech.mistechmod.archive.ArchiveNet;
import com.mistech.mistechmod.archive.ArchiveRegistry;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.registry.GolemContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Golem de trabalho (Terracota, Ametista, Mitril, Pedra-negra, Oricalco). A rota (origem/destino) vem da Vara de Comando.
 * <ul>
 * <li>Botão direito com item: filtro (Artesão: aprende a receita do item; Zelador: põe/tira a regra "manter N", N = pilha na mão).</li>
 * <li>Agachar + botão direito com uma runa: melhoria (até 2): Ar = mais rápido, Terra = carga ×2, Espaço = alcance/teleporte,
 * Tempo = sem espera, Fogo = derrete o que carrega.</li>
 * <li>Mão vazia: mostra a configuração; agachado com mão vazia: limpa filtro/regras.</li>
 * </ul>
 */
public class WorkGolemEntity extends PathfinderMob implements RoutedGolem {
    public static final int MAX_RUNES = 2;
    public static final int MAX_RULES = 9;
    public static final double REACH = 3.0;

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(WorkGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> CARRIED = SynchedEntityData.defineId(WorkGolemEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final Identifier AIR_SPEED = Identifier.fromNamespaceAndPath(com.mistech.mistechmod.Mistech.MODID, "golem_air_rune");

    private @Nullable BlockPos source;
    private @Nullable BlockPos target;
    private ItemStack filter = ItemStack.EMPTY;
    private final List<RuneElement> runes = new ArrayList<>();
    private @Nullable ResourceKey<Recipe<?>> recipe;
    private final List<ItemStack> rules = new ArrayList<>();
    int mood;

    public WorkGolemEntity(EntityType<? extends WorkGolemEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
        builder.define(CARRIED, ItemStack.EMPTY);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WorkGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public GolemKind kind() {
        return GolemKind.byId(this.entityData.get(KIND));
    }

    public void setKind(GolemKind kind) {
        this.entityData.set(KIND, kind.ordinal());
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(kind.speed);
        }
    }

    public ItemStack getCarried() {
        return this.entityData.get(CARRIED);
    }

    void setCarried(ItemStack stack) {
        this.entityData.set(CARRIED, stack);
    }

    boolean hasRune(RuneElement element) {
        return this.runes.contains(element);
    }

    int carry() {
        return this.kind().carry * (this.hasRune(RuneElement.EARTH) ? 2 : 1);
    }

    int cooldown() {
        return this.hasRune(RuneElement.TIME) ? 5 : 20;
    }

    @Nullable BlockPos source() {
        return this.source;
    }

    @Nullable BlockPos target() {
        return this.target;
    }

    // ------------------------------------------------------------------ RoutedGolem (Vara de Comando)

    @Override
    public void setRoute(BlockPos source, BlockPos target) {
        this.source = source.immutable();
        this.target = target.immutable();
        this.mood = 0;
    }

    @Override
    public boolean hasRoute() {
        return switch (this.kind()) {
            case COLLECTOR, KEEPER -> this.target != null;
            default -> this.source != null && this.target != null;
        };
    }

    @Override
    public void showRoute(ServerLevel level) {
        if (this.source != null) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.source.getX() + 0.5, this.source.getY() + 1.1, this.source.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.0);
        }
        if (this.target != null) {
            level.sendParticles(ParticleTypes.WAX_ON, this.target.getX() + 0.5, this.target.getY() + 1.1, this.target.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.0);
        }
    }

    @Override
    public Component describe() {
        Component name = Component.translatable("entity.mistech.golem." + this.kind().id);
        String route = (this.source == null ? "-" : this.source.toShortString()) + " → " + (this.target == null ? "-" : this.target.toShortString());
        String runes = this.runes.isEmpty() ? "-" : String.join(", ", this.runes.stream()
                .map(r -> Component.translatable(r.translationKey()).getString()).toList());
        String extra = switch (this.kind()) {
            case CRAFTER -> this.recipe == null ? "-" : this.recipe.identifier().getPath();
            case KEEPER -> this.rules.isEmpty() ? "-" : String.join(", ", this.rules.stream()
                    .map(s -> s.getCount() + "× " + s.getHoverName().getString()).toList());
            default -> this.filter.isEmpty() ? Component.translatable("message.mistech.clay_golem.any").getString() : this.filter.getHoverName().getString();
        };
        return Component.translatable("message.mistech.golem.describe", name, route, extra, runes);
    }

    // ------------------------------------------------------------------ interação

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(GolemContent.GOLEM_WAND.get())) {
            return InteractionResult.PASS;
        }
        if (!(this.level() instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        RuneElement rune = RuneElement.ofRune(held);
        if (rune != null && player.isShiftKeyDown()) {
            if (this.runes.contains(rune) || this.runes.size() >= MAX_RUNES) {
                player.sendOverlayMessage(Component.translatable("message.mistech.golem.rune_full", MAX_RUNES).withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            this.runes.add(rune);
            this.applyRunes();
            if (!player.hasInfiniteMaterials()) {
                held.shrink(1);
            }
            server.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.4, 0.3, 0.3);
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
            player.sendOverlayMessage(Component.translatable("message.mistech.golem.rune_added", Component.translatable(rune.translationKey()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty()) {
            if (player.isShiftKeyDown()) {
                this.filter = ItemStack.EMPTY;
                this.rules.clear();
                this.recipe = null;
                player.sendOverlayMessage(Component.translatable("message.mistech.clay_golem.filter_cleared"));
            } else {
                player.sendOverlayMessage(this.describe());
            }
            return InteractionResult.SUCCESS;
        }
        switch (this.kind()) {
            case CRAFTER -> {
                this.recipe = GolemCrafting.find(server, held);
                player.sendOverlayMessage(this.recipe == null
                        ? Component.translatable("message.mistech.golem.no_recipe", held.getHoverName()).withStyle(ChatFormatting.RED)
                        : Component.translatable("message.mistech.golem.recipe", held.getHoverName()).withStyle(ChatFormatting.GOLD));
            }
            case KEEPER -> {
                boolean removed = this.rules.removeIf(r -> r.is(held.getItem()));
                if (!removed) {
                    if (this.rules.size() >= MAX_RULES) {
                        player.sendOverlayMessage(Component.translatable("message.mistech.golem.rules_full", MAX_RULES).withStyle(ChatFormatting.RED));
                        return InteractionResult.FAIL;
                    }
                    this.rules.add(held.copy());
                    player.sendOverlayMessage(Component.translatable("message.mistech.golem.rule_added", held.getCount(), held.getHoverName())
                            .withStyle(ChatFormatting.GOLD));
                } else {
                    player.sendOverlayMessage(Component.translatable("message.mistech.golem.rule_removed", held.getHoverName()));
                }
            }
            default -> {
                this.filter = held.copyWithCount(1);
                player.sendOverlayMessage(Component.translatable("message.mistech.clay_golem.filter", held.getHoverName()));
            }
        }
        this.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.6F, 1.2F);
        return InteractionResult.SUCCESS;
    }

    private void applyRunes() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(AIR_SPEED);
            if (this.hasRune(RuneElement.AIR)) {
                speed.addPermanentModifier(new AttributeModifier(AIR_SPEED, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    boolean accepts(ItemResource resource) {
        return this.filter.isEmpty() || resource.is(this.filter.getItem());
    }

    // ------------------------------------------------------------------ recipientes

    @Nullable ResourceHandler<ItemResource> handlerAt(@Nullable BlockPos pos, @Nullable Direction side) {
        return pos == null ? null : this.level().getCapability(Capabilities.Item.BLOCK, pos, side);
    }

    /** Coloca a carga (derretida antes, com Runa de Fogo). @return true se ficou de mãos vazias. */
    boolean depositInto(@Nullable BlockPos pos) {
        ItemStack carried = this.getCarried();
        ResourceHandler<ItemResource> handler = this.handlerAt(pos, Direction.UP);
        if (handler == null || carried.isEmpty()) {
            return carried.isEmpty();
        }
        if (this.hasRune(RuneElement.FIRE) && this.level() instanceof ServerLevel server) {
            var smelt = server.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(carried), server);
            if (smelt.isPresent()) {
                ItemStack out = smelt.get().value().assemble(new SingleRecipeInput(carried));
                if (!out.isEmpty()) {
                    carried = out.copyWithCount(out.getCount() * carried.getCount());
                    server.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }
        int inserted;
        try (Transaction tx = Transaction.openRoot()) {
            inserted = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(carried), carried.getCount(), tx);
            tx.commit();
        }
        ItemStack rest = carried.copy();
        rest.shrink(inserted);
        this.setCarried(rest);
        if (inserted > 0) {
            this.playSound(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 0.5F, 1.0F);
        }
        return rest.isEmpty();
    }

    /** Pega até {@code carry()} da origem (que passe no filtro). */
    boolean takeFrom(@Nullable BlockPos pos) {
        ResourceHandler<ItemResource> handler = this.handlerAt(pos, Direction.DOWN);
        if (handler == null) {
            return false;
        }
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource r = handler.getResource(slot);
            if (r.isEmpty() || handler.getAmountAsLong(slot) <= 0 || !this.accepts(r)) {
                continue;
            }
            int taken;
            try (Transaction tx = Transaction.openRoot()) {
                taken = handler.extract(slot, r, Math.min(this.carry(), r.getMaxStackSize()), tx);
                tx.commit();
            }
            if (taken > 0) {
                this.setCarried(r.toStack(taken));
                this.playSound(SoundEvents.ITEM_PICKUP, 0.5F, 0.8F);
                return true;
            }
        }
        return false;
    }

    /** Terracota: o baú perto do destino que já tem esse item e cabe (senão o próprio destino). */
    @Nullable BlockPos sortDestination(ItemStack stack) {
        if (this.target == null) {
            return null;
        }
        int r = this.hasRune(RuneElement.SPACE) ? 16 : 8;
        ItemResource resource = ItemResource.of(stack);
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(this.target.offset(-r, -3, -r), this.target.offset(r, 3, r))) {
            if (pos.equals(this.source)) {
                continue;
            }
            ResourceHandler<ItemResource> handler = this.handlerAt(pos, Direction.UP);
            if (handler == null || !ResourceHandlerUtil.contains(handler, resource)) {
                continue;
            }
            int room;
            try (Transaction sim = Transaction.openRoot()) {
                room = ResourceHandlerUtil.insertStacking(handler, resource, 1, sim);
            }
            double d = pos.distSqr(this.blockPosition());
            if (room > 0 && d < bestDist) {
                best = pos.immutable();
                bestDist = d;
            }
        }
        return best != null ? best : this.target;
    }

    /** Ametista: item no chão perto do destino. */
    @Nullable ItemEntity findLoot() {
        if (this.target == null) {
            return null;
        }
        double r = this.hasRune(RuneElement.SPACE) ? 16 : 8;
        return this.level().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(this.target).inflate(r),
                e -> e.isAlive() && !e.hasPickUpDelay() && this.accepts(ItemResource.of(e.getItem())))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    /** Artesão: faz a receita uma vez com o que tem na origem. */
    boolean craftFromSource(ServerLevel level) {
        CraftingRecipe r = GolemCrafting.get(level, this.recipe);
        ResourceHandler<ItemResource> handler = this.handlerAt(this.source, Direction.DOWN);
        if (r == null || handler == null) {
            return false;
        }
        List<ItemStack> out = GolemCrafting.craftOnce(level, r, handler);
        if (out.isEmpty()) {
            return false;
        }
        this.setCarried(out.get(0));
        for (int i = 1; i < out.size(); i++) { // sobras (balde vazio...) voltam para a origem
            ItemStack rest = out.get(i);
            try (Transaction tx = Transaction.openRoot()) {
                int put = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(rest), rest.getCount(), tx);
                tx.commit();
                if (put < rest.getCount()) {
                    this.spawnAtLocation(level, rest.copyWithCount(rest.getCount() - put));
                }
            }
        }
        level.sendParticles(ParticleTypes.WAX_OFF, this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
        this.playSound(SoundEvents.VILLAGER_WORK_TOOLSMITH, 0.6F, 1.2F);
        return true;
    }

    /** Oricalco: completa o estoque do destino a partir do Arquivo. @return true se mexeu em algo. */
    boolean restock(ServerLevel level) {
        if (this.target == null || this.rules.isEmpty()) {
            return false;
        }
        ResourceHandler<ItemResource> handler = this.handlerAt(this.target, Direction.UP);
        ArchiveCoreBlockEntity core = ArchiveRegistry.coreFor(level, this.target);
        if (handler == null || core == null || !core.isPowered()) {
            this.mood = 1;
            return false;
        }
        ArchiveNet net = ArchiveNet.of(level, core);
        boolean moved = false;
        for (ItemStack rule : this.rules) {
            ItemResource r = ItemResource.of(rule);
            long have = 0;
            for (int i = 0; i < handler.size(); i++) {
                if (handler.getResource(i).equals(r)) {
                    have += handler.getAmountAsLong(i);
                }
            }
            int want = (int) Math.min(this.carry(), rule.getCount() - have);
            if (want <= 0) {
                continue;
            }
            ItemStack got = net.extractStack(r, want);
            if (got.isEmpty()) {
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int put = ResourceHandlerUtil.insertStacking(handler, r, got.getCount(), tx);
                tx.commit();
                if (put < got.getCount()) {
                    net.insert(r, got.getCount() - put, false);
                }
                moved |= put > 0;
            }
        }
        if (moved) {
            level.sendParticles(ParticleTypes.PORTAL, this.target.getX() + 0.5, this.target.getY() + 1.0, this.target.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.2);
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.5F);
        }
        this.mood = 0;
        return moved;
    }

    /** Mensageiro com Runa do Espaço: aparece do lado do ponto. */
    boolean blinkTo(BlockPos pos) {
        if (!(this.level() instanceof ServerLevel server)) {
            return false;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos at = pos.relative(d);
            if (server.getBlockState(at).isAir() && server.getBlockState(at.above()).isAir() && !server.getBlockState(at.below()).isAir()) {
                Vec3 from = this.position();
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 0.5, from.z, 16, 0.2, 0.4, 0.2, 0.05);
                this.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                server.sendParticles(ParticleTypes.PORTAL, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 16, 0.2, 0.4, 0.2, 0.3);
                this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.4F, 1.6F);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ vida

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel server && this.tickCount % 30 == 0) {
            if (this.mood == 1) {
                server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.1, 0.05, 0.1, 0.0);
            } else if (this.mood == 2) {
                server.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
            }
        }
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

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STONE_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.STONE_STEP, 0.3F, 1.3F);
    }

    // ------------------------------------------------------------------ salvar

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Kind", this.kind().ordinal());
        if (this.source != null) output.store("Source", BlockPos.CODEC, this.source);
        if (this.target != null) output.store("Target", BlockPos.CODEC, this.target);
        if (!this.filter.isEmpty()) output.store("Filter", ItemStack.OPTIONAL_CODEC, this.filter);
        if (!this.getCarried().isEmpty()) output.store("Carried", ItemStack.OPTIONAL_CODEC, this.getCarried());
        output.store("Runes", com.mojang.serialization.Codec.STRING.listOf(), this.runes.stream().map(RuneElement::id).toList());
        if (this.recipe != null) output.store("Recipe", ResourceKey.codec(Registries.RECIPE), this.recipe);
        output.store("Rules", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(this.rules));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setKind(GolemKind.byId(input.getIntOr("Kind", 0)));
        this.source = input.read("Source", BlockPos.CODEC).orElse(null);
        this.target = input.read("Target", BlockPos.CODEC).orElse(null);
        this.filter = input.read("Filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.setCarried(input.read("Carried", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.runes.clear();
        for (String id : input.read("Runes", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of())) {
            RuneElement e = RuneElement.byId(id);
            if (e != null) this.runes.add(e);
        }
        this.applyRunes();
        this.recipe = input.read("Recipe", ResourceKey.codec(Registries.RECIPE)).orElse(null);
        this.rules.clear();
        this.rules.addAll(input.read("Rules", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()).stream().filter(s -> !s.isEmpty()).toList());
    }

    // ------------------------------------------------------------------ trabalho

    /** Anda até os pontos e faz o trabalho do tipo. Perto do ponto, age a cada {@link #cooldown()} ticks. */
    static class WorkGoal extends Goal {
        private final WorkGolemEntity golem;
        private int repath, stuck, cooldown, failures, blinkCooldown;
        private boolean returning;
        private @Nullable BlockPos sortTo;

        WorkGoal(WorkGolemEntity golem) {
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

        /** Vai até {@code pos}. @return true quando está perto. */
        private boolean approach(BlockPos pos) {
            WorkGolemEntity g = this.golem;
            g.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            double dist = g.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist <= REACH * REACH) {
                g.getNavigation().stop();
                this.stuck = 0;
                return true;
            }
            if (g.kind() == GolemKind.MESSENGER && g.hasRune(RuneElement.SPACE) && --this.blinkCooldown <= 0) {
                this.blinkCooldown = 40;
                if (g.blinkTo(pos)) {
                    return false;
                }
            }
            this.cooldown = 0;
            if (--this.repath <= 0) {
                this.repath = 20;
                boolean found = g.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1, 1.0);
                this.stuck = found ? 0 : this.stuck + 1;
                g.mood = this.stuck >= 3 ? 2 : g.mood == 2 ? 0 : g.mood;
            }
            return false;
        }

        private boolean ready() {
            if (--this.cooldown > 0) {
                return false;
            }
            this.cooldown = this.golem.cooldown();
            return true;
        }

        @Override
        public void tick() {
            WorkGolemEntity g = this.golem;
            if (!(g.level() instanceof ServerLevel level)) {
                return;
            }
            boolean carrying = !g.getCarried().isEmpty();
            switch (g.kind()) {
                case COLLECTOR -> {
                    if (carrying && (g.getCarried().getCount() >= g.carry() || g.findLoot() == null)) {
                        if (this.approach(g.target()) && this.ready()) {
                            g.mood = g.depositInto(g.target()) ? 0 : 1;
                        }
                        return;
                    }
                    ItemEntity loot = g.findLoot();
                    if (loot == null) {
                        if (this.approach(g.target())) {
                            g.mood = 0;
                        }
                        return;
                    }
                    if (this.approach(loot.blockPosition()) || g.distanceToSqr(loot) < 2.25) {
                        ItemStack have = g.getCarried();
                        ItemStack stack = loot.getItem();
                        if (have.isEmpty() || ItemStack.isSameItemSameComponents(have, stack)) {
                            int room = Math.min(g.carry(), stack.getMaxStackSize()) - have.getCount();
                            int take = Math.min(room, stack.getCount());
                            if (take > 0) {
                                g.setCarried(stack.copyWithCount(have.getCount() + take));
                                stack.shrink(take);
                                loot.setItem(stack);
                                if (stack.isEmpty()) {
                                    loot.discard();
                                }
                                g.playSound(SoundEvents.ITEM_PICKUP, 0.5F, 1.2F);
                            }
                        }
                    }
                }
                case KEEPER -> {
                    if (this.approach(g.target()) && this.ready()) {
                        g.restock(level);
                    }
                }
                case CRAFTER -> {
                    if (!carrying) {
                        if (this.approach(g.source()) && this.ready()) {
                            g.mood = g.craftFromSource(level) ? 0 : 1;
                        }
                    } else if (this.approach(g.target()) && this.ready()) {
                        g.mood = g.depositInto(g.target()) ? 0 : 1;
                    }
                }
                default -> { // SORTER e MESSENGER: origem -> destino
                    if (!carrying) {
                        this.returning = false;
                        this.sortTo = null;
                        if (this.approach(g.source()) && this.ready()) {
                            g.mood = g.takeFrom(g.source()) ? 0 : 1;
                        }
                        return;
                    }
                    if (this.returning) {
                        if (this.approach(g.source()) && this.ready()) {
                            g.depositInto(g.source());
                            if (!g.getCarried().isEmpty()) {
                                g.spawnAtLocation(level, g.getCarried());
                                g.setCarried(ItemStack.EMPTY);
                            }
                            this.returning = false;
                        }
                        return;
                    }
                    if (this.sortTo == null) {
                        this.sortTo = g.kind() == GolemKind.SORTER ? g.sortDestination(g.getCarried()) : g.target();
                    }
                    if (this.sortTo != null && this.approach(this.sortTo) && this.ready()) {
                        if (g.depositInto(this.sortTo)) {
                            this.failures = 0;
                            g.mood = 0;
                        } else if (++this.failures >= 10) {
                            this.failures = 0;
                            this.returning = true;
                            g.mood = 1;
                        } else {
                            this.sortTo = g.kind() == GolemKind.SORTER ? g.target() : this.sortTo;
                            g.mood = 1;
                        }
                    }
                }
            }
        }
    }

    public Optional<BlockPos> sourceOpt() {
        return Optional.ofNullable(this.source);
    }
}
