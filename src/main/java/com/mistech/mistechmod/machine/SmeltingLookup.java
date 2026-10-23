package com.mistech.mistechmod.machine;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/**
 * Receita de forno para as fornalhas do Mistech: primeiro a da fornalha comum, senão a do alto-forno
 * (minérios de adamantina e oricalco só têm receita de alto-forno).
 */
public final class SmeltingLookup {
    private SmeltingLookup() {}

    /** Resultado, tempo (ticks na máquina de origem) e experiência. */
    public record Result(ItemStack output, int cookingTime, float experience, boolean blasting) {}

    public static Optional<Result> find(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput in = new SingleRecipeInput(input);
        var smelting = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, in, level);
        if (smelting.isPresent()) {
            return Optional.of(of(smelting.get().value(), in, false));
        }
        return level.recipeAccess().getRecipeFor(RecipeType.BLASTING, in, level).map(r -> of(r.value(), in, true));
    }

    private static Result of(AbstractCookingRecipe recipe, SingleRecipeInput in, boolean blasting) {
        return new Result(recipe.assemble(in), recipe.cookingTime(), recipe.experience(), blasting);
    }

    public static boolean canSmelt(ServerLevel level, ItemStack input) {
        return find(level, input).isPresent();
    }
}
