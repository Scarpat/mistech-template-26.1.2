package com.mistech.mistechmod.block.ritual;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.RitualContent;
import com.mistech.mistechmod.ritual.AltarStructure;
import com.mistech.mistechmod.ritual.RitualActions;
import com.mistech.mistechmod.ritual.RitualInput;
import com.mistech.mistechmod.ritual.RitualRecipe;
import com.mistech.mistechmod.ritual.RitualWards;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Altar Impuro: guarda o catalisador (1 item) e até {@link #MAX_ESSENCE} níveis de Essência Impura.
 * A Pedra Guia inicia o ritual; ele roda {@code duration} ticks conferindo o multibloco a cada 10 ticks.
 * Se algo quebrar no meio → Refluxo Impuro (perde os itens, essência derrama, Wither em volta).
 */
public class ImpureAltarBlockEntity extends SingleItemBlockEntity {
    public static final int MAX_ESSENCE = RitualRecipe.MAX_ESSENCE;
    public static final double BACKLASH_RADIUS = 6.0;

    private int essence;
    /** Essência que o ritual em andamento vai gastar (-1 = a da receita). Na Lua de Sangue é zero. */
    private int cost = -1;
    private @Nullable ResourceKey<Recipe<?>> running;
    private int progress;
    private int duration;
    private @Nullable UUID starter;
    private long wardUntil;

    /** Tier deste altar (1–3; sobe com o Ritual de Ascensão). */
    public int tier() {
        BlockState state = this.getBlockState();
        return state.getBlock() instanceof ImpureAltarBlock altar ? altar.tier() : 1;
    }

    /**
     * Ritual de Ascensão: troca o bloco pelo altar do tier seguinte no mesmo lugar, levando essência, selo e
     * o que estiver na bacia. @return o novo tier (ou o atual se já é o máximo)
     */
    public int ascend() {
        if (this.level == null || this.tier() >= 3) {
            return this.tier();
        }
        Level level = this.level;
        BlockPos pos = this.worldPosition;
        int next = this.tier() + 1;
        int keepEssence = this.essence;
        long keepWard = this.wardUntil;
        ItemStack keepStored = this.stored;
        this.stored = ItemStack.EMPTY; // não derruba nada ao trocar o bloco
        Block block = next == 2 ? RitualContent.IMPURE_ALTAR_2.get() : RitualContent.IMPURE_ALTAR_3.get();
        BlockState old = this.getBlockState();
        level.setBlock(pos, block.defaultBlockState().setValue(ImpureAltarBlock.FILLED, old.getValue(ImpureAltarBlock.FILLED)), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof ImpureAltarBlockEntity fresh) {
            fresh.essence = keepEssence;
            fresh.wardUntil = keepWard;
            fresh.stored = keepStored;
            if (keepWard > 0) {
                RitualWards.set(level, pos, keepWard);
            }
            fresh.setChanged();
            level.sendBlockUpdated(pos, fresh.getBlockState(), fresh.getBlockState(), Block.UPDATE_ALL);
        }
        return next;
    }

    private @Nullable RitualRecipe resolve(ServerLevel level) {
        if (this.running == null) {
            return null;
        }
        Optional<RecipeHolder<?>> holder = level.recipeAccess().byKey(this.running);
        return holder.isPresent() && holder.get().value() instanceof RitualRecipe recipe ? recipe : null;
    }

    public ImpureAltarBlockEntity(BlockPos pos, BlockState state) {
        super(RitualContent.IMPURE_ALTAR_BE.get(), pos, state);
    }

    public int getEssence() {
        return this.essence;
    }

    public boolean isRunning() {
        return this.running != null;
    }

    public float progressFraction() {
        return this.duration <= 0 ? 0 : this.progress / (float) this.duration;
    }

    @Override
    protected boolean isLocked() {
        return this.isRunning();
    }

    /** Muda a essência; atualiza o visual (bacia cheia). */
    public void setEssence(int essence) {
        this.essence = Math.max(0, Math.min(MAX_ESSENCE, essence));
        this.updateState();
        this.setChanged();
    }

    public void setWard(long until) {
        this.wardUntil = until;
        if (this.level != null) {
            RitualWards.set(this.level, this.worldPosition, until);
        }
        this.setChanged();
    }

    public long wardRemaining() {
        return this.level == null ? 0 : Math.max(0, this.wardUntil - this.level.getGameTime());
    }

    private void updateState() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        BlockState state = this.getBlockState();
        BlockState updated = state.setValue(ImpureAltarBlock.FILLED, this.essence > 0).setValue(ImpureAltarBlock.RUNNING, this.isRunning());
        if (updated != state) {
            this.level.setBlock(this.worldPosition, updated, Block.UPDATE_ALL);
        }
    }

    // ----- ritual -----

    private List<RitualPedestalBlockEntity> pedestals() {
        List<RitualPedestalBlockEntity> list = new ArrayList<>(4);
        if (this.level != null) {
            for (BlockPos pos : AltarStructure.pedestals(this.worldPosition, this.tier())) {
                if (this.level.getBlockEntity(pos) instanceof RitualPedestalBlockEntity pedestal) {
                    list.add(pedestal);
                }
            }
        }
        return list;
    }

    /** Itens que estão nos pedestais agora (para ações que dependem de qual item foi usado). */
    public List<ItemStack> pedestalItems() {
        List<ItemStack> items = new ArrayList<>(4);
        for (RitualPedestalBlockEntity pedestal : this.pedestals()) {
            if (!pedestal.getStored().isEmpty()) {
                items.add(pedestal.getStored().copy());
            }
        }
        return items;
    }

    /** Itens dos pedestais do último ritual concluído (válido durante {@link RitualActions#run}). */
    private List<ItemStack> lastPedestals = List.of();

    public List<ItemStack> lastPedestals() {
        return this.lastPedestals;
    }

    private RitualInput input() {
        List<ItemStack> items = new ArrayList<>(4);
        for (RitualPedestalBlockEntity pedestal : this.pedestals()) {
            items.add(pedestal.getStored());
        }
        while (items.size() < RitualRecipe.MAX_INGREDIENTS) {
            items.add(ItemStack.EMPTY);
        }
        return new RitualInput(this.stored, items);
    }

    /** Pedra Guia no altar. */
    public void tryStart(ServerLevel level, Player player) {
        if (this.isRunning()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.already_running"));
            return;
        }
        AltarStructure.Result structure = AltarStructure.check(level, this.worldPosition, this.tier(), true);
        if (!structure.ok()) {
            BlockPos bad = structure.pos();
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.structure." + structure.problem().name().toLowerCase(java.util.Locale.ROOT),
                    bad.getX(), bad.getY(), bad.getZ()).withStyle(ChatFormatting.RED));
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, bad.getX() + 0.5, bad.getY() + 0.8, bad.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.0);
            return;
        }
        if (this.stored.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.no_catalyst"));
            return;
        }
        Optional<RecipeHolder<RitualRecipe>> found = level.recipeAccess().getRecipeFor(RitualContent.RITUAL_TYPE.get(), this.input(), level);
        if (found.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.unknown").withStyle(ChatFormatting.GRAY));
            level.sendParticles(ParticleTypes.SMOKE, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.1, this.worldPosition.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
            return;
        }
        RitualRecipe recipe = found.get().value();
        if (recipe.minTier() > this.tier()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.altar_tier", recipe.minTier()).withStyle(ChatFormatting.RED));
            return;
        }
        if (!recipe.condition().test(level, this.worldPosition)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.condition",
                    Component.translatable(recipe.condition().translationKey())));
            return;
        }
        boolean bloodFed = com.mistech.mistechmod.celestial.Celestial.isActive(level, com.mistech.mistechmod.celestial.CelestialEvent.BLOOD_MOON);
        int cost = bloodFed ? 0 : recipe.essence();
        if (this.essence < cost) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.essence", recipe.essence(), this.essence));
            return;
        }
        Component error = RitualActions.check(recipe.action(), level, this.worldPosition, player);
        if (error != null) {
            player.sendOverlayMessage(error);
            return;
        }
        this.running = found.get().id();
        this.progress = 0;
        this.duration = recipe.duration();
        this.cost = cost;
        this.starter = player.getUUID();
        this.updateState();
        this.setChanged();
        player.sendOverlayMessage(bloodFed && recipe.essence() > 0
                ? Component.translatable("message.mistech.celestial.blood_fed").withStyle(ChatFormatting.DARK_RED)
                : Component.translatable("message.mistech.ritual.started"));
        level.playSound(null, this.worldPosition, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 0.6F);
    }

    /** Adaga Ritual (agachado): encerra o ritual sem refluxo. */
    public void cancel(ServerLevel level, @Nullable Player player) {
        if (!this.isRunning()) {
            return;
        }
        this.stop();
        level.playSound(null, this.worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.SMOKE, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.1, this.worldPosition.getZ() + 0.5, 15, 0.3, 0.2, 0.3, 0.01);
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.cancelled"));
        }
    }

    private int cost(RitualRecipe recipe) {
        return this.cost >= 0 ? this.cost : recipe.essence();
    }

    private void stop() {
        this.cost = -1;
        this.running = null;
        this.progress = 0;
        this.duration = 0;
        this.starter = null;
        this.updateState();
        this.setChanged();
        for (RitualPedestalBlockEntity pedestal : this.pedestals()) {
            pedestal.setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ImpureAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel server) || altar.running == null) {
            return;
        }
        RitualRecipe recipe = altar.resolve(server);
        if (recipe == null) {
            altar.stop();
            return;
        }
        altar.progress++;
        Vec3 center = Vec3.atCenterOf(pos).add(0, 0.75, 0);

        if (altar.progress % 10 == 0) {
            if (!AltarStructure.check(level, pos, altar.tier(), true).ok() || !recipe.matches(altar.input(), level) || altar.essence < altar.cost(recipe)) {
                altar.backlash(server);
                return;
            }
        }
        com.mistech.mistechmod.ritual.RitualEffect effect = recipe.effect();
        if (altar.progress % 2 == 0) {
            // fios de magia saindo dos pedestais e dos incensos para o altar (partículas do estilo do ritual)
            for (BlockPos from : AltarStructure.pedestals(pos, altar.tier())) {
                stream(server, Vec3.atCenterOf(from).add(0, 0.6, 0), center, effect.stream());
            }
            if (altar.progress % 6 == 0) {
                for (BlockPos from : AltarStructure.holders(pos, altar.tier())) {
                    stream(server, Vec3.atCenterOf(from).add(0, 0.5, 0), center, effect.holder());
                }
                if (altar.tier() >= 2) {
                    for (BlockPos from : AltarStructure.obelisks(pos)) {
                        stream(server, Vec3.atCenterOf(from).add(0, 1.2, 0), center, effect.stream());
                    }
                }
            }
        }
        effect.tick(server, pos, altar.tier(), altar.progress, altar.progressFraction());
        if (altar.progress >= altar.duration) {
            altar.finish(server, recipe);
        }
    }

    /** Uma partícula num ponto aleatório da linha entre os dois pontos, andando em direção ao altar. */
    private static void stream(ServerLevel level, Vec3 from, Vec3 to, net.minecraft.core.particles.SimpleParticleType type) {
        double t = level.getRandom().nextDouble();
        Vec3 p = from.lerp(to, t);
        Vec3 dir = to.subtract(from).normalize();
        level.sendParticles(type, p.x, p.y, p.z, 0, dir.x, dir.y + 0.05, dir.z, 0.15);
    }

    private void finish(ServerLevel level, RitualRecipe recipe) {
        Player player = this.starter == null ? null : level.getPlayerByUUID(this.starter);
        RitualInput input = this.input();
        ItemStack result = recipe.assemble(input);

        // afinidade: runas usadas no ritual
        if (player != null) {
            for (ItemStack stack : input.pedestals()) {
                RuneElement element = RuneElement.ofRune(stack);
                if (element != null) {
                    Affinity.grant(player, element, 5);
                }
            }
        }
        this.lastPedestals = this.pedestalItems();
        for (RitualPedestalBlockEntity pedestal : this.pedestals()) {
            ItemStack used = pedestal.getStored();
            net.minecraft.world.item.ItemStackTemplate remainder = used.getItem().getCraftingRemainder(used);
            pedestal.setStored(remainder == null ? ItemStack.EMPTY : remainder.create());
        }
        this.stored = ItemStack.EMPTY;
        this.essence -= this.cost(recipe);
        this.stop();
        if (!result.isEmpty()) {
            this.stored = result;
        }
        this.setChanged();

        // magia impura macula quem fez o ritual (1 por nível de essência gasto)
        if (player != null) {
            com.mistech.mistechmod.purity.Taint.add(player, recipe.essence());
        }

        int tierBefore = this.tier();
        RitualActions.run(recipe.action(), level, this.worldPosition, this, player);

        BlockPos pos = this.worldPosition;
        recipe.effect().finish(level, pos, tierBefore);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 25, 0.2, 0.4, 0.2, 0.08);
        level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 30, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, pos, SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.done").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Refluxo Impuro: o ritual foi interrompido. */
    public void backlash(ServerLevel level) {
        BlockPos pos = this.worldPosition;
        this.stored = ItemStack.EMPTY;
        for (RitualPedestalBlockEntity pedestal : this.pedestals()) {
            pedestal.setStored(ItemStack.EMPTY);
        }
        int spilled = Math.max(1, this.essence / 2);
        this.essence = 0;
        this.stop();

        // a essência derrama em volta (só em lugares vazios)
        BlockState essence = Mistech.IMPURE_ESSENCE_BLOCK.get().defaultBlockState();
        for (int i = 0; i < 12 && spilled > 0; i++) {
            BlockPos p = pos.offset(level.getRandom().nextInt(7) - 3, 0, level.getRandom().nextInt(7) - 3);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolid()) {
                level.setBlock(p, essence, Block.UPDATE_ALL);
                spilled--;
            }
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(BACKLASH_RADIUS))) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0));
            if (entity instanceof Player victim) {
                com.mistech.mistechmod.purity.Taint.add(victim, 15.0F);
            }
        }
        // a terra em volta fica maculada
        com.mistech.mistechmod.purity.block.TaintedSoilBlock.contaminateNear(level, pos.below(), 4, 4, 1);
        level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.8, 0.4, 0.8, 0.0);
        level.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40, 1.5, 0.5, 1.5, 0.05);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.8F, 0.6F);
        level.playSound(null, pos, SoundEvents.WITHER_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.2F);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(16))) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.backlash").withStyle(ChatFormatting.DARK_RED));
        }
    }

    // ----- ciclo de vida / salvar -----

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide() && this.wardUntil > 0) {
            RitualWards.set(this.level, this.worldPosition, this.wardUntil);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            RitualWards.remove(this.level, this.worldPosition);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.essence = input.getIntOr("Essence", 0);
        this.wardUntil = input.getLongOr("WardUntil", 0L);
        this.progress = input.getIntOr("Progress", 0);
        this.duration = input.getIntOr("Duration", 0);
        this.cost = input.getIntOr("Cost", -1);
        this.running = input.getString("Ritual").map(id -> ResourceKey.create(Registries.RECIPE, Identifier.parse(id))).orElse(null);
        this.starter = input.read("Starter", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Essence", this.essence);
        output.putLong("WardUntil", this.wardUntil);
        if (this.running != null) {
            output.putString("Ritual", this.running.identifier().toString());
            output.putInt("Progress", this.progress);
            output.putInt("Duration", this.duration);
            output.putInt("Cost", this.cost);
        }
        if (this.starter != null) {
            output.store("Starter", UUIDUtil.CODEC, this.starter);
        }
    }
}
