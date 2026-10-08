package com.mistech.mistechmod.recipe;

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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Receita do Triturador Rúnico (tipo "mistech:crushing"): 1 item de entrada → resultado.
 * {@code bonus_chance}: chance de sair 1 item a mais. {@code rune_bonus}: com Runa de Terra no triturador sai +1
 * (usado nos minérios).
 *
 * <pre>
 * { "type": "mistech:crushing",
 *   "ingredient": "#c:ores/iron",
 *   "result": { "id": "mistech:iron_dust", "count": 2 },
 *   "bonus_chance": 0.25,
 *   "rune_bonus": true }
 * </pre>
 */
public record CrushingRecipe(Recipe.CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result,
        float bonusChance, boolean runeBonus) implements Recipe<SingleRecipeInput> {

    public static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(CrushingRecipe::commonInfo),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(CrushingRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("bonus_chance", 0.0F).forGetter(CrushingRecipe::bonusChance),
            Codec.BOOL.optionalFieldOf("rune_bonus", false).forGetter(CrushingRecipe::runeBonus)
    ).apply(i, CrushingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, CrushingRecipe::commonInfo,
            Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, CrushingRecipe::result,
            ByteBufCodecs.FLOAT, CrushingRecipe::bonusChance,
            ByteBufCodecs.BOOL, CrushingRecipe::runeBonus,
            CrushingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.create();
    }

    @Override
    public boolean isSpecial() {
        return true;
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
    public RecipeSerializer<CrushingRecipe> getSerializer() {
        return MistechRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<CrushingRecipe> getType() {
        return MistechRecipes.CRUSHING_TYPE.get();
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
