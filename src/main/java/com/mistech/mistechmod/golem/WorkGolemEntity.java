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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Golem de trabalho (Terracota, Ametista, Mitril, Pedra-negra, Oricalco). A rota (origem/destino) vem da Vara de Comando.
 * <ul>
 * <li>Botão direito com item: filtro (Artesão: aprende a receita do item; Zelador: põe/tira a regra "manter N", N = pilha na mão).</li>
 * <li>Agachar + botão direito com uma runa: melhoria (até 2): Ar = mais rápido, Terra = carga ×2, Espaço = alcance/teleporte,
 * Tempo = sem espera, Fogo = derrete o que carrega, Água = carrega líquidos em vez de itens (só Terracota e Mitril).</li>
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
    /** Todas as origens/destinos (Vara de Comando Rúnica); {@link #source}/{@link #target} são os escolhidos para a viagem atual. */
    private GolemRoute route = GolemRoute.EMPTY;
    private int rrSource, rrTarget;
    private @Nullable BlockPos target;
    private ItemStack filter = ItemStack.EMPTY;
    private final List<RuneElement> runes = new ArrayList<>();
    private @Nullable ResourceKey<Recipe<?>> recipe;
    private final List<ItemStack> rules = new ArrayList<>();
    int mood;
    /** Runa da Água: o "balde" do golem (1 líquido, capacidade = carga × 125 mB). */
    private final GolemTank tank = new GolemTank(this);

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
        this.tank.setCapacity(this.fluidCarry());
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

    /** Runa da Água (só Terracota e Mitril): carrega líquido de tanque em tanque. */
    boolean fluidMode() {
        return this.hasRune(RuneElement.WATER) && (this.kind() == GolemKind.SORTER || this.kind() == GolemKind.MESSENGER);
    }

    /** Terracota 4 baldes, Mitril 8 baldes; Runa da Terra dobra. */
    int fluidCarry() {
        return this.carry() * 125;
    }

    int fluidAmount() {
        return this.tank.getAmountAsInt(0);
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
        this.setRoute(new GolemRoute(List.of(source.immutable()), List.of(target.immutable()), GolemRoute.Collect.ORDER, GolemRoute.Deliver.ORDER));
    }

    @Override
    public void setRoute(GolemRoute route) {
        if (!route.isComplete()) {
            return;
        }
        this.route = route;
        this.source = route.sources().get(0);
        this.target = route.targets().get(0);
        this.rrSource = 0;
        this.rrTarget = 0;
        this.mood = 0;
    }

    @Override
    public GolemRoute getRoute() {
        if (this.route.isComplete()) {
            return this.route;
        }
        return this.source != null && this.target != null
                ? new GolemRoute(List.of(this.source), List.of(this.target), GolemRoute.Collect.ORDER, GolemRoute.Deliver.ORDER)
                : GolemRoute.EMPTY;
    }

    boolean multiSource() {
        return this.route.sources().size() > 1;
    }

    boolean multiTarget() {
        return this.route.targets().size() > 1;
    }

    // ------------------------------------------------------------------ escolha de origem/destino (várias)

    /** Quanto dá para pegar dessa origem (com o filtro). */
    private long availableAt(BlockPos pos) {
        if (this.fluidMode()) {
            ResourceHandler<FluidResource> handler = this.fluidHandlerAt(pos);
            if (handler == null) {
                for (BlockPos at : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                    var state = this.level().getFluidState(at);
                    if (state.isSource() && this.acceptsFluid(FluidResource.of(state.getType()))) {
                        return FluidType.BUCKET_VOLUME;
                    }
                }
                return 0;
            }
            long total = 0;
            for (int i = 0; i < handler.size(); i++) {
                FluidResource r = handler.getResource(i);
                if (!r.isEmpty() && this.acceptsFluid(r)) {
                    total += handler.getAmountAsLong(i);
                }
            }
            return total;
        }
        ResourceHandler<ItemResource> handler = this.handlerAt(pos, Direction.DOWN);
        if (handler == null) {
            return 0;
        }
        if (this.kind() == GolemKind.CRAFTER) {
            return 1; // o Artesão precisa ir lá para saber se a receita fecha
        }
        long total = 0;
        for (int i = 0; i < handler.size(); i++) {
            ItemResource r = handler.getResource(i);
            if (!r.isEmpty() && this.accepts(r)) {
                total += handler.getAmountAsLong(i);
            }
        }
        return total;
    }

    /** Quanto desse destino já tem do que o golem carrega; -1 se não cabe. */
    private long heldAt(BlockPos pos) {
        if (this.fluidMode()) {
            FluidResource carried = this.tank.getResource(0);
            ResourceHandler<FluidResource> handler = this.fluidHandlerAt(pos);
            if (handler == null || carried.isEmpty()) {
                return -1;
            }
            int room;
            try (Transaction sim = Transaction.openRoot()) {
                room = handler.insert(carried, this.fluidAmount(), sim);
            }
            if (room <= 0) {
                return -1;
            }
            long have = 0;
            for (int i = 0; i < handler.size(); i++) {
                if (handler.getResource(i).equals(carried)) {
                    have += handler.getAmountAsLong(i);
                }
            }
            return have;
        }
        ItemStack carried = this.getCarried();
        ResourceHandler<ItemResource> handler = this.handlerAt(pos, Direction.UP);
        if (handler == null || carried.isEmpty()) {
            return -1;
        }
        ItemResource resource = ItemResource.of(carried);
        int room;
        try (Transaction sim = Transaction.openRoot()) {
            room = ResourceHandlerUtil.insertStacking(handler, resource, 1, sim);
        }
        if (room <= 0) {
            return -1;
        }
        long have = 0;
        for (int i = 0; i < handler.size(); i++) {
            if (handler.getResource(i).equals(resource)) {
                have += handler.getAmountAsLong(i);
            }
        }
        return have;
    }

    /** Escolhe a origem da próxima viagem conforme o modo de coleta. Se nenhuma tem nada, fica onde está. */
    void chooseSource() {
        List<BlockPos> list = this.route.sources();
        int n = list.size();
        if (n <= 1) {
            return;
        }
        GolemRoute.Collect mode = this.route.collect();
        BlockPos best = null;
        long bestAmount = -1;
        for (int k = 0; k < n; k++) {
            int idx = mode == GolemRoute.Collect.ROUND_ROBIN ? (this.rrSource + k) % n : k;
            BlockPos pos = list.get(idx);
            long amount = this.availableAt(pos);
            if (amount <= 0) {
                continue;
            }
            if (mode == GolemRoute.Collect.FULLEST) {
                if (amount > bestAmount) {
                    best = pos;
                    bestAmount = amount;
                }
                continue;
            }
            best = pos;
            if (mode == GolemRoute.Collect.ROUND_ROBIN) {
                this.rrSource = (idx + 1) % n;
            }
            break;
        }
        if (best != null) {
            this.source = best;
        }
    }

    /** Escolhe o destino da carga atual conforme o modo de entrega (só os que têm espaço). */
    void chooseTarget() {
        List<BlockPos> list = this.route.targets();
        int n = list.size();
        if (n <= 1) {
            return;
        }
        GolemRoute.Deliver mode = this.route.deliver();
        BlockPos best = null;
        long bestHeld = Long.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            int idx = mode == GolemRoute.Deliver.ROUND_ROBIN ? (this.rrTarget + k) % n : k;
            BlockPos pos = list.get(idx);
            long held = this.heldAt(pos);
            if (held < 0) {
                continue;
            }
            if (mode == GolemRoute.Deliver.EQUAL) {
                if (held < bestHeld) {
                    best = pos;
                    bestHeld = held;
                }
                continue;
            }
            best = pos;
            if (mode == GolemRoute.Deliver.ROUND_ROBIN) {
                this.rrTarget = (idx + 1) % n;
            }
            break;
        }
        if (best != null) {
            this.target = best;
        }
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
        GolemRoute r = this.getRoute();
        for (BlockPos p : r.sources()) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.0);
        }
        for (BlockPos p : r.targets()) {
            level.sendParticles(ParticleTypes.WAX_ON, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.0);
        }
    }

    @Override
    public Component describe() {
        Component name = Component.translatable("entity.mistech.golem." + this.kind().id);
        String route = this.multiSource() || this.multiTarget()
                ? Component.translatable("message.mistech.golem.multi_route", this.route.sources().size(),
                        Component.translatable(this.route.collect().key()), this.route.targets().size(),
                        Component.translatable(this.route.deliver().key())).getString()
                : (this.source == null ? "-" : this.source.toShortString()) + " → " + (this.target == null ? "-" : this.target.toShortString());
        String runes = this.runes.isEmpty() ? "-" : String.join(", ", this.runes.stream()
                .map(r -> Component.translatable(r.translationKey()).getString()).toList());
        String extra = switch (this.kind()) {
            case CRAFTER -> this.recipe == null ? "-" : this.recipe.identifier().getPath();
            case KEEPER -> this.rules.isEmpty() ? "-" : String.join(", ", this.rules.stream()
                    .map(s -> s.getCount() + "× " + s.getHoverName().getString()).toList());
            default -> this.filter.isEmpty() ? Component.translatable("message.mistech.clay_golem.any").getString() : this.filter.getHoverName().getString();
        };
        if (this.fluidMode()) {
            FluidStack fluid = FluidUtil.getStack(this.tank, 0);
            extra = extra + " | " + (fluid.isEmpty() ? "0" : fluid.getAmount() + " mB " + fluid.getHoverName().getString())
                    + " / " + this.fluidCarry() + " mB";
        }
        return Component.translatable("message.mistech.golem.describe", name, route, extra, runes);
    }

    // ------------------------------------------------------------------ interação

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(GolemContent.GOLEM_WAND.get()) || held.is(GolemContent.RUNIC_GOLEM_WAND.get())) {
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
            if (rune == RuneElement.WATER && this.kind() != GolemKind.SORTER && this.kind() != GolemKind.MESSENGER) {
                player.sendOverlayMessage(Component.translatable("message.mistech.golem.water_rune_kind").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (rune == RuneElement.WATER && !this.getCarried().isEmpty()) {
                // larga os itens que estava carregando: agora ele só leva líquido
                this.spawnAtLocation(server, this.getCarried());
                this.setCarried(ItemStack.EMPTY);
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
        this.tank.setCapacity(this.fluidCarry());
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

    // ------------------------------------------------------------------ líquidos (Runa da Água)

    @Nullable ResourceHandler<FluidResource> fluidHandlerAt(@Nullable BlockPos pos) {
        return pos == null ? null : this.level().getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP);
    }

    /** Filtro: um balde (ou qualquer recipiente) com líquido = só aquele líquido; sem filtro = qualquer um. */
    boolean acceptsFluid(FluidResource resource) {
        if (this.filter.isEmpty()) {
            return true;
        }
        FluidStack wanted = FluidUtil.getFirstStackContained(this.filter);
        return wanted.isEmpty() || resource.getFluid().isSame(wanted.getFluid());
    }

    /**
     * Enche o balde do golem na origem: de um tanque (qualquer mod) ou, se não tiver tanque, de uma fonte de líquido
     * encostada no ponto (lago, poça infinita de água). @return true se pegou algo.
     */
    boolean takeFluid(ServerLevel level, @Nullable BlockPos pos) {
        int room = this.fluidCarry() - this.fluidAmount();
        if (pos == null || room <= 0) {
            return false;
        }
        ResourceHandler<FluidResource> handler = this.fluidHandlerAt(pos);
        if (handler != null) {
            int moved;
            FluidResource before = this.tank.getResource(0);
            try (Transaction tx = Transaction.openRoot()) {
                moved = ResourceHandlerUtil.move(handler, this.tank, this::acceptsFluid, room, tx);
                tx.commit();
            }
            if (moved > 0) {
                FluidResource got = before.isEmpty() ? this.tank.getResource(0) : before;
                FluidUtil.triggerSoundAndGameEvent(got, level, this.position(), null, true);
                return true;
            }
            return false;
        }
        if (room < FluidType.BUCKET_VOLUME) {
            return false;
        }
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            FluidState state = level.getFluidState(at);
            if (!state.isSource() || !this.acceptsFluid(FluidResource.of(state.getType()))) {
                continue;
            }
            FluidStack got;
            try (Transaction tx = Transaction.openRoot()) {
                got = FluidUtil.tryPickupFluid(this.tank, null, level, at.immutable(), tx);
                tx.commit();
            }
            if (!got.isEmpty()) {
                level.sendParticles(ParticleTypes.SPLASH, at.getX() + 0.5, at.getY() + 0.9, at.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.0);
                return true;
            }
        }
        return false;
    }

    /** Despeja no tanque do ponto. @return true se ficou vazio ou se despejou alguma coisa (ex.: Compressor só aceita 1 frasco). */
    boolean depositFluid(@Nullable BlockPos pos) {
        FluidResource carried = this.tank.getResource(0);
        if (carried.isEmpty()) {
            return true;
        }
        ResourceHandler<FluidResource> handler = this.fluidHandlerAt(pos);
        if (handler == null) {
            return false;
        }
        int moved;
        try (Transaction tx = Transaction.openRoot()) {
            moved = ResourceHandlerUtil.move(this.tank, handler, r -> true, this.fluidAmount(), tx);
            tx.commit();
        }
        if (moved > 0) {
            FluidUtil.triggerSoundAndGameEvent(carried, this.level(), this.position(), null, false);
        }
        return moved > 0 || this.fluidAmount() <= 0;
    }

    /** Terracota com Runa da Água: o tanque perto do destino que já tem esse líquido e cabe (senão o próprio destino). */
    @Nullable BlockPos sortFluidDestination() {
        FluidResource resource = this.tank.getResource(0);
        if (this.target == null || resource.isEmpty()) {
            return this.target;
        }
        int r = this.hasRune(RuneElement.SPACE) ? 16 : 8;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(this.target.offset(-r, -3, -r), this.target.offset(r, 3, r))) {
            if (pos.equals(this.source)) {
                continue;
            }
            ResourceHandler<FluidResource> handler = this.fluidHandlerAt(pos);
            if (handler == null || !ResourceHandlerUtil.contains(handler, resource)) {
                continue;
            }
            int room;
            try (Transaction sim = Transaction.openRoot()) {
                room = handler.insert(resource, 1, sim);
            }
            double d = pos.distSqr(this.blockPosition());
            if (room > 0 && d < bestDist) {
                best = pos.immutable();
                bestDist = d;
            }
        }
        return best != null ? best : this.target;
    }

    /** Mostra na mão do golem um balde do líquido que ele carrega. */
    void updateFluidDisplay() {
        FluidStack fluid = FluidUtil.getStack(this.tank, 0);
        if (fluid.isEmpty()) {
            if (this.fluidMode() || this.getCarried().is(Items.BUCKET) || this.getCarried().getItem() instanceof net.minecraft.world.item.BucketItem) {
                this.setCarried(ItemStack.EMPTY);
            }
            return;
        }
        Item bucket = fluid.getFluid().getBucket();
        this.setCarried(new ItemStack(bucket == null || bucket == Items.AIR ? Items.BUCKET : bucket));
    }

    /** O balde interno do golem: avisa o golem quando muda (para trocar o balde na mão). */
    static final class GolemTank extends FluidStacksResourceHandler {
        private final WorkGolemEntity golem;

        GolemTank(WorkGolemEntity golem) {
            super(1, 4000);
            this.golem = golem;
        }

        void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            if (this.golem != null) {
                this.golem.updateFluidDisplay();
            }
        }
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
        // volta a ser a estátua (as runas aplicadas se perdem)
        var statue = GolemRegistry.STATUE_ITEMS.get(this.kind());
        if (statue != null) {
            this.spawnAtLocation(level, new ItemStack(statue.get()));
        }
        FluidStack fluid = FluidUtil.getStack(this.tank, 0);
        if (!fluid.isEmpty()) {
            // o balde não cai: o líquido derrama onde o golem estava (1 bloco, se couber)
            this.setCarried(ItemStack.EMPTY);
            BlockPos at = this.blockPosition();
            if (fluid.getAmount() >= FluidType.BUCKET_VOLUME && level.getBlockState(at).canBeReplaced()) {
                level.setBlock(at, fluid.getFluid().defaultFluidState().createLegacyBlock(), 3);
            }
            this.tank.set(0, FluidResource.EMPTY, 0);
        }
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
        if (this.route.isComplete()) output.store("Route", GolemRoute.CODEC, this.route);
        if (!this.filter.isEmpty()) output.store("Filter", ItemStack.OPTIONAL_CODEC, this.filter);
        if (!this.getCarried().isEmpty() && this.fluidAmount() <= 0) output.store("Carried", ItemStack.OPTIONAL_CODEC, this.getCarried());
        this.tank.serialize(output.child("Tank"));
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
        this.route = input.read("Route", GolemRoute.CODEC).orElse(GolemRoute.EMPTY);
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
        input.child("Tank").ifPresent(this.tank::deserialize);
        if (this.fluidAmount() > 0) {
            this.updateFluidDisplay();
        }
    }

    // ------------------------------------------------------------------ trabalho

    /** Anda até os pontos e faz o trabalho do tipo. Perto do ponto, age a cada {@link #cooldown()} ticks. */
    static class WorkGoal extends Goal {
        private final WorkGolemEntity golem;
        private int repath, stuck, cooldown, failures, blinkCooldown;
        private boolean returning, filling, pickedSource, pickedTarget;
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

        /** Runa da Água: enche na origem (até lotar ou a origem secar) e despeja no destino (Terracota: no tanque com o mesmo líquido). */
        private void tickFluid(ServerLevel level) {
            WorkGolemEntity g = this.golem;
            int amount = g.fluidAmount();
            if (amount <= 0 || (this.filling && amount < g.fluidCarry() && !this.returning)) {
                this.returning = false;
                this.sortTo = null;
                if (!this.filling && !this.pickedSource) {
                    g.chooseSource();
                    this.pickedSource = true;
                }
                if (this.approach(g.source()) && this.ready()) {
                    boolean took = g.takeFluid(level, g.source());
                    this.filling = took;
                    this.pickedSource = took;
                    g.mood = took || g.fluidAmount() > 0 ? 0 : 1;
                }
                return;
            }
            this.pickedSource = false;
            this.filling = false;
            if (this.returning) {
                if (this.approach(g.source()) && this.ready()) {
                    g.depositFluid(g.source());
                    this.returning = false;
                }
                return;
            }
            if (this.sortTo == null) {
                g.chooseTarget();
                this.sortTo = g.kind() == GolemKind.SORTER && !g.multiTarget() ? g.sortFluidDestination() : g.target();
            }
            if (this.sortTo != null && this.approach(this.sortTo) && this.ready()) {
                if (g.depositFluid(this.sortTo)) {
                    this.failures = 0;
                    g.mood = 0;
                } else if (++this.failures >= 10) {
                    this.failures = 0;
                    this.returning = true;
                    g.mood = 1;
                } else {
                    this.sortTo = g.multiTarget() ? null : g.kind() == GolemKind.SORTER ? g.target() : this.sortTo;
                    g.mood = 1;
                }
            }
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
            if (g.fluidMode()) {
                this.tickFluid(level);
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
                        this.pickedTarget = false;
                        if (!this.pickedSource) {
                            g.chooseSource();
                            this.pickedSource = true;
                        }
                        if (this.approach(g.source()) && this.ready()) {
                            g.mood = g.craftFromSource(level) ? 0 : 1;
                            this.pickedSource = false;
                        }
                        return;
                    }
                    if (!this.pickedTarget) {
                        g.chooseTarget();
                        this.pickedTarget = true;
                    }
                    if (this.approach(g.target()) && this.ready()) {
                        boolean done = g.depositInto(g.target());
                        g.mood = done ? 0 : 1;
                        if (!done) {
                            this.pickedTarget = false; // tenta outro destino
                        }
                    }
                }
                default -> { // SORTER e MESSENGER: origem -> destino
                    if (!carrying) {
                        this.returning = false;
                        this.sortTo = null;
                        if (!this.pickedSource) {
                            g.chooseSource();
                            this.pickedSource = true;
                        }
                        if (this.approach(g.source()) && this.ready()) {
                            g.mood = g.takeFrom(g.source()) ? 0 : 1;
                            this.pickedSource = false;
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
                        g.chooseTarget();
                        this.sortTo = g.kind() == GolemKind.SORTER && !g.multiTarget() ? g.sortDestination(g.getCarried()) : g.target();
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
                            this.sortTo = g.multiTarget() ? null : g.kind() == GolemKind.SORTER ? g.target() : this.sortTo;
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
