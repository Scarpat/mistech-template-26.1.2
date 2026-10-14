package com.mistech.mistechmod.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.compat.jei.MachineRecipe.Entry;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.level.ItemLike;

/**
 * Categoria genérica para as máquinas do Mistech: [estação] entradas → saídas, e notas embaixo.
 * Cada máquina é uma instância com seu próprio tipo, título e ícone.
 */
public class MachineCategory extends AbstractRecipeCategory<MachineRecipe> {
    public static final int WIDTH = 160;
    public static final int HEIGHT = 64;
    private static final int TOP = 2;
    private static final long FLUID_CAPACITY = 1000;

    public MachineCategory(IRecipeType<MachineRecipe> type, IGuiHelper guiHelper, String titleKey, ItemLike icon) {
        super(type, Component.translatable(titleKey), guiHelper.createDrawableItemLike(icon), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        int x = 0;
        if (recipe.station != null) {
            fill(builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, x + 1, TOP + 1), recipe.station);
            x += 24;
        }
        for (Entry input : recipe.inputs) {
            fill(builder.addInputSlot(x + 1, TOP + 1).setStandardSlotBackground(), input);
            x += 18;
        }
        x += 26; // seta
        for (Entry output : recipe.outputs) {
            fill(builder.addOutputSlot(x + 1, TOP + 1).setOutputSlotBackground(), output);
            x += 18;
        }
    }

    private static void fill(IRecipeSlotBuilder slot, Entry entry) {
        if (entry.fluid() != null) {
            slot.setFluidRenderer(Math.max(FLUID_CAPACITY, entry.amount()), false, 16, 16).add(entry.fluid(), entry.amount());
        } else {
            slot.addItemStacks(entry.items());
        }
    }

    @Override
    public net.minecraft.resources.@org.jspecify.annotations.Nullable Identifier getIdentifier(MachineRecipe recipe) {
        return recipe.id;
    }

    @Override
    public List<net.minecraft.resources.Identifier> getSourceRecipeIds(MachineRecipe recipe) {
        return List.of(); // não vem de uma receita de verdade
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        int x = (recipe.station != null ? 24 : 0) + recipe.inputs.size() * 18;
        builder.addRecipeArrowWidget().setPosition(x + 2, TOP + 1);
        if (recipe.label != null) {
            int labelX = x + 26 + recipe.outputs.size() * 18;
            builder.addText(recipe.label, WIDTH - labelX, 18).setPosition(labelX, TOP + 5).setColor(0xFF5B2A86);
        }
        if (!recipe.notes.isEmpty()) {
            List<FormattedText> lines = new ArrayList<>(recipe.notes);
            builder.addText(lines, WIDTH, HEIGHT - 24).setPosition(0, 24).setColor(0xFF505050);
        }
    }
}
