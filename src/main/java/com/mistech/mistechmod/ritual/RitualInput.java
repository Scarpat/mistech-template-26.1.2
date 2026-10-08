package com.mistech.mistechmod.ritual;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Catalisador (no altar) + os pedestais. Índice 0 = catalisador, 1.. = pedestais.
 * {@code sanctuary} = vem da Fonte do Santuário (só casa com receitas "sanctuary": true).
 */
public record RitualInput(ItemStack catalyst, List<ItemStack> pedestals, boolean sanctuary) implements RecipeInput {
    public RitualInput(ItemStack catalyst, List<ItemStack> pedestals) {
        this(catalyst, pedestals, false);
    }

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? this.catalyst : this.pedestals.get(index - 1);
    }

    @Override
    public int size() {
        return 1 + this.pedestals.size();
    }
}
