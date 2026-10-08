package com.mistech.mistechmod.tarot;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Carta do Arcano: prêmio por vencer o arcano. Na aba Forja da Mesa de Tarologia vira um dos equipamentos dele. */
public class TarotCardItem extends Item {
    private final TarotArcana arcana;

    public TarotCardItem(Item.Properties properties, TarotArcana arcana) {
        super(properties);
        this.arcana = arcana;
    }

    public TarotArcana arcana() {
        return this.arcana;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.literal(this.arcana.numeral()).withStyle(ChatFormatting.GOLD));
        builder.accept(this.arcana.description().copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        builder.accept(Component.translatable("item.mistech.tarot_card.hint").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
