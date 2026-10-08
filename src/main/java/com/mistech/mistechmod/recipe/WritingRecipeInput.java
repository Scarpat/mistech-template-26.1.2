package com.mistech.mistechmod.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Os 4 espaços em cruz da Mesa de Escrita. */
public record WritingRecipeInput(ItemStack top, ItemStack left, ItemStack right, ItemStack bottom) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> this.top;
            case 1 -> this.left;
            case 2 -> this.right;
            case 3 -> this.bottom;
            default -> throw new IllegalArgumentException("Índice inválido: " + index);
        };
    }

    @Override
    public int size() {
        return 4;
    }
}
