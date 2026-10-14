package com.mistech.mistechmod.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.recipe.CrushingRecipe;
import com.mistech.mistechmod.registry.MachineContent;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Triturador Rúnico (e a Mineradora): 1 item → resultado, com chance de bônus e +1 com Runa da Terra. */
public class CrushingCategory extends AbstractRecipeCategory<RecipeHolder<CrushingRecipe>> {
    public static final int WIDTH = 120;
    public static final int HEIGHT = 44;

    public CrushingCategory(IRecipeHolderType<CrushingRecipe> type, IGuiHelper guiHelper) {
        super(type, Component.translatable("block.mistech.runic_crusher"), guiHelper.createDrawableItemLike(MachineContent.RUNIC_CRUSHER_ITEM.get()),
                WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrushingRecipe> holder, IFocusGroup focuses) {
        builder.addInputSlot(20, 4).setStandardSlotBackground().add(holder.value().ingredient());
        builder.addOutputSlot(82, 4).setOutputSlotBackground().add(holder.value().result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<CrushingRecipe> holder, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(60).setPosition(48, 4);
        CrushingRecipe recipe = holder.value();
        List<FormattedText> lines = new ArrayList<>();
        if (recipe.bonusChance() > 0) {
            lines.add(Component.translatable("jei.mistech.crushing.bonus", Math.round(recipe.bonusChance() * 100)));
        }
        if (recipe.runeBonus()) {
            lines.add(Component.translatable("jei.mistech.crushing.rune_bonus"));
        }
        if (!lines.isEmpty()) {
            builder.addText(lines, WIDTH, 20).setPosition(0, 25).setColor(0xFF606060);
        }
    }

    @Override
    public Identifier getIdentifier(RecipeHolder<CrushingRecipe> holder) {
        return holder.id().identifier();
    }
}
