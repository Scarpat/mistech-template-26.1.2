package com.mistech.mistechmod.machine.arcane;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.recipe.CrushingRecipe;
import com.mistech.mistechmod.registry.MistechRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Triturador Arcano: versão avançada do Triturador Rúnico.
 * <ul>
 * <li>2 entradas trabalhando juntas, {@link #WORK_TICKS} ticks por item ({@link #FE_PER_LANE} FE/t por linha).</li>
 * <li>Faz as receitas de Tier II ({@code "tier": 2}): minérios de Mitril, Adamantina e Oricalco.</li>
 * <li>Runa da Terra embutida: receitas de minério ({@code rune_bonus}) sempre dão +1; com Runa da Terra no espaço, +1 de novo.</li>
 * </ul>
 * Espaços: 0..1 = entradas, 2 = runa, 3..8 = saídas.
 */
public class ArcaneCrusherBlockEntity extends MachineBlockEntity {
    public static final int LANES = 2;
    public static final int WORK_TICKS = 50;
    public static final int FE_PER_LANE = 30;
    public static final int RUNE_OPS_PER_USE = 16;

    private final int[] laneProgress = new int[LANES];
    private @Nullable RecipeHolder<CrushingRecipe> lastRecipe;

    public ArcaneCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneMachines.ARCANE_CRUSHER_BE.get(), pos, state, ArcaneMachines.CRUSHER_LAYOUT, 400, 0);
        this.maxProgress = WORK_TICKS * 2;
    }

    /** Qualquer receita de trituração (Tier I e II). Também usada pela Forja Trituradora. */
    public static Optional<RecipeHolder<CrushingRecipe>> recipe(ServerLevel level, ItemStack input, @Nullable RecipeHolder<CrushingRecipe> hint) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return level.recipeAccess().getRecipeFor(MistechRecipes.CRUSHING_TYPE.get(), new SingleRecipeInput(input), level, hint);
    }

    /** Quantos itens a receita dá aqui (sem a chance de bônus). */
    private int baseCount(CrushingRecipe recipe) {
        int count = recipe.result().create().getCount();
        if (recipe.runeBonus()) {
            count += 1 + (this.hasRune(Mistech.EARTH_RUNE.get()) ? 1 : 0);
        }
        return count;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean working = false;
        int shown = 0;
        for (int lane = 0; lane < LANES; lane++) {
            ItemStack input = this.items.get(lane);
            Optional<RecipeHolder<CrushingRecipe>> found = recipe(level, input, this.lastRecipe);
            if (found.isEmpty()) {
                this.laneProgress[lane] = 0;
                continue;
            }
            this.lastRecipe = found.get();
            CrushingRecipe recipe = found.get().value();
            ItemStack max = recipe.result().create();
            max.setCount(this.baseCount(recipe) + 1);
            int cost = this.energyStep(FE_PER_LANE);
            if (!this.canOutput(max) || !this.hasEnergy(cost)) {
                continue;
            }
            working = true;
            this.drainEnergy(cost);
            this.laneProgress[lane] += this.workStep();
            if (this.laneProgress[lane] >= this.maxProgress) {
                this.laneProgress[lane] = 0;
                ItemStack result = recipe.result().create();
                result.setCount(this.baseCount(recipe));
                if (recipe.bonusChance() > 0 && level.getRandom().nextFloat() < recipe.bonusChance()) {
                    result.grow(1);
                }
                if (recipe.runeBonus() && this.hasRune(Mistech.EARTH_RUNE.get())) {
                    this.countRuneOp(RUNE_OPS_PER_USE);
                }
                input.shrink(1);
                this.insertOutput(result);
                level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35F, 1.1F + level.getRandom().nextFloat() * 0.3F);
            }
            shown = Math.max(shown, this.laneProgress[lane]);
        }
        this.progress = shown;
        if (working) {
            this.setChanged();
        }
        this.setLit(level, pos, state, working);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (!super.canPlaceItemThroughFace(slot, stack, direction)) {
            return false;
        }
        if (slot < LANES && this.level instanceof ServerLevel server) {
            return recipe(server, stack, null).isPresent();
        }
        return true;
    }

    @Override
    public void addInfo(List<Component> lines) {
        for (int lane = 0; lane < LANES; lane++) {
            if (this.laneProgress[lane] > 0) {
                lines.add(Component.translatable("info.mistech.lane", lane + 1, this.laneProgress[lane] * 100 / Math.max(1, this.maxProgress)));
            }
        }
        super.addInfo(lines);
        lines.removeIf(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("info.mistech.progress"));
    }

    @Override
    protected void loadExtra(ValueInput input) {
        int[] p = input.getIntArray("Lanes").orElse(new int[0]);
        for (int i = 0; i < Math.min(p.length, LANES); i++) {
            this.laneProgress[i] = p[i];
        }
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putIntArray("Lanes", this.laneProgress.clone());
    }
}
