package com.mistech.mistechmod.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.registry.RitualContent;
import com.mistech.mistechmod.ritual.RitualCondition;
import com.mistech.mistechmod.ritual.RitualRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

/**
 * Rituais: o catalisador no meio (no altar), os ingredientes em roda (nos pedestais) e o resultado à direita.
 * Rituais sem item (invocações, mudar o tempo...) mostram o nome do ritual no lugar. Uma categoria para o Altar Impuro e
 * outra para a Fonte do Santuário.
 */
public class RitualCategory extends AbstractRecipeCategory<RecipeHolder<RitualRecipe>> {
    public static final int WIDTH = 160;
    public static final int HEIGHT = 96;
    private static final int CENTER_X = 38, CENTER_Y = 38, RADIUS = 30;

    private final boolean sanctuary;

    public RitualCategory(IRecipeType<RecipeHolder<RitualRecipe>> type, IGuiHelper guiHelper, boolean sanctuary, ItemLike icon, String title) {
        super(type, Component.translatable(title), guiHelper.createDrawableItemLike(icon), WIDTH, HEIGHT);
        this.sanctuary = sanctuary;
    }

    public boolean isSanctuary() {
        return this.sanctuary;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RitualRecipe> holder, IFocusGroup focuses) {
        RitualRecipe recipe = holder.value();
        builder.addInputSlot(CENTER_X, CENTER_Y).setStandardSlotBackground().add(recipe.catalyst());
        int n = recipe.ingredients().size();
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / n;
            int x = CENTER_X + (int) Math.round(Math.cos(angle) * RADIUS);
            int y = CENTER_Y + (int) Math.round(Math.sin(angle) * RADIUS);
            builder.addInputSlot(x, y).setStandardSlotBackground().add(recipe.ingredients().get(i));
        }
        recipe.result().ifPresent(result -> builder.addOutputSlot(130, CENTER_Y).setOutputSlotBackground().add(result));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<RitualRecipe> holder, IFocusGroup focuses) {
        RitualRecipe recipe = holder.value();
        builder.addRecipeArrowWidget().setPosition(100, CENTER_Y);

        if (recipe.result().isEmpty()) {
            // ritual sem item: o nome do ritual no lugar da saída
            builder.addText(ritualName(holder), 44, 30).setPosition(116, CENTER_Y - 10).setColor(0xFF5B2A86);
        }

        List<FormattedText> info = new ArrayList<>();
        if (recipe.essence() > 0) {
            info.add(Component.translatable(this.sanctuary ? "guide.mistech.ui.ritual_pure_essence" : "guide.mistech.ui.ritual_essence",
                    recipe.essence()));
        }
        if (recipe.condition() != RitualCondition.NONE) {
            info.add(Component.translatable("ritual.mistech.condition." + recipe.condition().getSerializedName()));
        }
        if (!this.sanctuary && recipe.minTier() > 1) {
            info.add(Component.translatable("guide.mistech.ui.ritual_tier", recipe.minTier()).withStyle(ChatFormatting.DARK_RED));
        }
        if (!info.isEmpty()) {
            builder.addText(info, 84, 30).setPosition(76, CENTER_Y + 22).setColor(0xFF505050);
        }
    }

    /** Nome do ritual: o título do Grimório se houver, senão a ação. */
    static Component ritualName(RecipeHolder<RitualRecipe> holder) {
        String path = holder.id().identifier().getPath();
        String base = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        String key = "guide.mistech.ritual." + base;
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        String action = holder.value().action();
        String actionKey = "guide.mistech.ritual." + action;
        if (!action.isEmpty() && I18n.exists(actionKey)) {
            return Component.translatable(actionKey);
        }
        return Component.literal(action.isEmpty() ? base : action);
    }

    @Override
    public Identifier getIdentifier(RecipeHolder<RitualRecipe> holder) {
        return holder.id().identifier();
    }

    static ItemLike altarIcon() {
        return RitualContent.IMPURE_ALTAR_ITEM.get();
    }
}
