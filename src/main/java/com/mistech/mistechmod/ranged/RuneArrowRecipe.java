package com.mistech.mistechmod.ranged;

import java.util.List;

import com.mistech.mistechmod.item.RuneElement;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Flechas Rúnicas: 8 flechas comuns + 1 runa (qualquer posição) → 8 Flechas Rúnicas do elemento.
 * A runa volta para a mesa com 1 uso a menos (a Selada volta inteira).
 * <pre>{ "type": "mistech:rune_arrow" }</pre>
 */
public class RuneArrowRecipe extends CustomRecipe {
    public static final RuneArrowRecipe INSTANCE = new RuneArrowRecipe();
    public static final MapCodec<RuneArrowRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, RuneArrowRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final int ARROWS = 8;

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int arrows = 0;
        int runes = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.ARROW)) {
                arrows++;
            } else if (RuneElement.ofRune(stack) != null) {
                runes++;
            } else {
                return false;
            }
        }
        return arrows == ARROWS && runes == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            RuneElement element = RuneElement.ofRune(input.getItem(i));
            if (element != null) {
                return of(element, ARROWS);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Pilha de Flechas Rúnicas do elemento (com a cor da ponta). */
    public static ItemStack of(RuneElement element, int count) {
        ItemStack out = new ItemStack(RangedContent.RUNE_ARROW.get(), count);
        out.set(RangedContent.ARROW_RUNE.get(), element.id());
        out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(element.color())));
        return out;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (RuneElement.ofRune(stack) != null) {
                ItemStack rune = stack.copyWithCount(1);
                if (rune.isDamageableItem()) {
                    rune.setDamageValue(rune.getDamageValue() + 1);
                    if (rune.getDamageValue() >= rune.getMaxDamage()) {
                        rune = ItemStack.EMPTY;
                    }
                }
                out.set(i, rune);
            }
        }
        return out;
    }

    @Override
    public RecipeSerializer<RuneArrowRecipe> getSerializer() {
        return RangedContent.RUNE_ARROW_SERIALIZER.get();
    }
}
