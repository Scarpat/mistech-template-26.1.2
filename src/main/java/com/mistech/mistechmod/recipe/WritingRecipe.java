package com.mistech.mistechmod.recipe;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.registry.MistechRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Receita da Mesa de Escrita (tipo "mistech:writing").
 * De 1 a 4 ingredientes, em qualquer posição da cruz (sem forma).
 * Toda receita gasta 1 papel; o giz perde {@code chalk_damage} de durabilidade.
 *
 * Exemplo de JSON:
 * <pre>
 * { "type": "mistech:writing",
 *   "ingredients": [ "minecraft:gunpowder", "#minecraft:coals", "minecraft:flint" ],
 *   "chalk_damage": 2,
 *   "result": { "id": "mistech:fire_rune" } }
 * </pre>
 */
public class WritingRecipe implements Recipe<WritingRecipeInput> {
    public static final int PAPER_COST = 1;
    public static final int MAX_INGREDIENTS = 4;

    public static final MapCodec<WritingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(WritingRecipe::commonInfo),
            Ingredient.CODEC.listOf(1, MAX_INGREDIENTS).fieldOf("ingredients").forGetter(WritingRecipe::ingredients),
            Codec.intRange(0, 1000).optionalFieldOf("chalk_damage", 1).forGetter(WritingRecipe::chalkDamage),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(WritingRecipe::result)
    ).apply(i, WritingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WritingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, WritingRecipe::commonInfo,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INGREDIENTS)), WritingRecipe::ingredients,
            ByteBufCodecs.VAR_INT, WritingRecipe::chalkDamage,
            ItemStackTemplate.STREAM_CODEC, WritingRecipe::result,
            WritingRecipe::new);

    private final Recipe.CommonInfo commonInfo;
    private final List<Ingredient> ingredients;
    private final int chalkDamage;
    private final ItemStackTemplate result;

    public WritingRecipe(Recipe.CommonInfo commonInfo, List<Ingredient> ingredients, int chalkDamage, ItemStackTemplate result) {
        this.commonInfo = commonInfo;
        this.ingredients = List.copyOf(ingredients);
        this.chalkDamage = chalkDamage;
        this.result = result;
    }

    public Recipe.CommonInfo commonInfo() {
        return this.commonInfo;
    }

    public List<Ingredient> ingredients() {
        return this.ingredients;
    }

    public int chalkDamage() {
        return this.chalkDamage;
    }

    public ItemStackTemplate result() {
        return this.result;
    }

    @Override
    public boolean matches(WritingRecipeInput input, Level level) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                stacks.add(input.getItem(i));
            }
        }
        if (stacks.size() != this.ingredients.size()) {
            return false;
        }
        return assign(stacks, 0, new boolean[this.ingredients.size()]);
    }

    /** Tenta casar cada item com um ingrediente diferente (backtracking; no máximo 4 itens). */
    private boolean assign(List<ItemStack> stacks, int index, boolean[] used) {
        if (index == stacks.size()) {
            return true;
        }
        for (int i = 0; i < this.ingredients.size(); i++) {
            if (!used[i] && this.ingredients.get(i).test(stacks.get(index))) {
                used[i] = true;
                if (assign(stacks, index + 1, used)) {
                    return true;
                }
                used[i] = false;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(WritingRecipeInput input) {
        return this.result.create();
    }

    @Override
    public boolean isSpecial() {
        return true; // não aparece no livro de receitas vanilla
    }

    @Override
    public boolean showNotification() {
        return this.commonInfo.showNotification();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<WritingRecipe> getSerializer() {
        return MistechRecipes.WRITING_SERIALIZER.get();
    }

    @Override
    public RecipeType<WritingRecipe> getType() {
        return MistechRecipes.WRITING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
