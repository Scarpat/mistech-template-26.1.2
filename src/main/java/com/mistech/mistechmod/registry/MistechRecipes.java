package com.mistech.mistechmod.registry;

import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.recipe.WritingRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MistechRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Mistech.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Mistech.MODID);

    public static final Supplier<RecipeType<WritingRecipe>> WRITING_TYPE = RECIPE_TYPES.register("writing",
            () -> RecipeType.simple(Identifier.fromNamespaceAndPath(Mistech.MODID, "writing")));
    public static final Supplier<RecipeSerializer<WritingRecipe>> WRITING_SERIALIZER = RECIPE_SERIALIZERS.register("writing",
            () -> new RecipeSerializer<>(WritingRecipe.CODEC, WritingRecipe.STREAM_CODEC));

    public static final Supplier<RecipeSerializer<com.mistech.mistechmod.recipe.BackpackRecipe>> BACKPACK_SERIALIZER = RECIPE_SERIALIZERS.register("backpack",
            () -> new RecipeSerializer<>(com.mistech.mistechmod.recipe.BackpackRecipe.CODEC, com.mistech.mistechmod.recipe.BackpackRecipe.STREAM_CODEC));

    public static final Supplier<RecipeSerializer<com.mistech.mistechmod.recipe.MagicSmokeRecipe>> MAGIC_SMOKE_SERIALIZER = RECIPE_SERIALIZERS.register("magic_smoke",
            () -> new RecipeSerializer<>(com.mistech.mistechmod.recipe.MagicSmokeRecipe.CODEC, com.mistech.mistechmod.recipe.MagicSmokeRecipe.STREAM_CODEC));

    public static final Supplier<RecipeType<com.mistech.mistechmod.recipe.CrushingRecipe>> CRUSHING_TYPE = RECIPE_TYPES.register("crushing",
            () -> RecipeType.simple(Identifier.fromNamespaceAndPath(Mistech.MODID, "crushing")));
    public static final Supplier<RecipeSerializer<com.mistech.mistechmod.recipe.CrushingRecipe>> CRUSHING_SERIALIZER = RECIPE_SERIALIZERS.register("crushing",
            () -> new RecipeSerializer<>(com.mistech.mistechmod.recipe.CrushingRecipe.CODEC, com.mistech.mistechmod.recipe.CrushingRecipe.STREAM_CODEC));

    private MistechRecipes() {}

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
