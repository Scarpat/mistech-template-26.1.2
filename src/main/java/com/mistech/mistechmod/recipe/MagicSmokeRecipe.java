package com.mistech.mistechmod.recipe;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.MagicSmokableItem;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.registry.MistechRecipes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Charuto/Baseado Mágico na mesa de trabalho (sem forma): 1 runa + 1 folha seca (tabaco = charuto, cânhamo = baseado)
 * + 1 Resíduo Mágico. A runa não é gasta inteira: perde 1 dos seus 16 usos e volta para a grade.
 */
public class MagicSmokeRecipe extends CustomRecipe {
    public static final MagicSmokeRecipe INSTANCE = new MagicSmokeRecipe();
    public static final MapCodec<MagicSmokeRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicSmokeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    /** O que foi encontrado na grade (índices), ou null se não bate. */
    private record Found(int runeSlot, RuneElement element, boolean tobacco) {}

    private static @Nullable Found find(CraftingInput input) {
        if (input.ingredientCount() != 3) {
            return null;
        }
        int runeSlot = -1;
        RuneElement element = null;
        Boolean tobacco = null;
        boolean residue = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            RuneElement rune = RuneElement.ofRune(stack);
            if (rune != null && element == null && hasUseLeft(stack)) {
                element = rune;
                runeSlot = i;
            } else if (stack.is(Mistech.DRIED_TOBACCO_LEAF.get()) && tobacco == null) {
                tobacco = true;
            } else if (stack.is(Mistech.DRIED_HEMP_LEAF.get()) && tobacco == null) {
                tobacco = false;
            } else if (stack.is(Mistech.MAGIC_RESIDUE.get()) && !residue) {
                residue = true;
            } else {
                return null;
            }
        }
        return element != null && tobacco != null && residue ? new Found(runeSlot, element, tobacco) : null;
    }

    public static boolean hasUseLeft(ItemStack rune) {
        return !rune.isDamageableItem() || rune.getDamageValue() < rune.getMaxDamage();
    }

    /** A runa depois de gastar 1 uso (vazio se quebrou). */
    public static ItemStack usedRune(ItemStack rune) {
        ItemStack copy = rune.copyWithCount(1);
        if (com.mistech.mistechmod.purity.TaintEvents.isSealed(copy)) {
            return copy; // runa selada: volta inteira
        }
        if (!copy.isDamageableItem() && !com.mistech.mistechmod.beyond.AmberSeal.isAmber(copy)) {
            return ItemStack.EMPTY;
        }
        int damage = copy.getDamageValue() + 1;
        if (damage >= copy.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        copy.setDamageValue(damage);
        return copy;
    }

    public static Item resultItem(boolean tobacco) {
        return tobacco ? Mistech.MAGIC_CIGAR.get() : Mistech.MAGIC_JOINT.get();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Found found = find(input);
        return found == null ? ItemStack.EMPTY : MagicSmokableItem.create(resultItem(found.tobacco()), found.element(), 1);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        Found found = find(input);
        if (found != null) {
            remaining.set(found.runeSlot(), usedRune(input.getItem(found.runeSlot())));
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<MagicSmokeRecipe> getSerializer() {
        return MistechRecipes.MAGIC_SMOKE_SERIALIZER.get();
    }
}
