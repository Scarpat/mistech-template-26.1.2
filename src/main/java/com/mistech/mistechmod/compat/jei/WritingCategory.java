package com.mistech.mistechmod.compat.jei;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.recipe.WritingRecipe;
import com.mistech.mistechmod.registry.MistechTags;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Mesa de Escrita: giz e papel à esquerda, os ingredientes em cruz no meio, a saída à direita (como na tela da mesa). */
public class WritingCategory extends AbstractRecipeCategory<RecipeHolder<WritingRecipe>> {
    public static final int WIDTH = 132;
    public static final int HEIGHT = 64;
    private static final int CROSS_X = 47, CROSS_Y = 19;

    public WritingCategory(IRecipeHolderType<WritingRecipe> type, IGuiHelper guiHelper) {
        super(type, Component.translatable("block.mistech.writing_table"), guiHelper.createDrawableItemLike(Mistech.WRITING_TABLE_ITEM.get()),
                WIDTH, HEIGHT);
    }

    static List<ItemStack> chalks() {
        return BuiltInRegistries.ITEM.getTagOrEmpty(MistechTags.CHALKS).iterator().hasNext()
                ? java.util.stream.StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(MistechTags.CHALKS).spliterator(), false)
                        .map(holder -> new ItemStack(holder)).toList()
                : List.of(new ItemStack(Mistech.ASH_CHALK.get()));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<WritingRecipe> holder, IFocusGroup focuses) {
        WritingRecipe recipe = holder.value();
        if (recipe.chalkDamage() > 0) {
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 1, 10).setStandardSlotBackground().addItemStacks(chalks());
        }
        builder.addInputSlot(1, 36).setStandardSlotBackground().add(new ItemStack(Items.PAPER, WritingRecipe.PAPER_COST));

        // cruz: cima, esquerda, direita, baixo
        int[][] cross = {{CROSS_X, CROSS_Y - 18}, {CROSS_X - 18, CROSS_Y}, {CROSS_X + 18, CROSS_Y}, {CROSS_X, CROSS_Y + 18}};
        for (int i = 0; i < 4; i++) {
            var slot = builder.addInputSlot(cross[i][0], cross[i][1]).setStandardSlotBackground();
            if (i < recipe.ingredients().size()) {
                slot.add(recipe.ingredients().get(i));
            }
        }
        builder.addOutputSlot(111, CROSS_Y).setOutputSlotBackground().add(recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<WritingRecipe> holder, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(85, CROSS_Y);
        if (holder.value().chalkDamage() > 0) {
            builder.addText(Component.translatable("guide.mistech.ui.chalk", holder.value().chalkDamage()), 60, 10)
                    .setPosition(85, CROSS_Y + 22).setColor(0xFF606060);
        }
    }

    @Override
    public Identifier getIdentifier(RecipeHolder<WritingRecipe> holder) {
        return holder.id().identifier();
    }
}
