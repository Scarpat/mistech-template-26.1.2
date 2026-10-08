package com.mistech.mistechmod.network;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.recipe.WritingRecipe;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Servidor -> cliente: a lista de runas do Confector (na mesma ordem dos botões), com o resultado,
 * um item de exemplo de cada ingrediente e o gasto de giz. As receitas não chegam ao cliente sozinhas.
 */
public record ConfectorRecipesPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConfectorRecipesPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Mistech.MODID, "confector_recipes"));

    public record Entry(ItemStack result, List<ItemStack> ingredients, int chalkDamage) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Entry::result,
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(8)), Entry::ingredients,
                ByteBufCodecs.VAR_INT, Entry::chalkDamage,
                Entry::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfectorRecipesPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(256)), ConfectorRecipesPayload::entries,
            ConfectorRecipesPayload::new);

    public static ConfectorRecipesPayload of(List<RecipeHolder<WritingRecipe>> recipes) {
        List<Entry> entries = new ArrayList<>();
        for (RecipeHolder<WritingRecipe> holder : recipes) {
            WritingRecipe recipe = holder.value();
            List<ItemStack> ingredients = new ArrayList<>();
            for (Ingredient ingredient : recipe.ingredients()) {
                ingredients.add(ingredient.items().findFirst().map(Holder<Item>::value).map(ItemStack::new).orElse(ItemStack.EMPTY));
            }
            entries.add(new Entry(recipe.result().create(), ingredients, recipe.chalkDamage()));
        }
        return new ConfectorRecipesPayload(entries);
    }

    @Override
    public CustomPacketPayload.Type<ConfectorRecipesPayload> type() {
        return TYPE;
    }
}
