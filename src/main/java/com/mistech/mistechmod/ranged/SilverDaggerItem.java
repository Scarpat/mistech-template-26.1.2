package com.mistech.mistechmod.ranged;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Adaga de Prata (mitril + Presa de Lobisomem): dano 4, bem rápida. Golpe pelas costas e contra mortos-vivos ×2
 * (em {@link RangedEvents}); conta como prata contra lobisomens (tag {@code mistech:silver_weapons}). Sem energia.
 */
public class SilverDaggerItem extends Item {
    public static final float BACKSTAB_MULTIPLIER = 2.0F;
    public static final float UNDEAD_MULTIPLIER = 2.0F;

    public SilverDaggerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.silver_dagger.desc").withStyle(ChatFormatting.GRAY));
    }
}
