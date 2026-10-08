package com.mistech.mistechmod.recipe;

import com.mistech.mistechmod.registry.MistechRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Mochila: "core" no centro e "strap" em cima, embaixo, esquerda e direita (cantos vazios).
 * O resultado recebe os itens guardados (e o nome) do barril do centro.
 *
 * <pre>
 * { "type": "mistech:backpack",
 *   "core": "mistech:spatial_barrel", "strap": "mistech:hemp_fiber",
 *   "result": { "id": "mistech:carryable_barrel" } }
 * </pre>
 */
public class BackpackRecipe extends CustomRecipe {
    public static final MapCodec<BackpackRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("core").forGetter(r -> r.core),
            Ingredient.CODEC.fieldOf("strap").forGetter(r -> r.strap),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(i, BackpackRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.core,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.strap,
            ItemStackTemplate.STREAM_CODEC, r -> r.result,
            BackpackRecipe::new);

    private final Ingredient core;
    private final Ingredient strap;
    private final ItemStackTemplate result;

    public BackpackRecipe(Ingredient core, Ingredient strap, ItemStackTemplate result) {
        this.core = core;
        this.strap = strap;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 5) {
            return false;
        }
        return this.core.test(input.getItem(1, 1))
                && this.strap.test(input.getItem(1, 0))
                && this.strap.test(input.getItem(0, 1))
                && this.strap.test(input.getItem(2, 1))
                && this.strap.test(input.getItem(1, 2));
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack barrel = input.getItem(1, 1);
        DataComponentPatch.Builder patch = DataComponentPatch.builder();
        ItemContainerContents contents = barrel.get(DataComponents.CONTAINER);
        if (contents != null) {
            patch.set(DataComponents.CONTAINER, contents);
        }
        if (barrel.has(DataComponents.CUSTOM_NAME)) {
            patch.set(DataComponents.CUSTOM_NAME, barrel.get(DataComponents.CUSTOM_NAME));
        }
        return this.result.apply(patch.build());
    }

    @Override
    public RecipeSerializer<BackpackRecipe> getSerializer() {
        return MistechRecipes.BACKPACK_SERIALIZER.get();
    }
}
