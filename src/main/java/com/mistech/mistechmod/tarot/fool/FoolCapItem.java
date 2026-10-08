package com.mistech.mistechmod.tarot.fool;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Chapéu do Bobo (Forja do Louco). Capacete com defesa de diamante, metade do dano de queda, +2 blocos de queda segura
 * e +8% de velocidade. <b>Salto do Louco</b>: agachado no chão por um instante e pular → grande salto em arco na direção
 * do olhar, sem dano de queda no pouso (recarga {@link FoolEvents#LEAP_COOLDOWN}). Os sininhos tocam.
 */
public class FoolCapItem extends Item {
    public FoolCapItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.fool_cap.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        builder.accept(Component.translatable("item.mistech.fool_cap.leap").withStyle(ChatFormatting.AQUA));
    }
}
