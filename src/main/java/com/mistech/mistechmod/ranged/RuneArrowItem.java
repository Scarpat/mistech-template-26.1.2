package com.mistech.mistechmod.ranged;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Flecha Rúnica: 8 flechas + 1 runa na mesa de trabalho (a runa perde só 1 uso). Uma flecha comum que carrega o elemento
 * da runa com força {@link ArrowMagic#RUNE_ARROW_POWER} — funciona em qualquer arco ou besta comum. Efeitos em {@link RangedEvents}.
 */
public class RuneArrowItem extends ArrowItem {
    public RuneArrowItem(Properties properties) {
        super(properties);
    }

    public static @Nullable RuneElement element(ItemStack stack) {
        return RuneElement.byId(stack.get(RangedContent.ARROW_RUNE.get()));
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack projectile, LivingEntity owner, @Nullable ItemStack weapon) {
        AbstractArrow arrow = super.createArrow(level, projectile, owner, weapon);
        RuneElement element = element(projectile);
        if (element != null) {
            arrow.setData(RangedContent.ARROW_MAGIC, arrow.getData(RangedContent.ARROW_MAGIC).with(element, ArrowMagic.RUNE_ARROW_POWER).sneaking(true));
        }
        return arrow;
    }

    @Override
    public Component getName(ItemStack stack) {
        RuneElement element = element(stack);
        return element == null ? super.getName(stack)
                : Component.translatable("item.mistech.rune_arrow.of", Component.translatable(element.translationKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        RuneElement element = element(stack);
        if (element != null) {
            builder.accept(Component.translatable("arrow_rune.mistech." + element.id()).withColor(element.color()));
        } else {
            builder.accept(Component.translatable("item.mistech.rune_arrow.blank").withStyle(ChatFormatting.GRAY));
        }
    }
}
