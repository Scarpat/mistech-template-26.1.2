package com.mistech.mistechmod.golem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Receitas do Golem Artesão: achar a receita de um item e fazer com o que tem num recipiente. */
public final class GolemCrafting {
    private GolemCrafting() {}

    /** Primeira receita de mesa de trabalho cujo resultado é {@code wanted}. */
    public static @Nullable ResourceKey<Recipe<?>> find(ServerLevel level, ItemStack wanted) {
        for (RecipeHolder<CraftingRecipe> holder : level.recipeAccess().recipeMap().byType(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            if (recipe.isSpecial()) {
                continue;
            }
            CraftingInput input = sampleInput(recipe);
            if (input == null) {
                continue;
            }
            ItemStack out = recipe.assemble(input);
            if (!out.isEmpty() && out.is(wanted.getItem())) {
                return holder.id();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static @Nullable CraftingRecipe get(ServerLevel level, @Nullable ResourceKey<Recipe<?>> key) {
        if (key == null) {
            return null;
        }
        Optional<RecipeHolder<?>> holder = level.recipeAccess().byKey(key);
        return holder.isPresent() && holder.get().value() instanceof CraftingRecipe recipe ? recipe : null;
    }

    /** Uma entrada qualquer que a receita aceita (para descobrir o resultado). */
    private static @Nullable CraftingInput sampleInput(CraftingRecipe recipe) {
        List<Optional<Ingredient>> slots = slots(recipe);
        if (slots == null) {
            return null;
        }
        List<ItemStack> items = new ArrayList<>();
        for (Optional<Ingredient> slot : slots) {
            if (slot.isEmpty()) {
                items.add(ItemStack.EMPTY);
                continue;
            }
            var any = slot.get().items().findFirst();
            if (any.isEmpty()) {
                return null;
            }
            items.add(new ItemStack(any.get()));
        }
        return input(recipe, items);
    }

    /** Ingredientes por posição (receita com forma: largura × altura; sem forma: em sequência). */
    private static @Nullable List<Optional<Ingredient>> slots(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getIngredients();
        }
        List<Optional<Ingredient>> list = new ArrayList<>();
        for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
            list.add(Optional.of(ingredient));
        }
        return list.isEmpty() || list.size() > 9 ? null : list;
    }

    private static CraftingInput input(CraftingRecipe recipe, List<ItemStack> items) {
        if (recipe instanceof ShapedRecipe shaped) {
            return CraftingInput.of(shaped.getWidth(), shaped.getHeight(), items);
        }
        int n = items.size();
        int w = Math.min(3, n), h = (n + 2) / 3;
        List<ItemStack> padded = new ArrayList<>(items);
        while (padded.size() < w * h) {
            padded.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(w, h, padded);
    }

    /**
     * Tira os ingredientes de {@code handler} e faz a receita uma vez.
     * @return resultado + sobras (balde vazio etc.); vazio se faltar algo (nada é tirado nesse caso)
     */
    public static List<ItemStack> craftOnce(ServerLevel level, CraftingRecipe recipe, ResourceHandler<ItemResource> handler) {
        List<Optional<Ingredient>> slots = slots(recipe);
        if (slots == null) {
            return List.of();
        }
        try (Transaction tx = Transaction.openRoot()) {
            List<ItemStack> used = new ArrayList<>();
            for (Optional<Ingredient> slot : slots) {
                if (slot.isEmpty()) {
                    used.add(ItemStack.EMPTY);
                    continue;
                }
                ItemStack got = ItemStack.EMPTY;
                for (int i = 0; i < handler.size() && got.isEmpty(); i++) {
                    ItemResource r = handler.getResource(i);
                    if (!r.isEmpty() && handler.getAmountAsLong(i) > 0 && slot.get().test(r.toStack())) {
                        if (handler.extract(i, r, 1, tx) == 1) {
                            got = r.toStack(1);
                        }
                    }
                }
                if (got.isEmpty()) {
                    return List.of(); // falta ingrediente: a transação é desfeita
                }
                used.add(got);
            }
            CraftingInput input = input(recipe, used);
            if (!recipe.matches(input, level)) {
                return List.of();
            }
            List<ItemStack> out = new ArrayList<>();
            out.add(recipe.assemble(input));
            NonNullList<ItemStack> rest = recipe.getRemainingItems(input);
            for (ItemStack r : rest) {
                if (!r.isEmpty()) {
                    out.add(r);
                }
            }
            tx.commit();
            return out;
        }
    }
}
