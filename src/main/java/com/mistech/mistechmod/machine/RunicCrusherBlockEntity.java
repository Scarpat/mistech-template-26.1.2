package com.mistech.mistechmod.machine;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.recipe.CrushingRecipe;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Triturador Rúnico: tritura 1 item a cada 5 s gastando 20 FE/tick (receitas "mistech:crushing").
 * Espaços: 0 = entrada, 1 = runa, 2..5 = saída.
 * Runa de Terra no espaço de runa: +1 produto nas receitas de minério ({@code rune_bonus}); gasta 1 uso a cada 8.
 */
public class RunicCrusherBlockEntity extends MachineBlockEntity {
    public static final int WORK_TICKS = 100;
    public static final int ENERGY_PER_TICK = 20;
    public static final int RUNE_OPS_PER_USE = 8;
    public static final int SLOT_INPUT = 0;

    private @Nullable RecipeHolder<CrushingRecipe> lastRecipe;

    public RunicCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.RUNIC_CRUSHER.get(), pos, state, MachineLayouts.CRUSHER, 200, 0);
        this.maxProgress = WORK_TICKS * 2;
    }

    private Optional<RecipeHolder<CrushingRecipe>> findRecipe(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return level.recipeAccess().getRecipeFor(MistechRecipes.CRUSHING_TYPE.get(), new SingleRecipeInput(input), level, this.lastRecipe);
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack input = this.items.get(SLOT_INPUT);
        boolean working = false;
        Optional<RecipeHolder<CrushingRecipe>> found = this.findRecipe(level, input);
        if (found.isPresent()) {
            this.lastRecipe = found.get();
            CrushingRecipe recipe = found.get().value();
            ItemStack maxResult = recipe.result().create();
            maxResult.grow(1 + (recipe.runeBonus() && this.hasRune(Mistech.EARTH_RUNE.get()) ? 1 : 0));
            int cost = this.energyStep(ENERGY_PER_TICK);
            if (this.canOutput(maxResult) && this.hasEnergy(cost)) {
                working = true;
                this.drainEnergy(cost);
                this.progress += this.workStep();
                if (this.progress >= this.maxProgress) {
                    this.progress = 0;
                    this.finish(level, pos, recipe, input);
                }
                this.setChanged();
            }
        } else if (this.progress > 0) {
            this.progress = 0;
            this.setChanged();
        }
        this.setLit(level, pos, state, working);
    }

    private void finish(ServerLevel level, BlockPos pos, CrushingRecipe recipe, ItemStack input) {
        ItemStack result = recipe.result().create();
        if (recipe.bonusChance() > 0 && level.getRandom().nextFloat() < recipe.bonusChance()) {
            result.grow(1);
        }
        if (recipe.runeBonus() && this.hasRune(Mistech.EARTH_RUNE.get())) {
            result.grow(1);
            this.countRuneOp(RUNE_OPS_PER_USE);
        }
        input.shrink(1);
        this.insertOutput(result);
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.4F, 0.8F + level.getRandom().nextFloat() * 0.4F);
    }

    /** Funis e canos só colocam itens que têm receita. */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (!super.canPlaceItemThroughFace(slot, stack, direction)) {
            return false;
        }
        if (slot == SLOT_INPUT && this.level instanceof ServerLevel server) {
            return this.findRecipe(server, stack).isPresent();
        }
        return true;
    }
}
