package com.mistech.mistechmod.purity.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.ritual.RitualPedestalBlockEntity;
import com.mistech.mistechmod.block.ritual.SingleItemBlockEntity;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.SanctuaryStructure;
import com.mistech.mistechmod.purity.Taint;
import com.mistech.mistechmod.registry.RitualContent;
import com.mistech.mistechmod.ritual.RitualInput;
import com.mistech.mistechmod.ritual.RitualRecipe;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
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
 * Fonte do Santuário: o "altar" da magia pura. Guarda o catalisador e até {@link #MAX_ESSENCE} níveis de Essência Purificada.
 * Usa as mesmas receitas "mistech:ritual" com {@code "sanctuary": true}. Diferente do Altar Impuro, um ritual interrompido
 * não tem refluxo: só se desfaz (os itens ficam onde estão). Quem ativa precisa estar Puro (Mácula &lt; 20),
 * exceto na Purificação da Alma, que serve justamente para limpar quem não está.
 */
public class SanctuaryFontBlockEntity extends SingleItemBlockEntity {
    public static final int MAX_ESSENCE = RitualRecipe.MAX_ESSENCE;
    /** Raio em volta da Fonte onde a Purificação da Alma alcança os jogadores. */
    public static final double SOUL_RADIUS = 3.5;
    public static final int CONSECRATE_RADIUS = 16;

    private int essence;
    private @Nullable ResourceKey<Recipe<?>> running;
    private int progress;
    private int duration;
    private @Nullable UUID starter;

    public SanctuaryFontBlockEntity(BlockPos pos, BlockState state) {
        super(PurityContent.SANCTUARY_FONT_BE.get(), pos, state);
    }

    public int getEssence() {
        return this.essence;
    }

    public boolean isRunning() {
        return this.running != null;
    }

    @Override
    protected boolean isLocked() {
        return this.isRunning();
    }

    public void setEssence(int essence) {
        this.essence = Math.max(0, Math.min(MAX_ESSENCE, essence));
        this.updateState();
        this.setChanged();
    }

    private void updateState() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        BlockState state = this.getBlockState();
        BlockState updated = state.setValue(SanctuaryFontBlock.FILLED, this.essence > 0).setValue(SanctuaryFontBlock.RUNNING, this.isRunning());
        if (updated != state) {
            this.level.setBlock(this.worldPosition, updated, Block.UPDATE_ALL);
        }
    }

    private List<RitualPedestalBlockEntity> pedestals() {
        List<RitualPedestalBlockEntity> list = new ArrayList<>(4);
        if (this.level != null) {
            for (BlockPos pos : SanctuaryStructure.pedestals(this.worldPosition)) {
                if (this.level.getBlockEntity(pos) instanceof RitualPedestalBlockEntity pedestal) {
                    list.add(pedestal);
                }
            }
        }
        return list;
    }

    private RitualInput input() {
        List<ItemStack> items = new ArrayList<>(RitualRecipe.MAX_INGREDIENTS);
        for (RitualPedestalBlockEntity pedestal : this.pedestals()) {
            items.add(pedestal.getStored());
        }
        while (items.size() < RitualRecipe.MAX_INGREDIENTS) {
            items.add(ItemStack.EMPTY);
        }
        return new RitualInput(this.stored, items, true);
    }

    private @Nullable RitualRecipe resolve(ServerLevel level) {
        if (this.running == null) {
            return null;
        }
        Optional<RecipeHolder<?>> holder = level.recipeAccess().byKey(this.running);
        return holder.isPresent() && holder.get().value() instanceof RitualRecipe recipe ? recipe : null;
    }

    private static Component problem(SanctuaryStructure.Result result) {
        BlockPos bad = result.pos();
        return Component.translatable("message.mistech.sanctuary.structure." + result.problem().name().toLowerCase(java.util.Locale.ROOT),
                bad.getX(), bad.getY(), bad.getZ()).withStyle(ChatFormatting.RED);
    }

    /** Pedra Guia na Fonte. */
    public void tryStart(ServerLevel level, Player player) {
        if (this.isRunning()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.already_running"));
            return;
        }
        SanctuaryStructure.Result structure = SanctuaryStructure.check(level, this.worldPosition);
        if (!structure.ok()) {
            BlockPos bad = structure.pos();
            player.sendOverlayMessage(problem(structure));
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
            level.sendParticles(ParticleTypes.CLOUD, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.1, this.worldPosition.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
            return;
        }
        RitualRecipe recipe = found.get().value();
        if (!recipe.action().equals("purify_soul") && Taint.get(player) >= Taint.EARLY_FLOOR) {
            player.sendOverlayMessage(Component.translatable("message.mistech.sanctuary.tainted", Taint.describe(Taint.get(player)))
                    .withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
        if (!recipe.condition().test(level, this.worldPosition)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.condition",
                    Component.translatable(recipe.condition().translationKey())));
            return;
        }
        if (this.essence < recipe.essence()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.sanctuary.essence", recipe.essence(), this.essence));
            return;
        }
        Component error = com.mistech.mistechmod.celestial.CelestialRituals.check(recipe.action(), level, this.worldPosition, player, this.stored);
        if (error != null) {
            player.sendOverlayMessage(error);
            return;
        }
        this.running = found.get().id();
        this.progress = 0;
        this.duration = recipe.duration();
        this.starter = player.getUUID();
        this.updateState();
        this.setChanged();
        player.sendOverlayMessage(Component.translatable("message.mistech.sanctuary.started").withStyle(ChatFormatting.YELLOW));
        level.playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.3F);
        level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.5F);
    }

    private void stop() {
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

    /** O círculo quebrou ou a pureza se perdeu: o ritual se desfaz, sem refluxo (nada é gasto). */
    private void fizzle(ServerLevel level, SanctuaryStructure.@Nullable Result why) {
        this.stop();
        BlockPos pos = this.worldPosition;
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.5, 0.3, 0.5, 0.02);
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(16))) {
            player.sendOverlayMessage(why != null && !why.ok() ? problem(why)
                    : Component.translatable("message.mistech.sanctuary.fizzle").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SanctuaryFontBlockEntity font) {
        if (!(level instanceof ServerLevel server) || font.running == null) {
            return;
        }
        RitualRecipe recipe = font.resolve(server);
        if (recipe == null) {
            font.stop();
            return;
        }
        font.progress++;
        if (font.progress % 10 == 0) {
            SanctuaryStructure.Result shape = SanctuaryStructure.checkShape(level, pos);
            if (!shape.ok()) {
                font.fizzle(server, shape);
                return;
            }
            if (!recipe.matches(font.input(), level) || font.essence < recipe.essence()) {
                font.fizzle(server, null);
                return;
            }
        }
        if (font.progress % 100 == 0) {
            SanctuaryStructure.Result purity = SanctuaryStructure.checkPurity(level, pos);
            if (!purity.ok()) {
                font.fizzle(server, purity);
                return;
            }
        }
        Vec3 center = Vec3.atCenterOf(pos).add(0, 0.8, 0);
        if (font.progress % 2 == 0) {
            for (BlockPos from : SanctuaryStructure.pedestals(pos)) {
                stream(server, Vec3.atCenterOf(from).add(0, 0.6, 0), center);
            }
            if (font.progress % 6 == 0) {
                for (BlockPos from : SanctuaryStructure.obelisks(pos)) {
                    stream(server, Vec3.atCenterOf(from).add(0, 1.2, 0), center);
                }
            }
        }
        recipe.effect().tick(server, pos, 1, font.progress, font.duration <= 0 ? 0 : font.progress / (float) font.duration);
        if (font.progress >= font.duration) {
            font.finish(server, recipe);
        }
    }

    private static void stream(ServerLevel level, Vec3 from, Vec3 to) {
        double t = level.getRandom().nextDouble();
        Vec3 p = from.lerp(to, t);
        Vec3 dir = to.subtract(from).normalize();
        level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, dir.x, dir.y + 0.05, dir.z, 0.12);
    }

    private void finish(ServerLevel level, RitualRecipe recipe) {
        Player player = this.starter == null ? null : level.getPlayerByUUID(this.starter);
        ItemStack result = recipe.assemble(this.input());
        for (RitualPedestalBlockEntity pedestal : this.pedestals()) {
            ItemStack used = pedestal.getStored();
            net.minecraft.world.item.ItemStackTemplate remainder = used.getItem().getCraftingRemainder(used);
            pedestal.setStored(remainder == null ? ItemStack.EMPTY : remainder.create());
        }
        this.stored = ItemStack.EMPTY;
        this.essence -= recipe.essence();
        this.stop();
        if (!result.isEmpty()) {
            this.stored = result;
        }
        this.setChanged();
        this.runAction(level, recipe.action(), player);

        BlockPos pos = this.worldPosition;
        recipe.effect().finish(level, pos, 1);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 40, 0.3, 0.6, 0.3, 0.08);
        level.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.4F);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.6F);
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.sanctuary.done").withStyle(ChatFormatting.GOLD));
        }
    }

    private void runAction(ServerLevel level, String action, @Nullable Player starter) {
        BlockPos pos = this.worldPosition;
        switch (action) {
            case "purify_soul" -> {
                List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(SOUL_RADIUS, 2.0, SOUL_RADIUS));
                if (starter != null && !players.contains(starter)) {
                    players.add(starter);
                }
                for (Player p : players) {
                    Taint.set(p, 0.0F);
                    level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.3, 0.8, 0.3, 0.05);
                    p.sendSystemMessage(Component.translatable("message.mistech.sanctuary.soul_purified").withStyle(ChatFormatting.GOLD));
                }
            }
            case "consecrate_soil" -> {
                int cleaned = 0;
                int r = CONSECRATE_RADIUS;
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -6, -r), pos.offset(r, 6, r))) {
                    if (level.getBlockState(p).is(PurityContent.TAINTED_SOIL.get())) {
                        TaintedSoilBlock.cleanse(level, p.immutable());
                        cleaned++;
                    }
                }
                if (starter != null) {
                    starter.sendSystemMessage(Component.translatable("message.mistech.sanctuary.consecrated", cleaned).withStyle(ChatFormatting.GOLD));
                }
            }
            default -> com.mistech.mistechmod.celestial.CelestialRituals.run(action, level, pos, this, starter);
        }
    }

    // ----- salvar -----

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.essence = input.getIntOr("Essence", 0);
        this.progress = input.getIntOr("Progress", 0);
        this.duration = input.getIntOr("Duration", 0);
        this.running = input.getString("Ritual").map(id -> ResourceKey.create(Registries.RECIPE, Identifier.parse(id))).orElse(null);
        this.starter = input.read("Starter", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Essence", this.essence);
        if (this.running != null) {
            output.putString("Ritual", this.running.identifier().toString());
            output.putInt("Progress", this.progress);
            output.putInt("Duration", this.duration);
        }
        if (this.starter != null) {
            output.store("Starter", UUIDUtil.CODEC, this.starter);
        }
    }
}
