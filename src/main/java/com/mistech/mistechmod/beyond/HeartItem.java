package com.mistech.mistechmod.beyond;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Os corações do eixo central (Sculk, Rangente, Infernal, Primordial): material raro com uma linha de descrição e brilho. */
public class HeartItem extends Item {
    public HeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.is(BeyondContent.PRIMORDIAL_HEART.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        if (stack.is(BeyondContent.PRIMORDIAL_HEART.get())) {
            builder.accept(Component.translatable("item.mistech.primordial_heart.soon").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
